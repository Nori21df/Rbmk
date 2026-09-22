package com.phat.rbmk.sim;

import com.phat.rbmk.multiblock.ChannelType;
import com.phat.rbmk.multiblock.HexStructure;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Mô phỏng neutron + nhiệt cho lò RBMK trên lưới lục giác. Mỗi phần tử là một CỘT kênh.
 *
 * Đơn vị: flux chuẩn hoá (1.0 = công suất danh định), nhiệt độ °C, thời gian giây.
 * Hằng số đã được chỉnh bằng mô phỏng Python để có các hành vi:
 *  - khởi động êm với bộ điều chỉnh tự động, ổn định ở 100% với rod cắm ~27%
 *  - mất nước làm mát -> nóng dần -> vỡ kênh sau ~150 s
 *  - hố xenon ở công suất thấp, rod bị rút gần hết
 *  - AZ-5 khi rod rút hết -> hiệu ứng đầu graphite -> prompt critical -> nổ
 */
public class ReactorSim {
    // ---- Hằng số vật lý (gameplay) ----
    static final double K_FUEL = 1.23;          // hệ số nhân cơ bản của kênh nhiên liệu
    static final double MOD = 0.12;             // thưởng điều tiết theo số ô graphite kề
    static final double K_GRAPHITE = 0.97;
    static final double K_WATER = 0.88;
    static final double K_ROD_OUT = 0.88;       // kênh rod khi rút ra = đầy nước
    static final double ROD_NEIGH = 3.0;        // rod hấp thụ neutron của kênh nhiên liệu kề
    static final double TIP_BONUS = 1.0;        // hiệu ứng đầu graphite (AZ-5)
    static final double TIP_LEN = 0.35;         // phần hành trình rod có đầu graphite chiếm chỗ nước
    static final double WATER_ABS = 0.05;       // nước lỏng hấp thụ neutron -> void coefficient dương
    static final double VOID_SPAN = 60.0;
    static final double DOPPLER = 0.00012;      // phản hồi âm theo nhiệt độ nhiên liệu
    static final double XE_ABS = 0.04;
    static final double BURNUP_PENALTY = 0.5;
    static final double GEN = 1.0;              // thời gian thế hệ hiệu dụng (neutron trễ)
    static final double GEN_PROMPT = 0.05;      // khi vượt prompt critical
    static final double BETA = 0.06;
    static final double SOURCE = 1.0e-3;        // nguồn neutron tự phát
    static final double W_SELF = 2.0;
    static final double T_SAT = 280.0;
    static final double T_AMBIENT = 20.0;
    static final double H_WATER = 10.0;         // HU/s/°C trên mỗi khối nhiên liệu khi đủ nước
    static final double H_PASSIVE = 0.2;
    static final double HEAT_CAP = 400.0;       // HU/°C trên mỗi khối
    public static final double Q_PER_FLUX = 1000.0; // HU/s mỗi khối nhiên liệu ở flux 1.0
    static final double MAX_FLUX = 1.0e4;

    // ---- Tốc độ thanh điều khiển (phần hành trình / giây) ----
    static final double ROD_WITHDRAW_SPEED = 0.05;
    static final double ROD_INSERT_SPEED = 0.12;
    static final double SCRAM_SPEED = 0.08;     // AZ-5 chậm như RBMK thật

    // ---- Cấu trúc ----
    private final int n;
    private final int height;
    private final byte[] types;
    private final int[][] neighbors;
    private final int[] graphiteNeighbors;
    private final int[] rodNeighbors;

    // ---- Nhiên liệu (controller cập nhật mỗi bước) ----
    private final int[] fuelBlocks;
    private final double[] burnup;

    // ---- Trạng thái ----
    private final double[] phi;
    private final double[] temp;
    private final double[] iodine;
    private final double[] xenon;
    private final double[] voidFrac;
    private double rod = 1.0;
    private double rodTarget = 1.0;
    private boolean inserting;
    private boolean scram;
    private double lastLogPower = Double.NaN;

    // ---- Kết quả bước gần nhất ----
    private double avgPower;
    private double maxTemp = T_AMBIENT;
    private double avgVoid;
    private double maxXenon;
    private double waterFraction = 1.0;
    private int hottestCell = -1;

    public ReactorSim(HexStructure s) {
        n = s.cells().size();
        height = s.height();
        types = new byte[n];
        neighbors = s.neighbors();
        graphiteNeighbors = new int[n];
        rodNeighbors = new int[n];
        for (int i = 0; i < n; i++) {
            types[i] = s.cells().get(i).type().id();
        }
        for (int i = 0; i < n; i++) {
            for (int j : neighbors[i]) {
                if (j < 0) continue;
                if (types[j] == ChannelType.GRAPHITE.id()) graphiteNeighbors[i]++;
                if (types[j] == ChannelType.ROD.id()) rodNeighbors[i]++;
            }
        }
        fuelBlocks = new int[n];
        burnup = new double[n];
        phi = new double[n];
        temp = new double[n];
        iodine = new double[n];
        xenon = new double[n];
        voidFrac = new double[n];
        java.util.Arrays.fill(temp, T_AMBIENT);
    }

    public int size() { return n; }

    public void setFuel(int i, int blocks, double avgBurnup) {
        fuelBlocks[i] = blocks;
        burnup[i] = avgBurnup;
    }

    public record StepResult(double waterUsedMb, double steamMb, double heatHU) {}

    /**
     * Một bước mô phỏng.
     *
     * @param setpoint       công suất mong muốn (0..1.5), bộ điều chỉnh tự động bám theo
     * @param waterAvailMb   nước có sẵn trong các Coolant Port
     * @param backpressure   phần hơi không thoát được ở bước trước (0..1), làm giảm làm mát
     * @param heatPerMb      nhiệt cần cho 1 mB hơi
     */
    public StepResult step(double dt, double setpoint, double waterAvailMb, double backpressure, double heatPerMb) {
        regulate(dt, setpoint);
        moveRods(dt);

        // Hệ số nhân từng ô
        double[] m = new double[n];
        double voidSum = 0;
        int fuelCols = 0;
        for (int i = 0; i < n; i++) {
            double a = 1.0 - Math.exp(-Math.max(0.0, temp[i] - T_SAT) / VOID_SPAN);
            voidFrac[i] = a;
            ChannelType t = ChannelType.byId(types[i]);
            switch (t) {
                case FUEL -> {
                    double frac = fuelBlocks[i] / (double) height;
                    double k = K_FUEL
                            * (1.0 + MOD * graphiteNeighbors[i] / 6.0)
                            * (1.0 - WATER_ABS * (1.0 - a))
                            * (1.0 - DOPPLER * (temp[i] - T_AMBIENT))
                            * (1.0 - XE_ABS * xenon[i])
                            * (1.0 - BURNUP_PENALTY * burnup[i])
                            * Math.max(0.0, 1.0 - ROD_NEIGH * rod * rodNeighbors[i] / 6.0);
                    m[i] = frac * k + (1.0 - frac) * K_WATER;
                    if (fuelBlocks[i] > 0) {
                        voidSum += a;
                        fuelCols++;
                    }
                }
                case GRAPHITE -> m[i] = K_GRAPHITE;
                case WATER -> m[i] = K_WATER;
                case ROD -> m[i] = K_ROD_OUT * (1.0 - rod);
            }
        }
        avgVoid = fuelCols > 0 ? voidSum / fuelCols : 0.0;

        // Hiệu ứng đầu graphite: rod đang cắm vào từ vị trí rút gần hết -> graphite đẩy nước ra khỏi đáy kênh
        double tip = 1.0;
        if (inserting && rod < TIP_LEN) {
            tip = 1.0 + TIP_BONUS * (1.0 - rod / TIP_LEN) * (1.0 - avgVoid);
        }

        // Khuếch tán
        double[] src = new double[n];
        double srcSum = 0;
        double phiSum = 0;
        for (int i = 0; i < n; i++) {
            double s = W_SELF * phi[i] * m[i];
            for (int j : neighbors[i]) {
                if (j >= 0) s += phi[j] * m[j];
            }
            src[i] = tip * s / (W_SELF + 6.0);
            srcSum += src[i];
            phiSum += phi[i];
        }
        double globalExcess = phiSum > 1e-9 ? srcSum / phiSum - 1.0 : 0.0;
        double gen = globalExcess <= BETA ? GEN : GEN_PROMPT;
        double relax = Math.exp(-dt / gen);
        for (int i = 0; i < n; i++) {
            double next = src[i] + (phi[i] - src[i]) * relax;
            if (types[i] == ChannelType.FUEL.id() && fuelBlocks[i] > 0) {
                next += SOURCE * dt;
            }
            phi[i] = Math.max(0.0, Math.min(MAX_FLUX, next));
        }

        // Nhiệt + nước
        double demandMb = 0;
        for (int i = 0; i < n; i++) {
            if (types[i] == ChannelType.FUEL.id() && fuelBlocks[i] > 0) {
                demandMb += H_WATER * fuelBlocks[i] * Math.max(0.0, temp[i] - T_SAT) * dt / heatPerMb;
            }
        }
        double f = demandMb > 1e-9 ? Math.min(1.0, waterAvailMb / demandMb) : 1.0;
        f *= Math.max(0.0, 1.0 - backpressure);
        waterFraction = f;

        double heat = 0;
        double steamMb = 0;
        double totalQ = 0;
        int totalBlocks = 0;
        maxTemp = T_AMBIENT;
        hottestCell = -1;
        maxXenon = 0;
        for (int i = 0; i < n; i++) {
            boolean fuel = types[i] == ChannelType.FUEL.id() && fuelBlocks[i] > 0;
            int blocks = Math.max(1, fuelBlocks[i]);
            double q = fuel ? phi[i] * Q_PER_FLUX * fuelBlocks[i] : 0.0;
            double removedWater = fuel ? f * H_WATER * fuelBlocks[i] * Math.max(0.0, temp[i] - T_SAT) : 0.0;
            double removed = removedWater + H_PASSIVE * blocks * (temp[i] - T_AMBIENT);
            temp[i] += dt * (q - removed) / (HEAT_CAP * blocks);
            if (fuel) {
                totalQ += q;
                totalBlocks += fuelBlocks[i];
                heat += q * dt;
                steamMb += removedWater * dt / heatPerMb;
            }
            if (temp[i] > maxTemp) {
                maxTemp = temp[i];
                hottestCell = i;
            }

            // Iod -> Xenon (tích phân ẩn để ổn định)
            double flux = fuel ? phi[i] : 0.0;
            iodine[i] = (iodine[i] + dt * flux / 60.0) / (1.0 + dt / 120.0);
            xenon[i] = (xenon[i] + dt * iodine[i] / 120.0) / (1.0 + dt * (1.0 / 180.0 + flux / 60.0));
            maxXenon = Math.max(maxXenon, xenon[i]);
        }
        avgPower = totalBlocks > 0 ? totalQ / (Q_PER_FLUX * totalBlocks) : 0.0;
        return new StepResult(steamMb, steamMb, heat);
    }

    /** Bộ điều chỉnh tự động: bám công suất mong muốn với chu kỳ ~30 s. */
    private void regulate(double dt, double setpoint) {
        double logP = Math.log(Math.max(avgPower, 1e-6));
        double rate = Double.isNaN(lastLogPower) ? 0.0 : (logP - lastLogPower) / dt;
        lastLogPower = logP;
        if (scram) {
            rodTarget = 1.0;
            return;
        }
        if (setpoint <= 1e-3) {
            rodTarget = 1.0;
            return;
        }
        double want = Math.max(-0.05, Math.min(0.03, Math.log(setpoint / Math.max(avgPower, 1e-6)) / 30.0));
        rodTarget = Math.max(0.0, Math.min(1.0, rod + (rate - want) * 2.0));
    }

    private void moveRods(double dt) {
        if (rod < rodTarget) {
            inserting = true;
            double speed = scram ? SCRAM_SPEED : ROD_INSERT_SPEED;
            rod = Math.min(rodTarget, rod + speed * dt);
        } else if (rod > rodTarget) {
            inserting = false;
            rod = Math.max(rodTarget, rod - ROD_WITHDRAW_SPEED * dt);
        } else {
            inserting = false;
        }
    }

    // ---------- Điều khiển ----------

    public void setScram(boolean scram) { this.scram = scram; }

    public boolean isScram() { return scram; }

    /** Tắt nguội ngay lập tức (dùng khi chunk unload). */
    public void coldShutdown() {
        rod = 1.0;
        rodTarget = 1.0;
        java.util.Arrays.fill(phi, 0.0);
        avgPower = 0.0;
        lastLogPower = Double.NaN;
    }

    // ---------- Getter ----------

    public double avgPower() { return avgPower; }
    public double maxTemp() { return maxTemp; }
    public double avgVoid() { return avgVoid; }
    public double maxXenon() { return maxXenon; }
    public double rodInsertion() { return rod; }
    public double waterFraction() { return waterFraction; }
    public int hottestCell() { return hottestCell; }
    public double flux(int i) { return phi[i]; }
    public double temp(int i) { return temp[i]; }

    // ---------- NBT ----------

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("n", n);
        tag.put("phi", toList(phi));
        tag.put("temp", toList(temp));
        tag.put("iodine", toList(iodine));
        tag.put("xenon", toList(xenon));
        tag.putDouble("rod", rod);
        tag.putBoolean("scram", scram);
        return tag;
    }

    /** Nạp trạng thái cũ nếu kích thước lõi không đổi. */
    public void load(CompoundTag tag) {
        if (tag.getInt("n") != n) {
            return;
        }
        fromList(tag.getList("phi", Tag.TAG_DOUBLE), phi);
        fromList(tag.getList("temp", Tag.TAG_DOUBLE), temp);
        fromList(tag.getList("iodine", Tag.TAG_DOUBLE), iodine);
        fromList(tag.getList("xenon", Tag.TAG_DOUBLE), xenon);
        rod = tag.getDouble("rod");
        rodTarget = rod;
        scram = tag.getBoolean("scram");
    }

    private static ListTag toList(double[] arr) {
        ListTag list = new ListTag();
        for (double v : arr) list.add(DoubleTag.valueOf(v));
        return list;
    }

    private static void fromList(ListTag list, double[] arr) {
        for (int i = 0; i < Math.min(list.size(), arr.length); i++) {
            arr[i] = list.getDouble(i);
        }
    }
}

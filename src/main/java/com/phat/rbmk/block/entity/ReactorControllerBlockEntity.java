package com.phat.rbmk.block.entity;

import com.phat.rbmk.RbmkMod;
import com.phat.rbmk.config.RbmkServerConfig;
import com.phat.rbmk.hazard.ReactorHazards;
import com.phat.rbmk.multiblock.ChannelType;
import com.phat.rbmk.multiblock.HexStructure;
import com.phat.rbmk.multiblock.HexValidator;
import com.phat.rbmk.registry.ModBlockEntities;
import com.phat.rbmk.sim.ReactorSim;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.phat.rbmk.menu.ReactorControllerMenu;
import com.phat.rbmk.network.ReactorStatusPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.PacketDistributor;

public class ReactorControllerBlockEntity extends BlockEntity implements MenuProvider {
    private static final int VALIDATE_INTERVAL = 40;
    private static final int ALARM_INTERVAL = 40;
    private static final double RUPTURE_SECONDS = 2.0;

    @Nullable private HexStructure structure;
    @Nullable private ReactorSim sim;
    @Nullable private CompoundTag pendingSimTag;
    private Component structureError = Component.translatable("message.rbmk.not_checked");

    private long ticks;
    private boolean az5Latched;
    private double backpressure;
    private double[] ruptureTimers = new double[0];
    private int comparator;

    // Số liệu để hiển thị (mỗi tick)
    private double steamPerTick;
    private double waterPerTick;
    private double fePerTick;
    private double setpoint;
    private int loadedFuel;
    private int fuelSlots;
    private int coolantPorts;
    private int outputPorts;

    // Điều khiển từ GUI
    private boolean manualMode;
    private double manualSetpoint = 0.3;

    public ReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROLLER.get(), pos, state);
    }

    // =====================================================================
    // Tick
    // =====================================================================

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReactorControllerBlockEntity be) {
        be.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        ticks++;
        if (structure == null || ticks % VALIDATE_INTERVAL == 0) {
            revalidate(level);
        }
        if (ticks % 10 == 0) {
            sendStatusToViewers(level);
        }
        if (structure == null || sim == null) {
            updateComparator(level, 0);
            return;
        }
        int interval = RbmkServerConfig.SIM_INTERVAL.get();
        if (ticks % interval != 0) {
            return;
        }
        double dt = interval / 20.0;
        simulate(level, structure, sim, dt, interval);
    }

    private void revalidate(ServerLevel level) {
        HexValidator.Result result = HexValidator.validate(level, worldPosition,
                RbmkServerConfig.MAX_RADIUS.get(), RbmkServerConfig.MAX_HEIGHT.get());
        if (!result.valid()) {
            if (structure != null && sim != null) {
                // Cấu trúc bị phá khi đang chạy: mất lò, không có mô phỏng nữa
                sim.coldShutdown();
            }
            structure = null;
            structureError = result.error();
            return;
        }
        HexStructure fresh = result.structure();
        boolean sameShape = structure != null && sim != null
                && structure.cells().size() == fresh.cells().size()
                && structure.radius() == fresh.radius() && structure.height() == fresh.height();
        if (!sameShape) {
            ReactorSim newSim = new ReactorSim(fresh);
            if (pendingSimTag != null) {
                newSim.load(pendingSimTag);
                pendingSimTag = null;
            } else if (sim != null) {
                // Hình dạng đổi -> bắt đầu lại từ trạng thái nguội
                RbmkMod.LOGGER.debug("RBMK tại {} đổi hình dạng, reset mô phỏng", worldPosition);
            }
            newSim.setScram(az5Latched);
            sim = newSim;
            ruptureTimers = new double[fresh.cells().size()];
        }
        structure = fresh;
        structureError = null;
    }

    private void simulate(ServerLevel level, HexStructure s, ReactorSim sim, double dt, int interval) {
        // 1) Nhiên liệu từng cột
        List<List<FuelChannelBlockEntity>> fuelByCell = new ArrayList<>(s.cells().size());
        int loadedTotal = 0;
        int slotTotal = 0;
        for (int i = 0; i < s.cells().size(); i++) {
            HexStructure.Cell cell = s.cells().get(i);
            List<FuelChannelBlockEntity> list = new ArrayList<>();
            if (cell.type() == ChannelType.FUEL) {
                int loaded = 0;
                double burn = 0;
                for (BlockPos p : cell.blocks()) {
                    if (level.getBlockEntity(p) instanceof FuelChannelBlockEntity fc) {
                        list.add(fc);
                        if (fc.hasActiveFuel()) {
                            loaded++;
                            burn += fc.getBurnup();
                        }
                    }
                }
                sim.setFuel(i, loaded, loaded > 0 ? burn / loaded : 0.0);
                loadedTotal += loaded;
                slotTotal += cell.blocks().size();
            }
            fuelByCell.add(list);
        }

        // 2) Port
        List<PortBlockEntity> coolant = new ArrayList<>();
        List<PortBlockEntity> steam = new ArrayList<>();
        List<PortBlockEntity> energy = new ArrayList<>();
        for (BlockPos p : s.ports()) {
            if (level.getBlockEntity(p) instanceof PortBlockEntity port) {
                switch (port.getPortType()) {
                    case COOLANT -> coolant.add(port);
                    case STEAM -> steam.add(port);
                    case ENERGY -> energy.add(port);
                }
            }
        }
        long water = 0;
        for (PortBlockEntity c : coolant) water += c.getWater();
        loadedFuel = loadedTotal;
        fuelSlots = slotTotal;
        coolantPorts = coolant.size();
        outputPorts = steam.size() + energy.size();

        // 3) Mô phỏng
        int signal = level.getBestNeighborSignal(worldPosition);
        setpoint = az5Latched ? 0.0 : manualMode ? manualSetpoint : signal / 15.0 * 1.5;
        double heatPerMb = RbmkServerConfig.HEAT_PER_MB.get();
        ReactorSim.StepResult r = sim.step(dt, setpoint, water, backpressure, heatPerMb);

        // 4) Rút nước
        int needWater = (int) Math.ceil(r.waterUsedMb());
        for (PortBlockEntity c : coolant) {
            if (needWater <= 0) break;
            needWater -= c.drainWater(needWater);
        }

        // 5) Phân phối hơi: Energy Port trước, rồi Steam Port, phần dư = áp suất ngược
        double steamLeft = r.steamMb();
        double fePerMb = RbmkServerConfig.FE_PER_MB.get();
        double feMade = 0;
        if (fePerMb > 0) {
            for (PortBlockEntity e : energy) {
                if (steamLeft <= 0) break;
                double canMb = e.energySpace() / fePerMb;
                double useMb = Math.min(steamLeft, canMb);
                int fe = e.acceptEnergy((long) Math.floor(useMb * fePerMb));
                feMade += fe;
                steamLeft -= fe / fePerMb;
            }
        }
        for (PortBlockEntity st : steam) {
            if (steamLeft < 1) break;
            steamLeft -= st.acceptSteam((int) Math.floor(steamLeft));
        }
        steamLeft = Math.max(0, steamLeft);
        backpressure = r.steamMb() > 1 ? Math.min(1.0, steamLeft / r.steamMb()) : 0.0;

        steamPerTick = (r.steamMb() - steamLeft) / interval;
        waterPerTick = r.waterUsedMb() / interval;
        fePerTick = feMade / interval;

        // 6) Độ cháy nhiên liệu
        double burnSeconds = RbmkServerConfig.BURN_SECONDS.get();
        for (int i = 0; i < s.cells().size(); i++) {
            double flux = sim.flux(i);
            for (FuelChannelBlockEntity fc : fuelByCell.get(i)) {
                fc.setLastFlux(flux);
                fc.addBurnup(dt * flux / burnSeconds);
            }
        }

        // 7) Sự cố
        if (checkHazards(level, s, sim, dt)) {
            return;
        }

        // 8) Cảnh báo + comparator
        if (ticks % ALARM_INTERVAL == 0) {
            alarm(level, sim);
        }
        double rupture = RbmkServerConfig.RUPTURE_TEMP.get();
        updateComparator(level, (int) Math.max(0, Math.min(15, Math.round(sim.maxTemp() / rupture * 15.0))));
        setChanged();
    }

    /** @return true nếu lò vừa bị phá huỷ (dừng xử lý tiếp). */
    private boolean checkHazards(ServerLevel level, HexStructure s, ReactorSim sim, double dt) {
        double crit = RbmkServerConfig.CRIT_POWER_MULT.get();
        double meltdownT = RbmkServerConfig.MELTDOWN_TEMP.get();
        if (sim.avgPower() > crit || sim.maxTemp() > meltdownT) {
            ReactorHazards.meltdown(level, s, sim);
            structure = null;
            structureError = Component.translatable("message.rbmk.destroyed");
            sim.coldShutdown();
            return true;
        }
        double ruptureT = RbmkServerConfig.RUPTURE_TEMP.get();
        for (int i = 0; i < s.cells().size(); i++) {
            HexStructure.Cell cell = s.cells().get(i);
            if (cell.type() != ChannelType.FUEL) continue;
            if (sim.temp(i) > ruptureT) {
                ruptureTimers[i] += dt;
                if (ruptureTimers[i] >= RUPTURE_SECONDS) {
                    ReactorHazards.ruptureChannel(level, s, cell);
                    structure = null;
                    structureError = Component.translatable("message.rbmk.channel_ruptured");
                    return true;
                }
            } else {
                ruptureTimers[i] = 0;
            }
        }
        return false;
    }

    private void alarm(ServerLevel level, ReactorSim sim) {
        double rupture = RbmkServerConfig.RUPTURE_TEMP.get();
        boolean running = sim.avgPower() > 0.02;
        Component warn = null;
        if (sim.maxTemp() > rupture * 0.8) {
            warn = Component.translatable("message.rbmk.warn_temp", Math.round(sim.maxTemp()));
        } else if (running && sim.waterFraction() < 0.9) {
            warn = Component.translatable("message.rbmk.warn_water", Math.round(sim.waterFraction() * 100));
        } else if (running && sim.rodInsertion() < RbmkServerConfig.ORM_WARNING.get()) {
            warn = Component.translatable("message.rbmk.warn_orm", Math.round(sim.rodInsertion() * 100));
        } else if (backpressure > 0.1) {
            warn = Component.translatable("message.rbmk.warn_steam", Math.round(backpressure * 100));
        }
        if (warn == null) return;
        level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.5f, 0.5f);
        Component msg = warn.copy().withStyle(ChatFormatting.RED);
        for (Player p : level.players()) {
            if (p.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 32 * 32) {
                p.displayClientMessage(msg, true);
            }
        }
    }

    private void updateComparator(Level level, int value) {
        if (value != comparator) {
            comparator = value;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    // =====================================================================
    // Điều khiển
    // =====================================================================

    public void triggerAz5() {
        az5Latched = true;
        if (sim != null) sim.setScram(true);
        setChanged();
    }

    /** Reset AZ-5 chỉ khi rod đã cắm hết. */
    public boolean resetAz5() {
        if (!az5Latched) return false;
        if (sim != null && sim.rodInsertion() < 0.99) return false;
        az5Latched = false;
        if (sim != null) sim.setScram(false);
        setChanged();
        return true;
    }

    public int getComparatorOutput() { return comparator; }

    /** Nút bấm từ GUI (xem ReactorControllerMenu.BTN_*). */
    public void handleButton(int id) {
        switch (id) {
            case ReactorControllerMenu.BTN_MINUS -> {
                if (!manualMode) enterManual();
                manualSetpoint = Math.max(0.0, Math.round((manualSetpoint - 0.1) * 10.0) / 10.0);
            }
            case ReactorControllerMenu.BTN_PLUS -> {
                if (!manualMode) enterManual();
                manualSetpoint = Math.min(1.5, Math.round((manualSetpoint + 0.1) * 10.0) / 10.0);
            }
            case ReactorControllerMenu.BTN_MODE -> {
                if (manualMode) manualMode = false;
                else enterManual();
            }
            case ReactorControllerMenu.BTN_AZ5 -> triggerAz5();
            case ReactorControllerMenu.BTN_RESET -> resetAz5();
            default -> { }
        }
        setChanged();
        if (level instanceof ServerLevel sl) sendStatusToViewers(sl);
    }

    /** Chuyển sang thủ công, giữ nguyên mức đang đặt để không bị giật công suất. */
    private void enterManual() {
        manualMode = true;
        manualSetpoint = Math.round(Math.min(1.5, setpoint) * 10.0) / 10.0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.rbmk.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ReactorControllerMenu(containerId, inventory, worldPosition);
    }

    public void sendStatusTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildStatus());
    }

    private void sendStatusToViewers(ServerLevel level) {
        ReactorStatusPayload payload = null;
        for (ServerPlayer p : level.players()) {
            if (p.containerMenu instanceof ReactorControllerMenu m && m.getPos().equals(worldPosition)) {
                if (payload == null) payload = buildStatus();
                PacketDistributor.sendToPlayer(p, payload);
            }
        }
    }

    private ReactorStatusPayload buildStatus() {
        HexStructure s = structure;
        ReactorSim sm = sim;
        boolean formed = s != null && sm != null;
        int n = formed ? s.cells().size() : 0;
        byte[] qs = new byte[n];
        byte[] rs = new byte[n];
        byte[] types = new byte[n];
        float[] temps = new float[n];
        float[] fluxes = new float[n];
        for (int i = 0; i < n; i++) {
            HexStructure.Cell c = s.cells().get(i);
            qs[i] = (byte) c.q();
            rs[i] = (byte) c.r();
            types[i] = c.type().id();
            temps[i] = (float) sm.temp(i);
            fluxes[i] = (float) sm.flux(i);
        }
        Component msg = formed ? Component.empty()
                : (structureError != null ? structureError : Component.translatable("message.rbmk.not_checked"));
        return new ReactorStatusPayload(worldPosition, formed, msg,
                formed ? s.radius() : 0, formed ? s.height() : 0,
                loadedFuel, fuelSlots, coolantPorts, outputPorts, az5Latched, manualMode,
                (float) (az5Latched ? 0.0 : manualMode ? manualSetpoint : setpoint),
                formed ? (float) sm.avgPower() : 0f,
                formed ? (float) sm.maxTemp() : 20f,
                RbmkServerConfig.RUPTURE_TEMP.get().floatValue(),
                formed ? (float) sm.avgVoid() : 0f,
                formed ? (float) sm.maxXenon() : 0f,
                formed ? (float) sm.rodInsertion() : 1f,
                formed ? (float) sm.waterFraction() : 1f,
                (float) waterPerTick, (float) steamPerTick, (float) fePerTick, (float) backpressure,
                RbmkServerConfig.ORM_WARNING.get().floatValue(),
                qs, rs, types, temps, fluxes);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level == null || level.isClientSide || sim == null) return;
        try {
            if (RbmkServerConfig.AUTO_SCRAM_ON_UNLOAD.get()) {
                sim.coldShutdown();
            }
        } catch (IllegalStateException ignored) {
            // config đã unload khi server tắt
        }
    }

    // =====================================================================
    // Trạng thái hiển thị
    // =====================================================================

    public List<Component> statusLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("status.rbmk.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (structure == null || sim == null) {
            lines.add(Component.translatable("status.rbmk.not_formed").withStyle(ChatFormatting.RED));
            if (structureError != null) lines.add(structureError.copy().withStyle(ChatFormatting.GRAY));
            return lines;
        }
        HexStructure s = structure;
        lines.add(Component.translatable("status.rbmk.core", s.radius(), s.height(),
                s.countType(ChannelType.FUEL), s.countType(ChannelType.ROD), s.countType(ChannelType.GRAPHITE)));
        lines.add(colored(Component.translatable("status.rbmk.fuel", loadedFuel, fuelSlots), loadedFuel == 0));
        if (coolantPorts == 0) {
            lines.add(Component.translatable("status.rbmk.no_coolant_port").withStyle(ChatFormatting.RED));
        }
        if (outputPorts == 0) {
            lines.add(Component.translatable("status.rbmk.no_output_port").withStyle(ChatFormatting.RED));
        }
        lines.add(line("status.rbmk.power", pct(sim.avgPower()), pct(setpoint)));
        double rupture = RbmkServerConfig.RUPTURE_TEMP.get();
        lines.add(colored(Component.translatable("status.rbmk.temp", Math.round(sim.maxTemp()), Math.round(rupture)),
                sim.maxTemp() > rupture * 0.8));
        lines.add(line("status.rbmk.void_xenon", pct(sim.avgVoid()), String.format("%.2f", sim.maxXenon())));
        boolean ormLow = sim.avgPower() > 0.02 && sim.rodInsertion() < RbmkServerConfig.ORM_WARNING.get();
        lines.add(colored(Component.translatable("status.rbmk.rods", pct(sim.rodInsertion())), ormLow));
        lines.add(colored(Component.translatable("status.rbmk.water", Math.round(waterPerTick), pct(sim.waterFraction())),
                sim.waterFraction() < 0.9));
        lines.add(line("status.rbmk.output", Math.round(steamPerTick), Math.round(fePerTick)));
        if (backpressure > 0.01) {
            lines.add(Component.translatable("status.rbmk.backpressure", pct(backpressure)).withStyle(ChatFormatting.RED));
        }
        if (az5Latched) {
            lines.add(Component.translatable("status.rbmk.az5").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
        return lines;
    }

    private static long pct(double v) { return Math.round(v * 100.0); }

    private static MutableComponent line(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.WHITE);
    }

    private static Component colored(MutableComponent c, boolean bad) {
        return c.withStyle(bad ? ChatFormatting.RED : ChatFormatting.WHITE);
    }

    // =====================================================================
    // NBT
    // =====================================================================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("az5", az5Latched);
        tag.putDouble("backpressure", backpressure);
        tag.putBoolean("manual", manualMode);
        tag.putDouble("manualSetpoint", manualSetpoint);
        if (sim != null) {
            tag.put("sim", sim.save(registries));
        } else if (pendingSimTag != null) {
            tag.put("sim", pendingSimTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        az5Latched = tag.getBoolean("az5");
        backpressure = tag.getDouble("backpressure");
        manualMode = tag.getBoolean("manual");
        if (tag.contains("manualSetpoint")) manualSetpoint = tag.getDouble("manualSetpoint");
        if (tag.contains("sim")) {
            pendingSimTag = tag.getCompound("sim");
        }
    }
}

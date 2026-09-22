package com.phat.rbmk.multiblock;

import com.phat.rbmk.block.PortBlock;
import com.phat.rbmk.registry.ModBlocks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Kiểm tra cấu trúc lò lục giác. Controller nằm ở tâm lớp nắp trên.
 *
 * <pre>
 * y = topY            : nắp  (upper_shield / az5_panel / port), tâm = controller, vòng 0..R+1
 * y = topY-1..topY-H  : lõi  vòng 0..R = cột (graphite / fuel / rod / water), vòng R+1 = tường (shield_casing / port)
 * y = topY-H-1        : đáy  (shield_casing / port), vòng 0..R+1
 * </pre>
 */
public final class HexValidator {
    public static final int MIN_RADIUS = 2;
    public static final int MIN_HEIGHT = 2;

    public record Result(@Nullable HexStructure structure, @Nullable Component error) {
        static Result ok(HexStructure s) { return new Result(s, null); }
        static Result fail(Component e) { return new Result(null, e); }
        public boolean valid() { return structure != null; }
    }

    private HexValidator() {}

    public static Result validate(Level level, BlockPos center, int maxRadius, int maxHeight) {
        int cx = center.getX();
        int cz = center.getZ();
        int topY = center.getY();
        int coreTopY = topY - 1;

        // 1) Tìm bán kính: đi theo +q (trục X) trong lớp lõi trên cùng cho tới khi gặp tường
        int wall = -1;
        for (int q = 1; q <= maxRadius + 1; q++) {
            if (isWall(level.getBlockState(new BlockPos(cx + q, coreTopY, cz)))) {
                wall = q;
                break;
            }
        }
        if (wall < 0) {
            return Result.fail(Component.translatable("message.rbmk.err_no_wall", maxRadius));
        }
        int radius = wall - 1;
        if (radius < MIN_RADIUS) {
            return Result.fail(Component.translatable("message.rbmk.err_radius_small", MIN_RADIUS));
        }

        // 2) Tìm chiều cao: đi xuống từ tâm cho tới khi gặp đáy
        int height = 0;
        while (height <= maxHeight && coreType(level.getBlockState(new BlockPos(cx, coreTopY - height, cz))) != null) {
            height++;
        }
        if (height < MIN_HEIGHT || height > maxHeight) {
            return Result.fail(Component.translatable("message.rbmk.err_height", MIN_HEIGHT, maxHeight));
        }
        int bottomY = coreTopY - height;

        List<BlockPos> ports = new ArrayList<>();
        List<BlockPos> lid = new ArrayList<>();
        List<HexStructure.Cell> cells = new ArrayList<>();
        Map<Long, Integer> indexByAxial = new HashMap<>();

        for (int q = -(radius + 1); q <= radius + 1; q++) {
            for (int r = -(radius + 1); r <= radius + 1; r++) {
                int ring = HexStructure.ring(q, r);
                if (ring > radius + 1) continue;
                int x = cx + q;
                int z = cz + r;

                // Nắp
                BlockPos topPos = new BlockPos(x, topY, z);
                if (q != 0 || r != 0) {
                    BlockState s = level.getBlockState(topPos);
                    if (isPort(s)) {
                        ports.add(topPos);
                    } else if (s.is(ModBlocks.UPPER_SHIELD.get()) || s.is(ModBlocks.AZ5_PANEL.get())) {
                        lid.add(topPos);
                    } else {
                        return Result.fail(bad("message.rbmk.err_lid", topPos));
                    }
                }

                // Đáy
                BlockPos bottomPos = new BlockPos(x, bottomY, z);
                BlockState bs = level.getBlockState(bottomPos);
                if (isPort(bs)) {
                    ports.add(bottomPos);
                } else if (!bs.is(ModBlocks.SHIELD_CASING.get())) {
                    return Result.fail(bad("message.rbmk.err_bottom", bottomPos));
                }

                // Thân
                if (ring == radius + 1) {
                    for (int y = bottomY + 1; y <= coreTopY; y++) {
                        BlockPos p = new BlockPos(x, y, z);
                        BlockState s = level.getBlockState(p);
                        if (isPort(s)) {
                            ports.add(p);
                        } else if (!s.is(ModBlocks.SHIELD_CASING.get())) {
                            return Result.fail(bad("message.rbmk.err_wall", p));
                        }
                    }
                } else {
                    ChannelType columnType = null;
                    List<BlockPos> blocks = new ArrayList<>(height);
                    for (int y = coreTopY; y > bottomY; y--) {
                        BlockPos p = new BlockPos(x, y, z);
                        ChannelType t = coreType(level.getBlockState(p));
                        if (t == null) {
                            return Result.fail(bad("message.rbmk.err_core", p));
                        }
                        if (columnType == null) {
                            columnType = t;
                        } else if (t != columnType) {
                            return Result.fail(bad("message.rbmk.err_column_mixed", p));
                        }
                        blocks.add(p);
                    }
                    indexByAxial.put(key(q, r), cells.size());
                    cells.add(new HexStructure.Cell(q, r, columnType, List.copyOf(blocks)));
                }
            }
        }

        boolean hasFuel = cells.stream().anyMatch(c -> c.type() == ChannelType.FUEL);
        if (!hasFuel) {
            return Result.fail(Component.translatable("message.rbmk.err_no_fuel"));
        }

        int[][] neighbors = new int[cells.size()][6];
        for (int i = 0; i < cells.size(); i++) {
            HexStructure.Cell c = cells.get(i);
            for (int k = 0; k < 6; k++) {
                Integer idx = indexByAxial.get(key(c.q() + HexStructure.AXIAL_DIRS[k][0], c.r() + HexStructure.AXIAL_DIRS[k][1]));
                neighbors[i][k] = idx == null ? -1 : idx;
            }
        }

        return Result.ok(new HexStructure(radius, height, topY, center.immutable(),
                List.copyOf(cells), neighbors, List.copyOf(ports), List.copyOf(lid)));
    }

    private static long key(int q, int r) {
        return ((long) q << 32) ^ (r & 0xffffffffL);
    }

    private static Component bad(String key, BlockPos p) {
        return Component.translatable(key, p.getX(), p.getY(), p.getZ());
    }

    private static boolean isPort(BlockState s) {
        return s.getBlock() instanceof PortBlock;
    }

    private static boolean isWall(BlockState s) {
        return s.is(ModBlocks.SHIELD_CASING.get()) || isPort(s);
    }

    @Nullable
    public static ChannelType coreType(BlockState s) {
        if (s.is(ModBlocks.NUCLEAR_GRAPHITE_BLOCK.get())) return ChannelType.GRAPHITE;
        if (s.is(ModBlocks.FUEL_CHANNEL.get())) return ChannelType.FUEL;
        if (s.is(ModBlocks.CONTROL_ROD.get())) return ChannelType.ROD;
        if (s.is(ModBlocks.WATER_CHANNEL.get())) return ChannelType.WATER;
        return null;
    }
}

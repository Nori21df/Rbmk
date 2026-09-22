package com.phat.rbmk.multiblock;

import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Kết quả validate multiblock.
 * Toạ độ lục giác trục (axial): q = dx, r = dz. Vòng (ring) = max(|q|, |r|, |q + r|).
 *
 * @param radius     bán kính lõi (không tính tường)
 * @param height     số lớp lõi
 * @param topY       Y của lớp nắp (lớp có controller)
 * @param center     vị trí controller
 * @param cells      các cột lõi, index trùng với index trong mô phỏng
 * @param neighbors  neighbors[i][k] = index cột kề thứ k, -1 nếu là tường
 * @param ports      vị trí các port
 * @param lid        vị trí khối nắp (trừ controller)
 */
public record HexStructure(int radius, int height, int topY, BlockPos center,
                           List<Cell> cells, int[][] neighbors,
                           List<BlockPos> ports, List<BlockPos> lid) {

    public record Cell(int q, int r, ChannelType type, List<BlockPos> blocks) {}

    public static final int[][] AXIAL_DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, -1}, {-1, 1}};

    public static int ring(int q, int r) {
        return Math.max(Math.abs(q), Math.max(Math.abs(r), Math.abs(q + r)));
    }

    public int coreBottomY() { return topY - height; }

    public long countType(ChannelType type) {
        return cells.stream().filter(c -> c.type() == type).count();
    }
}

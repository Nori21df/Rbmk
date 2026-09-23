package com.phat.rbmk.block.entity;

import net.neoforged.neoforge.items.ItemStackHandler;

/** Ô chứa nhiên liệu đã cháy mà controller tự rút ra khỏi lò. Người chơi chỉ lấy ra được. */
public class SpentOutputHandler extends ItemStackHandler {
    public static final int SLOTS = 9;
    private final Runnable onChange;

    public SpentOutputHandler(Runnable onChange) {
        super(SLOTS);
        this.onChange = onChange;
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChange.run();
    }
}

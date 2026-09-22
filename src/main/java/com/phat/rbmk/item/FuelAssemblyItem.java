package com.phat.rbmk.item;

import com.phat.rbmk.registry.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Bó nhiên liệu. Độ cháy (burnup) lưu trong data component rbmk:burnup. */
public class FuelAssemblyItem extends Item {
    public FuelAssemblyItem(Properties properties) {
        super(properties);
    }

    public static float burnup(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BURNUP.get(), 0.0f);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return burnup(stack) > 0.0f;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * (1.0f - burnup(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float remaining = 1.0f - burnup(stack);
        return Mth.hsvToRgb(remaining / 3.0f, 1.0f, 1.0f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int remaining = Math.round((1.0f - burnup(stack)) * 100.0f);
        tooltip.add(Component.translatable("tooltip.rbmk.fuel_remaining", remaining).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.rbmk.fuel_hint").withStyle(ChatFormatting.GRAY));
    }
}

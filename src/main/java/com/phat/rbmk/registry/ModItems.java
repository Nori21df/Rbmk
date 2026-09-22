package com.phat.rbmk.registry;

import com.phat.rbmk.RbmkMod;
import com.phat.rbmk.item.FuelAssemblyItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RbmkMod.MODID);

    // Nguyên liệu trung gian
    public static final DeferredItem<Item> NUCLEAR_GRAPHITE_BILLET = ITEMS.registerSimpleItem("nuclear_graphite_billet");
    public static final DeferredItem<Item> ZIRCALOY_INGOT = ITEMS.registerSimpleItem("zircaloy_ingot");
    public static final DeferredItem<Item> PRESSURE_TUBE = ITEMS.registerSimpleItem("pressure_tube");
    public static final DeferredItem<Item> UO2_POWDER = ITEMS.registerSimpleItem("uo2_powder");
    public static final DeferredItem<Item> ENRICHED_PELLET = ITEMS.registerSimpleItem("enriched_pellet");

    // Nhiên liệu
    public static final DeferredItem<FuelAssemblyItem> FUEL_ASSEMBLY = ITEMS.register("fuel_assembly",
            () -> new FuelAssemblyItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SPENT_FUEL_ASSEMBLY = ITEMS.register("spent_fuel_assembly",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Block items
    public static final DeferredItem<BlockItem> NUCLEAR_GRAPHITE_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.NUCLEAR_GRAPHITE_BLOCK);
    public static final DeferredItem<BlockItem> SHIELD_CASING = ITEMS.registerSimpleBlockItem(ModBlocks.SHIELD_CASING);
    public static final DeferredItem<BlockItem> UPPER_SHIELD = ITEMS.registerSimpleBlockItem(ModBlocks.UPPER_SHIELD);
    public static final DeferredItem<BlockItem> FUEL_CHANNEL = ITEMS.registerSimpleBlockItem(ModBlocks.FUEL_CHANNEL);
    public static final DeferredItem<BlockItem> CONTROL_ROD = ITEMS.registerSimpleBlockItem(ModBlocks.CONTROL_ROD);
    public static final DeferredItem<BlockItem> WATER_CHANNEL = ITEMS.registerSimpleBlockItem(ModBlocks.WATER_CHANNEL);
    public static final DeferredItem<BlockItem> REACTOR_CONTROLLER = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_CONTROLLER);
    public static final DeferredItem<BlockItem> AZ5_PANEL = ITEMS.registerSimpleBlockItem(ModBlocks.AZ5_PANEL);
    public static final DeferredItem<BlockItem> COOLANT_PORT = ITEMS.registerSimpleBlockItem(ModBlocks.COOLANT_PORT);
    public static final DeferredItem<BlockItem> STEAM_PORT = ITEMS.registerSimpleBlockItem(ModBlocks.STEAM_PORT);
    public static final DeferredItem<BlockItem> ENERGY_PORT = ITEMS.registerSimpleBlockItem(ModBlocks.ENERGY_PORT);
    public static final DeferredItem<BlockItem> CORIUM = ITEMS.registerSimpleBlockItem(ModBlocks.CORIUM);

    private ModItems() {}
}

package com.dexer.aquanaut.core;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.item.AirSupplyItem;
import com.dexer.aquanaut.common.item.BubbleGunItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Aquanaut.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB, Aquanaut.MODID);

    public static final DeferredItem<Item> OCTOPUS_SHREDS = ITEMS.registerSimpleItem("octopus_shreds");
    public static final DeferredItem<Item> COOKED_OCTOPUS_SHREDS = ITEMS.registerSimpleItem("cooked_octopus_shreds",
            food(6, 0.8F));

    public static final DeferredItem<Item> SARDINE = ITEMS.registerSimpleItem("sardine");
    public static final DeferredItem<Item> COOKED_SARDINE = ITEMS.registerSimpleItem("cooked_sardine",
            food(4, 0.6F));

    public static final DeferredItem<Item> SHARK_FINS = ITEMS.registerSimpleItem("shark_fins");
    public static final DeferredItem<Item> COOKED_SHARK_FINS = ITEMS.registerSimpleItem("cooked_shark_fins",
            food(8, 0.9F));
    public static final DeferredItem<Item> FISHNUT = ITEMS.registerSimpleItem("fishnut");
    public static final DeferredItem<Item> COOKED_FISHNUT = ITEMS.registerSimpleItem("cooked_fishnut",
            food(5, 0.7F));
    public static final DeferredItem<AirSupplyItem> AIR_SOUP = ITEMS.registerItem("air_soup",
            props -> new AirSupplyItem(props.food(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.5F).build()),
                    3));
    public static final DeferredItem<AirSupplyItem> AIR_SANDWICH = ITEMS.registerItem("air_sandwich",
            props -> new AirSupplyItem(props.food(new FoodProperties.Builder()
                    .nutrition(9).saturationModifier(1.0F).build()),
                    6));
    public static final DeferredItem<Item> FANG = ITEMS.registerSimpleItem("fang");
    public static final DeferredItem<Item> ICE_FIN = ITEMS.registerSimpleItem("ice_fin");
    public static final DeferredItem<Item> ICE_CORE = ITEMS.registerSimpleItem("ice_core");

    public static final DeferredItem<Item> IRON_OXYGEN_TANK = equipmentItem("iron_oxygen_tank");
    public static final DeferredItem<Item> WOOD_OXYGEN_TANK = equipmentItem("wood_oxygen_tank");
    public static final DeferredItem<Item> SHARK_SKIN = ITEMS.registerSimpleItem("shark_skin");
    public static final DeferredItem<Item> SHARK_FLIPPERS = equipmentItem("shark_flippers");
    public static final DeferredItem<Item> WOOD_FLIPPERS = equipmentItem("wood_flippers");
    public static final DeferredItem<Item> CORAL_FLIPPERS = equipmentItem("coral_flippers");

    // Coral block items (log-like blocks)
    public static final DeferredItem<BlockItem> RED_CORAL_BLOCK = blockItem("red_coral_block",
            BlockRegistry.RED_CORAL_BLOCK);
    public static final DeferredItem<BlockItem> BLUE_CORAL_BLOCK = blockItem("blue_coral_block",
            BlockRegistry.BLUE_CORAL_BLOCK);
    public static final DeferredItem<BlockItem> PURPLE_CORAL_BLOCK = blockItem("purple_coral_block",
            BlockRegistry.PURPLE_CORAL_BLOCK);
    public static final DeferredItem<BlockItem> GREEN_CORAL_BLOCK = blockItem("green_coral_block",
            BlockRegistry.GREEN_CORAL_BLOCK);
    public static final DeferredItem<BlockItem> FLUORASCENT_BLUE_CORAL_BLOCK = blockItem(
            "fluorescent_blue_coral_block",
            BlockRegistry.FLUORASCENT_BLUE_CORAL_BLOCK);

    // Ringed coral block items
    public static final DeferredItem<BlockItem> RINGED_BLUE_CORAL_BLOCK = blockItem("ringed_blue_coral_block",
            BlockRegistry.RINGED_BLUE_CORAL_BLOCK);
    public static final DeferredItem<BlockItem> RINGED_GREEN_CORAL_BLOCK = blockItem("ringed_green_coral_block",
            BlockRegistry.RINGED_GREEN_CORAL_BLOCK);
    public static final DeferredItem<BlockItem> RINGED_PURPLE_CORAL_BLOCK = blockItem("ringed_purple_coral_block",
            BlockRegistry.RINGED_PURPLE_CORAL_BLOCK);
    public static final DeferredItem<BlockItem> RINGED_RED_CORAL_BLOCK = blockItem("ringed_red_coral_block",
            BlockRegistry.RINGED_RED_CORAL_BLOCK);
    public static final DeferredItem<BlockItem> RINGED_FLUORASCENT_BLUE_CORAL_BLOCK = blockItem(
            "ringed_fluorescent_blue_coral_block",
            BlockRegistry.RINGED_FLUORASCENT_BLUE_CORAL_BLOCK);

    public static final DeferredItem<Item> BUBBLE_GUN = ITEMS.registerItem("bubble_gun",
            properties -> new BubbleGunItem(properties.durability(60).stacksTo(1)));

    public static final DeferredItem<DeferredSpawnEggItem> OCTOPUS_SPAWN_EGG = spawnEgg("octopus_spawn_egg",
            EntityRegistry.OCTOPUS, 0x7A6250, 0x261B17);
    public static final DeferredItem<DeferredSpawnEggItem> SARDINE_SPAWN_EGG = spawnEgg("sardine_spawn_egg",
            EntityRegistry.SARDINE, 0x7BA4C6, 0x25435F);
    public static final DeferredItem<DeferredSpawnEggItem> ANGLERFISH_SPAWN_EGG = spawnEgg("anglerfish_spawn_egg",
            EntityRegistry.ANGLERFISH, 0xB78644, 0x53381D);
    public static final DeferredItem<DeferredSpawnEggItem> ELECTROFISH_SPAWN_EGG = spawnEgg("electrofish_spawn_egg",
            EntityRegistry.ELECTROFISH, 0x5C7BD0, 0x1D5A92);
    public static final DeferredItem<DeferredSpawnEggItem> DONUTFISH_SPAWN_EGG = spawnEgg("donutfish_spawn_egg",
            EntityRegistry.DONUTFISH, 0xD09146, 0x82441C);
    public static final DeferredItem<DeferredSpawnEggItem> SPRINGFISH_SPAWN_EGG = spawnEgg("springfish_spawn_egg",
            EntityRegistry.SPRINGFISH, 0x91A8BF, 0x46627A);
    public static final DeferredItem<DeferredSpawnEggItem> ICERAIL_SPAWN_EGG = spawnEgg("icerail_spawn_egg",
            EntityRegistry.ICERAIL, 0xA9E4FF, 0x3A79C2);
    public static final DeferredItem<DeferredSpawnEggItem> HELICOPRION_SPAWN_EGG = spawnEgg("helicoprion_spawn_egg",
            EntityRegistry.HELICOPRION, 0xA98F75, 0x624634);
    public static final DeferredItem<DeferredSpawnEggItem> CATFISH_SPAWN_EGG = spawnEgg("catfish_spawn_egg",
            EntityRegistry.CATFISH, 0xF7A35C, 0x5C4033);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOOD_TAB = tab("food",
            COOKED_SARDINE, output -> {
                output.accept(COOKED_OCTOPUS_SHREDS.get());
                output.accept(COOKED_SARDINE.get());
                output.accept(COOKED_SHARK_FINS.get());
                output.accept(FISHNUT.get());
                output.accept(COOKED_FISHNUT.get());
                output.accept(AIR_SOUP.get());
                output.accept(AIR_SANDWICH.get());
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATURES_TAB = tab("creatures",
            OCTOPUS_SPAWN_EGG, output -> {
                output.accept(OCTOPUS_SPAWN_EGG.get());
                output.accept(SARDINE_SPAWN_EGG.get());
                output.accept(ANGLERFISH_SPAWN_EGG.get());
                output.accept(ELECTROFISH_SPAWN_EGG.get());
                output.accept(DONUTFISH_SPAWN_EGG.get());
                output.accept(SPRINGFISH_SPAWN_EGG.get());
                output.accept(ICERAIL_SPAWN_EGG.get());
                output.accept(HELICOPRION_SPAWN_EGG.get());
                output.accept(CATFISH_SPAWN_EGG.get());
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MATERIALS_TAB = tab("materials",
            OCTOPUS_SHREDS, output -> {
                output.accept(OCTOPUS_SHREDS.get());
                output.accept(SARDINE.get());
                output.accept(SHARK_FINS.get());
                output.accept(SHARK_SKIN.get());
                output.accept(FANG.get());
                output.accept(ICE_FIN.get());
                output.accept(ICE_CORE.get());
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS_TAB = tab("tools",
            BUBBLE_GUN, output -> {
                output.accept(BUBBLE_GUN.get());
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EQUIPMENT_TAB = tab("equipment",
            IRON_OXYGEN_TANK, output -> {
                output.accept(IRON_OXYGEN_TANK.get());
                output.accept(WOOD_OXYGEN_TANK.get());
                output.accept(SHARK_FLIPPERS.get());
                output.accept(WOOD_FLIPPERS.get());
                output.accept(CORAL_FLIPPERS.get());
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENVIRONMENT_TAB = tab("environment",
            RED_CORAL_BLOCK, output -> {
                output.accept(RED_CORAL_BLOCK.get());
                output.accept(BLUE_CORAL_BLOCK.get());
                output.accept(PURPLE_CORAL_BLOCK.get());
                output.accept(GREEN_CORAL_BLOCK.get());
                output.accept(FLUORASCENT_BLUE_CORAL_BLOCK.get());
                // Ringed coral blocks
                output.accept(RINGED_RED_CORAL_BLOCK.get());
                output.accept(RINGED_BLUE_CORAL_BLOCK.get());
                output.accept(RINGED_PURPLE_CORAL_BLOCK.get());
                output.accept(RINGED_GREEN_CORAL_BLOCK.get());
                output.accept(RINGED_FLUORASCENT_BLUE_CORAL_BLOCK.get());
            });

    private ItemRegistry() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private static Item.Properties food(int nutrition, float saturationModifier) {
        return new Item.Properties().food(new FoodProperties.Builder().nutrition(nutrition)
                .saturationModifier(saturationModifier)
                .build());
    }

    private static DeferredItem<DeferredSpawnEggItem> spawnEgg(String name,
            Supplier<? extends EntityType<? extends Mob>> entityType, int primaryColor,
            int secondaryColor) {
        return ITEMS.registerItem(name,
                properties -> new DeferredSpawnEggItem(entityType, primaryColor, secondaryColor,
                        properties));
    }

    private static DeferredItem<BlockItem> blockItem(String name,
            Supplier<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.registerItem(name,
                properties -> new BlockItem(block.get(), properties));
    }

    private static DeferredItem<Item> equipmentItem(String name) {
        return ITEMS.registerItem(name, properties -> new Item(properties.stacksTo(1)));
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(String name,
            Supplier<? extends Item> iconItem,
            Consumer<CreativeModeTab.Output> contents) {
        return CREATIVE_MODE_TABS.register(name, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + Aquanaut.MODID + "." + name))
                .icon(() -> new ItemStack(iconItem.get()))
                .displayItems((parameters, output) -> contents.accept(output))
                .build());
    }
}

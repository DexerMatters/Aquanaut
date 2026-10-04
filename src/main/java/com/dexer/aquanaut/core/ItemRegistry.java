package com.dexer.aquanaut.core;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import com.dexer.aquanaut.common.inventory.CreativeTabHeader;
import com.dexer.aquanaut.common.inventory.SectionedTabOutput;
import com.dexer.aquanaut.common.item.AirSupplyItem;
import com.dexer.aquanaut.common.item.BiologicalDetectorItem;
import com.dexer.aquanaut.common.item.BubbleGunItem;
import com.dexer.aquanaut.common.item.CursorItem;
import com.dexer.aquanaut.common.item.DivingEquipmentItem;
import com.dexer.aquanaut.common.item.DevelopedPhotoItem;
import com.dexer.aquanaut.common.item.ExposedFilmItem;
import com.dexer.aquanaut.common.item.FishingNetBlockItem;
import com.dexer.aquanaut.common.item.GasFlowMeterItem;
import com.dexer.aquanaut.common.item.HandheldAirBladderItem;
import com.dexer.aquanaut.common.item.HandheldSearchlightItem;
import com.dexer.aquanaut.common.item.LargeHandheldAirBladderItem;
import com.dexer.aquanaut.common.item.HarpoonItem;
import com.dexer.aquanaut.common.item.NotebookItem;
import com.dexer.aquanaut.common.item.ScoopNetItem;
import com.dexer.aquanaut.common.item.ShellCameraItem;
import com.dexer.aquanaut.common.item.SubmarineCompassItem;
import com.dexer.aquanaut.common.item.SubmarineDroneControllerItem;
import com.dexer.aquanaut.common.item.SubmarineDroneItem;
import com.dexer.aquanaut.common.item.ThermophilicSampleItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ItemRegistry {
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Aquanaut.MODID);
        public static final DeferredItem<Item> MUD_BALL = ITEMS.registerSimpleItem("mud_ball");
        public static final DeferredItem<Item> NUTRIENT_MUD_BALL = ITEMS.registerSimpleItem("nutrient_mud_ball");
        public static final DeferredItem<Item> FOSSIL_FRAGMENT = ITEMS.registerSimpleItem("fossil_fragment");
        public static final DeferredItem<BlockItem> MUD = blockItem("mud", BlockRegistry.MUD);
        public static final DeferredItem<BlockItem> MUD_BRICKS = blockItem("mud_bricks", BlockRegistry.MUD_BRICKS);
        public static final DeferredItem<BlockItem> MUD_BRICK_STAIRS = blockItem("mud_brick_stairs",
                        BlockRegistry.MUD_BRICK_STAIRS);
        public static final DeferredItem<BlockItem> MUD_BRICK_SLAB = blockItem("mud_brick_slab",
                        BlockRegistry.MUD_BRICK_SLAB);
        public static final DeferredItem<BlockItem> MUD_BRICK_WALL = blockItem("mud_brick_wall",
                        BlockRegistry.MUD_BRICK_WALL);
        public static final DeferredItem<BlockItem> PARASITIC_MUD = blockItem("parasitic_mud", BlockRegistry.PARASITIC_MUD);
        public static final DeferredItem<BlockItem> FOSSIL_BED = blockItem("fossil_bed", BlockRegistry.FOSSIL_BED);
        public static final DeferredItem<BlockItem> SHELL_PILE = blockItem("shell_pile", BlockRegistry.SHELL_PILE);
        public static final DeferredItem<BlockItem> SEDIMENT_COLUMN = blockItem("sediment_column", BlockRegistry.SEDIMENT_COLUMN);
        public static final DeferredItem<BlockItem> FOSSIL_DISPLAY = blockItem("fossil_display", BlockRegistry.FOSSIL_DISPLAY);
        public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(
                        Registries.CREATIVE_MODE_TAB, Aquanaut.MODID);

        public static final DeferredItem<Item> OCTOPUS_SHREDS = ITEMS.registerSimpleItem("octopus_shreds");
        public static final DeferredItem<Item> COOKED_OCTOPUS_SHREDS = ITEMS.registerSimpleItem("cooked_octopus_shreds",
                        food(6, 0.8F));

        public static final DeferredItem<Item> SARDINE = ITEMS.registerSimpleItem("sardine");
        public static final DeferredItem<Item> COOKED_SARDINE = ITEMS.registerSimpleItem("cooked_sardine",
                        food(4, 0.6F));
        public static final DeferredItem<Item> CRAB_MEAT = ITEMS.registerSimpleItem("crab_meat",
                        food(3, 0.3F));
        public static final DeferredItem<Item> COOKED_CRAB_MEAT = ITEMS.registerSimpleItem("cooked_crab_meat",
                        food(6, 0.8F));
        public static final DeferredItem<Item> WORM_MUSCLE = ITEMS.registerSimpleItem("worm_muscle", food(3, 0.3F));
        public static final DeferredItem<Item> MUD_BEAN = ITEMS.registerSimpleItem("mud_bean", food(2, 0.4F));
        public static final DeferredItem<Item> HUMUS_GEL = ITEMS.registerSimpleItem("humus_gel");
        public static final DeferredItem<Item> ANCIENT_SHELL_FRAGMENT = ITEMS.registerSimpleItem("ancient_shell_fragment");
        public static final DeferredItem<DivingEquipmentItem> MUDWALKER_CHARM = flippersItem("mudwalker_charm", 256, 1.0F);

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
        public static final DeferredItem<AirSupplyItem> AIR_SAC = ITEMS.registerItem("air_sac",
                        props -> new AirSupplyItem(props.food(new FoodProperties.Builder()
                                        .nutrition(0).saturationModifier(0.0F).build()),
                                        2));
        /**
         * A translucent air membrane in a wrought iron bail handle, under a brass
         * cap. It is an air supply, and while it is held it keeps its owner
         * floating (see FlotationHelper). Unlike the other air supplies it is a
         * modelled object: the held geometry lives in
         * {@code models/item/handheld_air_bladder_held.json} and the inventory
         * sprite is a render of that same model, so the client extension
         * registered in {@code ClientModEvents} draws it.
         */
        public static final DeferredItem<HandheldAirBladderItem> HANDHELD_AIR_BLADDER = ITEMS.registerItem(
                        "handheld_air_bladder",
                        props -> new HandheldAirBladderItem(props.food(new FoodProperties.Builder()
                                        .nutrition(0).saturationModifier(0.0F).build()),
                                        5));
        /**
         * The same bladder, oversized: a full breath of air, and enough lift to
         * climb to the surface hard while it is held.
         */
        public static final DeferredItem<LargeHandheldAirBladderItem> LARGE_HANDHELD_AIR_BLADDER = ITEMS.registerItem(
                        "large_handheld_air_bladder",
                        props -> new LargeHandheldAirBladderItem(props.food(new FoodProperties.Builder()
                                        .nutrition(0).saturationModifier(0.0F).build()),
                                        10));
        public static final DeferredItem<Item> FANG = ITEMS.registerSimpleItem("fang");
        public static final DeferredItem<Item> ICE_FIN = ITEMS.registerSimpleItem("ice_fin");
        public static final DeferredItem<Item> ICE_CORE = ITEMS.registerSimpleItem("ice_core");
        public static final DeferredItem<NotebookItem> NOTEBOOK = ITEMS.registerItem("notebook",
                        NotebookItem::new);

        /** Places a {@code CursorEntity}: the under-sea location pin. */
        public static final DeferredItem<CursorItem> CURSOR = ITEMS.registerItem("cursor",
                        CursorItem::new);

        /**
         * Deploys a {@code SubmarineDroneEntity}, and takes one back: hitting a deployed drone
         * returns this item.
         */
        public static final DeferredItem<SubmarineDroneItem> SUBMARINE_DRONE = ITEMS.registerItem(
                        "submarine_drone", SubmarineDroneItem::new);

        /**
         * Deploys a {@code BiologicalDetectorEntity}, and takes one back: hitting a deployed
         * detector returns this item.
         */
        public static final DeferredItem<BiologicalDetectorItem> BIOLOGICAL_DETECTOR = ITEMS.registerItem(
                        "biological_detector", BiologicalDetectorItem::new);

        /** Points at a tagged marker, and lets the player choose which one. */
        public static final DeferredItem<SubmarineCompassItem> SUBMARINE_COMPASS = ITEMS.registerItem(
                        "submarine_compass", SubmarineCompassItem::new);

        /**
         * Takes over a submarine drone: press it against a free drone to bind, against a bound one
         * to let it go.
         */
        public static final DeferredItem<SubmarineDroneControllerItem> SUBMARINE_DRONE_CONTROLLER = ITEMS
                        .registerItem("submarine_drone_controller", SubmarineDroneControllerItem::new);

        public static final DeferredItem<DivingEquipmentItem> IRON_OXYGEN_TANK = tankItem("iron_oxygen_tank", 250, 5);
        public static final DeferredItem<DivingEquipmentItem> WOOD_OXYGEN_TANK = tankItem("wood_oxygen_tank", 59, 3);
        public static final DeferredItem<Item> SHARK_SKIN = ITEMS.registerSimpleItem("shark_skin");
        public static final DeferredItem<DivingEquipmentItem> SHARK_FLIPPERS = flippersItem("shark_flippers", 250,
                        1.20F);
        public static final DeferredItem<DivingEquipmentItem> WOOD_FLIPPERS = flippersItem("wood_flippers", 59, 1.08F);
        public static final DeferredItem<DivingEquipmentItem> CORAL_FLIPPERS = flippersItem("coral_flippers", 131,
                        1.12F);
        public static final DeferredItem<DivingEquipmentItem> SHELL_OXYGEN_TANK = tankItem("shell_oxygen_tank", 131,
                        4);
        public static final DeferredItem<DivingEquipmentItem> HARD_SHELL_OXYGEN_TANK = tankItem(
                        "hard_shell_oxygen_tank",
                        250, 6);
        public static final DeferredItem<DivingEquipmentItem> SHELL_FLIPPERS = flippersItem("shell_flippers", 131,
                        1.12F);
        public static final DeferredItem<DivingEquipmentItem> HARD_SHELL_FLIPPERS = flippersItem("hard_shell_flippers",
                        250, 1.24F);

        // Crafted and magical materials
        public static final DeferredItem<Item> MAGIC_BARNACLES = ITEMS.registerSimpleItem("magic_barnacles");
        public static final DeferredItem<Item> LUMINOUS_CUBE = ITEMS.registerSimpleItem("luminous_cube");
        public static final DeferredItem<Item> AEROGEL = ITEMS.registerSimpleItem("aerogel");
        public static final DeferredItem<Item> MARINE_ALLOY = ITEMS.registerSimpleItem("marine_alloy");

        // Marine alloy tier: between iron (250 uses, 6.0 speed, 2.0 dmg) and diamond
        // (1561, 8.0, 3.0)
        private static final Tier MARINE_ALLOY_TIER = new SimpleTier(
                        BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
                        750, 7.0F, 2.5F, 12,
                        () -> Ingredient.of(MARINE_ALLOY.get()));

        public static final DeferredItem<DivingEquipmentItem> MARINE_ALLOY_OXYGEN_TANK = tankItem(
                        "marine_alloy_oxygen_tank", 500, 10);
        public static final DeferredItem<DivingEquipmentItem> MARINE_ALLOY_FLIPPERS = flippersItem(
                        "marine_alloy_flippers",
                        500, 1.32F);

        // Masks — regenBonus scales with material tier; narcosisResistance raises
        // the pressure threshold before narcosis triggers (pressure units 0-1;
        // ~0.008 per block depth in the default Overworld).
        public static final DeferredItem<DivingEquipmentItem> IRON_MASK = maskItem("iron_mask", 250, 2, 0.08F);
        public static final DeferredItem<DivingEquipmentItem> CORAL_MASK = maskItem("coral_mask", 131, 1, 0.04F);
        public static final DeferredItem<DivingEquipmentItem> SHELL_MASK = maskItem("shell_mask", 131, 2, 0.08F);
        public static final DeferredItem<DivingEquipmentItem> HARD_SHELL_MASK = maskItem("hard_shell_mask", 250, 3,
                        0.12F);
        public static final DeferredItem<DivingEquipmentItem> MARINE_ALLOY_MASK = maskItem("marine_alloy_mask", 500, 5,
                        0.20F);
        public static final DeferredItem<DivingEquipmentItem> ANGLERFISH_MASK = maskItem("anglerfish_mask", 350, 6,
                        0.24F);
        public static final DeferredItem<DivingEquipmentItem> ENDER_MASK = maskItem("ender_mask", 500, 8, 0.32F);
        public static final DeferredItem<DivingEquipmentItem> SLIME_MASK = maskItem("slime_mask", 131, 2, 0.08F);
        public static final DeferredItem<Item> MARINE_ALLOY_AXE = axeItem("marine_alloy_axe", MARINE_ALLOY_TIER);
        public static final DeferredItem<Item> MARINE_ALLOY_PICKAXE = pickaxeItem("marine_alloy_pickaxe",
                        MARINE_ALLOY_TIER);
        public static final DeferredItem<Item> MARINE_ALLOY_SHOVEL = shovelItem("marine_alloy_shovel",
                        MARINE_ALLOY_TIER);
        public static final DeferredItem<Item> MARINE_ALLOY_HOE = hoeItem("marine_alloy_hoe", MARINE_ALLOY_TIER);
        // Copper tier (between stone and iron)
        private static final Tier COPPER_TIER = new SimpleTier(
                        BlockTags.INCORRECT_FOR_IRON_TOOL,
                        190, 5.0F, 1.5F, 10,
                        () -> Ingredient.of(Items.COPPER_INGOT));

        // Harpoons
        public static final DeferredItem<HarpoonItem> WOOD_HARPOON = ITEMS.registerItem("wood_harpoon",
                        props -> new HarpoonItem(props.durability(Tiers.WOOD.getUses()).stacksTo(1), 6.0F));
        public static final DeferredItem<HarpoonItem> STONE_HARPOON = ITEMS.registerItem("stone_harpoon",
                        props -> new HarpoonItem(props.durability(Tiers.STONE.getUses()).stacksTo(1), 7.5F));
        public static final DeferredItem<HarpoonItem> COPPER_HARPOON = ITEMS.registerItem("copper_harpoon",
                        props -> new HarpoonItem(props.durability(COPPER_TIER.getUses()).stacksTo(1), 8.25F));
        public static final DeferredItem<HarpoonItem> IRON_HARPOON = ITEMS.registerItem("iron_harpoon",
                        props -> new HarpoonItem(props.durability(Tiers.IRON.getUses()).stacksTo(1), 9.0F));
        public static final DeferredItem<HarpoonItem> GOLD_HARPOON = ITEMS.registerItem("gold_harpoon",
                        props -> new HarpoonItem(props.durability(Tiers.GOLD.getUses()).stacksTo(1), 6.0F));
        public static final DeferredItem<HarpoonItem> DIAMOND_HARPOON = ITEMS.registerItem("diamond_harpoon",
                        props -> new HarpoonItem(props.durability(Tiers.DIAMOND.getUses()).stacksTo(1), 10.5F));
        public static final DeferredItem<HarpoonItem> NETHERITE_HARPOON = ITEMS.registerItem("netherite_harpoon",
                        props -> new HarpoonItem(props.durability(Tiers.NETHERITE.getUses()).stacksTo(1)
                                        .fireResistant(), 12.0F));
        public static final DeferredItem<HarpoonItem> CORAL_HARPOON = ITEMS.registerItem("coral_harpoon",
                        props -> new HarpoonItem(props.durability(Tiers.STONE.getUses()).stacksTo(1), 7.5F));
        public static final DeferredItem<HarpoonItem> SHELL_HARPOON = ITEMS.registerItem("shell_harpoon",
                        props -> new HarpoonItem(props.durability(Tiers.STONE.getUses()).stacksTo(1), 8.25F));
        public static final DeferredItem<HarpoonItem> HARD_SHELL_HARPOON = ITEMS.registerItem("hard_shell_harpoon",
                        props -> new HarpoonItem(props.durability(Tiers.IRON.getUses()).stacksTo(1), 9.0F));
        public static final DeferredItem<HarpoonItem> MARINE_ALLOY_HARPOON = ITEMS.registerItem("marine_alloy_harpoon",
                        props -> new HarpoonItem(props.durability(MARINE_ALLOY_TIER.getUses()).stacksTo(1), 10.5F));

        public static final DeferredItem<Item> LIGHTNING_PEARL = ITEMS.registerSimpleItem("lightning_pearl");
        public static final DeferredItem<Item> ORGANIC_MATTER = ITEMS.registerSimpleItem("organic_matter");
        public static final DeferredItem<Item> KELP = ITEMS.registerSimpleItem("kelp");
        public static final DeferredItem<Item> SUSPICIOUS_FANG = ITEMS.registerSimpleItem("suspicious_fang");
        public static final DeferredItem<Item> STRANGE_FRAGMENTS = ITEMS.registerSimpleItem("strange_fragments");
        public static final DeferredItem<Item> CORAL_FRAGMENTS = ITEMS.registerSimpleItem("coral_fragments");
        public static final DeferredItem<Item> CORAL_STICK = ITEMS.registerSimpleItem("coral_stick");
        public static final DeferredItem<Item> VISCOUS_TISSUE = ITEMS.registerSimpleItem("viscous_tissue");
        public static final DeferredItem<Item> ELASTIC_BIOMASS = ITEMS.registerSimpleItem("elastic_biomass");
        public static final DeferredItem<Item> YELLOW_LAMP_FRUIT = ITEMS.registerSimpleItem("yellow_lamp_fruit");
        public static final DeferredItem<Item> BLUE_LAMP_FRUIT = ITEMS.registerSimpleItem("blue_lamp_fruit");
        public static final DeferredItem<Item> EYE_OF_THE_ABYSS = ITEMS.registerSimpleItem("eye_of_the_abyss");
        public static final DeferredItem<Item> ESSENCE_OF_THE_ABYSS = ITEMS.registerSimpleItem("essence_of_the_abyss");
        public static final DeferredItem<Item> ESSENCE_OF_THE_EUPHORIA = ITEMS.registerSimpleItem(
                        "essence_of_the_euphoria");
        public static final DeferredItem<Item> ESSENCE_OF_THE_FEAR = ITEMS.registerSimpleItem("essence_of_the_fear");
        public static final DeferredItem<Item> SHELL = ITEMS.registerSimpleItem("shell");
        public static final DeferredItem<Item> HARD_SHELL = ITEMS.registerSimpleItem("hard_shell");
        public static final DeferredItem<Item> TRANSPARENT_TISSUE = ITEMS.registerSimpleItem("transparent_tissue");
        public static final DeferredItem<Item> HARD_RIB = ITEMS.registerSimpleItem("hard_rib");
        public static final DeferredItem<Item> RED_JELLY = ITEMS.registerSimpleItem("red_jelly");
        public static final DeferredItem<Item> WHITE_JELLY = ITEMS.registerSimpleItem("white_jelly");
        public static final DeferredItem<Item> LIGHT_CYAN_JELLY = ITEMS.registerSimpleItem("light_cyan_jelly");
        public static final DeferredItem<Item> GOLDEN_JELLY = ITEMS.registerSimpleItem("golden_jelly");
        public static final DeferredItem<Item> RING_RIB = ITEMS.registerSimpleItem("ring_rib");
        public static final DeferredItem<Item> ROTTEN_TISSUE = ITEMS.registerSimpleItem("rotten_tissue");
        public static final DeferredItem<Item> SPRING = ITEMS.registerSimpleItem("spring");

        // Scoop nets — maxSize, extraSize
        public static final DeferredItem<ScoopNetItem> SCOOP_NET = ITEMS.registerItem("scoop_net",
                props -> new ScoopNetItem(props.stacksTo(1).durability(32), 1, 0));
        public static final DeferredItem<ScoopNetItem> MEDIUM_SCOOP_NET = ITEMS.registerItem("medium_scoop_net",
                props -> new ScoopNetItem(props.stacksTo(1).durability(64), 2, 0));
        public static final DeferredItem<ScoopNetItem> BIG_SCOOP_NET = ITEMS.registerItem("big_scoop_net",
                props -> new ScoopNetItem(props.stacksTo(1).durability(128), 4, 0));
        public static final DeferredItem<ScoopNetItem> LARGE_SCOOP_NET = ITEMS.registerItem("large_scoop_net",
                props -> new ScoopNetItem(props.stacksTo(1).durability(256), 6, 0));

        // Tool sets
        public static final DeferredItem<Item> CORAL_AXE = axeItem("coral_axe", Tiers.STONE);
        public static final DeferredItem<Item> CORAL_PICKAXE = pickaxeItem("coral_pickaxe", Tiers.STONE);
        public static final DeferredItem<Item> CORAL_SHOVEL = shovelItem("coral_shovel", Tiers.STONE);
        public static final DeferredItem<Item> CORAL_HOE = hoeItem("coral_hoe", Tiers.STONE);
        public static final DeferredItem<Item> SHELL_AXE = axeItem("shell_axe", Tiers.STONE);
        public static final DeferredItem<Item> SHELL_PICKAXE = pickaxeItem("shell_pickaxe", Tiers.STONE);
        public static final DeferredItem<Item> SHELL_SHOVEL = shovelItem("shell_shovel", Tiers.STONE);
        public static final DeferredItem<Item> SHELL_HOE = hoeItem("shell_hoe", Tiers.STONE);
        public static final DeferredItem<Item> HARD_SHELL_AXE = axeItem("hard_shell_axe", Tiers.IRON);
        public static final DeferredItem<Item> HARD_SHELL_PICKAXE = pickaxeItem("hard_shell_pickaxe", Tiers.IRON);
        public static final DeferredItem<Item> HARD_SHELL_SHOVEL = shovelItem("hard_shell_shovel", Tiers.IRON);
        public static final DeferredItem<Item> HARD_SHELL_HOE = hoeItem("hard_shell_hoe", Tiers.IRON);

        // Coral block items (log-like blocks)
        public static final DeferredItem<BlockItem> RED_CORAL_BLOCK = blockItem("red_coral_block",
                        BlockRegistry.RED_CORAL_BLOCK);
        public static final DeferredItem<BlockItem> BLUE_CORAL_BLOCK = blockItem("blue_coral_block",
                        BlockRegistry.BLUE_CORAL_BLOCK);
        public static final DeferredItem<BlockItem> BLUE_SMOOTH_CORAL_BLOCK = blockItem(
                        "blue_smooth_coral_block",
                        BlockRegistry.BLUE_SMOOTH_CORAL_BLOCK);
        public static final DeferredItem<BlockItem> BLUE_CORAL_BRICKS = blockItem("blue_coral_bricks",
                        BlockRegistry.BLUE_CORAL_BRICKS);
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
        public static final DeferredItem<BlockItem> SHELL_BLOCK = blockItem("shell_block",
                        BlockRegistry.SHELL_BLOCK);
        public static final DeferredItem<BlockItem> SHELL_BRICKS = blockItem("shell_bricks",
                        BlockRegistry.SHELL_BRICKS);
        public static final DeferredItem<BlockItem> HARD_SHELL_BLOCK = blockItem("hard_shell_block",
                        BlockRegistry.HARD_SHELL_BLOCK);
        public static final DeferredItem<BlockItem> HARD_SHELL_BRICKS = blockItem("hard_shell_bricks",
                        BlockRegistry.HARD_SHELL_BRICKS);
        public static final DeferredItem<BlockItem> POLISHED_HARD_SHELL_BLOCK = blockItem(
                        "polished_hard_shell_block",
                        BlockRegistry.POLISHED_HARD_SHELL_BLOCK);
        public static final DeferredItem<BlockItem> HARD_SHELL_FRAME = blockItem("hard_shell_frame",
                        BlockRegistry.HARD_SHELL_FRAME);
        public static final DeferredItem<BlockItem> CORAL_SAND = blockItem("coral_sand",
                        BlockRegistry.CORAL_SAND);
        public static final DeferredItem<BlockItem> NUTRIENT_RICH_MUD = blockItem("nutrient_rich_mud",
                        BlockRegistry.NUTRIENT_RICH_MUD);
        public static final DeferredItem<BlockItem> DROOPING_SEAWEED = blockItem("drooping_seaweed",
                        BlockRegistry.DROOPING_SEAWEED);
        public static final DeferredItem<BlockItem> SHALE = blockItem("shale",
                        BlockRegistry.SHALE);
        public static final DeferredItem<BlockItem> LIMESTONE = blockItem("limestone",
                        BlockRegistry.LIMESTONE);

        public static final DeferredItem<BlockItem> HALITE_CRUST = blockItem("halite_crust",
                        BlockRegistry.HALITE_CRUST);
        public static final DeferredItem<BlockItem> HALITE_PIPE = blockItem("halite_pipe",
                        BlockRegistry.HALITE_PIPE);
        public static final DeferredItem<BlockItem> VARVE_SHALE = blockItem("varve_shale",
                        BlockRegistry.VARVE_SHALE);
        public static final DeferredItem<BlockItem> BRINE_MIRROR = blockItem("brine_mirror",
                        BlockRegistry.BRINE_MIRROR);
        public static final DeferredItem<BlockItem> CALCITE_QUILL = blockItem("calcite_quill",
                        BlockRegistry.CALCITE_QUILL);
        public static final DeferredItem<BlockItem> HALITE_ROSETTE = blockItem("halite_rosette",
                        BlockRegistry.HALITE_ROSETTE);
        public static final DeferredItem<BlockItem> SALT_FRINGE = blockItem("salt_fringe",
                        BlockRegistry.SALT_FRINGE);
        // Brine Mirror Gorge enrichment block items
        public static final DeferredItem<BlockItem> HOPPER_HALITE = blockItem("hopper_halite",
                        BlockRegistry.HOPPER_HALITE);
        public static final DeferredItem<BlockItem> HALITE_DRUSE = blockItem("halite_druse",
                        BlockRegistry.HALITE_DRUSE);
        public static final DeferredItem<BlockItem> GYPSUM_BLADE = blockItem("gypsum_blade",
                        BlockRegistry.GYPSUM_BLADE);
        public static final DeferredItem<BlockItem> SYLVITE_CRUST = blockItem("sylvite_crust",
                        BlockRegistry.SYLVITE_CRUST);
        public static final DeferredItem<BlockItem> MIRROR_FLAKE = blockItem("mirror_flake",
                        BlockRegistry.MIRROR_FLAKE);
        public static final DeferredItem<BlockItem> GYPSUM_ROSE = blockItem("gypsum_rose",
                        BlockRegistry.GYPSUM_ROSE);
        // Brimstone Caldera block items
        public static final DeferredItem<BlockItem> VOLCANIC_BASALT = blockItem("volcanic_basalt",
                        BlockRegistry.VOLCANIC_BASALT);
        public static final DeferredItem<BlockItem> SCORIA = blockItem("scoria",
                        BlockRegistry.SCORIA);
        public static final DeferredItem<BlockItem> PILLOW_BASALT = blockItem("pillow_basalt",
                        BlockRegistry.PILLOW_BASALT);
        public static final DeferredItem<BlockItem> VOLCANIC_AGGLOMERATE = blockItem("volcanic_agglomerate",
                        BlockRegistry.VOLCANIC_AGGLOMERATE);
        public static final DeferredItem<BlockItem> PUMICE = blockItem("pumice",
                        BlockRegistry.PUMICE);
        public static final DeferredItem<BlockItem> OBSIDIAN_GLASS = blockItem("obsidian_glass",
                        BlockRegistry.OBSIDIAN_GLASS);
        public static final DeferredItem<BlockItem> ACID_ETCHED_BASALT = blockItem("acid_etched_basalt",
                        BlockRegistry.ACID_ETCHED_BASALT);
        public static final DeferredItem<BlockItem> SULFUR_CRUST = blockItem("sulfur_crust",
                        BlockRegistry.SULFUR_CRUST);
        public static final DeferredItem<BlockItem> SINTER = blockItem("sinter",
                        BlockRegistry.SINTER);
        public static final DeferredItem<BlockItem> VENT_CHIMNEY = blockItem("vent_chimney",
                        BlockRegistry.VENT_CHIMNEY);
        public static final DeferredItem<BlockItem> VOLCANIC_ASH = blockItem("volcanic_ash",
                        BlockRegistry.VOLCANIC_ASH);
        public static final DeferredItem<BlockItem> ASH_LAYER = blockItem("ash_layer",
                        BlockRegistry.ASH_LAYER);
        public static final DeferredItem<BlockItem> SULFUR_MOSS = blockItem("sulfur_moss",
                        BlockRegistry.SULFUR_MOSS);
        public static final DeferredItem<BlockItem> SULFUR_CRYSTAL = blockItem("sulfur_crystal",
                        BlockRegistry.SULFUR_CRYSTAL);
        public static final DeferredItem<BlockItem> FIREBLOOM = blockItem("firebloom",
                        BlockRegistry.FIREBLOOM);
        public static final DeferredItem<BlockItem> SULFUR_STALACTITE = blockItem("sulfur_stalactite",
                        BlockRegistry.SULFUR_STALACTITE);
        public static final DeferredItem<BlockItem> EMBER_KELP = blockItem("ember_kelp",
                        BlockRegistry.EMBER_KELP);
        public static final DeferredItem<BlockItem> FUMAROLE = blockItem("fumarole",
                        BlockRegistry.FUMAROLE);
        public static final DeferredItem<BlockItem> THERMOPHILIC_MAT_GOLD = blockItem("thermophilic_mat_gold",
                        BlockRegistry.THERMOPHILIC_MAT_GOLD);
        public static final DeferredItem<BlockItem> THERMOPHILIC_MAT_RUST = blockItem("thermophilic_mat_rust",
                        BlockRegistry.THERMOPHILIC_MAT_RUST);
        public static final DeferredItem<BlockItem> THERMOPHILIC_MAT_OLIVE = blockItem("thermophilic_mat_olive",
                        BlockRegistry.THERMOPHILIC_MAT_OLIVE);
        // 嗜热菌样本: the mats grow in the world and stay out of the creative inventory;
        // these samples are what a player plants them with.
        public static final DeferredItem<ThermophilicSampleItem> THERMOPHILIC_SAMPLE_GOLD = ITEMS.registerItem(
                        "thermophilic_sample_gold",
                        props -> new ThermophilicSampleItem(BlockRegistry.THERMOPHILIC_MAT_GOLD.get(), props));
        public static final DeferredItem<ThermophilicSampleItem> THERMOPHILIC_SAMPLE_RUST = ITEMS.registerItem(
                        "thermophilic_sample_rust",
                        props -> new ThermophilicSampleItem(BlockRegistry.THERMOPHILIC_MAT_RUST.get(), props));
        public static final DeferredItem<ThermophilicSampleItem> THERMOPHILIC_SAMPLE_OLIVE = ITEMS.registerItem(
                        "thermophilic_sample_olive",
                        props -> new ThermophilicSampleItem(BlockRegistry.THERMOPHILIC_MAT_OLIVE.get(), props));
        // Crystal Nest (水晶巢) block items
        public static final DeferredItem<BlockItem> CRYSTAL_NEST_STONE = blockItem("crystal_nest_stone",
                        BlockRegistry.CRYSTAL_NEST_STONE);
        public static final DeferredItem<BlockItem> CRYSTAL_DRUSE = blockItem("crystal_druse",
                        BlockRegistry.CRYSTAL_DRUSE);
        public static final DeferredItem<BlockItem> CRYSTAL_COLUMN = blockItem("crystal_column",
                        BlockRegistry.CRYSTAL_COLUMN);
        public static final DeferredItem<BlockItem> WHITE_CRYSTAL_CLUSTER = blockItem("white_crystal_cluster",
                        BlockRegistry.WHITE_CRYSTAL_CLUSTER);
        public static final DeferredItem<BlockItem> ROSE_CRYSTAL_CLUSTER = blockItem("rose_crystal_cluster",
                        BlockRegistry.ROSE_CRYSTAL_CLUSTER);
        public static final DeferredItem<BlockItem> AMETHYST_CRYSTAL_CLUSTER = blockItem(
                        "amethyst_crystal_cluster", BlockRegistry.AMETHYST_CRYSTAL_CLUSTER);
        public static final DeferredItem<BlockItem> AQUA_CRYSTAL_CLUSTER = blockItem("aqua_crystal_cluster",
                        BlockRegistry.AQUA_CRYSTAL_CLUSTER);
        public static final DeferredItem<BlockItem> SMOKY_CRYSTAL_CLUSTER = blockItem("smoky_crystal_cluster",
                        BlockRegistry.SMOKY_CRYSTAL_CLUSTER);
        public static final DeferredItem<BlockItem> RESONANT_CRYSTAL_CLUSTER = blockItem(
                        "resonant_crystal_cluster", BlockRegistry.RESONANT_CRYSTAL_CLUSTER);
        public static final DeferredItem<BlockItem> LIFE_GEM_CLUSTER = blockItem("life_gem_cluster",
                        BlockRegistry.LIFE_GEM_CLUSTER);
        public static final DeferredItem<BlockItem> ALGAE_MAT = blockItem("algae_mat",
                        BlockRegistry.ALGAE_MAT);
        public static final DeferredItem<BlockItem> ALGAE_TUFT = blockItem("algae_tuft",
                        BlockRegistry.ALGAE_TUFT);
        public static final DeferredItem<BlockItem> CRYSTAL_SPROUT = blockItem("crystal_sprout",
                        BlockRegistry.CRYSTAL_SPROUT);
        public static final DeferredItem<BlockItem> CRYSTAL_FRINGE = blockItem("crystal_fringe",
                        BlockRegistry.CRYSTAL_FRINGE);
        public static final DeferredItem<BlockItem> SEAWEED = blockItem("seaweed",
                        BlockRegistry.SEAWEED);
        public static final DeferredItem<BlockItem> SEAWEED_FRUIT = blockItem("seaweed_fruit",
                        BlockRegistry.SEAWEED_FRUIT);
        public static final DeferredItem<BlockItem> SEAWEED_STEM = blockItem("seaweed_stem",
                        BlockRegistry.SEAWEED_STEM);
        public static final DeferredItem<BlockItem> MUD_BLOOM = blockItem("mud_bloom",
                        BlockRegistry.MUD_BLOOM);
        public static final DeferredItem<BlockItem> BEAN_KELP = blockItem("bean_kelp",
                        BlockRegistry.BEAN_KELP);
        public static final DeferredItem<BlockItem> GLOW_FUNGUS = blockItem("glow_fungus",
                        BlockRegistry.GLOW_FUNGUS);
        public static final DeferredItem<BlockItem> GLOW_FUNGUS_AMBER = blockItem("glow_fungus_amber",
                        BlockRegistry.GLOW_FUNGUS_AMBER);
        public static final DeferredItem<BlockItem> GLOW_FUNGUS_VIOLET = blockItem("glow_fungus_violet",
                        BlockRegistry.GLOW_FUNGUS_VIOLET);
        public static final DeferredItem<BlockItem> GLOW_MUSHROOM_STEM = blockItem("glow_mushroom_stem",
                        BlockRegistry.GLOW_MUSHROOM_STEM);
        public static final DeferredItem<BlockItem> GLOW_MUSHROOM_CAP = blockItem("glow_mushroom_cap",
                        BlockRegistry.GLOW_MUSHROOM_CAP);
        public static final DeferredItem<BlockItem> GLOW_MUSHROOM_INSIDE = blockItem("glow_mushroom_inside",
                        BlockRegistry.GLOW_MUSHROOM_INSIDE);
        // Jelly blocks
        public static final DeferredItem<BlockItem> LIGHT_RED_JELLY_BLOCK = blockItem("light_red_jelly_block",
                        BlockRegistry.LIGHT_RED_JELLY_BLOCK);
        public static final DeferredItem<BlockItem> LIGHT_CYAN_JELLY_BLOCK = blockItem("light_cyan_jelly_block",
                        BlockRegistry.LIGHT_CYAN_JELLY_BLOCK);
        public static final DeferredItem<BlockItem> WHITE_JELLY_BLOCK = blockItem("white_jelly_block",
                        BlockRegistry.WHITE_JELLY_BLOCK);
        public static final DeferredItem<BlockItem> LIGHT_GOLDEN_JELLY_BLOCK = blockItem("light_golden_jelly_block",
                        BlockRegistry.LIGHT_GOLDEN_JELLY_BLOCK);
        // Seaweed-wrapped jelly
        public static final DeferredItem<BlockItem> LIGHT_RED_JELLY_BLOCK_SEAWEED = blockItem("light_red_jelly_block_seaweed",
                        BlockRegistry.LIGHT_RED_JELLY_BLOCK_SEAWEED);
        public static final DeferredItem<BlockItem> LIGHT_CYAN_JELLY_BLOCK_SEAWEED = blockItem("light_cyan_jelly_block_seaweed",
                        BlockRegistry.LIGHT_CYAN_JELLY_BLOCK_SEAWEED);
        public static final DeferredItem<BlockItem> WHITE_JELLY_BLOCK_SEAWEED = blockItem("white_jelly_block_seaweed",
                        BlockRegistry.WHITE_JELLY_BLOCK_SEAWEED);
        public static final DeferredItem<BlockItem> LIGHT_GOLDEN_JELLY_BLOCK_SEAWEED = blockItem("light_golden_jelly_block_seaweed",
                        BlockRegistry.LIGHT_GOLDEN_JELLY_BLOCK_SEAWEED);
        // Vanilla coral slabs
        public static final DeferredItem<BlockItem> TUBE_CORAL_SLAB = blockItem("tube_coral_slab",
                        BlockRegistry.TUBE_CORAL_SLAB);
        public static final DeferredItem<BlockItem> BRAIN_CORAL_SLAB = blockItem("brain_coral_slab",
                        BlockRegistry.BRAIN_CORAL_SLAB);
        public static final DeferredItem<BlockItem> BUBBLE_CORAL_SLAB = blockItem("bubble_coral_slab",
                        BlockRegistry.BUBBLE_CORAL_SLAB);
        public static final DeferredItem<BlockItem> FIRE_CORAL_SLAB = blockItem("fire_coral_slab",
                        BlockRegistry.FIRE_CORAL_SLAB);
        public static final DeferredItem<BlockItem> HORN_CORAL_SLAB = blockItem("horn_coral_slab",
                        BlockRegistry.HORN_CORAL_SLAB);
        public static final DeferredItem<BlockItem> DEAD_TUBE_CORAL_SLAB = blockItem("dead_tube_coral_slab",
                        BlockRegistry.DEAD_TUBE_CORAL_SLAB);
        public static final DeferredItem<BlockItem> DEAD_BRAIN_CORAL_SLAB = blockItem("dead_brain_coral_slab",
                        BlockRegistry.DEAD_BRAIN_CORAL_SLAB);
        public static final DeferredItem<BlockItem> DEAD_BUBBLE_CORAL_SLAB = blockItem("dead_bubble_coral_slab",
                        BlockRegistry.DEAD_BUBBLE_CORAL_SLAB);
        public static final DeferredItem<BlockItem> DEAD_FIRE_CORAL_SLAB = blockItem("dead_fire_coral_slab",
                        BlockRegistry.DEAD_FIRE_CORAL_SLAB);
        public static final DeferredItem<BlockItem> DEAD_HORN_CORAL_SLAB = blockItem("dead_horn_coral_slab",
                        BlockRegistry.DEAD_HORN_CORAL_SLAB);
        public static final DeferredItem<BlockItem> GAS_PIPE = blockItem("gas_pipe",
                        BlockRegistry.GAS_PIPE);
        public static final DeferredItem<FishingNetBlockItem> FISHING_NET = ITEMS.registerItem("fishing_net",
                props -> new FishingNetBlockItem(BlockRegistry.FISHING_NET.get(), props));
        public static final DeferredItem<BlockItem> PLEXIGLASS = ITEMS.registerItem("plexiglass",
                props -> new BlockItem(BlockRegistry.PLEXIGLASS.get(), props));
        public static final DeferredItem<BlockItem> DISSECTION_TABLE = blockItem("dissection_table",
                        BlockRegistry.DISSECTION_TABLE);
        public static final DeferredItem<BlockItem> PHOTO_RINSING_BASIN = blockItem("photo_rinsing_basin",
                        BlockRegistry.PHOTO_RINSING_BASIN);
        public static final DeferredItem<BlockItem> INVESTIGATION_BOARD = blockItem("investigation_board",
                        BlockRegistry.INVESTIGATION_BOARD);
        public static final DeferredItem<GasFlowMeterItem> GAS_FLOW_METER = ITEMS.registerItem("gas_flow_meter",
                        properties -> new GasFlowMeterItem(properties.stacksTo(1)));
        public static final DeferredItem<HandheldSearchlightItem> HANDHELD_SEARCHLIGHT = ITEMS.registerItem(
                        "handheld_searchlight", properties -> new HandheldSearchlightItem(properties));
        public static final DeferredItem<ShellCameraItem> SHELL_CAMERA = ITEMS.registerItem("shell_camera",
                        ShellCameraItem::new);
        public static final DeferredItem<Item> PHOTOSENSITIVE_FILM = ITEMS.registerSimpleItem("photosensitive_film");
        public static final DeferredItem<Item> BRINE_DEVELOPING_SALTS = ITEMS.registerSimpleItem(
                        "brine_developing_salts");
        public static final DeferredItem<ExposedFilmItem> EXPOSED_FILM = ITEMS.registerItem("exposed_film",
                        ExposedFilmItem::new);
        public static final DeferredItem<DevelopedPhotoItem> DEVELOPED_PHOTO = ITEMS.registerItem("developed_photo",
                        DevelopedPhotoItem::new);
        public static final DeferredItem<Item> BUBBLE_GUN = ITEMS.registerItem("bubble_gun",
                        properties -> new BubbleGunItem(properties.durability(60).stacksTo(1)));

        public static final DeferredItem<DeferredSpawnEggItem> OCTOPUS_SPAWN_EGG = spawnEgg("octopus_spawn_egg",
                        EntityRegistry.OCTOPUS, 0x7A6250, 0x261B17);
        public static final DeferredItem<DeferredSpawnEggItem> SARDINE_SPAWN_EGG = spawnEgg("sardine_spawn_egg",
                        EntityRegistry.SARDINE, 0x7BA4C6, 0x25435F);
        public static final DeferredItem<DeferredSpawnEggItem> SALT_CRUST_SPAWN_EGG = spawnEgg("salt_crust_spawn_egg",
                        EntityRegistry.SALT_CRUST, 0xF2E8DC, 0xE8A9B4);
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
        public static final DeferredItem<DeferredSpawnEggItem> MANTA_RAY_SPAWN_EGG = spawnEgg("manta_ray_spawn_egg",
                        EntityRegistry.MANTA_RAY, 0x28333D, 0x7CA8B8);
        public static final DeferredItem<DeferredSpawnEggItem> GIANT_OCTOPUS_TENTACLE_SPAWN_EGG = spawnEgg(
                        "giant_octopus_tentacle_spawn_egg",
                        EntityRegistry.GIANT_OCTOPUS_TENTACLE, 0x8E6B6B, 0xD7C9BA);
        public static final DeferredItem<DeferredSpawnEggItem> GIANT_ABYSS_WORM_SPAWN_EGG = spawnEgg(
                        "giant_abyss_worm_spawn_egg",
                        EntityRegistry.GIANT_ABYSS_WORM, 0x1A0A2E, 0x6A1E8C);

        public static final DeferredItem<DeferredSpawnEggItem> LIGHTING_WORM_SPAWN_EGG = spawnEgg(
                        "lighting_worm_spawn_egg",
                        EntityRegistry.LIGHTING_WORM, 0x50285A, 0xFFEB3C);

        public static final DeferredItem<DeferredSpawnEggItem> CREEPORPEDO_SPAWN_EGG = spawnEgg(
                        "creeporpedo_spawn_egg",
                        EntityRegistry.CREEPORPEDO, 0x358838, 0xE86B1C);

        public static final DeferredItem<DeferredSpawnEggItem> SWIRL_MAKER_SPAWN_EGG = spawnEgg(
                        "swirl_maker_spawn_egg",
                        EntityRegistry.SWIRL_MAKER, 0x889098, 0xC8B8A0);

        public static final DeferredItem<DeferredSpawnEggItem> GLOOMGAZER_SPAWN_EGG = spawnEgg(
                        "gloomgazer_spawn_egg",
                        EntityRegistry.GLOOMGAZER, 0x4B322D, 0xD5C8BE);

        public static final DeferredItem<DeferredSpawnEggItem> RADIOANEMONE_SPAWN_EGG = spawnEgg(
                        "radioanemone_spawn_egg",
                        EntityRegistry.RADIOANEMONE, 0x326428, 0x78B464);

        public static final DeferredItem<DeferredSpawnEggItem> OXYGEN_BREEDER_SPAWN_EGG = spawnEgg(
                        "oxygen_breeder_spawn_egg",
                        EntityRegistry.OXYGEN_BREEDER, 0x588232, 0xBEDCAA);

        public static final DeferredItem<DeferredSpawnEggItem> RED_JELLYFISH_SPAWN_EGG = spawnEgg(
                        "red_jellyfish_spawn_egg",
                        EntityRegistry.RED_JELLYFISH, 0xC32D46, 0xFAB4BE);

        public static final DeferredItem<DeferredSpawnEggItem> RINGFISH_SPAWN_EGG = spawnEgg(
                        "ringfish_spawn_egg",
                        EntityRegistry.RINGFISH, 0x8C9196, 0xDCE1E6);

        public static final DeferredItem<DeferredSpawnEggItem> TRIPOD_SPAWN_EGG = spawnEgg(
                        "tripod_spawn_egg",
                        EntityRegistry.TRIPOD, 0x805650, 0xD2B4A5);

        public static final DeferredItem<DeferredSpawnEggItem> BLUE_RINGED_WORMFISH_SPAWN_EGG = spawnEgg(
                        "blue_ringed_wormfish_spawn_egg",
                        EntityRegistry.BLUE_RINGED_WORMFISH, 0x193260, 0x78C8FF);

        public static final DeferredItem<DeferredSpawnEggItem> BLUE_JELLYFISH_SPAWN_EGG = spawnEgg(
                        "blue_jellyfish_spawn_egg",
                        EntityRegistry.BLUE_JELLYFISH, 0x1E64C8, 0xB4DCFF);

        public static final DeferredItem<DeferredSpawnEggItem> FLATFISH_SPAWN_EGG = spawnEgg(
                        "flatfish_spawn_egg",
                        EntityRegistry.FLATFISH, 0x6E6C4B, 0xC3B9A0);
        public static final DeferredItem<DeferredSpawnEggItem> MUD_SILVERFISH_SPAWN_EGG = spawnEgg(
                        "mud_silverfish_spawn_egg", EntityRegistry.MUD_SILVERFISH, 0x4B3A2C, 0x1E1713);
        public static final DeferredItem<DeferredSpawnEggItem> AMBUSH_FISH_SPAWN_EGG = spawnEgg(
                        "ambush_fish_spawn_egg", EntityRegistry.AMBUSH_FISH, 0x6B6250, 0x30281F);
        public static final DeferredItem<DeferredSpawnEggItem> GARDEN_EEL_SPAWN_EGG = spawnEgg(
                        "garden_eel_spawn_egg", EntityRegistry.GARDEN_EEL, 0x8A7A43, 0x3B4A2B);
        public static final DeferredItem<DeferredSpawnEggItem> HERMIT_CRAB_SPAWN_EGG = spawnEgg(
                        "hermit_crab_spawn_egg", EntityRegistry.HERMIT_CRAB, 0xA56D45, 0x59402C);
        public static final DeferredItem<DeferredSpawnEggItem> SEDIMENT_WORM_SPAWN_EGG = spawnEgg(
                        "sediment_worm_spawn_egg", EntityRegistry.SEDIMENT_WORM, 0x59463D, 0x2E241F);
        public static final DeferredItem<DeferredSpawnEggItem> HUMUS_JELLY_SPAWN_EGG = spawnEgg(
                        "humus_jelly_spawn_egg", EntityRegistry.HUMUS_JELLY, 0x4D663B, 0x26351F);
        public static final DeferredItem<DeferredSpawnEggItem> ANCIENT_NAUTILUS_SPAWN_EGG = spawnEgg(
                        "ancient_nautilus_spawn_egg", EntityRegistry.ANCIENT_NAUTILUS, 0xA89C83, 0x574A3C);

        public static final DeferredItem<DeferredSpawnEggItem> VAMPREY_SPAWN_EGG = spawnEgg(
                        "vamprey_spawn_egg",
                        EntityRegistry.VAMPREY, 0xD9D4C8, 0x827F77);

        public static final DeferredItem<DeferredSpawnEggItem> ORESUCKER_SPAWN_EGG = spawnEgg(
                        "oresucker_spawn_egg",
                        EntityRegistry.ORESUCKER, 0xC1F4EB, 0x708781);

        public static final DeferredItem<DeferredSpawnEggItem> FLAGELLONAUTILUS_SPAWN_EGG = spawnEgg(
                        "flagellonautilus_spawn_egg",
                        EntityRegistry.FLAGELLONAUTILUS, 0x685247, 0x3F2E27);

        public static final DeferredItem<DeferredSpawnEggItem> SKELETON_CARP_SPAWN_EGG = spawnEgg(
                        "skeleton_carp_spawn_egg",
                        EntityRegistry.SKELETON_CARP, 0xCECEC8, 0x807F7B);

        public static final DeferredItem<DeferredSpawnEggItem> GOLDEN_CARP_SPAWN_EGG = spawnEgg(
                        "golden_carp_spawn_egg",
                        EntityRegistry.GOLDEN_CARP, 0xF8CA31, 0x987416);

        public static final DeferredItem<DeferredSpawnEggItem> SILVER_CARP_SPAWN_EGG = spawnEgg(
                        "silver_carp_spawn_egg",
                        EntityRegistry.SILVER_CARP, 0xD8D5CA, 0x887E64);

        public static final DeferredItem<DeferredSpawnEggItem> GENTLEFISH_SPAWN_EGG = spawnEgg(
                        "gentlefish_spawn_egg",
                        EntityRegistry.GENTLEFISH, 0x9F6936, 0x5D3E1E);

        public static final DeferredItem<DeferredSpawnEggItem> SLIMMY_SPAWN_EGG = spawnEgg(
                        "slimmy_spawn_egg",
                        EntityRegistry.SLIMMY, 0x96FB74, 0x51963F);

        public static final DeferredItem<DeferredSpawnEggItem> IONFIN_SPAWN_EGG = spawnEgg(
                        "ionfin_spawn_egg",
                        EntityRegistry.IONFIN, 0xDAE1E5, 0x7F8486);

        public static final DeferredItem<DeferredSpawnEggItem> OPTICICHTHUS_SPAWN_EGG = spawnEgg(
                        "opticichthus_spawn_egg",
                        EntityRegistry.OPTICICHTHUS, 0x92999B, 0x4F5456);

        public static final DeferredItem<DeferredSpawnEggItem> GEMINI_JELLYFISH_SPAWN_EGG = spawnEgg(
                        "gemini_jellyfish_spawn_egg",
                        EntityRegistry.GEMINI_JELLYFISH, 0xEBEBEA, 0x89847F);

        public static final DeferredItem<DeferredSpawnEggItem> ECOFISH_SPAWN_EGG = spawnEgg(
                        "ecofish_spawn_egg",
                        EntityRegistry.ECOFISH, 0x217C36, 0x0F3D1C);

        public static final DeferredItem<DeferredSpawnEggItem> PALE_ABYSS_HYDRA_SPAWN_EGG = spawnEgg(
                        "pale_abyss_hydra_spawn_egg",
                        EntityRegistry.PALE_ABYSS_HYDRA, 0xF0DD8F, 0x8C8154);

        public static final DeferredItem<DeferredSpawnEggItem> THREE_HEADED_SHARK_SPAWN_EGG = spawnEgg(
                        "three_headed_shark_spawn_egg",
                        EntityRegistry.THREE_HEADED_SHARK, 0x6A7582, 0x3E444D);

        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOOD_TAB = tab("food",
                        COOKED_SARDINE, output -> {
                                output.accept(COOKED_OCTOPUS_SHREDS.get());
                                output.accept(COOKED_SARDINE.get());
                                output.accept(CRAB_MEAT.get());
                                output.accept(COOKED_CRAB_MEAT.get());
                                output.accept(WORM_MUSCLE.get());
                                output.accept(MUD_BEAN.get());
                                output.accept(COOKED_SHARK_FINS.get());
                                output.accept(FISHNUT.get());
                                output.accept(COOKED_FISHNUT.get());
                                output.accept(AIR_SOUP.get());
                                output.accept(AIR_SANDWICH.get());
                                output.accept(AIR_SAC.get());
                        });

        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATURES_TAB = tab("creatures",
                        OCTOPUS_SPAWN_EGG, output -> {
                                output.accept(OCTOPUS_SPAWN_EGG.get());
                                output.accept(SARDINE_SPAWN_EGG.get());
                                output.accept(SALT_CRUST_SPAWN_EGG.get());
                                output.accept(ANGLERFISH_SPAWN_EGG.get());
                                output.accept(ELECTROFISH_SPAWN_EGG.get());
                                output.accept(DONUTFISH_SPAWN_EGG.get());
                                output.accept(SPRINGFISH_SPAWN_EGG.get());
                                output.accept(ICERAIL_SPAWN_EGG.get());
                                output.accept(HELICOPRION_SPAWN_EGG.get());
                                output.accept(CATFISH_SPAWN_EGG.get());
                                output.accept(MANTA_RAY_SPAWN_EGG.get());
                                output.accept(GIANT_OCTOPUS_TENTACLE_SPAWN_EGG.get());
                                output.accept(GIANT_ABYSS_WORM_SPAWN_EGG.get());
                                output.accept(LIGHTING_WORM_SPAWN_EGG.get());
                                output.accept(CREEPORPEDO_SPAWN_EGG.get());
                                output.accept(SWIRL_MAKER_SPAWN_EGG.get());
                                output.accept(GLOOMGAZER_SPAWN_EGG.get());
                                output.accept(RADIOANEMONE_SPAWN_EGG.get());
                                output.accept(OXYGEN_BREEDER_SPAWN_EGG.get());
                                output.accept(RED_JELLYFISH_SPAWN_EGG.get());
                                output.accept(RINGFISH_SPAWN_EGG.get());
                                output.accept(TRIPOD_SPAWN_EGG.get());
                                output.accept(BLUE_RINGED_WORMFISH_SPAWN_EGG.get());
                                output.accept(BLUE_JELLYFISH_SPAWN_EGG.get());
                                output.accept(FLATFISH_SPAWN_EGG.get());
                                output.accept(VAMPREY_SPAWN_EGG.get());
                                output.accept(ORESUCKER_SPAWN_EGG.get());
                                output.accept(FLAGELLONAUTILUS_SPAWN_EGG.get());
                                output.accept(SKELETON_CARP_SPAWN_EGG.get());
                                output.accept(GOLDEN_CARP_SPAWN_EGG.get());
                                output.accept(SILVER_CARP_SPAWN_EGG.get());
                                output.accept(GENTLEFISH_SPAWN_EGG.get());
                                output.accept(SLIMMY_SPAWN_EGG.get());
                                output.accept(IONFIN_SPAWN_EGG.get());
                                output.accept(OPTICICHTHUS_SPAWN_EGG.get());
                                output.accept(GEMINI_JELLYFISH_SPAWN_EGG.get());
                                output.accept(ECOFISH_SPAWN_EGG.get());
                                output.accept(PALE_ABYSS_HYDRA_SPAWN_EGG.get());
                                output.accept(THREE_HEADED_SHARK_SPAWN_EGG.get());
                                output.accept(MUD_SILVERFISH_SPAWN_EGG.get());
                                output.accept(AMBUSH_FISH_SPAWN_EGG.get());
                                output.accept(GARDEN_EEL_SPAWN_EGG.get());
                                output.accept(HERMIT_CRAB_SPAWN_EGG.get());
                                output.accept(SEDIMENT_WORM_SPAWN_EGG.get());
                                output.accept(HUMUS_JELLY_SPAWN_EGG.get());
                                output.accept(ANCIENT_NAUTILUS_SPAWN_EGG.get());
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
                                output.accept(MAGIC_BARNACLES.get());
                                output.accept(LUMINOUS_CUBE.get());
                                output.accept(AEROGEL.get());
                                output.accept(MARINE_ALLOY.get());
                                output.accept(LIGHTNING_PEARL.get());
                                output.accept(ORGANIC_MATTER.get());
                                output.accept(KELP.get());
                                output.accept(SUSPICIOUS_FANG.get());
                                output.accept(STRANGE_FRAGMENTS.get());
                                output.accept(CORAL_FRAGMENTS.get());
                                output.accept(CORAL_STICK.get());
                                output.accept(VISCOUS_TISSUE.get());
                                output.accept(ELASTIC_BIOMASS.get());
                                output.accept(YELLOW_LAMP_FRUIT.get());
                                output.accept(BLUE_LAMP_FRUIT.get());
                                output.accept(EYE_OF_THE_ABYSS.get());
                                output.accept(ESSENCE_OF_THE_ABYSS.get());
                                output.accept(ESSENCE_OF_THE_EUPHORIA.get());
                                output.accept(ESSENCE_OF_THE_FEAR.get());
                                output.accept(SHELL.get());
                                output.accept(HARD_SHELL.get());
                                output.accept(TRANSPARENT_TISSUE.get());
                                output.accept(HARD_RIB.get());
                                output.accept(RED_JELLY.get());
                                output.accept(WHITE_JELLY.get());
                                output.accept(LIGHT_CYAN_JELLY.get());
                                output.accept(GOLDEN_JELLY.get());
                                output.accept(RING_RIB.get());
                                output.accept(ROTTEN_TISSUE.get());
                                output.accept(SPRING.get());
                                output.accept(THERMOPHILIC_SAMPLE_GOLD.get());
                                output.accept(THERMOPHILIC_SAMPLE_RUST.get());
                                output.accept(THERMOPHILIC_SAMPLE_OLIVE.get());
                                output.accept(PHOTOSENSITIVE_FILM.get());
                                output.accept(BRINE_DEVELOPING_SALTS.get());
                                output.accept(MUD_BALL.get());
                                output.accept(NUTRIENT_MUD_BALL.get());
                                output.accept(FOSSIL_FRAGMENT.get());
                                output.accept(HUMUS_GEL.get());
                                output.accept(ANCIENT_SHELL_FRAGMENT.get());
                        });

        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS_TAB = tab("tools",
                        BUBBLE_GUN, output -> {
                                output.accept(GAS_FLOW_METER.get());
                                output.accept(HANDHELD_SEARCHLIGHT.get());
                                output.accept(SHELL_CAMERA.get());
                                output.accept(DEVELOPED_PHOTO.get());
                                output.accept(CURSOR.get());
                                output.accept(SUBMARINE_COMPASS.get());
                                output.accept(SUBMARINE_DRONE.get());
                                output.accept(SUBMARINE_DRONE_CONTROLLER.get());
                                output.accept(BIOLOGICAL_DETECTOR.get());
                                output.accept(BUBBLE_GUN.get());
                                output.accept(WOOD_HARPOON.get());
                                output.accept(STONE_HARPOON.get());
                                output.accept(COPPER_HARPOON.get());
                                output.accept(IRON_HARPOON.get());
                                output.accept(GOLD_HARPOON.get());
                                output.accept(DIAMOND_HARPOON.get());
                                output.accept(NETHERITE_HARPOON.get());
                                output.accept(CORAL_HARPOON.get());
                                output.accept(SHELL_HARPOON.get());
                                output.accept(HARD_SHELL_HARPOON.get());
                                output.accept(MARINE_ALLOY_HARPOON.get());
                                output.accept(NOTEBOOK.get());
                                output.accept(SCOOP_NET.get());
                                output.accept(MEDIUM_SCOOP_NET.get());
                                output.accept(BIG_SCOOP_NET.get());
                                output.accept(LARGE_SCOOP_NET.get());
                                output.accept(CORAL_AXE.get());
                                output.accept(CORAL_PICKAXE.get());
                                output.accept(CORAL_SHOVEL.get());
                                output.accept(CORAL_HOE.get());
                                output.accept(SHELL_AXE.get());
                                output.accept(SHELL_PICKAXE.get());
                                output.accept(SHELL_SHOVEL.get());
                                output.accept(SHELL_HOE.get());
                                output.accept(HARD_SHELL_AXE.get());
                                output.accept(HARD_SHELL_PICKAXE.get());
                                output.accept(HARD_SHELL_SHOVEL.get());
                                output.accept(HARD_SHELL_HOE.get());
                                output.accept(MARINE_ALLOY_AXE.get());
                                output.accept(MARINE_ALLOY_PICKAXE.get());
                                output.accept(MARINE_ALLOY_SHOVEL.get());
                                output.accept(MARINE_ALLOY_HOE.get());
                        });

        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EQUIPMENT_TAB = tab("equipment",
                        IRON_OXYGEN_TANK, output -> {
                                output.accept(IRON_OXYGEN_TANK.get());
                                output.accept(WOOD_OXYGEN_TANK.get());
                                output.accept(SHELL_OXYGEN_TANK.get());
                                output.accept(HARD_SHELL_OXYGEN_TANK.get());
                                output.accept(MARINE_ALLOY_OXYGEN_TANK.get());
                                output.accept(SHARK_FLIPPERS.get());
                                output.accept(WOOD_FLIPPERS.get());
                                output.accept(CORAL_FLIPPERS.get());
                                output.accept(SHELL_FLIPPERS.get());
                                output.accept(HARD_SHELL_FLIPPERS.get());
                                output.accept(MARINE_ALLOY_FLIPPERS.get());
                                output.accept(IRON_MASK.get());
                                output.accept(CORAL_MASK.get());
                                output.accept(SHELL_MASK.get());
                                output.accept(HARD_SHELL_MASK.get());
                                output.accept(MARINE_ALLOY_MASK.get());
                                output.accept(HANDHELD_AIR_BLADDER.get());
                                output.accept(LARGE_HANDHELD_AIR_BLADDER.get());
                                output.accept(ANGLERFISH_MASK.get());
                                output.accept(ENDER_MASK.get());
                                output.accept(SLIME_MASK.get());
                                output.accept(MUDWALKER_CHARM.get());
                        });

        /**
         * The natural tab: everything the ocean grows, filed under the biome it grows in. The
         * headers name the biome and its blocks come from that biome's world generation -- the
         * middle-sea floor of {@code OceanColumnPlanner}, the reef of {@code CoralForestPillar},
         * the flora of the jelly jungle features, the brine, brimstone and crystal features --
         * so the classification reads the world rather than a hand-picked mood board. The four
         * fabricated blocks at the end grow nowhere and claim no biome.
         */
        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENVIRONMENT_TAB = tab("environment",
                        RED_CORAL_BLOCK, output -> {
                                // Middle-Level Ocean: the reef floor geology of the middle sea --
                                // cap sand and mud over limestone and shale strata.
                                output.header(CreativeTabHeader.biome(BiomeRegistry.MIDDLE_LEVEL_OCEAN));
                                output.accept(CORAL_SAND.get());
                                output.accept(NUTRIENT_RICH_MUD.get());
                                output.accept(SHALE.get());
                                output.accept(LIMESTONE.get());

                                // Coral Forest: the reef itself -- living and dead coral, the
                                // ringed coral trees of the reef pillars, and the shellstone
                                // built from reef shellfish.
                                output.header(CreativeTabHeader.biome(BiomeRegistry.CORAL_FOREST));
                                output.accept(RED_CORAL_BLOCK.get());
                                output.accept(BLUE_CORAL_BLOCK.get());
                                output.accept(BLUE_SMOOTH_CORAL_BLOCK.get());
                                output.accept(BLUE_CORAL_BRICKS.get());
                                output.accept(PURPLE_CORAL_BLOCK.get());
                                output.accept(GREEN_CORAL_BLOCK.get());
                                output.accept(FLUORASCENT_BLUE_CORAL_BLOCK.get());
                                // Ringed coral blocks
                                output.accept(RINGED_RED_CORAL_BLOCK.get());
                                output.accept(RINGED_BLUE_CORAL_BLOCK.get());
                                output.accept(RINGED_PURPLE_CORAL_BLOCK.get());
                                output.accept(RINGED_GREEN_CORAL_BLOCK.get());
                                output.accept(RINGED_FLUORASCENT_BLUE_CORAL_BLOCK.get());
                                output.accept(TUBE_CORAL_SLAB.get());
                                output.accept(BRAIN_CORAL_SLAB.get());
                                output.accept(BUBBLE_CORAL_SLAB.get());
                                output.accept(FIRE_CORAL_SLAB.get());
                                output.accept(HORN_CORAL_SLAB.get());
                                output.accept(DEAD_TUBE_CORAL_SLAB.get());
                                output.accept(DEAD_BRAIN_CORAL_SLAB.get());
                                output.accept(DEAD_BUBBLE_CORAL_SLAB.get());
                                output.accept(DEAD_FIRE_CORAL_SLAB.get());
                                output.accept(DEAD_HORN_CORAL_SLAB.get());
                                output.accept(SHELL_BLOCK.get());
                                output.accept(SHELL_BRICKS.get());
                                output.accept(HARD_SHELL_BLOCK.get());
                                output.accept(HARD_SHELL_BRICKS.get());
                                output.accept(POLISHED_HARD_SHELL_BLOCK.get());
                                output.accept(HARD_SHELL_FRAME.get());

                                // Jelly Jungle: the seaweed forests and the jelly bulges the
                                // jungle vegetation grows through and wraps in kelp.
                                output.header(CreativeTabHeader.biome(BiomeRegistry.JELLY_JUNGLE));
                                output.accept(DROOPING_SEAWEED.get());
                                output.accept(SEAWEED.get());
                                output.accept(SEAWEED_FRUIT.get());
                                output.accept(SEAWEED_STEM.get());
                                output.accept(LIGHT_RED_JELLY_BLOCK.get());
                                output.accept(LIGHT_CYAN_JELLY_BLOCK.get());
                                output.accept(WHITE_JELLY_BLOCK.get());
                                output.accept(LIGHT_GOLDEN_JELLY_BLOCK.get());
                                output.accept(LIGHT_RED_JELLY_BLOCK_SEAWEED.get());
                                output.accept(LIGHT_CYAN_JELLY_BLOCK_SEAWEED.get());
                                output.accept(WHITE_JELLY_BLOCK_SEAWEED.get());
                                output.accept(LIGHT_GOLDEN_JELLY_BLOCK_SEAWEED.get());

                                // Mud Zone: the sediment floor, its mudstone builds, fossil beds
                                // and shell litter -- filed under its own biome, not the jungle.
                                output.header(CreativeTabHeader.biome(BiomeRegistry.MUD_ZONE));
                                output.accept(MUD.get());
                                output.accept(NUTRIENT_RICH_MUD.get());
                                output.accept(PARASITIC_MUD.get());
                                output.accept(MUD_BRICKS.get());
                                output.accept(MUD_BRICK_STAIRS.get());
                                output.accept(MUD_BRICK_SLAB.get());
                                output.accept(MUD_BRICK_WALL.get());
                                output.accept(FOSSIL_BED.get());
                                output.accept(FOSSIL_DISPLAY.get());
                                output.accept(SHELL_PILE.get());
                                output.accept(SEDIMENT_COLUMN.get());
                                output.accept(MUD_BLOOM.get());
                                output.accept(BEAN_KELP.get());
                                output.accept(GLOW_FUNGUS.get());
                                output.accept(GLOW_FUNGUS_AMBER.get());
                                output.accept(GLOW_FUNGUS_VIOLET.get());
                                output.accept(GLOW_MUSHROOM_STEM.get());
                                output.accept(GLOW_MUSHROOM_CAP.get());
                                output.accept(GLOW_MUSHROOM_INSIDE.get());

                                // Brine Mirror Gorge: the evaporite terraces, diapirs and
                                // crystal grottoes of the brine features.
                                output.header(CreativeTabHeader.biome(BiomeRegistry.BRINE_MIRROR_GORGE));
                                output.accept(HALITE_CRUST.get());
                                output.accept(HALITE_PIPE.get());
                                output.accept(VARVE_SHALE.get());
                                output.accept(BRINE_MIRROR.get());
                                output.accept(CALCITE_QUILL.get());
                                output.accept(HALITE_ROSETTE.get());
                                output.accept(SALT_FRINGE.get());
                                output.accept(HOPPER_HALITE.get());
                                output.accept(HALITE_DRUSE.get());
                                output.accept(GYPSUM_BLADE.get());
                                output.accept(SYLVITE_CRUST.get());
                                output.accept(MIRROR_FLAKE.get());
                                output.accept(GYPSUM_ROSE.get());

                                // Brimstone Caldera: the volcanic terrain, its sulfur crusts
                                // and the vent flora of the smoker and fumarole fields.
                                output.header(CreativeTabHeader.biome(BiomeRegistry.BRIMSTONE_CALDERA));
                                output.accept(VOLCANIC_BASALT.get());
                                output.accept(SCORIA.get());
                                output.accept(PILLOW_BASALT.get());
                                output.accept(VOLCANIC_AGGLOMERATE.get());
                                output.accept(PUMICE.get());
                                output.accept(OBSIDIAN_GLASS.get());
                                output.accept(ACID_ETCHED_BASALT.get());
                                output.accept(SULFUR_CRUST.get());
                                output.accept(SINTER.get());
                                output.accept(VENT_CHIMNEY.get());
                                output.accept(VOLCANIC_ASH.get());
                                output.accept(ASH_LAYER.get());
                                output.accept(SULFUR_MOSS.get());
                                output.accept(SULFUR_CRYSTAL.get());
                                output.accept(FIREBLOOM.get());
                                output.accept(SULFUR_STALACTITE.get());
                                output.accept(EMBER_KELP.get());
                                output.accept(FUMAROLE.get());

                                // Crystal Nest: the hanging lattice and its crystal clusters,
                                // algae mats and seaweed.
                                output.header(CreativeTabHeader.biome(BiomeRegistry.CRYSTAL_NEST));
                                output.accept(CRYSTAL_NEST_STONE.get());
                                output.accept(CRYSTAL_DRUSE.get());
                                output.accept(CRYSTAL_COLUMN.get());
                                output.accept(WHITE_CRYSTAL_CLUSTER.get());
                                output.accept(ROSE_CRYSTAL_CLUSTER.get());
                                output.accept(AMETHYST_CRYSTAL_CLUSTER.get());
                                output.accept(AQUA_CRYSTAL_CLUSTER.get());
                                output.accept(SMOKY_CRYSTAL_CLUSTER.get());
                                output.accept(RESONANT_CRYSTAL_CLUSTER.get());
                                output.accept(LIFE_GEM_CLUSTER.get());
                                output.accept(ALGAE_MAT.get());
                                output.accept(ALGAE_TUFT.get());
                                output.accept(CRYSTAL_SPROUT.get());
                                output.accept(CRYSTAL_FRINGE.get());

                                // Structures: the fabricated blocks, which grow in no biome.
                                output.header(CreativeTabHeader.of("structures"));
                                output.accept(GAS_PIPE.get());
                                output.accept(FISHING_NET.get());
                                output.accept(PLEXIGLASS.get());
                                output.accept(DISSECTION_TABLE.get());
                                output.accept(PHOTO_RINSING_BASIN.get());
                                output.accept(INVESTIGATION_BOARD.get());
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

        private static DeferredItem<DivingEquipmentItem> tankItem(String name, int durability, int tankAirBubbles) {
                return ITEMS.registerItem(name,
                                properties -> new DivingEquipmentItem(properties.stacksTo(1).durability(durability),
                                                DivingEquipmentSlotType.TANK, tankAirBubbles, 0, 0.0F, 1.0F));
        }

        private static DeferredItem<DivingEquipmentItem> flippersItem(String name, int durability,
                        float underwaterSpeedMultiplier) {
                return ITEMS.registerItem(name,
                                properties -> new DivingEquipmentItem(properties.stacksTo(1).durability(durability),
                                                DivingEquipmentSlotType.FLIPPERS, 0, 0, 0.0F,
                                                underwaterSpeedMultiplier));
        }

        private static DeferredItem<DivingEquipmentItem> maskItem(String name, int durability, int maskRegenBonus,
                        float maskNarcosisResistance) {
                return ITEMS.registerItem(name,
                                properties -> new DivingEquipmentItem(properties.stacksTo(1).durability(durability),
                                                DivingEquipmentSlotType.MASK, 0, maskRegenBonus, maskNarcosisResistance,
                                                1.0F));
        }

        private static DeferredItem<Item> axeItem(String name, Tier tier) {
                return ITEMS.registerItem(name, properties -> new AxeItem(tier, properties));
        }

        private static DeferredItem<Item> pickaxeItem(String name, Tier tier) {
                return ITEMS.registerItem(name, properties -> new PickaxeItem(tier, properties));
        }

        private static DeferredItem<Item> shovelItem(String name, Tier tier) {
                return ITEMS.registerItem(name, properties -> new ShovelItem(tier, properties));
        }

        private static DeferredItem<Item> hoeItem(String name, Tier tier) {
                return ITEMS.registerItem(name, properties -> new HoeItem(tier, properties));
        }

        private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(String name,
                        Supplier<? extends Item> iconItem,
                        Consumer<SectionedTabOutput> contents) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, name);
                return CREATIVE_MODE_TABS.register(name, () -> CreativeModeTab.builder()
                                .title(Component.translatable("itemGroup." + Aquanaut.MODID + "." + name))
                                .icon(() -> new ItemStack(iconItem.get()))
                                .displayItems((parameters, output) -> contents
                                                .accept(SectionedTabOutput.of(id, output)))
                                .build());
        }
}

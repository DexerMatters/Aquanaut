package com.dexer.aquanaut.core;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import com.dexer.aquanaut.common.item.AirSupplyItem;
import com.dexer.aquanaut.common.item.BubbleGunItem;
import com.dexer.aquanaut.common.item.DivingEquipmentItem;
import com.dexer.aquanaut.common.item.GasFlowMeterItem;
import com.dexer.aquanaut.common.item.HarpoonItem;
import com.dexer.aquanaut.common.item.ScoopNetItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
        public static final DeferredItem<BlockItem> GAS_PIPE = blockItem("gas_pipe",
                        BlockRegistry.GAS_PIPE);
        public static final DeferredItem<BlockItem> LIGHTNING_GENERATOR = blockItem("lightning_generator",
                        BlockRegistry.LIGHTNING_GENERATOR);
        public static final DeferredItem<BlockItem> BUBBLE_MACHINE = blockItem("bubble_machine",
                        BlockRegistry.BUBBLE_MACHINE);
        public static final DeferredItem<BlockItem> SWIRL_GENERATOR = blockItem("swirl_generator",
                        BlockRegistry.SWIRL_GENERATOR);
        public static final DeferredItem<BlockItem> TORPEDO_LAUNCHER = blockItem("torpedo_launcher",
                        BlockRegistry.TORPEDO_LAUNCHER);
        public static final DeferredItem<BlockItem> AIR_PUMP = blockItem("air_pump",
                        BlockRegistry.AIR_PUMP);
        public static final DeferredItem<BlockItem> SHIELD_GENERATOR = blockItem("shield_generator",
                        BlockRegistry.SHIELD_GENERATOR);
        public static final DeferredItem<BlockItem> AIR_SUPPLY_BLOCK = blockItem("air_supply",
                        BlockRegistry.AIR_SUPPLY);

        public static final DeferredItem<GasFlowMeterItem> GAS_FLOW_METER = ITEMS.registerItem("gas_flow_meter",
                        properties -> new GasFlowMeterItem(properties.stacksTo(1)));
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
                                output.accept(MANTA_RAY_SPAWN_EGG.get());
                                output.accept(GIANT_OCTOPUS_TENTACLE_SPAWN_EGG.get());
                                output.accept(GIANT_ABYSS_WORM_SPAWN_EGG.get());
                                output.accept(LIGHTING_WORM_SPAWN_EGG.get());
                                output.accept(CREEPORPEDO_SPAWN_EGG.get());
                                output.accept(SWIRL_MAKER_SPAWN_EGG.get());
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
                        });

        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS_TAB = tab("tools",
                        BUBBLE_GUN, output -> {
                                output.accept(GAS_FLOW_METER.get());
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
                                output.accept(ANGLERFISH_MASK.get());
                                output.accept(ENDER_MASK.get());
                                output.accept(SLIME_MASK.get());
                        });

        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENVIRONMENT_TAB = tab("environment",
                        RED_CORAL_BLOCK, output -> {
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
                                output.accept(SHELL_BLOCK.get());
                                output.accept(SHELL_BRICKS.get());
                                output.accept(HARD_SHELL_BLOCK.get());
                                output.accept(HARD_SHELL_BRICKS.get());
                                output.accept(POLISHED_HARD_SHELL_BLOCK.get());
                                output.accept(HARD_SHELL_FRAME.get());
                                output.accept(GAS_PIPE.get());
                                output.accept(LIGHTNING_GENERATOR.get());
                                output.accept(BUBBLE_MACHINE.get());
                                output.accept(SWIRL_GENERATOR.get());
                                output.accept(TORPEDO_LAUNCHER.get());
                                output.accept(AIR_PUMP.get());
                                output.accept(SHIELD_GENERATOR.get());
                                output.accept(AIR_SUPPLY_BLOCK.get());
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
                        Consumer<CreativeModeTab.Output> contents) {
                return CREATIVE_MODE_TABS.register(name, () -> CreativeModeTab.builder()
                                .title(Component.translatable("itemGroup." + Aquanaut.MODID + "." + name))
                                .icon(() -> new ItemStack(iconItem.get()))
                                .displayItems((parameters, output) -> contents.accept(output))
                                .build());
        }
}

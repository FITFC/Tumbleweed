package net.konwboy.tumbleweed.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.konwboy.tumbleweed.Tumbleweed;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fmllegacy.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
public class TumbleweedConfig {

	public static double spawnChance;
	public static int maxPerPlayer;
	public static boolean enableDrops;
	public static boolean damageCrops;
	public static boolean dropOnlyByPlayer;

	private static final ForgeConfigSpec.ConfigValue<Double> SPAWN_CHANCE;
	private static final ForgeConfigSpec.ConfigValue<Integer> MAX_PER_PLAYER;
	private static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_DROPS;
	private static final ForgeConfigSpec.ConfigValue<Boolean> DAMAGE_CROPS;
	private static final ForgeConfigSpec.ConfigValue<Boolean> DROP_ONLY_BY_PLAYER;
	private static final ForgeConfigSpec.ConfigValue<List<String>> DROP_DATA;
	private static final ForgeConfigSpec.ConfigValue<List<String>> BLOCK_DATA;
	private static final ForgeConfigSpec.ConfigValue<List<String>> BIOME_DATA;

	public static final ForgeConfigSpec SPEC;

	private static final String[] DEFAULT_DROPS = { "3 minecraft:bone", "3 minecraft:dead_bush", "3 minecraft:string", "3 minecraft:feather", "3 minecraft:wheat", "3 minecraft:stick", "3 minecraft:sugar_cane", "2 minecraft:melon_seeds", "2 minecraft:pumpkin_seeds", "2 minecraft:gold_nugget", "1 minecraft:name_tag", "1 minecraft:saddle", "1 minecraft:emerald", "1 minecraft:diamond", "1 minecraft:iron_ingot", "1 minecraft:gold_ingot" };
	private static final String[] DEFAULT_BLOCKS = { "minecraft:dead_bush" };

	private static IForgeRegistry<Item> ITEMS = GameRegistry.findRegistry(Item.class);
	private static IForgeRegistry<Block> BLOCKS = GameRegistry.findRegistry(Block.class);
	private static IForgeRegistry<Biome> BIOMES = GameRegistry.findRegistry(Biome.class);

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		SPAWN_CHANCE = builder
				.comment("Chance a tumbleweed spawns in a chunk.")
				.define("spawnChance", 0.5);

		MAX_PER_PLAYER = builder
				.comment("Maximum number of tumbleweeds existing per player (technically 17x17 loaded chunks).")
				.define("maxPerPlayer", 8);

		ENABLE_DROPS = builder
				.define("enableDrops", true);

		DAMAGE_CROPS = builder
				.comment("Should tumbleweeds damage crops.")
				.define("damageCrops", true);

		DROP_ONLY_BY_PLAYER = builder
				.comment("Drop items only when destroyed by player (normally also drops on lava and cactus damage, for example).")
				.define("dropOnlyByPlayer", false);

		DROP_DATA = builder
				.comment(
						"These items will drop from a tumbleweed upon destroying. The identifier can include an NBT compound. The amount parameter is optional.",
						"weight item amount",
						"Example entry: 2 minecraft:arrow{display:{Name:'{\"text\":\"Cool arrow\"}'}} 20")
				.define("drops", Lists.newArrayList(DEFAULT_DROPS));

		BLOCK_DATA = builder
				.comment(
						"Blocks in which tumbleweeds can spawn. Only works with non-solid blocks.",
						"The actual location also requires sky access and a solid block below it.",
						"<mod>:<block>")
				.define("spawningBlocks", Lists.newArrayList(DEFAULT_BLOCKS));

		BIOME_DATA = builder
				.comment(
						"If not empty, tumbleweeds spawn ONLY in the specified biomes. Else they appear in all hot, dry biomes.",
						"Example entry: minecraft:desert")
				.define("biomeWhitelist", Lists.newArrayList());

		SPEC = builder.build();
	}

	private static List<WeightedItem> weightedItems = Lists.newArrayList();
	private static int totalWeight;

	public static Set<ResourceLocation> biomeWhitelist = Sets.newHashSet();
	public static Set<ResourceLocation> spawningBlocks = Sets.newHashSet();

	public static boolean canLoad = false;

	public static void load() {
		if (!canLoad)
			return;

		totalWeight = 0;
		weightedItems.clear();
		biomeWhitelist.clear();
		spawningBlocks.clear();

		spawnChance = SPAWN_CHANCE.get();
		maxPerPlayer = MAX_PER_PLAYER.get();
		enableDrops = ENABLE_DROPS.get();
		damageCrops = DAMAGE_CROPS.get();
		dropOnlyByPlayer = DROP_ONLY_BY_PLAYER.get();

		for (String s : DROP_DATA.get()) {
			String[] itemData = s.split(" ");
			if (itemData.length < 2)
				continue;

			double weight = Double.parseDouble(itemData[0]);
			String item = itemData[1];

			try {
				new ItemParser(new StringReader(item), false).parse();
			} catch (CommandSyntaxException e) {
				e.printStackTrace();
				continue;
			}

			int amount = 1;

			if (itemData.length >= 3) {
				try {
					amount = Integer.parseInt(itemData[2]);
				} catch (NumberFormatException ignore) {
				}
			}

			weightedItems.add(new WeightedItem(item, amount, weight));
			totalWeight += weight;
		}

		for (String entry : BIOME_DATA.get()) {
			ResourceLocation id = new ResourceLocation(entry);
			if (BIOMES.containsKey(id))
				biomeWhitelist.add(id);
			else
				Tumbleweed.logger.warn("Biome {} is invalid.", id);
		}

		for (String entry : BLOCK_DATA.get()) {
			ResourceLocation location = new ResourceLocation(entry);
			if (!BLOCKS.containsKey(location)) {
				Tumbleweed.logger.warn("Block {} is invalid.", entry);
				continue;
			}

			spawningBlocks.add(location);
		}
	}

	public static ItemStack getRandomItem() {
		double randomWeight = totalWeight * Math.random();
		double countedWeight = 0.0;

		for (WeightedItem weightedItem : weightedItems) {
			countedWeight += weightedItem.weight;

			if (countedWeight < randomWeight) continue;

			try {
				ItemParser parser = new ItemParser(new StringReader(weightedItem.item), false).parse();
				ItemStack stack = new ItemStack(parser.getItem(), weightedItem.amount);
				if (parser.getNbt() != null)
					stack.setTag(parser.getNbt());
				return stack;
			} catch (CommandSyntaxException e) {
				e.printStackTrace();
			}

			return null;
		}

		return null;
	}

	private static class WeightedItem {
		public final String item;
		public final int amount;
		public final double weight;

		public WeightedItem(String item, int amount, double weight) {
			this.item = item;
			this.amount = amount;
			this.weight = weight;
		}
	}

	@SubscribeEvent
	public static void configReloading(ModConfigEvent.Reloading event) {
		if (event.getConfig().getSpec() == SPEC) {
			TumbleweedConfig.load();
		}
	}

}

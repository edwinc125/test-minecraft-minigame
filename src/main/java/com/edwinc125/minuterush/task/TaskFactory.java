package com.edwinc125.minuterush.task;

import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.stats.Stats;

public final class TaskFactory {
	private TaskFactory() {}

	public static MinuteRushTask next(ServerLevel level, Random random) {
		return next(level, random, Set.of()).task();
	}

	public static Selection next(ServerLevel level, Random random, Set<String> recent) {
		return next(level, random, recent, true);
	}

	public static Selection next(ServerLevel level, Random random, Set<String> recent, boolean allowPlayerInteraction) {
		List<Selection> candidates = new ArrayList<>();
		addBasicTasks(level, candidates);
		addLegacyTasks(level, candidates);
		if (!allowPlayerInteraction) {
			candidates.removeIf(selection -> selection.id().equals("interact-player"));
		}
		List<Selection> available = candidates.stream()
			.filter(selection -> !recent.contains(selection.id()))
			.toList();
		if (available.isEmpty()) {
			available = candidates;
		}
		return TaskSelection.chooseWeighted(random, available);
	}

	private static void addBasicTasks(ServerLevel level, List<Selection> candidates) {
		for (BasicTaskCatalog.Entry entry : BasicTaskCatalog.entries()) {
			if (entry.block() != null) {
				BlockPos target = level.getServer().getRespawnData().pos().below();
				if (entry.mine()) {
					candidates.add(new Selection("basic:" + entry.id() + ":mine",
						createMineTask(level, target, entry.block()), entry.rarityWeight(), "mine"));
				}
				if (entry.place()) {
					candidates.add(new Selection("basic:" + entry.id() + ":place",
						new PlaceBlockTask(entry.block()), entry.rarityWeight(), "place"));
				}
			}
			if (entry.collect()) {
				candidates.add(new Selection("basic:" + entry.id() + ":collect",
					new CollectItemTask(entry.item(), 2), entry.rarityWeight(), "collect"));
			}
			if (entry.hold()) {
				candidates.add(new Selection("basic:" + entry.id() + ":hold",
					new HoldItemTask("Hold a " + entry.id().replace('-', ' '),
						stack -> stack.is(entry.item())), entry.rarityWeight(), "hold"));
			}
		}
	}

	private static MinuteRushTask createMineTask(ServerLevel level, BlockPos target, Block block) {
		level.setBlock(target, block.defaultBlockState(), 3);
		return new DigBlockTask(block, target);
	}

	private static void addLegacyTasks(ServerLevel level, List<Selection> candidates) {
		craft(candidates, "craft-stick", Items.STICK, 1.25);
		craft(candidates, "craft-crafting-table", Items.CRAFTING_TABLE, 1.25);
		craft(candidates, "craft-oak-planks", Items.OAK_PLANKS, 1.25);
		craft(candidates, "craft-wooden-pickaxe", Items.WOODEN_PICKAXE, 1.25);
		craft(candidates, "craft-wooden-axe", Items.WOODEN_AXE, 1.25);
		craft(candidates, "craft-wooden-shovel", Items.WOODEN_SHOVEL, 1.25);
		craft(candidates, "craft-wooden-hoe", Items.WOODEN_HOE, 1.0);
		craft(candidates, "craft-wooden-sword", Items.WOODEN_SWORD, 1.0);
		craft(candidates, "craft-wooden-button", Items.OAK_BUTTON, 1.0);
		craft(candidates, "craft-wooden-pressure-plate", Items.OAK_PRESSURE_PLATE, 1.0);
		craft(candidates, "craft-bowl", Items.BOWL, 1.0);
		craft(candidates, "craft-chest", Items.CHEST, 1.0);
		craft(candidates, "craft-torch", Items.TORCH, 1.25);
		craft(candidates, "craft-stone-pickaxe", Items.STONE_PICKAXE, 1.0);
		craft(candidates, "craft-furnace", Items.FURNACE, 1.0);
		craft(candidates, "craft-leather-helmet", Items.LEATHER_HELMET, 1.0);
		craft(candidates, "craft-leather-chestplate", Items.LEATHER_CHESTPLATE, 1.0);
		craft(candidates, "craft-leather-leggings", Items.LEATHER_LEGGINGS, 1.0);
		craft(candidates, "craft-leather-boots", Items.LEATHER_BOOTS, 1.0);
		craft(candidates, "craft-iron-helmet", Items.IRON_HELMET, 0.75);
		craft(candidates, "craft-iron-chestplate", Items.IRON_CHESTPLATE, 0.75);
		craft(candidates, "craft-iron-leggings", Items.IRON_LEGGINGS, 0.75);
		craft(candidates, "craft-iron-boots", Items.IRON_BOOTS, 0.75);
		craft(candidates, "craft-iron-sword", Items.IRON_SWORD, 0.75);
		craft(candidates, "craft-iron-pickaxe", Items.IRON_PICKAXE, 0.75);
		craft(candidates, "craft-iron-axe", Items.IRON_AXE, 0.75);
		craft(candidates, "craft-iron-shovel", Items.IRON_SHOVEL, 0.75);
		craft(candidates, "craft-iron-hoe", Items.IRON_HOE, 0.75);
		craft(candidates, "craft-iron-bucket", Items.BUCKET, 0.75);
		craft(candidates, "craft-shears", Items.SHEARS, 0.75);
		craft(candidates, "craft-flint-and-steel", Items.FLINT_AND_STEEL, 0.75);
		craft(candidates, "craft-iron-door", Items.IRON_DOOR, 0.75);
		craft(candidates, "craft-iron-trapdoor", Items.IRON_TRAPDOOR, 0.75);
		craft(candidates, "craft-iron-bars", Items.IRON_BARS, 0.75);
		craft(candidates, "craft-minecart", Items.MINECART, 0.75);
		candidates.add(new Selection("hit-cow", new EntityEventTask("hit:cow", "Hit a cow")));
		candidates.add(new Selection("hit-pig", new EntityEventTask("hit:pig", "Hit a pig")));
		candidates.add(new Selection("hit-sheep", new EntityEventTask("hit:sheep", "Hit a sheep")));
		candidates.add(new Selection("hit-chicken", new EntityEventTask("hit:chicken", "Hit a chicken")));
		candidates.add(new Selection("hit-zombie", new EntityEventTask("hit:zombie", "Hit a zombie")));
		candidates.add(new Selection("hit-skeleton", new EntityEventTask("hit:skeleton", "Hit a skeleton")));
		candidates.add(new Selection("hit-creeper", new EntityEventTask("hit:creeper", "Hit a creeper")));
		candidates.add(new Selection("critical-hit", new EntityEventTask("combat:critical", "Deal a critical hit")));
		candidates.add(new Selection("fall-damage", new EntityEventTask("damage:fall", "Take exactly 1 instance of fall damage")));
		candidates.add(new Selection("cactus-damage", new EntityEventTask("damage:cactus", "Take cactus damage")));
		candidates.add(new Selection("kill-hostile-mob", new EntityEventTask("kill:hostile", "Kill any hostile mob")));
		candidates.add(new Selection("kill-animal", new EntityEventTask("kill:animal", "Kill any animal")));
		candidates.add(new Selection("shear-sheep", new EntityEventTask("use:shear-sheep", "Shear a sheep")));
		candidates.add(new Selection("milk-cow", new EntityEventTask("use:milk-cow", "Milk a cow")));
		candidates.add(new Selection("block-attack-shield",
			new EntityEventTask("combat:shield-block", "Block an attack with a shield")));
		event(candidates, "projectile-damage", "damage:projectile", "Take projectile damage");
		event(candidates, "fire-damage", "damage:fire", "Take fire damage");
		event(candidates, "lava-damage", "damage:lava", "Take lava damage and survive");
		event(candidates, "sweet-berry-damage", "damage:sweet-berry", "Take sweet berry bush damage");
		event(candidates, "sprint-hit", "combat:sprint-hit", "Deal damage while sprinting");
		event(candidates, "crouch-hit", "combat:crouch-hit", "Deal damage while crouching");
		event(candidates, "kill-zombie", "kill:zombie", "Kill a zombie");
		event(candidates, "kill-skeleton", "kill:skeleton", "Kill a skeleton");
		event(candidates, "kill-creeper", "kill:creeper", "Kill a creeper");
		event(candidates, "kill-cow", "kill:cow", "Kill a cow");
		event(candidates, "kill-pig", "kill:pig", "Kill a pig");
		event(candidates, "kill-sheep", "kill:sheep", "Kill a sheep");
		event(candidates, "kill-chicken", "kill:chicken", "Kill a chicken");
		event(candidates, "hit-spider", "hit:spider", "Hit a spider");
		event(candidates, "hit-enderman", "hit:enderman", "Hit an enderman");
		event(candidates, "hit-villager", "hit:villager", "Hit a villager");
		event(candidates, "shear-white-sheep", "use:shear-sheep:white", "Shear a white sheep");
		event(candidates, "shear-black-sheep", "use:shear-sheep:black", "Shear a black sheep");
		event(candidates, "shear-brown-sheep", "use:shear-sheep:brown", "Shear a brown sheep");
		event(candidates, "milk-mooshroom", "use:milk-mooshroom", "Milk a mooshroom");
		event(candidates, "put-mob-in-boat", "use:boat-mob", "Put a mob in a boat");
		event(candidates, "feed-cow", "feed:cow", "Feed a cow");
		event(candidates, "feed-pig", "feed:pig", "Feed a pig");
		event(candidates, "feed-sheep", "feed:sheep", "Feed a sheep");
		event(candidates, "feed-chicken", "feed:chicken", "Feed a chicken");
		event(candidates, "feed-wolf", "feed:wolf", "Feed a wolf");
		event(candidates, "feed-cat", "feed:cat", "Feed a cat");
		event(candidates, "place-crouching", "block:place:crouch", "Place a block while crouching");
		event(candidates, "break-crouching", "block:break:crouch", "Break a block while crouching");
		event(candidates, "open-door", "block:open:door", "Open a door");
		event(candidates, "open-trapdoor", "block:open:trapdoor", "Open a trapdoor");
		event(candidates, "toggle-campfire", "block:use:campfire", "Use a campfire");
		event(candidates, "enter-vehicle", "vehicle:enter", "Enter a vehicle");
		event(candidates, "exit-vehicle", "vehicle:exit", "Exit a vehicle");
		event(candidates, "climb-ladder", "climb:ladder", "Climb a ladder");
		event(candidates, "climb-scaffolding", "climb:scaffolding", "Climb scaffolding");
		event(candidates, "climb-vines", "climb:vines", "Climb vines");
		event(candidates, "drop-item", "inventory:drop", "Drop an item");
		event(candidates, "swap-offhand", "inventory:swap-offhand", "Swap an item to the offhand");
		event(candidates, "shoot-bow", "use:bow", "Shoot a bow");
		event(candidates, "fire-crossbow", "use:crossbow", "Fire a crossbow");
		event(candidates, "throw-snowball", "use:snowball", "Throw a snowball");
		event(candidates, "throw-egg", "use:egg", "Throw an egg");
		event(candidates, "use-fishing-rod", "use:fishing-rod", "Use a fishing rod");
		event(candidates, "use-flint-and-steel", "use:flint-and-steel", "Use flint and steel");
		event(candidates, "use-shears", "use:shears", "Use shears");
		event(candidates, "use-hoe", "use:hoe", "Use a hoe");
		event(candidates, "use-bucket", "use:bucket", "Use a bucket");
		event(candidates, "use-lead", "use:lead", "Use a lead");
		event(candidates, "use-map", "use:map", "Use a map");
		event(candidates, "use-compass", "use:compass", "Use a compass");
		event(candidates, "use-clock", "use:clock", "Use a clock");
		event(candidates, "use-shield", "use:shield", "Use a shield");
		event(candidates, "break-leaves", "block:break:leaves", "Break leaves");
		event(candidates, "break-torch", "block:break:torch", "Break a torch");
		event(candidates, "break-door", "block:break:door", "Break a door");
		event(candidates, "break-bed", "block:break:bed", "Break a bed");
		event(candidates, "place-sign", "block:place:sign", "Place a sign");
		event(candidates, "place-banner", "block:place:banner", "Place a banner");
		event(candidates, "place-flower-pot", "block:place:flower-pot", "Place a flower pot");
		event(candidates, "plant-seed", "block:place:seed", "Plant a seed");
		event(candidates, "harvest-crop", "block:break:crop", "Harvest a crop");
		event(candidates, "ring-bell", "block:use:bell", "Ring a bell");
		event(candidates, "press-button-use", "block:use:button", "Press a button");
		event(candidates, "pull-lever-use", "block:use:lever", "Pull a lever");
		event(candidates, "open-chest-use", "block:use:chest", "Open a chest");
		event(candidates, "open-barrel-use", "block:use:barrel", "Open a barrel");
		event(candidates, "open-furnace-use", "block:use:furnace", "Open a furnace");
		event(candidates, "use-crafting-table", "block:use:crafting_table", "Use a crafting table");
		event(candidates, "hit-horse", "hit:horse", "Hit a horse");
		event(candidates, "kill-spider", "kill:spider", "Kill a spider");
		event(candidates, "kill-enderman", "kill:enderman", "Kill an enderman");
		event(candidates, "kill-villager", "kill:villager", "Kill a villager");
		event(candidates, "feed-horse", "feed:horse", "Feed a horse");
		event(candidates, "feed-donkey", "feed:donkey", "Feed a donkey");
		event(candidates, "place-sign-use", "block:place:sign", "Place a sign");
		event(candidates, "place-banner-use", "block:place:banner", "Place a banner");
		event(candidates, "place-flower-pot-use", "block:place:flower-pot", "Place a flower pot");
		event(candidates, "plant-seed-use", "block:place:seed", "Plant a seed");
		event(candidates, "light-campfire", "block:campfire:light", "Light a campfire");
		event(candidates, "extinguish-campfire", "block:campfire:extinguish", "Extinguish a campfire");
		event(candidates, "enter-water", "environment:enter-water", "Enter water");
		event(candidates, "exit-water", "environment:exit-water", "Exit water");
		event(candidates, "stand-powder-snow", "environment:powder-snow", "Stand in powder snow");
		stat(candidates, "submerge-underwater", "Submerge underwater", Stats.CUSTOM, Stats.WALK_UNDER_WATER_ONE_CM, 1);
		stat(candidates, "swim-underwater", "Swim underwater", Stats.CUSTOM, Stats.SWIM_ONE_CM, 1);
		stat(candidates, "crouch-walk", "Crouch-walk for 10 blocks", Stats.CUSTOM, Stats.CROUCH_ONE_CM, 1000);
		stat(candidates, "fall-five-blocks", "Fall at least five blocks", Stats.CUSTOM, Stats.FALL_ONE_CM, 500);
		stat(candidates, "ride-boat", "Ride a boat", Stats.CUSTOM, Stats.BOAT_ONE_CM, 1);
		stat(candidates, "ride-minecart", "Ride a minecart", Stats.CUSTOM, Stats.MINECART_ONE_CM, 1);
		stat(candidates, "climb-anything", "Climb 5 blocks", Stats.CUSTOM, Stats.CLIMB_ONE_CM, 500);
		candidates.add(new Selection("eat-rotten-flesh", new ConsumeItemTask(Items.ROTTEN_FLESH)));
		candidates.add(new Selection("eat-spider-eye", new ConsumeItemTask(Items.SPIDER_EYE)));
		candidates.add(new Selection("eat-suspicious-stew", new ConsumeItemTask(Items.SUSPICIOUS_STEW)));
		candidates.add(new Selection("eat-poisonous-potato", new ConsumeItemTask(Items.POISONOUS_POTATO)));
		candidates.add(new Selection("eat-pufferfish", new ConsumeItemTask(Items.PUFFERFISH)));
		candidates.add(new Selection("drink-milk", new ConsumeItemTask(Items.MILK_BUCKET)));
		candidates.add(new Selection("drink-potion", new ConsumeItemTask(Items.POTION)));
		candidates.add(new Selection("consume-honey-bottle", new ConsumeItemTask(Items.HONEY_BOTTLE)));
		candidates.add(new Selection("open-chest", new InteractionTask(InteractionTask.Kind.BLOCK, "chest", 1)));
		candidates.add(new Selection("open-barrel", new InteractionTask(InteractionTask.Kind.BLOCK, "barrel", 1)));
		candidates.add(new Selection("open-furnace", new InteractionTask(InteractionTask.Kind.BLOCK, "furnace", 1)));
		candidates.add(new Selection("open-crafting-table", new InteractionTask(InteractionTask.Kind.BLOCK, "crafting_table", 1)));
		candidates.add(new Selection("press-button", new InteractionTask(InteractionTask.Kind.BLOCK, "button", 1)));
		candidates.add(new Selection("pull-lever", new InteractionTask(InteractionTask.Kind.BLOCK, "lever", 1)));
		candidates.add(new Selection("step-on-pressure-plate",
			new InteractionTask(InteractionTask.Kind.BLOCK, "pressure_plate", 1)));
		candidates.add(new Selection("jump", new JumpTask(), 1.0, "movement"));
		candidates.add(new Selection("jump-count", new StatTask("Jump 3 times", 3,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.JUMP)), 1.0, "movement"));
		candidates.add(new Selection("hold-tool", new HoldItemTask("Hold a pickaxe", stack ->
			stack.is(Items.WOODEN_PICKAXE) || stack.is(Items.STONE_PICKAXE) || stack.is(Items.IRON_PICKAXE)
				|| stack.is(Items.GOLDEN_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE)
				|| stack.is(Items.NETHERITE_PICKAXE))));
		candidates.add(new Selection("interact-player", new InteractionTask(InteractionTask.Kind.PLAYER, "player", 1), 1.0, "interactive"));
		candidates.add(new Selection("interact-mob", new InteractionTask(InteractionTask.Kind.MOB, "any-mob", 1), 1.0, "interactive"));
		candidates.add(new Selection("interact-block", new InteractionTask(InteractionTask.Kind.BLOCK, "any-block", 1), 1.0, "interactive"));
		movement(candidates);
	}

	private static void craft(List<Selection> candidates, String id, net.minecraft.world.item.Item item, double weight) {
		candidates.add(new Selection(id, new CraftItemTask(item), weight, "crafting"));
	}

	private static void movement(List<Selection> candidates) {
		candidates.add(new Selection("walk-25", new StatTask("Walk 25 blocks", 2500,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.WALK_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("walk-50", new StatTask("Walk 50 blocks", 5000,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.WALK_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("sprint-15", new SprintTask(15), 1.0, "movement"));
		candidates.add(new Selection("sprint-25", new SprintTask(25), 1.0, "movement"));
		candidates.add(new Selection("swim-20", new StatTask("Swim 20 blocks", 2000,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.SWIM_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("swim-40", new StatTask("Swim 40 blocks", 4000,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.SWIM_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("underwater-15", new StatTask("Swim underwater for 15 blocks", 1500,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.WALK_UNDER_WATER_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("underwater-30", new StatTask("Swim underwater for 30 blocks", 3000,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.WALK_UNDER_WATER_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("crouch-walk-10", new StatTask("Crouch-walk 10 blocks", 1000,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.CROUCH_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("crouch-walk-15", new StatTask("Crouch-walk 15 blocks", 1500,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.CROUCH_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("climb-8", new StatTask("Climb 8 blocks", 800,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.CLIMB_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("climb-15", new StatTask("Climb 15 blocks", 1500,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.CLIMB_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("travel-24", new TravelTask(24), 1.0, "movement"));
		candidates.add(new Selection("travel-40", new TravelTask(40), 1.0, "movement"));
		candidates.add(new Selection("boat-20", new StatTask("Ride a boat for 20 blocks", 2000,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.BOAT_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("minecart-20", new StatTask("Ride a minecart for 20 blocks", 2000,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.MINECART_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("jump-5", new StatTask("Jump 5 times", 5,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.JUMP)), 1.0, "movement"));
		candidates.add(new Selection("reach-y-100", new YLevelTask(true, 100), 0.75, "movement"));
		candidates.add(new Selection("reach-y-50", new YLevelTask(false, 50), 0.75, "movement"));
		candidates.add(new Selection("fall-water", new WaterLandingTask(), 0.75, "movement"));
	}

	private static void event(List<Selection> candidates, String id, String key, String prompt) {
		candidates.add(new Selection(id, new EntityEventTask(key, prompt)));
	}

	private static void stat(List<Selection> candidates, String id, String prompt,
			net.minecraft.stats.StatType<net.minecraft.resources.Identifier> type,
			net.minecraft.resources.Identifier stat, int required) {
		candidates.add(new Selection(id, new StatTask(prompt, required,
			player -> player.getStats().getValue(type, stat)), 1.0, "movement"));
	}

	public record Selection(String id, MinuteRushTask task, double rarityWeight, String actionType)
		implements TaskSelection.Weighted {
		public Selection(String id, MinuteRushTask task) {
			this(id, task, 1.0, "interactive");
		}

		@Override
		public double weight() {
			return rarityWeight * switch (actionType) {
				case "crafting" -> 1.50;
				case "movement" -> 1.40;
				case "interactive" -> 1.25;
				case "mine", "place", "collect" -> 0.80;
				case "hold" -> 0.40;
				default -> 1.0;
			};
		}
	}
}

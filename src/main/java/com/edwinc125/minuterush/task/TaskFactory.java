package com.edwinc125.minuterush.task;

import java.util.Random;
import java.util.Set;
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
		int poolSize = 18;
		java.util.Set<Integer> excluded = new java.util.HashSet<>(
			recent.stream().map(TaskFactory::indexOf).filter(index -> index >= 0).collect(java.util.stream.Collectors.toSet()));
		if (!allowPlayerInteraction) {
			excluded.add(9);
		}
		int kind = TaskSelection.chooseIndex(random, poolSize, excluded);
		if (kind == 0) {
			Block block = random.nextBoolean() ? Blocks.STONE : Blocks.TORCH;
			return new Selection("place", new PlaceBlockTask(block));
		}
		if (kind == 1) {
			return new Selection("collect", new CollectItemTask(random.nextBoolean() ? Items.APPLE : Items.TORCH, 2));
		}
		if (kind == 2) {
			BlockPos target = level.getServer().getRespawnData().pos().below();
			level.setBlock(target, Blocks.STONE.defaultBlockState(), 3);
			return new Selection("dig", new DigBlockTask(Blocks.STONE, target));
		}
		if (kind == 3) {
			return new Selection("jump", new JumpTask());
		}
		if (kind == 4) {
			return new Selection("travel", new TravelTask(8));
		}
		if (kind == 5) {
			return new Selection("sprint", new StatTask("Sprint for 10 seconds", 1000,
				player -> player.getStats().getValue(Stats.CUSTOM, Stats.SPRINT_ONE_CM)));
		}
		if (kind == 6) {
			return new Selection("jump-count", new StatTask("Jump 3 times", 3,
				player -> player.getStats().getValue(Stats.CUSTOM, Stats.JUMP)));
		}
		if (kind == 7) {
			return new Selection("hold-tool", new HoldItemTask("Hold a pickaxe", stack ->
				stack.is(Items.WOODEN_PICKAXE) || stack.is(Items.STONE_PICKAXE) || stack.is(Items.IRON_PICKAXE)
					|| stack.is(Items.GOLDEN_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE)
					|| stack.is(Items.NETHERITE_PICKAXE)));
		}
		if (kind == 8) {
			return new Selection("hold-torch", new HoldItemTask("Hold a torch", stack -> stack.is(Items.TORCH)));
		}
		if (kind == 9) {
			return new Selection("interact-player", new InteractionTask(InteractionTask.Kind.PLAYER, "player", 1));
		}
		if (kind == 10) {
			return new Selection("interact-mob", new InteractionTask(InteractionTask.Kind.MOB, "any-mob", 1));
		}
		if (kind == 11) {
			return new Selection("interact-block", new InteractionTask(InteractionTask.Kind.BLOCK, "any-block", 1));
		}
		if (kind == 12) {
			return new Selection("travel-far", new TravelTask(16));
		}
		if (kind == 13) {
			return new Selection("walk", new StatTask("Walk 20 blocks", 2000,
				player -> player.getStats().getValue(Stats.CUSTOM, Stats.WALK_ONE_CM)));
		}
		if (kind == 14) {
			return new Selection("swim", new StatTask("Swim 5 blocks", 500,
				player -> player.getStats().getValue(Stats.CUSTOM, Stats.SWIM_ONE_CM)));
		}
		if (kind == 15) {
			return new Selection("place-torch", new PlaceBlockTask(Blocks.TORCH));
		}
		if (kind == 16) {
			return new Selection("collect-apple", new CollectItemTask(Items.APPLE, 2));
		}
		BlockPos target = level.getServer().getRespawnData().pos().below(2);
		level.setBlock(target, Blocks.DIRT.defaultBlockState(), 3);
		return new Selection("dig-dirt", new DigBlockTask(Blocks.DIRT, target));
	}

	private static int indexOf(String id) {
		return switch (id) {
			case "place" -> 0;
			case "collect" -> 1;
			case "dig" -> 2;
			case "jump" -> 3;
			case "travel" -> 4;
			case "sprint" -> 5;
			case "jump-count" -> 6;
			case "hold-tool" -> 7;
			case "hold-torch" -> 8;
			case "interact-player" -> 9;
			case "interact-mob" -> 10;
			case "interact-block" -> 11;
			case "travel-far" -> 12;
			case "walk" -> 13;
			case "swim" -> 14;
			case "place-torch" -> 15;
			case "collect-apple" -> 16;
			case "dig-dirt" -> 17;
			default -> -1;
		};
	}

	public record Selection(String id, MinuteRushTask task) {}
}

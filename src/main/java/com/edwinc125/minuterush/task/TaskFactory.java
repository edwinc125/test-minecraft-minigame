package com.edwinc125.minuterush.task;

import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class TaskFactory {
	private TaskFactory() {}

	public static MinuteRushTask next(ServerLevel level, Random random) {
		return next(level, random, Set.of()).task();
	}

	public static Selection next(ServerLevel level, Random random, Set<String> recent) {
		int kind = TaskSelection.chooseIndex(random, 5,
			recent.stream().map(TaskFactory::indexOf).filter(index -> index >= 0).collect(java.util.stream.Collectors.toSet()));
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
		if (random.nextBoolean()) {
			return new Selection("jump", new JumpTask());
		}
		return new Selection("travel", new TravelTask(8));
	}

	private static int indexOf(String id) {
		return switch (id) {
			case "place" -> 0;
			case "collect" -> 1;
			case "dig" -> 2;
			case "jump" -> 3;
			case "travel" -> 4;
			default -> -1;
		};
	}

	public record Selection(String id, MinuteRushTask task) {}
}

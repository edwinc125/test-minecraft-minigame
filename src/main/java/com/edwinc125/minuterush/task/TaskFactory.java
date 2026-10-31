package com.edwinc125.minuterush.task;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public final class TaskFactory {
	private TaskFactory() {}

	public static MinuteRushTask next(ServerLevel level, Random random) {
		if (random.nextBoolean()) {
			BlockPos target = new BlockPos(level.getSharedSpawnPos()).above();
			level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
			return new PlaceBlockTask(target);
		}
		return new JumpTask();
	}
}

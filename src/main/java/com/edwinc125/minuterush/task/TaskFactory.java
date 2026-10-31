package com.edwinc125.minuterush.task;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class TaskFactory {
	private TaskFactory() {}

	public static MinuteRushTask next(ServerLevel level, Random random) {
		int kind = random.nextInt(4);
		if (kind == 0) {
			BlockPos target = level.getServer().getRespawnData().pos().above();
			if (!level.getBlockState(target).isAir()) {
				level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
			}
			Block block = random.nextBoolean() ? Blocks.STONE : Blocks.TORCH;
			return new PlaceBlockTask(target, block);
		}
		if (kind == 1) {
			return new CollectItemTask(random.nextBoolean() ? Items.APPLE : Items.TORCH, 2);
		}
		if (kind == 2) {
			BlockPos target = level.getServer().getRespawnData().pos().below();
			level.setBlock(target, Blocks.STONE.defaultBlockState(), 3);
			return new DigBlockTask(Blocks.STONE, target);
		}
		if (random.nextBoolean()) {
			return new JumpTask();
		}
		return new TravelTask(8);
	}
}

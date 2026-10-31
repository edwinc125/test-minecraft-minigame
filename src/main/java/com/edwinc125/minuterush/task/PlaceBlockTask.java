package com.edwinc125.minuterush.task;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

public final class PlaceBlockTask implements MinuteRushTask {
	private final BlockPos target;
	private final BlockPos.MutableBlockPos completedBy = new BlockPos.MutableBlockPos();

	public PlaceBlockTask(BlockPos target) {
		this.target = target.immutable();
	}

	@Override
	public String description() {
		return "Place a stone block at " + target.toShortString();
	}

	@Override
	public void begin(ServerPlayer player) {
		player.sendSystemMessage(net.minecraft.network.chat.Component.literal(description()));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		var level = player.serverLevel();
		return level.getBlockState(target).is(Blocks.STONE);
	}
}

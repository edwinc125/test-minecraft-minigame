package com.edwinc125.minuterush.task;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public final class PlaceBlockTask implements MinuteRushTask {
	private final BlockPos target;

	public PlaceBlockTask(BlockPos target) {
		this.target = target.immutable();
	}

	@Override
	public String description() {
		return "Place a stone block at " + target.toShortString();
	}

	@Override
	public void begin(ServerPlayer player) {
		player.sendSystemMessage(Component.literal(description()));
		if (!player.getInventory().hasAnyOf(Set.of(Blocks.STONE.asItem()))) {
			player.getInventory().add(new ItemStack(Blocks.STONE.asItem()));
		}
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		var level = player.level();
		return level.getBlockState(target).is(Blocks.STONE);
	}
}

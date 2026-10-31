package com.edwinc125.minuterush.task;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public final class PlaceBlockTask implements MinuteRushTask {
	private final BlockPos target;
	private final net.minecraft.world.level.block.Block block;

	public PlaceBlockTask(BlockPos target) {
		this(target, Blocks.STONE);
	}

	public PlaceBlockTask(BlockPos target, net.minecraft.world.level.block.Block block) {
		this.target = target.immutable();
		this.block = block;
	}

	@Override
	public String description() {
		return "Place a " + block.toString() + " at " + target.toShortString();
	}

	@Override
	public void begin(ServerPlayer player) {
		player.sendSystemMessage(Component.literal(description()));
		if (!player.getInventory().hasAnyOf(Set.of(block.asItem()))) {
			player.getInventory().add(new ItemStack(block.asItem()));
		}
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		var level = player.level();
		return level.getBlockState(target).is(block);
	}
}

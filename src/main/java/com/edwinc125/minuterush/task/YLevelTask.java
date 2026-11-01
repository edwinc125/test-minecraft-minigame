package com.edwinc125.minuterush.task;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class YLevelTask implements MinuteRushTask {
	private final boolean atLeast;
	private final int level;

	public YLevelTask(boolean atLeast, int level) {
		this.atLeast = atLeast;
		this.level = level;
	}

	@Override
	public String description() {
		return (atLeast ? "Reach Y " : "Reach Y ") + (atLeast ? ">=" : "<=") + level;
	}

	@Override
	public void begin(ServerPlayer player) {
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.BLUE));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return atLeast ? player.getY() >= level : player.getY() <= level;
	}
}

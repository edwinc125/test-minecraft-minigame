package com.edwinc125.minuterush.task;

import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class JumpTask implements MinuteRushTask {
	private final Set<UUID> jumped = new java.util.HashSet<>();

	@Override
	public String description() {
		return "Jump once";
	}

	@Override
	public void begin(ServerPlayer player) {
		jumped.remove(player.getUUID());
		player.sendSystemMessage(net.minecraft.network.chat.Component.literal(description()));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		if (player.getDeltaMovement().y > 0.1D || !player.onGround()) {
			jumped.add(player.getUUID());
		}
		return jumped.contains(player.getUUID()) && player.onGround();
	}
}

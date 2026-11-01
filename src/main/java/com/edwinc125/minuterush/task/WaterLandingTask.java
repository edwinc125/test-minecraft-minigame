package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class WaterLandingTask implements MinuteRushTask {
	private final Map<UUID, Boolean> falling = new HashMap<>();
	private final Map<UUID, Double> startY = new HashMap<>();

	@Override
	public String description() {
		return "Fall at least 3 blocks and land in water";
	}

	@Override
	public void begin(ServerPlayer player) {
		falling.put(player.getUUID(), false);
		startY.put(player.getUUID(), player.getY());
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.BLUE));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		UUID id = player.getUUID();
		if (!player.onGround() && player.getDeltaMovement().y < -0.08
				&& startY.getOrDefault(id, player.getY()) - player.getY() >= 3.0) {
			falling.put(id, true);
		}
		return falling.getOrDefault(id, false) && player.isInWater();
	}
}

package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TravelTask implements MinuteRushTask {
	private final double distance;
	private final Map<UUID, double[]> starts = new HashMap<>();

	public TravelTask(double distance) {
		this.distance = distance;
	}

	@Override
	public String description() {
		return "Travel " + (int) distance + " blocks";
	}

	@Override
	public void begin(ServerPlayer player) {
		starts.put(player.getUUID(), new double[] {player.getX(), player.getZ()});
		player.sendSystemMessage(Component.literal(description()));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		double[] start = starts.get(player.getUUID());
		if (start == null) {
			begin(player);
			start = starts.get(player.getUUID());
		}
		return Math.hypot(player.getX() - start[0], player.getZ() - start[1]) >= distance;
	}
}

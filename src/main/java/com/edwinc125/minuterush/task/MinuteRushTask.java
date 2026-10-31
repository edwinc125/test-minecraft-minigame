package com.edwinc125.minuterush.task;

import net.minecraft.server.level.ServerPlayer;

public interface MinuteRushTask {
	String description();
	void begin(ServerPlayer player);
	boolean isComplete(ServerPlayer player);
}

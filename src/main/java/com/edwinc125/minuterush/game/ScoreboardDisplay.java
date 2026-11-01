package com.edwinc125.minuterush.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public final class ScoreboardDisplay {
	private static final String OBJECTIVE = "minute_rush";

	private ScoreboardDisplay() {}

	public static void refresh(MinecraftServer server, MinuteRushGame game) {
		ServerScoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective(OBJECTIVE);
		if (objective == null) {
			objective = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY,
				Component.literal("Minute Rush"), ObjectiveCriteria.RenderType.INTEGER, false, null);
			scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			scoreboard.getOrCreatePlayerScore(ScoreHolder.fromGameProfile(player.getGameProfile()), objective)
				.set(game.score(player.getUUID()));
		}
	}

	public static void clear(MinecraftServer server) {
		Objective objective = server.getScoreboard().getObjective(OBJECTIVE);
		if (objective != null) {
			server.getScoreboard().removeObjective(objective);
		}
	}
}

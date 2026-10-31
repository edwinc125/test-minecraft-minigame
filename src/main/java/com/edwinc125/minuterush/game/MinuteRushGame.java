package com.edwinc125.minuterush.game;

import com.edwinc125.minuterush.config.MinuteRushConfig;
import com.edwinc125.minuterush.task.MinuteRushTask;
import com.edwinc125.minuterush.task.TaskFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class MinuteRushGame {
	private final MinecraftServer server;
	private MinuteRushConfig config;
	private final Map<UUID, Integer> scores = new HashMap<>();
	private final Random random = new Random();
	private MatchState state = MatchState.IDLE;
	private MinuteRushTask task;
	private int ticksRemaining;

	public MinuteRushGame(MinecraftServer server) {
		this(server, MinuteRushConfig.DEFAULT);
	}

	public MinuteRushGame(MinecraftServer server, MinuteRushConfig config) {
		this.server = server;
		this.config = config;
	}

	public MinecraftServer server() {
		return server;
	}

	public MatchState state() {
		return state;
	}

	public int score(UUID playerId) {
		return scores.getOrDefault(playerId, 0);
	}

	public void setTargetScore(int targetScore) {
		config = config.withTargetScore(targetScore);
	}

	public void setRoundDurationSeconds(int roundDurationSeconds) {
		config = config.withRoundDurationSeconds(roundDurationSeconds);
	}

	public void playerJoined(ServerPlayer player) {
		scores.putIfAbsent(player.getUUID(), 0);
		player.sendSystemMessage(Component.literal("Minute Rush: use /minute-rush start when everyone is ready."));
	}

	public void playerLeft(ServerPlayer player) {
		if (state == MatchState.ROUND_ACTIVE && activePlayers().size() < 2) {
			reset("Not enough players remain; match reset.");
		}
	}

	public void tick() {
		if (state != MatchState.ROUND_ACTIVE) {
			return;
		}
		if (--ticksRemaining <= 0) {
			server.getPlayerList().broadcastSystemMessage(Component.literal("Time's up! The round is skipped."), false);
			startRound();
			return;
		}
		for (ServerPlayer player : activePlayers()) {
			if (task.isComplete(player)) {
				winRound(player);
				return;
			}
		}
	}

	public boolean blockUsed(ServerPlayer player, net.minecraft.core.BlockPos pos) {
		return state == MatchState.ROUND_ACTIVE && task != null && task.isComplete(player);
	}

	public String start() {
		if (activePlayers().size() < 2) {
			return "At least two players are required to start.";
		}
		scores.replaceAll((id, score) -> 0);
		state = MatchState.ROUND_ACTIVE;
		startRound();
		return "Minute Rush started!";
	}

	public String reset(String message) {
		state = MatchState.IDLE;
		task = null;
		ticksRemaining = 0;
		scores.replaceAll((id, score) -> 0);
		server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
		return message;
	}

	public String status() {
		return "State: " + state + ", target score: " + config.targetScore() + ", round seconds: " + config.roundDurationSeconds();
	}

	private void startRound() {
		if (state != MatchState.ROUND_ACTIVE) return;
		task = TaskFactory.next(server.overworld(), random);
		ticksRemaining = config.roundDurationSeconds() * 20;
		server.getPlayerList().broadcastSystemMessage(Component.literal("New round: " + task.description()), false);
		for (ServerPlayer player : activePlayers()) task.begin(player);
	}

	private void winRound(ServerPlayer winner) {
		int newScore = score(winner.getUUID()) + 1;
		scores.put(winner.getUUID(), newScore);
		server.getPlayerList().broadcastSystemMessage(Component.literal(winner.getGameProfile().name() + " wins the round! (" + newScore + "/" + config.targetScore() + ")"), false);
		if (newScore >= config.targetScore()) {
			state = MatchState.MATCH_OVER;
			server.getPlayerList().broadcastSystemMessage(Component.literal(winner.getGameProfile().name() + " wins the match! Use /minute-rush start to play again."), false);
		} else {
			startRound();
		}
	}

	private java.util.Collection<ServerPlayer> activePlayers() {
		return server.getPlayerList().getPlayers();
	}
}

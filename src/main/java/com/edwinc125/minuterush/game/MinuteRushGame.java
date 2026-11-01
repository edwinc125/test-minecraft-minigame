package com.edwinc125.minuterush.game;

import com.edwinc125.minuterush.config.MinuteRushConfig;
import com.edwinc125.minuterush.task.MinuteRushTask;
import com.edwinc125.minuterush.task.TaskFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.core.registries.BuiltInRegistries;

public final class MinuteRushGame {
	private final MinecraftServer server;
	private MinuteRushConfig config;
	private final Map<UUID, Integer> scores = new HashMap<>();
	private final java.util.Set<UUID> participants = new java.util.HashSet<>();
	private final Deque<String> recentTaskIds = new ArrayDeque<>();
	private final Random random = new Random();
	private MatchState state = MatchState.IDLE;
	private MinuteRushTask task;
	private int ticksRemaining;
	private int countdownTicks;

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
		if ((state == MatchState.ROUND_ACTIVE || state == MatchState.COUNTDOWN)
				&& participants.remove(player.getUUID()) && activePlayers().isEmpty()) {
			reset("Not enough players remain; match reset.");
		}
	}

	public void tick() {
		if (state == MatchState.COUNTDOWN) {
			if (--countdownTicks <= 0) {
				state = MatchState.ROUND_ACTIVE;
				startRound();
			}
			return;
		}
		if (state != MatchState.ROUND_ACTIVE) {
			return;
		}
		if (--ticksRemaining <= 0) {
			server.getPlayerList().broadcastSystemMessage(Component.literal("Time's up! The round is skipped."), false);
			title("Time's up!", "Round skipped");
			beginCountdown();
			return;
		}
		for (ServerPlayer player : activePlayers()) {
			if (task.isComplete(player)) {
				winRound(player);
				return;
			}
		}
	}

	public String start() {
		if (activePlayers().size() < 2) {
			return "At least two players are required to start.";
		}
		participants.clear();
		recentTaskIds.clear();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			participants.add(player.getUUID());
		}
		return beginMatch("Minute Rush started!");
	}

	public String startSolo(ServerPlayer player) {
		participants.clear();
		participants.add(player.getUUID());
		return beginMatch("Singleplayer test mode started. Use /minute-rush solo-exit to stop.");
	}

	private String beginMatch(String message) {
		scores.replaceAll((id, score) -> 0);
		recentTaskIds.clear();
		state = MatchState.COUNTDOWN;
		countdownTicks = 100;
		title("Minute Rush", "Get ready!");
		ScoreboardDisplay.refresh(server, this);
		return message;
	}

	public String reset(String message) {
		state = MatchState.IDLE;
		task = null;
		recentTaskIds.clear();
		ticksRemaining = 0;
		countdownTicks = 0;
		participants.clear();
		scores.replaceAll((id, score) -> 0);
		server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
		ScoreboardDisplay.clear(server);
		return message;
	}

	public String skipRound() {
		if (state != MatchState.ROUND_ACTIVE && state != MatchState.COUNTDOWN) {
			return "No round is active.";
		}
		server.getPlayerList().broadcastSystemMessage(Component.literal("Round skipped by operator."), false);
		beginCountdown();
		return "Round skipped.";
	}

	public String status() {
		String mode = participants.size() == 1 ? "singleplayer test" : "multiplayer";
		return "State: " + state + " (" + mode + "), target score: " + config.targetScore() + ", round seconds: " + config.roundDurationSeconds();
	}

	private void startRound() {
		if (state != MatchState.ROUND_ACTIVE) return;
		TaskFactory.Selection selection = TaskFactory.next(server.overworld(), random, Set.copyOf(recentTaskIds),
			activePlayers().size() > 1);
		task = selection.task();
		recentTaskIds.addLast(selection.id());
		while (recentTaskIds.size() > 3) {
			recentTaskIds.removeFirst();
		}
		ticksRemaining = config.roundDurationSeconds() * 20;
		server.getPlayerList().broadcastSystemMessage(Component.literal("New round: " + task.description()), false);
		title("NEW TASK", task.description());
		for (ServerPlayer player : activePlayers()) task.begin(player);
		ScoreboardDisplay.refresh(server, this);
	}

	private void beginCountdown() {
		state = MatchState.COUNTDOWN;
		countdownTicks = 60;
		title("Next round", "Starting soon...");
	}

	public void blockUsed(ServerPlayer player, net.minecraft.core.BlockPos pos) {
		if (state == MatchState.ROUND_ACTIVE && task instanceof com.edwinc125.minuterush.task.InteractionTask interaction) {
			String block = BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock()).getPath();
			interaction.record(player, block);
		}
	}

	public void entityUsed(ServerPlayer player, String target) {
		if (state == MatchState.ROUND_ACTIVE && task instanceof com.edwinc125.minuterush.task.InteractionTask interaction) {
			interaction.record(player, target);
		}
	}

	private void title(String title, String subtitle) {
		for (ServerPlayer player : activePlayers()) {
			player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 10));
			player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
			player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
		}
	}

	private void winRound(ServerPlayer winner) {
		int newScore = score(winner.getUUID()) + 1;
		scores.put(winner.getUUID(), newScore);
		String winnerName = winner.getGameProfile().name();
		server.getPlayerList().broadcastSystemMessage(Component.literal(winnerName + " wins the round! (" + newScore + "/" + config.targetScore() + ")"), false);
		title(winnerName + " wins!", "Round score: " + newScore + "/" + config.targetScore());
		ScoreboardDisplay.refresh(server, this);
		if (newScore >= config.targetScore()) {
			state = MatchState.MATCH_OVER;
			server.getPlayerList().broadcastSystemMessage(Component.literal(winner.getGameProfile().name() + " wins the match! Use /minute-rush start to play again."), false);
		} else {
			beginCountdown();
		}
	}

	private java.util.Collection<ServerPlayer> activePlayers() {
		return server.getPlayerList().getPlayers().stream()
			.filter(player -> participants.contains(player.getUUID()))
			.toList();
	}
}

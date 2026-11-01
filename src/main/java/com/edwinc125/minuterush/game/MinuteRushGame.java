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
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.core.registries.BuiltInRegistries;

public final class MinuteRushGame {
	private static final Set<String> INTERACTABLE_BLOCKS = Set.of(
		"crafting_table", "furnace", "blast_furnace", "smoker", "chest", "trapped_chest",
		"barrel", "hopper", "dispenser", "dropper", "lever", "stone_button", "oak_button",
		"oak_door", "oak_trapdoor", "oak_fence_gate", "bed", "cartography_table",
		"fletching_table", "loom", "smithing_table", "stonecutter", "grindstone");
	private final MinecraftServer server;
	private MinuteRushConfig config;
	private final Map<UUID, Integer> scores = new HashMap<>();
	private final java.util.Set<UUID> participants = new java.util.HashSet<>();
	private final Deque<String> recentTaskIds = new ArrayDeque<>();
	private final Map<UUID, String> previousStandingBlocks = new HashMap<>();
	private final Random random = new Random();
	private MatchState state = MatchState.IDLE;
	private MinuteRushTask task;
	private int ticksRemaining;
	private int countdownTicks;
	private final ServerBossEvent bossbar = new ServerBossEvent(
		UUID.fromString("a0e1e2d3-c4b5-4678-9012-3456789abcde"),
		Component.literal("Minute Rush").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
		BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

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

	public int taskScore() {
		return task == null ? 0 : 1;
	}

	public String taskDescription() {
		return task == null ? "Waiting for a round" : task.description();
	}

	public int timeScore() {
		return Math.max(0, (ticksRemaining + 19) / 20);
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
			} else {
				updatePresentation();
			}
			return;
		}
		if (state != MatchState.ROUND_ACTIVE) {
			return;
		}
		if (--ticksRemaining <= 0) {
			server.getPlayerList().broadcastSystemMessage(Component.literal("Time's up! The round is skipped."), false);
			title("Time's up!", "Round skipped");
			playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.9f, 0.7f);
			beginCountdown();
			return;
		}
		updatePresentation();
		for (ServerPlayer player : activePlayers()) {
			trackStandingBlock(player);
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
		updatePresentation();
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
		bossbar.removeAllPlayers();
		bossbar.setVisible(false);
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
		playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.2f);
		for (ServerPlayer player : activePlayers()) task.begin(player);
		for (ServerPlayer player : activePlayers()) {
			previousStandingBlocks.put(player.getUUID(), standingBlock(player));
		}
		ScoreboardDisplay.refresh(server, this);
		updatePresentation();
	}

	private void beginCountdown() {
		state = MatchState.COUNTDOWN;
		countdownTicks = 60;
		title("Next round", "Starting soon...");
		playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.8f, 1.0f);
		updatePresentation();
	}

	public void blockUsed(ServerPlayer player, net.minecraft.core.BlockPos pos) {
		if (state == MatchState.ROUND_ACTIVE && task instanceof com.edwinc125.minuterush.task.InteractionTask interaction) {
			String block = BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock()).getPath();
			if (isInteractableBlock(block) || interaction.accepts(block)) {
				interaction.record(player, block);
			}
		}
	}

	private void trackStandingBlock(ServerPlayer player) {
		String current = standingBlock(player);
		String previous = previousStandingBlocks.put(player.getUUID(), current);
		if (state == MatchState.ROUND_ACTIVE
				&& task instanceof com.edwinc125.minuterush.task.InteractionTask interaction
				&& interaction.accepts(current)
				&& (previous == null || !interaction.accepts(previous))) {
			interaction.record(player, current);
		}
	}

	private static String standingBlock(ServerPlayer player) {
		return BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(player.getOnPos()).getBlock()).getPath();
	}

	private static boolean isInteractableBlock(String block) {
		return INTERACTABLE_BLOCKS.contains(block)
			|| block.endsWith("_bed") || block.endsWith("_door") || block.endsWith("_trapdoor")
			|| block.endsWith("_button") || block.endsWith("_fence_gate") || block.endsWith("_sign");
	}

	public void entityUsed(ServerPlayer player, String target) {
		if (state == MatchState.ROUND_ACTIVE && task instanceof com.edwinc125.minuterush.task.InteractionTask interaction) {
			interaction.record(player, target);
		}
	}

	private void title(String title, String subtitle) {
		for (ServerPlayer player : activePlayers()) {
			player.connection.send(new ClientboundSetTitlesAnimationPacket(
				TitleTimings.FADE_IN_TICKS, TitleTimings.STAY_TICKS, TitleTimings.FADE_OUT_TICKS));
			player.connection.send(new ClientboundSetTitleTextPacket(
				Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
			player.connection.send(new ClientboundSetSubtitleTextPacket(
				Component.literal(subtitle).withStyle(ChatFormatting.WHITE)));
		}
	}

	private void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		Holder<net.minecraft.sounds.SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
		for (ServerPlayer player : activePlayers()) {
			player.connection.send(new ClientboundSoundPacket(
				holder, SoundSource.MASTER, player.getX(), player.getY(), player.getZ(),
				volume, pitch, random.nextLong()));
		}
	}

	private void updatePresentation() {
		for (ServerPlayer player : activePlayers()) {
			bossbar.addPlayer(player);
		}
		bossbar.setVisible(state == MatchState.COUNTDOWN || state == MatchState.ROUND_ACTIVE);
		if (state == MatchState.ROUND_ACTIVE && ticksRemaining > 0) {
			bossbar.setName(Component.literal(task.description() + "  |  " + timeScore() + "s")
				.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
			bossbar.setProgress(Math.max(0.0f, Math.min(1.0f,
				(float) ticksRemaining / (config.roundDurationSeconds() * 20.0f))));
		} else if (state == MatchState.COUNTDOWN) {
			bossbar.setName(Component.literal("Next round in " + Math.max(1, (countdownTicks + 19) / 20) + "s")
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
			bossbar.setProgress(Math.max(0.0f, Math.min(1.0f, countdownTicks / 100.0f)));
		}
		ScoreboardDisplay.refresh(server, this);
	}

	private void winRound(ServerPlayer winner) {
		int newScore = score(winner.getUUID()) + 1;
		scores.put(winner.getUUID(), newScore);
		String winnerName = winner.getGameProfile().name();
		server.getPlayerList().broadcastSystemMessage(Component.literal(winnerName + " wins the round! (" + newScore + "/" + config.targetScore() + ")"), false);
		title(winnerName + " wins!", "Round score: " + newScore + "/" + config.targetScore());
		playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.4f);
		playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
		ScoreboardDisplay.refresh(server, this);
		if (newScore >= config.targetScore()) {
			state = MatchState.MATCH_OVER;
			playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
			bossbar.setVisible(false);
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

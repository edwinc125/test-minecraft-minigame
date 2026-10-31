package com.edwinc125.minuterush;

import com.edwinc125.minuterush.command.MinuteRushCommands;
import com.edwinc125.minuterush.game.MinuteRushGame;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

public final class MinuteRushMod implements ModInitializer {
	public static final String MOD_ID = "minute_rush";
	private static MinuteRushGame game;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> game = new MinuteRushGame(server));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> game = null);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (game != null) {
				game.tick();
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (game != null) {
				game.playerJoined(handler.getPlayer());
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (game != null) {
				game.playerLeft(handler.getPlayer());
			}
		});
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (game == null || world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}
			return game.blockUsed(serverPlayer, hitResult.getBlockPos()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			MinuteRushCommands.register(dispatcher, () -> game));
	}

	public static void broadcast(String message) {
		if (game != null) {
			game.server().getPlayerList().broadcastSystemMessage(Component.literal(message), false);
		}
	}
}

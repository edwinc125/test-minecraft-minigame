package com.edwinc125.minuterush.command;

import com.edwinc125.minuterush.game.MinuteRushGame;
import java.util.function.Supplier;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public final class MinuteRushCommands {
	private MinuteRushCommands() {}

	public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
			Supplier<MinuteRushGame> gameSupplier) {
		dispatcher.register(Commands.literal("minute-rush")
			.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
			.then(Commands.literal("start").executes(context -> execute(context.getSource(), gameSupplier, "start")))
			.then(Commands.literal("reset").executes(context -> execute(context.getSource(), gameSupplier, "reset")))
			.then(Commands.literal("config")
				.then(Commands.literal("score")
					.then(Commands.argument("value", IntegerArgumentType.integer(1))
						.executes(context -> configure(context.getSource(), gameSupplier, "score",
							IntegerArgumentType.getInteger(context, "value")))))
				.then(Commands.literal("duration")
					.then(Commands.argument("seconds", IntegerArgumentType.integer(1))
						.executes(context -> configure(context.getSource(), gameSupplier, "duration",
							IntegerArgumentType.getInteger(context, "seconds"))))))
			.then(Commands.literal("status").requires(source -> true)
				.executes(context -> status(context.getSource(), gameSupplier))));
	}

	private static int configure(CommandSourceStack source, Supplier<MinuteRushGame> supplier, String setting, int value) {
		MinuteRushGame game = supplier.get();
		if (game == null) {
			source.sendFailure(Component.literal("The server is not ready."));
			return 0;
		}
		if (setting.equals("score")) {
			game.setTargetScore(value);
		} else {
			game.setRoundDurationSeconds(value);
		}
		source.sendSuccess(() -> Component.literal("Updated " + setting + " to " + value + "."), true);
		return 1;
	}

	private static int execute(CommandSourceStack source, Supplier<MinuteRushGame> supplier, String action) {
		MinuteRushGame game = supplier.get();
		if (game == null) {
			source.sendFailure(Component.literal("The server is not ready."));
			return 0;
		}
		String message = action.equals("start") ? game.start() : game.reset("Match reset.");
		source.sendSuccess(() -> Component.literal(message), true);
		return 1;
	}

	private static int status(CommandSourceStack source, Supplier<MinuteRushGame> supplier) {
		MinuteRushGame game = supplier.get();
		if (game == null) {
			source.sendFailure(Component.literal("The server is not ready."));
			return 0;
		}
		source.sendSuccess(() -> Component.literal(game.status()), false);
		return 1;
	}
}

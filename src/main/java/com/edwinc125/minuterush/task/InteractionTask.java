package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class InteractionTask implements MinuteRushTask {
	public enum Kind { PLAYER, MOB, BLOCK }

	private final Kind kind;
	private final String target;
	private final int required;
	private final Map<UUID, Integer> progress = new HashMap<>();

	public InteractionTask(Kind kind, String target, int required) {
		this.kind = kind;
		this.target = target;
		this.required = required;
	}

	@Override
	public String description() {
		return switch (kind) {
			case PLAYER -> "Interact with another player";
			case MOB -> "Interact with a " + target;
			case BLOCK -> "Interact with " + ("any-block".equals(target) ? "an interactable block" : "a " + target);
		};
	}

	@Override
	public void begin(ServerPlayer player) {
		progress.put(player.getUUID(), 0);
		player.sendSystemMessage(Component.literal(description()));
	}

	public void record(ServerPlayer player, String targetId) {
		if (accepts(targetId)) {
			progress.merge(player.getUUID(), 1, Integer::sum);
		}
	}

	public boolean accepts(String targetId) {
		return (kind == Kind.PLAYER && "player".equals(targetId))
			|| (kind == Kind.MOB && ("any-mob".equals(target) || target.equals(targetId)))
			|| (kind == Kind.BLOCK && ("any-block".equals(target) || target.equals(targetId)));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return progress.getOrDefault(player.getUUID(), 0) >= required;
	}
}

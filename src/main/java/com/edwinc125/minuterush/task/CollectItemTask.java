package com.edwinc125.minuterush.task;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CollectItemTask implements MinuteRushTask {
	private final Item item;
	private final int count;
	private final java.util.Map<java.util.UUID, Integer> baselines = new java.util.HashMap<>();

	public CollectItemTask(Item item, int count) {
		this.item = item;
		this.count = count;
	}

	@Override
	public String description() {
		return "Collect " + count + " " + item.toString();
	}

	@Override
	public void begin(ServerPlayer player) {
		player.sendSystemMessage(Component.literal(description()));
		baselines.put(player.getUUID(), player.getInventory().countItem(item));
		if (count > 1) {
			player.getInventory().add(new ItemStack(item, count - 1));
		}
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return player.getInventory().countItem(item) >= CollectProgress.requiredTotal(
			baselines.getOrDefault(player.getUUID(), 0), count);
	}
}

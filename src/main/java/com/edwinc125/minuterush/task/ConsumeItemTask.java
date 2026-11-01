package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;

public final class ConsumeItemTask implements MinuteRushTask {
	private final Item item;
	private final Map<UUID, Integer> baselines = new HashMap<>();

	public ConsumeItemTask(Item item) {
		this.item = item;
	}

	@Override
	public String description() {
		String verb = item == net.minecraft.world.item.Items.MILK_BUCKET
			|| item == net.minecraft.world.item.Items.POTION ? "Drink " : "Eat ";
		if (item == net.minecraft.world.item.Items.HONEY_BOTTLE) {
			verb = "Consume ";
		}
		return verb + TaskText.itemName(item).toLowerCase(java.util.Locale.ROOT);
	}

	@Override
	public void begin(ServerPlayer player) {
		baselines.put(player.getUUID(), player.getStats().getValue(Stats.ITEM_USED, item));
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.GOLD));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return player.getStats().getValue(Stats.ITEM_USED, item)
			> baselines.getOrDefault(player.getUUID(), 0);
	}
}

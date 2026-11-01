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
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Items;
import com.edwinc125.minuterush.task.EntityTaskProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.material.Fluids;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MinuteRushMod implements ModInitializer {
	public static final String MOD_ID = "minute_rush";
	private static MinuteRushGame game;
	private static final Map<UUID, Boolean> PASSENGER_STATE = new HashMap<>();
	private static final Map<UUID, String> MAIN_HAND_STATE = new HashMap<>();
	private static final Map<UUID, String> OFF_HAND_STATE = new HashMap<>();

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> game = new MinuteRushGame(server));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> game = null);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (game != null) {
				game.tick();
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				recordMovementEvents(player);
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
			ItemStack heldBefore = serverPlayer.getItemInHand(hand).copy();
			String block = blockId(serverPlayer, hitResult.getBlockPos());
			EntityTaskProgress.record(serverPlayer.getUUID(), "block:use:" + block);
			recordBlockUse(serverPlayer, block);
			if (isSeed(heldBefore.getItem()) || isBucket(heldBefore.getItem())) {
				recordSuccessfulItemUse(serverPlayer, hand, hitResult, heldBefore);
			}
			if (serverPlayer.isCrouching()) {
				EntityTaskProgress.record(serverPlayer.getUUID(), "block:place:crouch");
			}
			if (block.endsWith("_door") && !world.getBlockState(hitResult.getBlockPos()).getValue(net.minecraft.world.level.block.DoorBlock.OPEN)) {
				EntityTaskProgress.record(serverPlayer.getUUID(), "block:open:door");
			}
			if (block.endsWith("_trapdoor") && !world.getBlockState(hitResult.getBlockPos()).getValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN)) {
				EntityTaskProgress.record(serverPlayer.getUUID(), "block:open:trapdoor");
			}
			if (block.contains("campfire")) {
				EntityTaskProgress.record(serverPlayer.getUUID(), "block:use:campfire");
				BlockState state = world.getBlockState(hitResult.getBlockPos());
				if (state.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)
						&& !state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)
						&& serverPlayer.getItemInHand(hand).is(Items.FLINT_AND_STEEL)) {
					recordSuccessfulCampfireLighting(serverPlayer, hitResult.getBlockPos());
				}
				if (state.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)
						&& state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)
						&& BuiltInRegistries.ITEM.getKey(serverPlayer.getItemInHand(hand).getItem())
							.getPath().endsWith("_shovel")) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "block:campfire:extinguish");
				}
			}
			game.blockUsed(serverPlayer, hitResult.getBlockPos());
			return InteractionResult.PASS;
		});
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (game != null && !world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				ItemStack heldBefore = serverPlayer.getItemInHand(hand).copy();
				String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
				if (isBoat(heldBefore.getItem()) && !entity.isPassenger()) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "use:boat-mob");
				}
				if (isFeedItem(heldBefore.getItem(), entityId)) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "feed:" + feedTarget(entityId));
				}
				if (entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep
						&& heldBefore.is(Items.SHEARS) && !sheep.isSheared()) {
					recordSuccessfulShear(serverPlayer, hand, sheep,
							sheep.getColor().getName());
				}
				if (entity instanceof net.minecraft.world.entity.animal.cow.Cow
						&& heldBefore.is(Items.BUCKET)) {
					recordSuccessfulMilking(serverPlayer, hand, entity, "use:milk-cow");
				}
				if (entity instanceof net.minecraft.world.entity.animal.cow.MushroomCow
						&& heldBefore.is(Items.BUCKET)) {
					recordSuccessfulMilking(serverPlayer, hand, entity, "use:milk-mooshroom");
				}
				game.entityUsed(serverPlayer, entity instanceof ServerPlayer ? "player" : entityId);
			}
			return InteractionResult.PASS;
		});
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (game != null && !world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
				EntityTaskProgress.record(serverPlayer.getUUID(), "hit:" + entityId);
				if (serverPlayer.isSprinting()) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "combat:sprint-hit");
				}
				if (serverPlayer.isCrouching()) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "combat:crouch-hit");
				}
				if (!serverPlayer.onGround() && serverPlayer.fallDistance > 0.0f
						&& !serverPlayer.isSprinting() && !serverPlayer.isPassenger()) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "combat:critical");
				}
				game.entityUsed(serverPlayer, entity instanceof ServerPlayer ? "player" : entityId);
			}
			return InteractionResult.PASS;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof ServerPlayer player) {
				if (source.is(DamageTypeTags.IS_FALL)) {
					EntityTaskProgress.record(player.getUUID(), "damage:fall");
				}
				if ("cactus".equals(source.getMsgId())) {
					EntityTaskProgress.record(player.getUUID(), "damage:cactus");
				}
				if (source.is(DamageTypeTags.IS_PROJECTILE)) {
					EntityTaskProgress.record(player.getUUID(), "damage:projectile");
				}
				if (DamageTypeTags.IS_FIRE != null && source.is(DamageTypeTags.IS_FIRE)) {
					EntityTaskProgress.record(player.getUUID(), "damage:fire");
				}
				if ("lava".equals(source.getMsgId())) {
					EntityTaskProgress.record(player.getUUID(), "damage:lava");
				}
				if ("sweetBerryBush".equals(source.getMsgId())) {
					EntityTaskProgress.record(player.getUUID(), "damage:sweet-berry");
				}
				if (blocked) {
					EntityTaskProgress.record(player.getUUID(), "combat:shield-block");
				}
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer player) {
				if (entity.getType().getCategory() == MobCategory.MONSTER) {
					EntityTaskProgress.record(player.getUUID(), "kill:hostile");
				}
				if (entity instanceof Animal) {
					EntityTaskProgress.record(player.getUUID(), "kill:animal");
				}
				EntityTaskProgress.record(player.getUUID(),
					"kill:" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
			}
		});
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer && serverPlayer.isCrouching()) {
				EntityTaskProgress.record(serverPlayer.getUUID(), "block:break:crouch");
			}
			if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				recordBlockBreak(serverPlayer, blockId(serverPlayer, pos));
			}
			return InteractionResult.PASS;
		});
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				Item item = serverPlayer.getItemInHand(hand).getItem();
				if (isBucket(item)) {
					return InteractionResult.PASS;
				}
				String itemId = BuiltInRegistries.ITEM.getKey(item).getPath();
				EntityTaskProgress.record(serverPlayer.getUUID(), "use:" + itemId);
				if ("flint_and_steel".equals(itemId)) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "block:use:campfire");
				}
			}
			return InteractionResult.PASS;
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			MinuteRushCommands.register(dispatcher, () -> game));
	}

	private static void recordMovementEvents(ServerPlayer player) {
		boolean passenger = player.isPassenger();
		boolean previous = PASSENGER_STATE.getOrDefault(player.getUUID(), passenger);
		if (passenger && !previous) {
			EntityTaskProgress.record(player.getUUID(), "vehicle:enter");
		} else if (!passenger && previous) {
			EntityTaskProgress.record(player.getUUID(), "vehicle:exit");
		}
		PASSENGER_STATE.put(player.getUUID(), passenger);
		String block = blockId(player, player.blockPosition());
		boolean inWater = player.isInWater();
		boolean previousWater = WATER_STATE.getOrDefault(player.getUUID(), inWater);
		if (inWater && !previousWater) {
			EntityTaskProgress.record(player.getUUID(), "environment:enter-water");
		} else if (!inWater && previousWater) {
			EntityTaskProgress.record(player.getUUID(), "environment:exit-water");
		}
		WATER_STATE.put(player.getUUID(), inWater);
		if (block.contains("powder_snow")) {
			EntityTaskProgress.record(player.getUUID(), "environment:powder-snow");
		}
		if (block.contains("ladder")) {
			EntityTaskProgress.record(player.getUUID(), "climb:ladder");
		} else if (block.contains("scaffolding")) {
			EntityTaskProgress.record(player.getUUID(), "climb:scaffolding");
		} else if (block.contains("vine")) {
			EntityTaskProgress.record(player.getUUID(), "climb:vines");
		}
		int drops = player.getStats().getValue(Stats.CUSTOM, Stats.DROP);
		int previousDrops = DROP_STATE.getOrDefault(player.getUUID(), drops);
		if (drops > previousDrops) {
			EntityTaskProgress.record(player.getUUID(), "inventory:drop");
		}
		DROP_STATE.put(player.getUUID(), drops);
		String main = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).getPath();
		String off = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).getPath();
		String previousMain = MAIN_HAND_STATE.put(player.getUUID(), main);
		String previousOff = OFF_HAND_STATE.put(player.getUUID(), off);
		if (previousMain != null && previousOff != null && previousMain.equals(off) && previousOff.equals(main)) {
			EntityTaskProgress.record(player.getUUID(), "inventory:swap-offhand");
		}
	}

	private static final Map<UUID, Integer> DROP_STATE = new HashMap<>();
	private static final Map<UUID, Boolean> WATER_STATE = new HashMap<>();

	private static String blockId(ServerPlayer player, net.minecraft.core.BlockPos pos) {
		return BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock()).getPath();
	}

	private static void recordBlockUse(ServerPlayer player, String block) {
		if (block.endsWith("_button")) {
			EntityTaskProgress.record(player.getUUID(), "block:use:button");
		} else if ("lever".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:use:lever");
		} else if ("bell".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:use:bell");
		} else if ("chest".equals(block) || "trapped_chest".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:use:chest");
		} else if ("barrel".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:use:barrel");
		} else if (block.endsWith("furnace")) {
			EntityTaskProgress.record(player.getUUID(), "block:use:furnace");
		} else if ("crafting_table".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:use:crafting_table");
		}
		String held = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).getPath();
		if (held.endsWith("_sign")) {
			EntityTaskProgress.record(player.getUUID(), "block:place:sign");
		}
		if (held.endsWith("_banner")) {
			EntityTaskProgress.record(player.getUUID(), "block:place:banner");
		}
		if ("flower_pot".equals(held)) {
			EntityTaskProgress.record(player.getUUID(), "block:place:flower-pot");
		}
	}

	private static boolean isSeed(Item item) {
		String id = BuiltInRegistries.ITEM.getKey(item).getPath();
		return id.endsWith("_seeds") || "wheat".equals(id) || "carrots".equals(id)
				|| "potatoes".equals(id) || "beetroot_seeds".equals(id);
	}

	private static boolean isBucket(Item item) {
		return item == Items.BUCKET || item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET;
	}

	private static void recordSuccessfulItemUse(ServerPlayer player, InteractionHand hand,
			net.minecraft.world.phys.BlockHitResult hitResult, ItemStack heldBefore) {
		BlockState clickedBefore = player.level().getBlockState(hitResult.getBlockPos());
		net.minecraft.core.BlockPos adjacentPos = hitResult.getBlockPos().relative(hitResult.getDirection());
		BlockState adjacentBefore = player.level().getBlockState(adjacentPos);
		player.level().getServer().execute(() -> {
			if (game == null || !player.isAlive()) {
				return;
			}
			Item beforeItem = heldBefore.getItem();
			Item afterItem = player.getItemInHand(hand).getItem();
			if (isSeed(beforeItem) && plantedCrop(player, hitResult.getBlockPos(), clickedBefore,
					adjacentPos, adjacentBefore)) {
				EntityTaskProgress.record(player.getUUID(), "block:place:seed");
			}
			if (isBucket(beforeItem) && successfulBucketUse(player, beforeItem, afterItem,
					hitResult.getBlockPos(), adjacentPos)) {
				EntityTaskProgress.record(player.getUUID(), "use:bucket");
			}
		});
	}

	private static void recordSuccessfulMilking(ServerPlayer player, InteractionHand hand,
			Entity entity, String event) {
		player.level().getServer().execute(() -> {
			if (game != null && entity.isAlive()
					&& player.getItemInHand(hand).is(Items.MILK_BUCKET)) {
				EntityTaskProgress.record(player.getUUID(), event);
			}
		});
	}

	private static void recordSuccessfulShear(ServerPlayer player, InteractionHand hand,
			net.minecraft.world.entity.animal.sheep.Sheep sheep, String color) {
		player.level().getServer().execute(() -> {
			if (game == null || !sheep.isAlive()) {
				return;
			}
			if (sheep.isSheared()) {
				EntityTaskProgress.record(player.getUUID(), "use:shear-sheep");
				EntityTaskProgress.record(player.getUUID(), "use:shear-sheep:" + color);
			}
		});
	}

	private static void recordSuccessfulCampfireLighting(ServerPlayer player,
			net.minecraft.core.BlockPos pos) {
		player.level().getServer().execute(() -> {
			BlockState state = player.level().getBlockState(pos);
			if (game != null && state.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)
					&& state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)) {
				EntityTaskProgress.record(player.getUUID(), "block:campfire:light");
			}
		});
	}

	private static boolean plantedCrop(ServerPlayer player, net.minecraft.core.BlockPos clickedPos,
			BlockState clickedBefore, net.minecraft.core.BlockPos adjacentPos, BlockState adjacentBefore) {
		BlockState clickedAfter = player.level().getBlockState(clickedPos);
		BlockState adjacentAfter = player.level().getBlockState(adjacentPos);
		return ("farmland".equals(blockName(clickedBefore)) && isNewCrop(clickedBefore, clickedAfter))
				|| isNewCrop(adjacentBefore, adjacentAfter)
				|| isNewCrop(clickedBefore, clickedAfter);
	}

	private static boolean isNewCrop(BlockState before, BlockState after) {
		return !(before.getBlock() instanceof CropBlock) && after.getBlock() instanceof CropBlock;
	}

	private static boolean successfulBucketUse(ServerPlayer player, Item before, Item after,
			net.minecraft.core.BlockPos clickedPos, net.minecraft.core.BlockPos adjacentPos) {
		boolean filled = before == Items.BUCKET
				&& (after == Items.WATER_BUCKET || after == Items.LAVA_BUCKET);
		boolean placedWater = before == Items.WATER_BUCKET && after == Items.BUCKET
				&& (hasWater(player, clickedPos) || hasWater(player, adjacentPos));
		return filled || placedWater;
	}

	private static boolean hasWater(ServerPlayer player, net.minecraft.core.BlockPos pos) {
		return player.level().getFluidState(pos).is(Fluids.WATER);
	}

	private static String blockName(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
	}

	private static void recordBlockBreak(ServerPlayer player, String block) {
		EntityTaskProgress.record(player.getUUID(), "block:break:" + block);
		if (block.endsWith("_leaves")) {
			EntityTaskProgress.record(player.getUUID(), "block:break:leaves");
		}
		if (block.endsWith("_door")) {
			EntityTaskProgress.record(player.getUUID(), "block:break:door");
		}
		if (block.endsWith("_bed")) {
			EntityTaskProgress.record(player.getUUID(), "block:break:bed");
		}
		if ("torch".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:break:torch");
		}
		if (block.endsWith("_crop") || "wheat".equals(block) || "carrots".equals(block)
				|| "potatoes".equals(block) || "beetroots".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:break:crop");
		}
	}

	private static boolean isBoat(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).getPath().endsWith("boat");
	}

	private static boolean isFeedItem(Item item, String entityId) {
		String id = BuiltInRegistries.ITEM.getKey(item).getPath();
		return switch (entityId) {
			case "cow", "sheep" -> "wheat".equals(id);
			case "pig" -> id.equals("carrot") || id.equals("potato") || id.equals("beetroot");
			case "chicken" -> id.endsWith("_seeds");
			case "wolf" -> id.endsWith("_meat") || id.equals("rotten_flesh");
			case "cat" -> id.equals("cod") || id.equals("salmon");
			case "horse", "donkey" -> id.equals("wheat") || id.equals("hay_block")
				|| id.equals("golden_apple") || id.equals("golden_carrot");
			default -> false;
		};
	}

	private static String feedTarget(String entityId) {
		return switch (entityId) {
			case "cow", "sheep", "pig", "chicken", "wolf", "cat" -> entityId;
			default -> "unknown";
		};
	}

	public static void broadcast(String message) {
		if (game != null) {
			game.server().getPlayerList().broadcastSystemMessage(Component.literal(message), false);
		}
	}
}

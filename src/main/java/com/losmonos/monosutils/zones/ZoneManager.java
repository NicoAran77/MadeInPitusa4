package com.losmonos.monosutils.zones;

import com.losmonos.monosutils.config.Config;
import com.losmonos.monosutils.config.Zone;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZoneManager {

	private static final class Sel {
		String world;
		BlockPos p1;
		BlockPos p2;
	}

	private static final Map<UUID, Sel> SELECTIONS = new HashMap<>();

	// ---------- Helpers ----------

	public static String worldId(Level level) {
		return level.dimension().location().toString();
	}

	public static boolean isOp(Player player) {
		MinecraftServer server = player.getServer();
		return server != null && server.getPlayerList().isOp(player.getGameProfile());
	}

	public static boolean isExempt(Player player) {
		return isOp(player) || player.getTags().contains(Config.INSTANCE.exemptTag);
	}

	private static boolean holdingWand(Player player) {
		return player.getMainHandItem().is(Items.GOLDEN_AXE);
	}

	// ---------- Eventos ----------

	public static void registerEvents() {
		// Romper bloques
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
			if (isExempt(player)) return true;
			for (Zone z : Config.INSTANCE.zones.values()) {
				if (z.breakBlocks && z.contains(worldId(world), pos.getX(), pos.getY(), pos.getZ())) {
					if (player instanceof ServerPlayer sp) {
						sp.displayClientMessage(Component.literal("No puedes romper bloques en la zona '" + z.name + "'."), true);
					}
					return false;
				}
			}
			return true;
		});

		// Click izquierdo con la varita = pos1
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (hand == InteractionHand.MAIN_HAND && isOp(player) && holdingWand(player)) {
				Sel sel = SELECTIONS.computeIfAbsent(player.getUUID(), k -> new Sel());
				sel.world = worldId(world);
				sel.p1 = pos.immutable();
				player.displayClientMessage(Component.literal("pos1 = " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), false);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		// Click derecho: varita = pos2, o proteccion de "poner bloques"
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (hand == InteractionHand.MAIN_HAND && isOp(player) && holdingWand(player)) {
				Sel sel = SELECTIONS.computeIfAbsent(player.getUUID(), k -> new Sel());
				sel.world = worldId(world);
				sel.p2 = hit.getBlockPos().immutable();
				player.displayClientMessage(Component.literal("pos2 = " + hit.getBlockPos().getX() + " " + hit.getBlockPos().getY() + " " + hit.getBlockPos().getZ()), false);
				return InteractionResult.SUCCESS;
			}
			// Proteccion de colocacion (solo si sostiene un bloque)
			if (!isExempt(player) && player.getItemInHand(hand).getItem() instanceof BlockItem) {
				BlockPos placePos = hit.getBlockPos().relative(hit.getDirection());
				for (Zone z : Config.INSTANCE.zones.values()) {
					if (z.place && z.contains(worldId(world), placePos.getX(), placePos.getY(), placePos.getZ())) {
						if (player instanceof ServerPlayer sp) {
							sp.displayClientMessage(Component.literal("No puedes poner bloques en la zona '" + z.name + "'."), true);
						}
						return InteractionResult.FAIL;
					}
				}
			}
			return InteractionResult.PASS;
		});

		// PvP
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (entity instanceof Player victim) {
				for (Zone z : Config.INSTANCE.zones.values()) {
					if (z.pvp && z.contains(worldId(world), (int) Math.floor(victim.getX()), (int) Math.floor(victim.getY()), (int) Math.floor(victim.getZ()))) {
						return InteractionResult.FAIL;
					}
				}
			}
			return InteractionResult.PASS;
		});

		// Todo el dano
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			String w = worldId(entity.level());
			int x = (int) Math.floor(entity.getX());
			int y = (int) Math.floor(entity.getY());
			int zc = (int) Math.floor(entity.getZ());
			for (Zone z : Config.INSTANCE.zones.values()) {
				if (z.alldamage && z.contains(w, x, y, zc)) return false;
			}
			return true;
		});

		// Bloqueo de spawn de hostiles
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!(entity instanceof Enemy)) return;
			String w = worldId(world);
			int x = (int) Math.floor(entity.getX());
			int y = (int) Math.floor(entity.getY());
			int zc = (int) Math.floor(entity.getZ());
			for (Zone z : Config.INSTANCE.zones.values()) {
				if (z.spawnblock && z.contains(w, x, y, zc)) {
					entity.discard();
					return;
				}
			}
		});
	}

	// ---------- Comandos ----------

	public static void registerCommands(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("zone")
			.requires(s -> s.hasPermission(2))
			.then(Commands.literal("wand").executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				ItemStack stack = new ItemStack(Items.GOLDEN_AXE);
				stack.set(DataComponents.CUSTOM_NAME, Component.literal("Varita de Zonas"));
				p.getInventory().add(stack);
				ctx.getSource().sendSuccess(() -> Component.literal("Varita entregada. Click izq = pos1, click der = pos2."), false);
				return 1;
			}))
			.then(Commands.literal("pos1").executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				Sel sel = SELECTIONS.computeIfAbsent(p.getUUID(), k -> new Sel());
				sel.world = worldId(p.level());
				sel.p1 = p.blockPosition().immutable();
				ctx.getSource().sendSuccess(() -> Component.literal("pos1 = " + sel.p1.getX() + " " + sel.p1.getY() + " " + sel.p1.getZ()), false);
				return 1;
			}))
			.then(Commands.literal("pos2").executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				Sel sel = SELECTIONS.computeIfAbsent(p.getUUID(), k -> new Sel());
				sel.world = worldId(p.level());
				sel.p2 = p.blockPosition().immutable();
				ctx.getSource().sendSuccess(() -> Component.literal("pos2 = " + sel.p2.getX() + " " + sel.p2.getY() + " " + sel.p2.getZ()), false);
				return 1;
			}))
			.then(Commands.literal("create")
				.then(Commands.argument("nombre", StringArgumentType.word()).executes(ctx ->
					createOrRedefine(ctx.getSource(), StringArgumentType.getString(ctx, "nombre"), false))))
			.then(Commands.literal("redefine")
				.then(Commands.argument("nombre", StringArgumentType.word()).executes(ctx ->
					createOrRedefine(ctx.getSource(), StringArgumentType.getString(ctx, "nombre"), true))))
			.then(Commands.literal("remove")
				.then(Commands.argument("nombre", StringArgumentType.word()).executes(ctx -> {
					String name = StringArgumentType.getString(ctx, "nombre");
					if (Config.INSTANCE.zones.remove(name) != null) {
						Config.save();
						ctx.getSource().sendSuccess(() -> Component.literal("Zona '" + name + "' eliminada."), false);
					} else {
						ctx.getSource().sendFailure(Component.literal("No existe la zona '" + name + "'."));
					}
					return 1;
				})))
			.then(Commands.literal("list").executes(ctx -> {
				if (Config.INSTANCE.zones.isEmpty()) {
					ctx.getSource().sendSuccess(() -> Component.literal("No hay zonas."), false);
				} else {
					ctx.getSource().sendSuccess(() -> Component.literal("Zonas: " + String.join(", ", Config.INSTANCE.zones.keySet())), false);
				}
				return 1;
			}))
			.then(Commands.literal("info")
				.then(Commands.argument("nombre", StringArgumentType.word()).executes(ctx -> {
					String name = StringArgumentType.getString(ctx, "nombre");
					Zone z = Config.INSTANCE.zones.get(name);
					if (z == null) {
						ctx.getSource().sendFailure(Component.literal("No existe la zona '" + name + "'."));
						return 0;
					}
					ctx.getSource().sendSuccess(() -> Component.literal(
						"Zona '" + z.name + "' en " + z.world + "\n" +
						"  esquinas: (" + z.minX() + "," + z.minY() + "," + z.minZ() + ") -> (" + z.maxX() + "," + z.maxY() + "," + z.maxZ() + ")\n" +
						"  spawnblock=" + z.spawnblock + "  break=" + z.breakBlocks + "  place=" + z.place +
						"  pvp(bloqueado)=" + z.pvp + "  alldamage=" + z.alldamage + "  ignorey=" + z.ignorey), false);
					return 1;
				})))
			.then(Commands.literal("set")
				.then(Commands.argument("nombre", StringArgumentType.word())
					.then(Commands.argument("ajuste", StringArgumentType.word())
						.then(Commands.argument("valor", BoolArgumentType.bool()).executes(ctx -> {
							String name = StringArgumentType.getString(ctx, "nombre");
							String setting = StringArgumentType.getString(ctx, "ajuste").toLowerCase();
							boolean value = BoolArgumentType.getBool(ctx, "valor");
							Zone z = Config.INSTANCE.zones.get(name);
							if (z == null) {
								ctx.getSource().sendFailure(Component.literal("No existe la zona '" + name + "'."));
								return 0;
							}
							switch (setting) {
								case "spawnblock" -> z.spawnblock = value;
								case "break" -> z.breakBlocks = value;
								case "place" -> z.place = value;
								case "pvp" -> z.pvp = value;
								case "alldamage" -> z.alldamage = value;
								case "ignorey" -> z.ignorey = value;
								default -> {
									ctx.getSource().sendFailure(Component.literal("Ajuste invalido. Usa: spawnblock, break, place, pvp, alldamage, ignorey."));
									return 0;
								}
							}
							Config.save();
							ctx.getSource().sendSuccess(() -> Component.literal("'" + name + "' -> " + setting + " = " + value), false);
							return 1;
						})))))
			.then(Commands.literal("clear")
				.executes(ctx -> clearZones(ctx.getSource(), null))
				.then(Commands.argument("nombre", StringArgumentType.word()).executes(ctx ->
					clearZones(ctx.getSource(), StringArgumentType.getString(ctx, "nombre")))))
			.then(Commands.literal("reload").executes(ctx -> {
				Config.load();
				ctx.getSource().sendSuccess(() -> Component.literal("Config recargada."), false);
				return 1;
			}))
		);
	}

	private static int createOrRedefine(CommandSourceStack src, String name, boolean redefine) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = src.getPlayerOrException();
		Sel sel = SELECTIONS.get(p.getUUID());
		if (sel == null || sel.p1 == null || sel.p2 == null) {
			src.sendFailure(Component.literal("Marca pos1 y pos2 primero (varita o /zone pos1 /zone pos2)."));
			return 0;
		}
		boolean exists = Config.INSTANCE.zones.containsKey(name);
		if (exists && !redefine) {
			src.sendFailure(Component.literal("Ya existe '" + name + "'. Usa /zone redefine."));
			return 0;
		}
		if (!exists && redefine) {
			src.sendFailure(Component.literal("No existe '" + name + "'. Usa /zone create."));
			return 0;
		}
		Zone z = exists ? Config.INSTANCE.zones.get(name) : new Zone();
		z.name = name;
		z.world = sel.world;
		z.x1 = sel.p1.getX(); z.y1 = sel.p1.getY(); z.z1 = sel.p1.getZ();
		z.x2 = sel.p2.getX(); z.y2 = sel.p2.getY(); z.z2 = sel.p2.getZ();
		Config.INSTANCE.zones.put(name, z);
		Config.save();
		src.sendSuccess(() -> Component.literal((redefine ? "Zona '" : "Zona creada '") + name + "'."), false);
		return 1;
	}

	private static int clearZones(CommandSourceStack src, String name) {
		MinecraftServer server = src.getServer();
		int[] killed = {0};
		for (Zone z : Config.INSTANCE.zones.values()) {
			if (name != null && !z.name.equals(name)) continue;
			for (ServerLevel level : server.getAllLevels()) {
				if (!worldId(level).equals(z.world)) continue;
				for (Entity e : level.getAllEntities()) {
					if (e instanceof Enemy && z.contains(z.world, (int) Math.floor(e.getX()), (int) Math.floor(e.getY()), (int) Math.floor(e.getZ()))) {
						e.discard();
						killed[0]++;
					}
				}
			}
		}
		src.sendSuccess(() -> Component.literal("Hostiles eliminados: " + killed[0]), false);
		return 1;
	}
}

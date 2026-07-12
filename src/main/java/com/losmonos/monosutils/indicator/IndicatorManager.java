package com.losmonos.monosutils.indicator;

import com.losmonos.monosutils.config.Config;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

/**
 * Indicador social (NO ejecuta nada, solo muestra):
 *  - Circulo de color ①②③ segun nivel de prank (1 verde, 2 amarillo, 3 rojo), con descripcion al pasar el mouse en chat.
 *  - Estrella ★ de color segun cuantas ediciones de Made In Pitusa jugo el jugador.
 * Ambos van DESPUES del nombre (para no romper la deteccion de Chat Heads).
 */
public class IndicatorManager {

	// ---------- Construccion del sufijo ----------

	public static Component buildSuffix(Player player) {
		MutableComponent suffix = Component.empty();
		String uuid = player.getUUID().toString();

		Integer prank = Config.INSTANCE.prankLevels.get(uuid);
		if (prank != null) {
			suffix.append(Component.literal(" ")).append(circle(prank));
		}

		Integer editions = Config.INSTANCE.editionCounts.get(uuid);
		if (editions != null) {
			String hex = Config.INSTANCE.editionColors.get(editions);
			if (hex != null) {
				suffix.append(Component.literal(" ")).append(star(hex));
			}
		}
		return suffix;
	}

	private static MutableComponent circle(int level) {
		String ch = switch (level) { case 1 -> "\u2460"; case 2 -> "\u2461"; case 3 -> "\u2462"; default -> "?"; };
		int color = switch (level) { case 1 -> 0x55FF55; case 2 -> 0xFFFF55; case 3 -> 0xFF5555; default -> 0xFFFFFF; };
		String desc = Config.INSTANCE.prankDescriptions.getOrDefault(level, "");
		// API 1.21.11: HoverEvent es un record sellado -> HoverEvent.ShowText.
		Style style = Style.EMPTY.withColor(TextColor.fromRgb(color))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Prank nivel " + level + ": " + desc)));
		return Component.literal(ch).setStyle(style);
	}

	private static MutableComponent star(String hex) {
		return Component.literal("\u2605").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(parseHex(hex))));
	}

	private static int parseHex(String hex) {
		try { return Integer.parseInt(hex.replace("#", ""), 16); }
		catch (Exception e) { return 0xFFFFFF; }
	}

	// ---------- Refresco del TAB ----------

	public static void refreshTab(MinecraftServer server, ServerPlayer target) {
		try {
			ClientboundPlayerInfoUpdatePacket pkt = new ClientboundPlayerInfoUpdatePacket(
				EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), List.of(target));
			server.getPlayerList().broadcastAll(pkt);
		} catch (Throwable ignored) {}
	}

	// ---------- Comandos ----------

	public static void registerCommands(CommandDispatcher<CommandSourceStack> d) {
		// /prank
		d.register(Commands.literal("prank")
			.then(Commands.literal("set").requires(s -> s.hasPermission(2))
				.then(Commands.argument("jugador", EntityArgument.player())
					.then(Commands.argument("nivel", IntegerArgumentType.integer(1, 3)).executes(ctx -> {
						ServerPlayer target = EntityArgument.getPlayer(ctx, "jugador");
						int level = IntegerArgumentType.getInteger(ctx, "nivel");
						Config.INSTANCE.prankLevels.put(target.getUUID().toString(), level);
						Config.save();
						refreshTab(ctx.getSource().getServer(), target);
						ctx.getSource().sendSuccess(() -> Component.literal("Nivel de prank de " + target.getGameProfile().getName() + " = " + level), false);
						return 1;
					}))))
			.then(Commands.literal("clear").requires(s -> s.hasPermission(2))
				.then(Commands.argument("jugador", EntityArgument.player()).executes(ctx -> {
					ServerPlayer target = EntityArgument.getPlayer(ctx, "jugador");
					Config.INSTANCE.prankLevels.remove(target.getUUID().toString());
					Config.save();
					refreshTab(ctx.getSource().getServer(), target);
					ctx.getSource().sendSuccess(() -> Component.literal("Nivel de prank de " + target.getGameProfile().getName() + " quitado."), false);
					return 1;
				})))
			.then(Commands.literal("desc").requires(s -> s.hasPermission(2))
				.then(Commands.argument("nivel", IntegerArgumentType.integer(1, 3))
					.then(Commands.argument("texto", StringArgumentType.greedyString()).executes(ctx -> {
						int level = IntegerArgumentType.getInteger(ctx, "nivel");
						String text = StringArgumentType.getString(ctx, "texto");
						Config.INSTANCE.prankDescriptions.put(level, text);
						Config.save();
						ctx.getSource().sendSuccess(() -> Component.literal("Descripcion del nivel " + level + " actualizada."), false);
						return 1;
					}))))
			.then(Commands.literal("check")
				.executes(ctx -> checkPrank(ctx.getSource(), ctx.getSource().getPlayerOrException()))
				.then(Commands.argument("jugador", EntityArgument.player()).executes(ctx ->
					checkPrank(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador")))))
		);

		// /pitusa  (estrella por edicion)
		d.register(Commands.literal("pitusa")
			.then(Commands.literal("set").requires(s -> s.hasPermission(2))
				.then(Commands.argument("jugador", EntityArgument.player())
					.then(Commands.argument("ediciones", IntegerArgumentType.integer(1, 20)).executes(ctx -> {
						ServerPlayer target = EntityArgument.getPlayer(ctx, "jugador");
						int n = IntegerArgumentType.getInteger(ctx, "ediciones");
						Config.INSTANCE.editionCounts.put(target.getUUID().toString(), n);
						Config.save();
						refreshTab(ctx.getSource().getServer(), target);
						ctx.getSource().sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " jugo " + n + " ediciones."), false);
						return 1;
					}))))
			.then(Commands.literal("clear").requires(s -> s.hasPermission(2))
				.then(Commands.argument("jugador", EntityArgument.player()).executes(ctx -> {
					ServerPlayer target = EntityArgument.getPlayer(ctx, "jugador");
					Config.INSTANCE.editionCounts.remove(target.getUUID().toString());
					Config.save();
					refreshTab(ctx.getSource().getServer(), target);
					ctx.getSource().sendSuccess(() -> Component.literal("Estrella de " + target.getGameProfile().getName() + " quitada."), false);
					return 1;
				})))
			.then(Commands.literal("check")
				.executes(ctx -> checkPitusa(ctx.getSource(), ctx.getSource().getPlayerOrException()))
				.then(Commands.argument("jugador", EntityArgument.player()).executes(ctx ->
					checkPitusa(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador")))))
		);
	}

	private static int checkPrank(CommandSourceStack src, ServerPlayer target) {
		Integer level = Config.INSTANCE.prankLevels.get(target.getUUID().toString());
		if (level == null) {
			src.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " no tiene nivel de prank."), false);
		} else {
			String desc = Config.INSTANCE.prankDescriptions.getOrDefault(level, "");
			src.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " - prank nivel " + level + ": " + desc), false);
		}
		return 1;
	}

	private static int checkPitusa(CommandSourceStack src, ServerPlayer target) {
		Integer n = Config.INSTANCE.editionCounts.get(target.getUUID().toString());
		if (n == null) {
			src.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " no tiene ediciones registradas."), false);
		} else {
			src.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " jugo " + n + " ediciones de Made In Pitusa."), false);
		}
		return 1;
	}
}

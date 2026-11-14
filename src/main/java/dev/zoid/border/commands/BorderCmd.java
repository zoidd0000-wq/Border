package dev.zoid.border.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.zoid.border.impl.Border;
import dev.zoid.border.impl.BorderTicker;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public class BorderCmd {

    private static final double MAX_SIZE = 6.0E7;
    private static final double MAX_COORD = 3.0E7;
    private static final double MAX_SECONDS = 604800.0;

    private static final Map<String, Integer> COLORS = Map.ofEntries(
            Map.entry("red", 0xFF0000),
            Map.entry("orange", 0xFF8000),
            Map.entry("yellow", 0xFFFF00),
            Map.entry("lime", 0x80FF00),
            Map.entry("green", 0x00FF00),
            Map.entry("cyan", 0x00FFFF),
            Map.entry("blue", 0x0000FF),
            Map.entry("purple", 0x8000FF),
            Map.entry("magenta", 0xFF00FF),
            Map.entry("pink", 0xFF80C0),
            Map.entry("white", 0xFFFFFF)
    );

    private static final SimpleCommandExceptionType PLAYER_ONLY = new SimpleCommandExceptionType(new LiteralMessage("This needs a player"));
    private static final SimpleCommandExceptionType BAD_COLOR = new SimpleCommandExceptionType(new LiteralMessage("Use a color name, #RRGGBB or rgb <r> <g> <b>"));

    public BorderCmd(final Commands commands) {
        commands.register(
                Commands.literal("border")
                        .then(size())
                        .then(radius())
                        .then(center())
                        .then(color())
                        .then(set())
                        .then(stop())
                        .then(info())
                        .then(toggle("on", true))
                        .then(toggle("off", false))
                        .then(hide())
                        .then(show())
                        .build()
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> size() {
        return Commands.literal("size")
                .then(Commands.literal("add")
                        .then(timed(Commands.argument("amount", DoubleArgumentType.doubleArg(-MAX_SIZE, MAX_SIZE)), ctx -> {
                            Border border = BorderTicker.getBorder();
                            double target = Math.max(0.0, Math.min(MAX_SIZE, border.currentSize() + DoubleArgumentType.getDouble(ctx, "amount")));
                            border.size(target, ticks(ctx));
                            return report(ctx, "Size " + fixed(target) + over(ctx));
                        })))
                .then(timed(Commands.argument("diameter", DoubleArgumentType.doubleArg(0.0, MAX_SIZE)), ctx -> {
                    double target = DoubleArgumentType.getDouble(ctx, "diameter");
                    BorderTicker.getBorder().size(target, ticks(ctx));
                    return report(ctx, "Size " + fixed(target) + over(ctx));
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> radius() {
        return Commands.literal("radius")
                .then(timed(Commands.argument("radius", DoubleArgumentType.doubleArg(0.0, MAX_SIZE / 2.0)), ctx -> {
                    double target = DoubleArgumentType.getDouble(ctx, "radius") * 2.0;
                    BorderTicker.getBorder().size(target, ticks(ctx));
                    return report(ctx, "Size " + fixed(target) + over(ctx));
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> center() {
        Command<CommandSourceStack> here = ctx -> {
            Player player = player(ctx);
            BorderTicker.getBorder().center(player.getX(), player.getZ(), ticks(ctx));
            return report(ctx, "Center " + fixed(player.getX()) + " " + fixed(player.getZ()) + over(ctx));
        };
        return Commands.literal("center")
                .executes(here)
                .then(timed(Commands.literal("here"), here))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg(-MAX_COORD, MAX_COORD))
                        .then(timed(Commands.argument("z", DoubleArgumentType.doubleArg(-MAX_COORD, MAX_COORD)), ctx -> {
                            double cx = DoubleArgumentType.getDouble(ctx, "x");
                            double cz = DoubleArgumentType.getDouble(ctx, "z");
                            BorderTicker.getBorder().center(cx, cz, ticks(ctx));
                            return report(ctx, "Center " + fixed(cx) + " " + fixed(cz) + over(ctx));
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> color() {
        return Commands.literal("color")
                .then(Commands.literal("rgb")
                        .then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
                                .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
                                        .then(timed(Commands.argument("b", IntegerArgumentType.integer(0, 255)), ctx -> {
                                            int rgb = (IntegerArgumentType.getInteger(ctx, "r") << 16)
                                                    | (IntegerArgumentType.getInteger(ctx, "g") << 8)
                                                    | IntegerArgumentType.getInteger(ctx, "b");
                                            BorderTicker.getBorder().color(rgb, ticks(ctx));
                                            return report(ctx, "Color " + hex(rgb) + over(ctx));
                                        })))))
                .then(timed(colorArgument("value"), ctx -> {
                    int rgb = parse(StringArgumentType.getString(ctx, "value"));
                    BorderTicker.getBorder().color(rgb, ticks(ctx));
                    return report(ctx, "Color " + hex(rgb) + over(ctx));
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> set() {
        return Commands.literal("set")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg(-MAX_COORD, MAX_COORD))
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg(-MAX_COORD, MAX_COORD))
                                .then(Commands.argument("size", DoubleArgumentType.doubleArg(0.0, MAX_SIZE))
                                        .executes(ctx -> everything(ctx, null))
                                        .then(Commands.literal("over")
                                                .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.0, MAX_SECONDS))
                                                        .executes(ctx -> everything(ctx, null))))
                                        .then(timed(colorArgument("color"), ctx -> everything(ctx, parse(StringArgumentType.getString(ctx, "color"))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> stop() {
        return Commands.literal("stop")
                .executes(ctx -> {
                    Border border = BorderTicker.getBorder();
                    border.set(border.currentX(), border.currentZ(), border.currentSize(), 0);
                    border.color(border.currentColor(), 0);
                    return report(ctx, "Frozen at size " + fixed(border.size));
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> info() {
        return Commands.literal("info")
                .executes(ctx -> {
                    Border border = BorderTicker.getBorder();
                    return report(ctx, "Target: center " + fixed(border.x) + " " + fixed(border.z) + ", size " + fixed(border.size) + ", color " + hex(border.color)
                            + " | Now: center " + fixed(border.currentX()) + " " + fixed(border.currentZ()) + ", size " + fixed(border.currentSize()) + ", color " + hex(border.currentColor())
                            + " | Remaining " + fixed(border.remaining() / 20.0) + "s | " + (border.enabled ? "on" : "off"));
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggle(final String name, final boolean value) {
        return Commands.literal(name)
                .executes(ctx -> {
                    BorderTicker.getBorder().enabled = value;
                    return report(ctx, "Border " + name + " for everyone");
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> hide() {
        return Commands.literal("hide")
                .executes(ctx -> {
                    BorderTicker.getBorder().hide(player(ctx));
                    return report(ctx, "Hidden for you");
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> show() {
        return Commands.literal("show")
                .executes(ctx -> {
                    BorderTicker.getBorder().show(player(ctx));
                    return report(ctx, "Visible for you");
                });
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T timed(final T node, final Command<CommandSourceStack> run) {
        return node.executes(run)
                .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.0, MAX_SECONDS)).executes(run));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> colorArgument(final String name) {
        return Commands.argument(name, StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    COLORS.keySet().stream()
                            .filter(key -> key.startsWith(builder.getRemainingLowerCase()))
                            .forEach(key -> builder.suggest(key));
                    return builder.buildFuture();
                });
    }

    private static int everything(final CommandContext<CommandSourceStack> ctx, final Integer rgb) {
        Border border = BorderTicker.getBorder();
        int ticks = ticks(ctx);
        border.set(DoubleArgumentType.getDouble(ctx, "x"), DoubleArgumentType.getDouble(ctx, "z"), DoubleArgumentType.getDouble(ctx, "size"), ticks);
        if (rgb != null) {
            border.color(rgb, ticks);
        }
        return report(ctx, "Center " + fixed(border.x) + " " + fixed(border.z) + ", size " + fixed(border.size) + (rgb != null ? ", color " + hex(rgb) : "") + over(ctx));
    }

    private static int ticks(final CommandContext<CommandSourceStack> ctx) {
        try {
            return (int) Math.round(DoubleArgumentType.getDouble(ctx, "seconds") * 20.0);
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    private static String over(final CommandContext<CommandSourceStack> ctx) {
        int ticks = ticks(ctx);
        return ticks > 0 ? " over " + fixed(ticks / 20.0) + "s" : "";
    }

    private static int parse(final String text) throws CommandSyntaxException {
        String key = text.toLowerCase(Locale.ROOT);
        Integer named = COLORS.get(key);
        if (named != null) {
            return named;
        }
        String digits = key.startsWith("#") ? key.substring(1) : key.startsWith("0x") ? key.substring(2) : key;
        if (!digits.matches("[0-9a-f]{6}")) {
            throw BAD_COLOR.create();
        }
        return Integer.parseInt(digits, 16);
    }

    private static Player player(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (ctx.getSource().getExecutor() instanceof Player player) {
            return player;
        }
        throw PLAYER_ONLY.create();
    }

    private static int report(final CommandContext<CommandSourceStack> ctx, final String message) {
        ctx.getSource().getSender().sendMessage(Component.text(message));
        return Command.SINGLE_SUCCESS;
    }

    private static String fixed(final double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String hex(final int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }
}
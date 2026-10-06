package un.fibrant.candlekingdomscompanion;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class CandleKingdomsCompanion {
    private static final int[] DICE_SIDES = {2, 4, 6, 8, 10, 12, 20};
    private static final Map<String, Map<String, String>> TRANSLATIONS = new ConcurrentHashMap<>();

    private CandleKingdomsCompanion() {
    }


    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dice")
                .executes(context -> {
                    context.getSource().sendFailure(translated(context.getSource().getPlayer(),
                            "text.candle-kingdoms-dice.dice_throw.no_dice", "Choose a dice"));
                    return 0;
                })
                .then(Commands.argument("dice", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (int sides : DICE_SIDES) {
                                builder.suggest("D" + sides);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> throwDice(context, 1, Component.empty()))
                        .then(Commands.argument("throw_count", IntegerArgumentType.integer(1))
                                .executes(context -> throwDice(context, IntegerArgumentType.getInteger(context, "throw_count"), Component.empty()))
                                .then(Commands.argument("description", StringArgumentType.greedyString())
                                        .executes(context -> throwDice(context,
                                                IntegerArgumentType.getInteger(context, "throw_count"),
                                                Component.literal(" (" + StringArgumentType.getString(context, "description") + ")")))))));

        dispatcher.register(Commands.literal("sizeRoll")
                .then(Commands.literal("roll").executes(context -> {
                    double size = Math.round(ThreadLocalRandom.current().nextDouble(0.001, 3.2) * 100.0) / 100.0;
                    return setSize(context, size);
                }))
                .then(Commands.literal("normal").executes(context -> setSize(context, 1.0)))
                .then(Commands.literal("giant").executes(context -> setSize(context, 3.0)))
                .then(Commands.literal("small").executes(context -> setSize(context, 0.2)))
                .then(Commands.literal("micro").executes(context -> setSize(context, 0.01))));
    }

    private static int throwDice(CommandContext<CommandSourceStack> context, int throwCount, Component description) {
        String dice = StringArgumentType.getString(context, "dice").trim().toUpperCase(Locale.ROOT);
        int sides = parseSides(dice);
        if (sides == 0) {
            context.getSource().sendFailure(translated(context.getSource().getPlayer(),
                    "text.candle-kingdoms-dice.dice_throw.invalid_dice", "This dice does not exist"));
            return 0;
        }

        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatableWithFallback(
                    "text.candle-kingdoms-dice.dice_throw.player_only", "Only players can use this command"));
            return 0;
        }

        List<Integer> rolls = new ArrayList<>();
        for (int i = 0; i < throwCount; i++) {
            rolls.add(ThreadLocalRandom.current().nextInt(1, sides + 1));
        }

        // colors
        final int GOLD = 0xFFB800;
        final int RED = 0xFF3325;
        final int YELLOW = 0xFFFF35;
        final int GRAY = 0xAAAAAA;
        final int GREEN = 0x55FF55;

        // dice emoji
        MutableComponent logo = Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(" 🎲 ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));

        // player
        MutableComponent playerName = player.getDisplayName().copy()
                .withStyle(s -> s.withColor(TextColor.fromRgb(GREEN))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("Click to send a message").withStyle(ChatFormatting.GRAY)))
                        .withClickEvent(new ClickEvent.SuggestCommand("/msg " + player.getName().getString() + " ")));

        // dice
        MutableComponent diceName = Component.literal(dice)
                .withStyle(s -> s.withColor(TextColor.fromRgb(GOLD)).withBold(true));

        // Descript
        Component styledDesc = description.copy().withStyle(s -> s.withColor(TextColor.fromRgb(GRAY)).withItalic(true));

        String translationKey;
        String defaultTemplate;
        Object[] translationArgs;

        if (throwCount == 1) {
            int result = rolls.get(0);
            int color = result == sides ? GOLD : (result == 1 && sides != 2) ? RED : YELLOW;

            // Result with a cool tooltip
            Component resultComp = Component.literal(Integer.toString(result))
                    .withStyle(s -> s.withColor(TextColor.fromRgb(color)).withBold(true)
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("Rolled a " + result + " on a " + sides + "-sided die")
                                            .withStyle(ChatFormatting.GRAY))));

            translationKey = "text.candle-kingdoms-dice.dice_throw.one";
            defaultTemplate = "%s %s rolled %s and got %d.%s";
            translationArgs = new Object[]{logo, playerName, diceName, resultComp, styledDesc};
        } else {
            int sum = rolls.stream().mapToInt(Integer::intValue).sum();

            // colors for rolls yay
            MutableComponent rollsList = Component.literal("(").withStyle(ChatFormatting.DARK_GRAY);
            for (int i = 0; i < rolls.size(); i++) {
                int r = rolls.get(i);
                int c = r == sides ? GOLD : (r == 1 && sides != 2) ? RED : YELLOW;

                rollsList.append(Component.literal(Integer.toString(r))
                        .withStyle(s -> s.withColor(TextColor.fromRgb(c)).withBold(true)));

                if (i < rolls.size() - 1) {
                    rollsList.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                }
            }
            rollsList.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));

            Component sumComp = Component.literal(Integer.toString(sum))
                    .withStyle(s -> s.withColor(TextColor.fromRgb(GOLD)).withBold(true));

            translationKey = "text.candle-kingdoms-dice.dice_throw.multiple";
            defaultTemplate = "%s %s rolled %d %s and got total of: %d.%s%s";
            translationArgs = new Object[]{logo, playerName, throwCount, diceName, sumComp, styledDesc, rollsList};
        }

        for (ServerPlayer recipient : context.getSource().getServer().getPlayerList().getPlayers()) {
            recipient.sendSystemMessage(
                    translated(recipient, translationKey, defaultTemplate, translationArgs)
                            .copy()
                            .withStyle(Style.EMPTY.withColor(0xFFD430)),
                    false);
        }
        return 1;
    }

    private static int parseSides(String dice) {
        if (!dice.startsWith("D")) {
            return 0;
        }
        try {
            int sides = Integer.parseInt(dice.substring(1));
            for (int valid : DICE_SIDES) {
                if (valid == sides) return sides;
            }
        } catch (NumberFormatException ignored) {
        }
        return 0;
    }

    private static int setSize(CommandContext<CommandSourceStack> context, double size) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatableWithFallback(
                    "text.candle-kingdoms-dice.size_roll.player_only", "Only players can use this command"));
            return 0;
        }
        var attribute = player.getAttribute(Attributes.SCALE);
        if (attribute != null) {
            attribute.setBaseValue(size);
        }

        Component successMsg = Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(" 📏 ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(" Size set to ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format(Locale.US, "%.2fx", size)).withStyle(s -> s.withColor(0xFFB800).withBold(true)));

        context.getSource().sendSuccess(() -> successMsg, false);
        return 1;
    }

    private static Component translated(ServerPlayer player, String key, String defaultTemplate, Object... args) {
        if (player == null) {
            return Component.translatableWithFallback(key, defaultTemplate, args);
        }

        String language = player.clientInformation().language().toLowerCase(Locale.ROOT);
        Map<String, String> translations = TRANSLATIONS.computeIfAbsent(language,
                locale -> loadTranslations(player, locale));
        if (translations.isEmpty() && !"en_us".equals(language)) {
            translations = TRANSLATIONS.computeIfAbsent("en_us", locale -> loadTranslations(player, locale));
        }
        return Component.translatableWithFallback(key, translations.getOrDefault(key, defaultTemplate), args);
    }

    private static Map<String, String> loadTranslations(ServerPlayer player, String language) {
        Identifier resourceId = Identifier.fromNamespaceAndPath("candlekingdoms", "lang/" + language + ".json");
        return player.level().getServer().getResourceManager().getResource(resourceId)
                .map(resource -> {
                    Map<String, String> translations = new HashMap<>();
                    try (var input = resource.open()) {
                        Language.loadFromJson(input, translations::put);
                    } catch (IOException ignored) {
                        return Map.<String, String>of();
                    }
                    return translations;
                })
                .orElseGet(Map::of);
    }
}
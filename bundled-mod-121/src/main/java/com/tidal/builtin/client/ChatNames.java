package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatNames {
    private static final Component BEDROCK = Component.literal(" B").withStyle(style -> style.withColor(0x0349FC).withBold(true));

    private ChatNames() {}

    public static String mask(String username) {
        if (username == null || username.isBlank()) {
            return username;
        }
        int n = Math.min(4, username.length());
        return username.substring(0, n) + "...";
    }

    public static Component tab(Component name, PlayerInfo info) {
        Component rewritten = rewrite(name);
        if (TidalMods.bedrockDetect && BedrockPlayers.isBedrock(info)) {
            return Component.empty().append(rewritten).append(BEDROCK);
        }
        return rewritten;
    }

    public static Component rewrite(Component source) {
        Component rewritten = source;
        if (source != null && (TidalMods.streamerMode || TidalMods.bedrockDetect)) {
            rewritten = copy(source);
        }
        if (rewritten == null || !PlayMods.chatTimestamps) {
            return rewritten;
        }
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        return Component.literal("[" + time + "] ").append(rewritten);
    }

    private static MutableComponent copy(Component source) {
        String text = "";
        if (source.getContents() instanceof PlainTextContents plain) {
            text = rewriteLiteral(plain.text());
        }
        MutableComponent out = Component.literal(text).withStyle(source.getStyle());
        for (Component sibling : source.getSiblings()) {
            out.append(copy(sibling));
        }
        return out;
    }

    private static String rewriteLiteral(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        List<PlayerInfo> players = names();
        String result = text;
        for (PlayerInfo info : players) {
            String username = info.getProfile().getName();
            if (username == null || username.length() < 3) {
                continue;
            }
            Pattern pattern = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(username) + "(?![A-Za-z0-9_])");
            Matcher matcher = pattern.matcher(result);
            StringBuilder builder = new StringBuilder();
            while (matcher.find()) {
                String replacement = TidalMods.streamerMode ? mask(username) : username;
                if (TidalMods.bedrockDetect && BedrockPlayers.isBedrock(info) && !alreadyBadged(result, matcher.end())) {
                    replacement += " B";
                }
                matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(builder);
            result = builder.toString();
        }
        return result;
    }

    private static boolean alreadyBadged(String text, int end) {
        return end < text.length() && text.startsWith(" B", end);
    }

    private static List<PlayerInfo> names() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getConnection() == null) {
            return List.of();
        }
        List<PlayerInfo> list = new ArrayList<>(minecraft.getConnection().getOnlinePlayers());
        list.sort(Comparator.comparingInt((PlayerInfo info) -> info.getProfile().getName().length()).reversed());
        return list;
    }
}

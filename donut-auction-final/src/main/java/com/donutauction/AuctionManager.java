package com.donutauction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AuctionManager {
    public static boolean active = false;
    public static boolean hudVisible = false;
    public static ItemStack item = ItemStack.EMPTY;
    public static long endTimeMs = 0;

    /** Highest bid per bidder. */
    public static final Map<String, Long> bids = new LinkedHashMap<>();

    public static long sessionProfit = 0;
    public static long sessionLoss = 0;
    public static int sold = 0;
    public static int unsold = 0;
    private static int announceCount = 0;

    // "Name » 5k", "<Name> 5k", "Name: bid 5k", "[Rank] Name -> you: 5k" ...
    private static final Pattern LINE = Pattern.compile(
            "^(?:\\[[^\\]]*\\]\\s*)*<?([A-Za-z0-9_.]{3,16})>?\\s*(?:»|>>|->|:|>)?\\s*(?:you\\s*[:»>]\\s*)?"
                    + "(?:bid\\s+)?\\$?([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[kKmMbB]?)\\s*$",
            Pattern.CASE_INSENSITIVE);
    // Body only (for signed player chat where the name comes separately)
    private static final Pattern BODY = Pattern.compile(
            "^(?:bid\\s+)?\\$?([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[kKmMbB]?)\\s*$", Pattern.CASE_INSENSITIVE);
    // DonutSMP payments: "Name paid you $ 1K"
    private static final Pattern PAY = Pattern.compile(
            "^(?:\\[[^\\]]*\\]\\s*)*([A-Za-z0-9_.]{3,16})\\s+paid you\\s+[^0-9\\s]{0,2}\\s*"
                    + "([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[kKmMbB]?)\\s*\\.?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_.]{3,16}");

    private AuctionManager() {}

    // ---------- items ----------

    public static boolean isAllowed(Item i) {
        return i != Items.AIR;
    }

    public static void loadSelectedFromConfig() {
        try {
            Item i = Registries.ITEM.get(Identifier.of(AuctionConfig.INSTANCE.selectedItem));
            if (isAllowed(i)) item = i.getDefaultStack();
        } catch (Exception ignored) {
        }
    }

    public static void select(ItemStack stack) {
        if (stack.isEmpty() || !isAllowed(stack.getItem())) return;
        item = stack.copy();
        AuctionConfig.INSTANCE.selectedItem = Registries.ITEM.getId(stack.getItem()).toString();
        AuctionConfig.save();
    }

    // ---------- messaging ----------

    private static void msg(String s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal("§6[DonutAuction] §f" + s), false);
    }

    private static void say(String s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (AuctionConfig.INSTANCE.announceInChat && mc.player != null) {
            announceCount++;
            mc.player.networkHandler.sendChatMessage(s + " [#" + announceCount + "-" + (System.currentTimeMillis() % 1000) + "]");
        }
    }

    // ---------- lifecycle ----------

    public static void start() {
        if (active) { msg("An auction is already running."); return; }
        if (item.isEmpty()) { msg("Pick an item first (open the menu with the keybind)."); return; }
        AuctionConfig c = AuctionConfig.INSTANCE;
        bids.clear();
        active = true;
        hudVisible = true;
        endTimeMs = System.currentTimeMillis() + c.durationSeconds * 1000L;
        say("AUCTION: " + item.getName().getString() + " | min bid " + MoneyUtil.format(c.minBid)
                + " | " + c.durationSeconds + "s | type your bid in chat!");
        msg("Auction started.");
    }

    public static void end() {
        if (!active) return;
        active = false;
        Map.Entry<String, Long> top = topEntry();
        String name = item.getName().getString();
        if (top == null) {
            unsold++;
            say("AUCTION ENDED: no bids for " + name + ".");
            msg("Ended with no bids.");
            return;
        }
        long result = top.getValue() - AuctionConfig.INSTANCE.itemCost;
        if (result >= 0) sessionProfit += result; else sessionLoss += -result;
        sold++;
        WinEffect.start(top.getKey(), top.getValue(), item);
        say("AUCTION ENDED: " + name + " goes to " + top.getKey() + " for " + MoneyUtil.format(top.getValue()) + "!");
        msg("Winner: §a" + top.getKey() + " §f- " + MoneyUtil.format(top.getValue())
                + (result >= 0 ? " §a(profit +" : " §c(loss ") + MoneyUtil.format(result) + ")");
    }

    public static void tick() {
        if (active && System.currentTimeMillis() >= endTimeMs) end();
    }

    public static void resetSession() {
        sessionProfit = 0; sessionLoss = 0; sold = 0; unsold = 0;
    }

    // ---------- bids ----------

    public static void handleGameLine(String raw) {
        if (!active) return;
        String clean = raw.replaceAll("§.", "").trim();
        Matcher pm = PAY.matcher(clean);
        if (pm.matches()) {
            placeBid(pm.group(1), MoneyUtil.parse(pm.group(2)), true);
            return;
        }
        Matcher m = LINE.matcher(clean);
        if (!m.matches()) return;
        placeBid(m.group(1), MoneyUtil.parse(m.group(2)), false);
    }

    public static void handleChatBody(String senderName, String body) {
        if (!active) return;
        Matcher m = BODY.matcher(body.replaceAll("§.", "").trim());
        if (!m.matches()) return;
        // senderName may contain rank prefixes; take the last name-looking token
        Matcher n = NAME.matcher(senderName.replaceAll("§.", ""));
        String last = null;
        while (n.find()) last = n.group();
        if (last == null) return;
        placeBid(last, MoneyUtil.parse(m.group(1)), false);
    }

    private static void placeBid(String name, long amount, boolean payment) {
        if (!active || amount <= 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSession() != null && name.equalsIgnoreCase(mc.getSession().getUsername())) return;

        AuctionConfig c = AuctionConfig.INSTANCE;
        if (amount < c.minBid) {
            if (payment) msg("§c" + name + " paid " + MoneyUtil.format(amount) + " - below min bid, not counted.");
            return;
        }
        Map.Entry<String, Long> top = topEntry();
        if (top != null && amount < top.getValue() + c.minIncrement) {
            if (payment) msg("§c" + name + " paid " + MoneyUtil.format(amount) + " - doesn't beat the top bid enough, not counted.");
            return;
        }
        Long prev = bids.get(name);
        if (prev != null && amount <= prev) return;

        bids.put(name, amount);
        msg("§a" + name + (payment ? " §fpaid §e" : " §fbid §e") + MoneyUtil.format(amount));
    }

    // ---------- queries ----------

    public static Map.Entry<String, Long> topEntry() {
        Map.Entry<String, Long> best = null;
        for (Map.Entry<String, Long> e : bids.entrySet()) {
            if (best == null || e.getValue() > best.getValue()) best = e;
        }
        return best;
    }

    public static List<Map.Entry<String, Long>> topBids(int n) {
        List<Map.Entry<String, Long>> l = new ArrayList<>(bids.entrySet());
        l.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return l.size() > n ? l.subList(0, n) : l;
    }

    /** top bid minus your item cost. Positive = profit, negative = loss. */
    public static long currentResult() {
        Map.Entry<String, Long> top = topEntry();
        return top == null ? 0 : top.getValue() - AuctionConfig.INSTANCE.itemCost;
    }
}

package com.donutauction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.Map;

public final class AuctionRenderer {
    private AuctionRenderer() {}

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;
    private static final int GOLD = 0xFFFFAA00;
    private static final int YELLOW = 0xFFFFFF55;

    /**
     * Draws an item that "spins" (turntable-style squash + a gentle bob).
     * cx/cy = centre in GUI pixels, size = rendered size in pixels (16 = normal).
     */
    public static void drawSpinningItem(DrawContext ctx, ItemStack stack, float cx, float cy, float size) {
        if (stack.isEmpty()) return;
        long ms = System.currentTimeMillis();
        double angle = (ms % 2400L) / 2400.0 * Math.PI * 2;
        float widthScale = Math.max(0.08f, (float) Math.abs(Math.cos(angle)));
        float bob = (float) Math.sin(ms / 400.0) * 1.5f;
        float s = size / 16f;

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy + bob);
        m.scale(s * widthScale, s);
        m.translate(-8f, -8f);
        ctx.drawItem(stack, 0, 0);
        m.popMatrix();
    }

    public static void renderHud(DrawContext ctx) {
        if (!AuctionManager.hudVisible || AuctionManager.item.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        AuctionConfig cfg = AuctionConfig.INSTANCE;

        int x = 8, y = 8, w = 196, h = 196;
        ctx.fill(x, y, x + w, y + h, 0xA0000000);
        ctx.fill(x, y, x + w, y + 1, GOLD);

        // spinning item
        drawSpinningItem(ctx, AuctionManager.item, x + 34, y + 38, 48);

        // item info
        String name = tr.trimToWidth(AuctionManager.item.getName().getString(), w - 74);
        ctx.drawText(tr, name, x + 68, y + 14, YELLOW, true);

        String timer;
        int timerColor;
        if (AuctionManager.active) {
            long left = Math.max(0, (AuctionManager.endTimeMs - System.currentTimeMillis()) / 1000);
            timer = String.format("Time: %d:%02d", left / 60, left % 60);
            timerColor = left <= 10 ? RED : WHITE;
        } else {
            timer = "AUCTION ENDED";
            timerColor = RED;
        }
        ctx.drawText(tr, timer, x + 68, y + 28, timerColor, true);
        ctx.drawText(tr, "Min bid: " + MoneyUtil.format(cfg.minBid), x + 68, y + 42, GRAY, true);
        ctx.drawText(tr, "Your cost: " + MoneyUtil.format(cfg.itemCost), x + 68, y + 54, GRAY, true);

        // top bidders
        int by = y + 74;
        ctx.drawText(tr, "Top Bidders", x + 8, by, GOLD, true);
        List<Map.Entry<String, Long>> top = AuctionManager.topBids(5);
        for (int i = 0; i < 5; i++) {
            int ry = by + 13 + i * 11;
            if (i < top.size()) {
                Map.Entry<String, Long> e = top.get(i);
                ctx.drawText(tr, (i + 1) + ". " + e.getKey(), x + 8, ry, i == 0 ? GREEN : WHITE, true);
                String amt = MoneyUtil.format(e.getValue());
                ctx.drawText(tr, amt, x + w - 8 - tr.getWidth(amt), ry, i == 0 ? GREEN : WHITE, true);
            } else {
                ctx.drawText(tr, (i + 1) + ". ---", x + 8, ry, GRAY, true);
            }
        }

        // profit / loss
        int py = by + 13 + 5 * 11 + 6;
        ctx.fill(x + 6, py - 3, x + w - 6, py - 2, 0x60FFFFFF);
        if (AuctionManager.bids.isEmpty()) {
            ctx.drawText(tr, "This item: no bids yet", x + 8, py, GRAY, true);
        } else {
            long r = AuctionManager.currentResult();
            if (r > 0) ctx.drawText(tr, "This item: PROFIT +" + MoneyUtil.format(r), x + 8, py, GREEN, true);
            else if (r < 0) ctx.drawText(tr, "This item: LOSS " + MoneyUtil.format(r), x + 8, py, RED, true);
            else ctx.drawText(tr, "This item: break even", x + 8, py, YELLOW, true);
        }
        ctx.drawText(tr, "Session profit: +" + MoneyUtil.format(AuctionManager.sessionProfit), x + 8, py + 12, GREEN, true);
        ctx.drawText(tr, "Session loss: -" + MoneyUtil.format(AuctionManager.sessionLoss), x + 8, py + 24, RED, true);
        long net = AuctionManager.sessionProfit - AuctionManager.sessionLoss;
        ctx.drawText(tr, "Net: " + (net >= 0 ? "+" : "") + MoneyUtil.format(net)
                + "  (" + AuctionManager.sold + " sold / " + AuctionManager.unsold + " unsold)",
                x + 8, py + 36, net >= 0 ? GREEN : RED, true);
    }
}

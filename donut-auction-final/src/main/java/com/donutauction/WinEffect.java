package com.donutauction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import org.joml.Matrix3x2fStack;

/** Full-screen celebration: gold flash, confetti, firework bursts, "SOLD!" banner, big spinning item. */
public final class WinEffect {
    private static final long DURATION = 7000;
    private static final int[] COLORS = {0xFFD700, 0xFF5555, 0x55FF55, 0x55FFFF, 0xFF55FF, 0xFFFFFF};

    private static long startMs = -1;
    private static String winner = "";
    private static long amount = 0;
    private static ItemStack stack = ItemStack.EMPTY;

    private WinEffect() {}

    public static void start(String winnerName, long winAmount, ItemStack item) {
        winner = winnerName;
        amount = winAmount;
        stack = item.copy();
        startMs = System.currentTimeMillis();
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.send(() -> {
            if (mc.player == null) return;
            mc.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            mc.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            mc.player.playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
        });
    }

    private static float hash(int i, int k) {
        int n = i * 374761393 + k * 668265263;
        n = (n ^ (n >>> 13)) * 1274126177;
        n ^= n >>> 16;
        return (n & 0xFFFFFF) / (float) 0x1000000;
    }

    private static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static void render(DrawContext ctx) {
        if (startMs < 0) return;
        long t = System.currentTimeMillis() - startMs;
        if (t > DURATION) { startMs = -1; return; }

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float fade = t > DURATION - 1200 ? (DURATION - t) / 1200f : 1f;

        // gold flash
        if (t < 450) ctx.fill(0, 0, w, h, argb(0xFFD700, (1 - t / 450f) * 0.45f));

        // confetti rain
        for (int i = 0; i < 150; i++) {
            float delay = hash(i, 3) * 1500f;
            float lt = t - delay;
            if (lt < 0) continue;
            float speed = 60 + hash(i, 2) * 90;
            float y = (lt / 1000f * speed) % (h + 20) - 10;
            float x = hash(i, 1) * w + (float) Math.sin(lt / 300f + hash(i, 5) * 6.28f) * (6 + hash(i, 4) * 16);
            int s = 2 + (int) (hash(i, 6) * 3);
            int rgb = COLORS[(int) (hash(i, 7) * COLORS.length) % COLORS.length];
            ctx.fill((int) x, (int) y, (int) x + s, (int) y + s, argb(rgb, fade));
        }

        // firework bursts
        for (int b = 0; b < 7; b++) {
            long age = t - (300 + b * 650L);
            if (age < 0 || age > 1300) continue;
            float p = age / 1300f;
            float cx = w * (0.12f + hash(b, 10) * 0.76f);
            float cy = h * (0.10f + hash(b, 11) * 0.45f);
            float radius = (1 - (1 - p) * (1 - p)) * 55f;
            int rgb = COLORS[b % COLORS.length];
            for (int sp = 0; sp < 20; sp++) {
                double ang = sp / 20.0 * Math.PI * 2;
                int px = (int) (cx + Math.cos(ang) * radius);
                int py = (int) (cy + Math.sin(ang) * radius + p * p * 24);
                ctx.fill(px, py, px + 2, py + 2, argb(rgb, (1 - p) * fade));
            }
        }

        // banner
        float pop = t < 500 ? (float) (1 + Math.sin(t / 500.0 * Math.PI) * 0.35) : 1f;
        drawScaled(ctx, tr, "SOLD!", w / 2f, h * 0.17f, 5f * pop, argb(0xFFD700, fade));
        AuctionRenderer.drawSpinningItem(ctx, stack, w / 2f, h * 0.42f, 72);
        drawScaled(ctx, tr, winner + " wins!", w / 2f, h * 0.60f, 2.5f, argb(0x55FF55, fade));
        drawScaled(ctx, tr, "for " + MoneyUtil.format(amount), w / 2f, h * 0.60f + 26, 1.6f, argb(0xFFFFFF, fade));
    }

    private static void drawScaled(DrawContext ctx, TextRenderer tr, String s, float cx, float cy, float scale, int color) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale(scale, scale);
        ctx.drawText(tr, s, -tr.getWidth(s) / 2, -4, color, true);
        m.popMatrix();
    }
}

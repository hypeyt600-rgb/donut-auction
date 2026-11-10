package com.donutauction;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class DonutAuctionMod implements ClientModInitializer {
    public static final String MOD_ID = "donutauction";
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        AuctionConfig.load();
        AuctionManager.loadSelectedFromConfig();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutauction.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyBinding.Category.create(Identifier.of(MOD_ID, "keys"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AuctionManager.tick();
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ItemPickerScreen());
            }
        });

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "auction_hud"),
                (ctx, tickCounter) -> { AuctionRenderer.renderHud(ctx); WinEffect.render(ctx); });

        // Bids arrive either as system/game messages (most servers) or as signed chat.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) AuctionManager.handleGameLine(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) ->
                AuctionManager.handleChatBody(params.name().getString(), message.getString()));

        // Named /dauc so it does NOT shadow the server's own /auction or /ah commands.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("dauc")
                        .executes(ctx -> { openLater(new ItemPickerScreen()); return 1; })
                        .then(ClientCommandManager.literal("start").executes(ctx -> { AuctionManager.start(); return 1; }))
                        .then(ClientCommandManager.literal("end").executes(ctx -> { AuctionManager.end(); return 1; }))
                        .then(ClientCommandManager.literal("config").executes(ctx -> {
                            openLater(new ConfigScreen(null)); return 1; }))
                        .then(ClientCommandManager.literal("test").executes(ctx -> {
                            WinEffect.start("TestPlayer", 123456, AuctionManager.item.isEmpty()
                                    ? net.minecraft.item.Items.DIAMOND.getDefaultStack() : AuctionManager.item);
                            return 1; }))
                        .then(ClientCommandManager.literal("hide").executes(ctx -> {
                            AuctionManager.hudVisible = false; return 1; }))
                        .then(ClientCommandManager.literal("reset").executes(ctx -> {
                            AuctionManager.resetSession();
                            ctx.getSource().sendFeedback(Text.literal("Session profit/loss reset.")); return 1; }))
                        .then(ClientCommandManager.literal("minbid")
                                .then(ClientCommandManager.argument("amount", StringArgumentType.word()).executes(ctx -> {
                                    long v = MoneyUtil.parse(StringArgumentType.getString(ctx, "amount"));
                                    if (v < 0) { ctx.getSource().sendFeedback(Text.literal("Invalid amount.")); return 0; }
                                    AuctionConfig.INSTANCE.minBid = v;
                                    AuctionConfig.save();
                                    ctx.getSource().sendFeedback(Text.literal("Min bid set to " + MoneyUtil.format(v)));
                                    return 1; })))
                        .then(ClientCommandManager.literal("cost")
                                .then(ClientCommandManager.argument("amount", StringArgumentType.word()).executes(ctx -> {
                                    long v = MoneyUtil.parse(StringArgumentType.getString(ctx, "amount"));
                                    if (v < 0) { ctx.getSource().sendFeedback(Text.literal("Invalid amount.")); return 0; }
                                    AuctionConfig.INSTANCE.itemCost = v;
                                    AuctionConfig.save();
                                    ctx.getSource().sendFeedback(Text.literal("Item cost set to " + MoneyUtil.format(v)));
                                    return 1; })))));
    }

    private static void openLater(net.minecraft.client.gui.screen.Screen s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.send(() -> mc.setScreen(s));
    }
}

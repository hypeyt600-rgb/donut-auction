package com.donutauction;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Settings: minimum bid, increment, duration, your item cost, chat announcements. */
public class ConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget minBid, increment, duration, cost;
    private ButtonWidget announceBtn;
    private boolean announce;
    private String error = "";

    public ConfigScreen(Screen parent) {
        super(Text.literal("Donut Auction - Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AuctionConfig c = AuctionConfig.INSTANCE;
        announce = c.announceInChat;
        int w = 200, x = this.width / 2 - w / 2, y = 40, gap = 34;

        minBid = field(x, y, w, MoneyUtil.format(c.minBid).replace("K", "k").replace("M", "m").replace("B", "b"));
        increment = field(x, y + gap, w, MoneyUtil.format(c.minIncrement).toLowerCase());
        duration = field(x, y + gap * 2, w, String.valueOf(c.durationSeconds));
        cost = field(x, y + gap * 3, w, MoneyUtil.format(c.itemCost).toLowerCase());

        announceBtn = addDrawableChild(ButtonWidget.builder(announceLabel(), b -> {
            announce = !announce;
            b.setMessage(announceLabel());
        }).dimensions(x, y + gap * 4, w, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Save & Back"), b -> save())
                .dimensions(x, y + gap * 4 + 28, 96, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
                .dimensions(x + 104, y + gap * 4 + 28, 96, 20).build());
    }

    private TextFieldWidget field(int x, int y, int w, String value) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, x, y, w, 18, Text.empty());
        f.setMaxLength(20);
        f.setText(value);
        addDrawableChild(f);
        return f;
    }

    private Text announceLabel() {
        return Text.literal("Announce in chat: " + (announce ? "ON" : "OFF"));
    }

    private void save() {
        long mb = MoneyUtil.parse(minBid.getText());
        long inc = MoneyUtil.parse(increment.getText());
        long dur = MoneyUtil.parse(duration.getText());
        long cst = MoneyUtil.parse(cost.getText());
        if (mb < 0 || inc < 0 || dur < 5 || cst < 0) {
            error = "Invalid value (duration must be at least 5 seconds). Use numbers like 5000, 5k, 1.5m.";
            return;
        }
        AuctionConfig c = AuctionConfig.INSTANCE;
        c.minBid = mb;
        c.minIncrement = inc;
        c.durationSeconds = (int) dur;
        c.itemCost = cst;
        c.announceInChat = announce;
        AuctionConfig.save();
        close();
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = this.width / 2 - 100, y = 40, gap = 34;
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "Minimum bid (e.g. 5k, 1.5m)", x, y - 11, 0xFFFFFF55);
        ctx.drawTextWithShadow(this.textRenderer, "Minimum raise over top bid", x, y + gap - 11, 0xFFFFFF55);
        ctx.drawTextWithShadow(this.textRenderer, "Auction length (seconds)", x, y + gap * 2 - 11, 0xFFFFFF55);
        ctx.drawTextWithShadow(this.textRenderer, "Your item cost (for profit/loss)", x, y + gap * 3 - 11, 0xFFFFFF55);
        if (!error.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, error, this.width / 2, y + gap * 4 + 54, 0xFFFF5555);
        }
    }
}

package com.donutauction;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Pick any item in the game (except the dragon head) and start / stop the auction. */
public class ItemPickerScreen extends Screen {
    private static final int COLS = 11, ROWS = 5, CELL = 20;
    private static List<ItemStack> ALL;

    private final List<ItemStack> filtered = new ArrayList<>();
    private final List<ButtonWidget> cells = new ArrayList<>();
    private final List<ItemStack> cellStacks = new ArrayList<>();
    private TextFieldWidget search;
    private ButtonWidget startStop;
    private int page = 0;
    private int left, gridTop;

    public ItemPickerScreen() {
        super(Text.literal("Donut Auction - Pick Item"));
        if (ALL == null) {
            ALL = new ArrayList<>();
            for (Item i : Registries.ITEM) {
                if (AuctionManager.isAllowed(i)) ALL.add(i.getDefaultStack());
            }
        }
    }

    @Override
    protected void init() {
        left = this.width / 2 - (COLS * CELL) / 2;
        gridTop = 46;

        search = new TextFieldWidget(this.textRenderer, left, 22, COLS * CELL, 18, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search items..."));
        search.setChangedListener(s -> { page = 0; applyFilter(); });
        addDrawableChild(search);
        setInitialFocus(search);

        int navY = gridTop + ROWS * CELL + 4;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { if (page > 0) { page--; rebuildCells(); } })
                .dimensions(left, navY, 40, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            if ((page + 1) * COLS * ROWS < filtered.size()) { page++; rebuildCells(); }
        }).dimensions(left + COLS * CELL - 40, navY, 40, 18).build());

        int btnY = navY + 62;
        startStop = addDrawableChild(ButtonWidget.builder(startLabel(), b -> {
            if (AuctionManager.active) AuctionManager.end(); else AuctionManager.start();
            b.setMessage(startLabel());
        }).dimensions(left, btnY, 106, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Settings"),
                b -> this.client.setScreen(new ConfigScreen(this)))
                .dimensions(left + 114, btnY, 106, 20).build());

        applyFilter();
    }

    private Text startLabel() {
        return Text.literal(AuctionManager.active ? "End Auction" : "Start Auction");
    }

    private void applyFilter() {
        filtered.clear();
        String q = search == null ? "" : search.getText().trim().toLowerCase();
        for (ItemStack s : ALL) {
            if (q.isEmpty()
                    || s.getName().getString().toLowerCase().contains(q)
                    || Registries.ITEM.getId(s.getItem()).getPath().contains(q.replace(' ', '_'))) {
                filtered.add(s);
            }
        }
        rebuildCells();
    }

    private void rebuildCells() {
        for (ButtonWidget c : cells) remove(c);
        cells.clear();
        cellStacks.clear();
        int start = page * COLS * ROWS;
        for (int i = 0; i < COLS * ROWS; i++) {
            int idx = start + i;
            if (idx >= filtered.size()) break;
            ItemStack stack = filtered.get(idx);
            int cx = left + (i % COLS) * CELL;
            int cy = gridTop + (i / COLS) * CELL;
            ButtonWidget b = ButtonWidget.builder(Text.empty(), btn -> {
                if (AuctionManager.active) return; // can't swap item mid-auction
                AuctionManager.select(stack);
            }).dimensions(cx, cy, CELL, CELL).build();
            cells.add(b);
            cellStacks.add(stack);
            addDrawableChild(b);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFFFF);

        ItemStack hovered = null;
        for (int i = 0; i < cells.size(); i++) {
            ButtonWidget b = cells.get(i);
            ItemStack s = cellStacks.get(i);
            ctx.drawItem(s, b.getX() + 2, b.getY() + 2);
            if (!AuctionManager.item.isEmpty() && s.getItem() == AuctionManager.item.getItem()) {
                ctx.fill(b.getX(), b.getY(), b.getX() + CELL, b.getY() + 1, 0xFF55FF55);
                ctx.fill(b.getX(), b.getY() + CELL - 1, b.getX() + CELL, b.getY() + CELL, 0xFF55FF55);
                ctx.fill(b.getX(), b.getY(), b.getX() + 1, b.getY() + CELL, 0xFF55FF55);
                ctx.fill(b.getX() + CELL - 1, b.getY(), b.getX() + CELL, b.getY() + CELL, 0xFF55FF55);
            }
            if (mouseX >= b.getX() && mouseX < b.getX() + CELL && mouseY >= b.getY() && mouseY < b.getY() + CELL) {
                hovered = s;
            }
        }

        int navY = gridTop + ROWS * CELL + 4;
        int pages = Math.max(1, (filtered.size() + COLS * ROWS - 1) / (COLS * ROWS));
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Page " + (page + 1) + "/" + pages + "  (" + filtered.size() + " items)"),
                this.width / 2, navY + 5, 0xFFAAAAAA);

        // Spinning preview of the selected item
        if (!AuctionManager.item.isEmpty()) {
            AuctionRenderer.drawSpinningItem(ctx, AuctionManager.item, this.width / 2f, navY + 34, 36);
            ctx.drawCenteredTextWithShadow(this.textRenderer, AuctionManager.item.getName(),
                    this.width / 2, navY + 52, 0xFFFFFF55);
        } else {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Click an item to select it"),
                    this.width / 2, navY + 34, 0xFFAAAAAA);
        }

        if (hovered != null) ctx.drawItemTooltip(this.textRenderer, hovered, mouseX, mouseY);
    }
}

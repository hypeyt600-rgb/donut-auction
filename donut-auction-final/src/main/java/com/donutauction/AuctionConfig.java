package com.donutauction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class AuctionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("donutauction.json");

    public static AuctionConfig INSTANCE = new AuctionConfig();

    /** Lowest bid that will be accepted. */
    public long minBid = 1000;
    /** A new bid must beat the current top bid by at least this much. */
    public long minIncrement = 100;
    public int durationSeconds = 60;
    /** What the item cost you / is worth to you. Used for profit vs loss. */
    public long itemCost = 0;
    public boolean announceInChat = true;
    public String selectedItem = "minecraft:diamond";

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                AuctionConfig c = GSON.fromJson(Files.readString(PATH), AuctionConfig.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(INSTANCE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

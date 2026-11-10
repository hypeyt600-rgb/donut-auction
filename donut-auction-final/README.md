# Donut Auction (Fabric, Minecraft 1.21.11) - client-side

## Build
1. Install JDK 21.
2. Easiest: copy `src/` + `build.gradle` + `gradle.properties` + `settings.gradle` into a fresh Fabric
   example-mod project for 1.21.11 (it has the Gradle wrapper), or run `gradle wrapper --gradle-version 9.2.1`.
3. `./gradlew build` -> jar in `build/libs/`.
4. Put the jar in `.minecraft/mods/` with Fabric Loader 0.18+ and Fabric API 0.141.3+1.21.11.

## Use
- Press K (rebindable) -> pick any item (search + pages), Start Auction.
- Settings button / `/dauc config`: minimum bid, minimum raise, length, your item cost, chat announcements.
- Bids are read from chat: "Name » 5k", "<Name> 5k", whispers "Name -> you: 5k". Accepts 5000 / 5k / 1.5m / 2b.
- Commands: /dauc, /dauc start|end|config|hide|reset|test, /dauc minbid 5k, /dauc cost 2m
- Winner effect: gold flash, confetti, fireworks, SOLD! banner + spinning item + sounds. Preview with /dauc test.


- Profit = top bid - your item cost (green profit / red loss), plus session totals.

package max.singer.arrow.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.fabricmc.loader.api.FabricLoader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Path;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class ArrowClient implements ClientModInitializer {
    private static long timerEndTime = 0L;
    private static int timerX = 10;
    private static int timerY = 10;
    private static float scale = 1.0f;
    private static boolean expireAlertEnabled = true;
    private static final int MOVE_AMOUNT = 5;
    private static final String TIMER_TEXT = "§fSlowfall Timer: ";
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("slowfall_timer.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static class Config {
        int x = 10;
        int y = 10;
        float scale = 1.0f;
        boolean expireAlertEnabled = true;
    }

    @Override
    public void onInitializeClient() {
        loadConfig();
        registerCommands();

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (timerEndTime > System.currentTimeMillis()) {
                long currentTime = System.currentTimeMillis();
                int secondsLeft = (int)((timerEndTime - currentTime) / 1000);

                if (secondsLeft <= 0) {
                    // Natural timer expiration
                    checkAndPlayExpireAlert();
                    timerEndTime = 0;
                    return;
                }

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    drawContext.getMatrices().push();
                    drawContext.getMatrices().scale(scale, scale, 1.0f);
                    float scaledX = timerX / scale;
                    float scaledY = timerY / scale;

                    String timeColor;
                    if (secondsLeft > 14) {
                        timeColor = "§a";
                    } else if (secondsLeft > 6) {
                        timeColor = "§e";
                    } else {
                        timeColor = "§c";
                    }
                    String timeText = timeColor + secondsLeft + "s";

                    String fullText = TIMER_TEXT + timeText;
                    drawContext.drawText(
                            client.textRenderer,
                            fullText,
                            (int)scaledX,
                            (int)scaledY,
                            0xFFFFFFFF,
                            true
                    );

                    drawContext.getMatrices().pop();
                }
            }
        });
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("sf")
                    .then(literal("moveup")
                            .executes(context -> {
                                timerY = Math.max(0, timerY - MOVE_AMOUNT);
                                saveConfig();
                                context.getSource().sendFeedback(Text.literal("§aTimer moved up"));
                                return 1;
                            }))
                    .then(literal("movedown")
                            .executes(context -> {
                                timerY += MOVE_AMOUNT;
                                saveConfig();
                                context.getSource().sendFeedback(Text.literal("§aTimer moved down"));
                                return 1;
                            }))
                    .then(literal("moveleft")
                            .executes(context -> {
                                timerX = Math.max(0, timerX - MOVE_AMOUNT);
                                saveConfig();
                                context.getSource().sendFeedback(Text.literal("§aTimer moved left"));
                                return 1;
                            }))
                    .then(literal("moveright")
                            .executes(context -> {
                                timerX += MOVE_AMOUNT;
                                saveConfig();
                                context.getSource().sendFeedback(Text.literal("§aTimer moved right"));
                                return 1;
                            }))
                    .then(literal("size")
                            .then(argument("scale", integer(1, 300))
                                    .executes(context -> {
                                        scale = context.getArgument("scale", Integer.class) / 100f;
                                        saveConfig();
                                        context.getSource().sendFeedback(Text.literal("§aTimer size set to " + (scale * 100) + "%"));
                                        return 1;
                                    })))
                    .then(literal("togglealert")
                            .executes(context -> {
                                expireAlertEnabled = !expireAlertEnabled;
                                saveConfig();
                                String status = expireAlertEnabled ? "enabled" : "disabled";
                                context.getSource().sendFeedback(Text.literal("§aExpire alert " + status));
                                return 1;
                            }))
                    .then(literal("reset")
                            .executes(context -> {
                                timerX = 10;
                                timerY = 10;
                                scale = 1.0f;
                                saveConfig();
                                context.getSource().sendFeedback(Text.literal("§aTimer settings reset"));
                                return 1;
                            })));
        });
    }

    private void loadConfig() {
        try {
            if (CONFIG_PATH.toFile().exists()) {
                Config config = GSON.fromJson(new FileReader(CONFIG_PATH.toFile()), Config.class);
                timerX = config.x;
                timerY = config.y;
                scale = config.scale;
                expireAlertEnabled = config.expireAlertEnabled;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveConfig() {
        try {
            Config config = new Config();
            config.x = timerX;
            config.y = timerY;
            config.scale = scale;
            config.expireAlertEnabled = expireAlertEnabled;

            try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void checkAndPlayExpireAlert() {
        if (!expireAlertEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            long nearbyPlayerCount = client.world.getPlayers()
                    .stream()
                    .filter(p -> p != client.player)
                    .count();

            if (nearbyPlayerCount == 1) {
                // Fixed playSound method with correct parameters
                client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 0.5f);
                client.player.sendMessage(Text.literal("§cOpponent doesn't have slowfalling!"), true);
            }
        }
    }

    public static void startTimer() {
        timerEndTime = System.currentTimeMillis() + (30 * 1000);
    }

    public static void stopTimer(boolean isTotemPop) {
        // Only play expire sound if it's not from a totem pop
        if (!isTotemPop) {
            checkAndPlayExpireAlert();
        }
        timerEndTime = 0;
    }
}
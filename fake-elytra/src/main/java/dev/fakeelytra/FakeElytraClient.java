package dev.fakeelytra;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Singleplayer-only toggles.
 *   /fakeelytra          opens the module menu
 *   /fakeelytra toggle   switches Fake Elytra on/off straight away
 * Nothing is active outside a singleplayer world.
 */
public class FakeElytraClient implements ClientModInitializer {

    public static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("fakeelytra.properties");

    private boolean nightVisionApplied = false;
    private final Map<Long, SpawnerSim> spawners = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ModuleManager.load(CONFIG_PATH);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("fakeelytra")
                        .executes(ctx -> {
                            Minecraft mc = Minecraft.getInstance();
                            // Open on the next tick, after the chat screen has closed
                            mc.execute(() -> mc.setScreen(new ModulesScreen()));
                            return 1;
                        })
                        .then(ClientCommandManager.literal("toggle")
                                .executes(ctx -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    if (mc.player == null || mc.getSingleplayerServer() == null) {
                                        ctx.getSource().sendFeedback(Component.literal(
                                                "Fake Elytra only works in singleplayer worlds."));
                                        return 0;
                                    }
                                    boolean on = ModuleManager.FAKE_ELYTRA.toggle();
                                    ModuleManager.save(CONFIG_PATH);
                                    ctx.getSource().sendFeedback(Component.literal(
                                            "Fake Elytra: " + (on ? "ON (jump in the air to glide)" : "OFF")));
                                    return 1;
                                }))));

        // Spawner Demo: right-click a spawner block (singleplayer only) to open the demo menu
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!level.isClientSide()
                    || hand != InteractionHand.MAIN_HAND
                    || player.isShiftKeyDown()
                    || mc.getSingleplayerServer() == null
                    || !ModuleManager.SPAWNER_DEMO.isEnabled()
                    || !level.getBlockState(hit.getBlockPos()).is(Blocks.SPAWNER)) {
                return InteractionResult.PASS;
            }
            SpawnerSim sim = spawners.computeIfAbsent(
                    hit.getBlockPos().asLong(), k -> new SpawnerSim(System.currentTimeMillis()));
            mc.setScreen(new SpawnerScreen(sim));
            return InteractionResult.SUCCESS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
            ModuleManager.setLocalPlayer(null);
            nightVisionApplied = false;
            spawners.clear();
        });
    }

    private void onTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        boolean singleplayer = player != null && mc.getSingleplayerServer() != null;

        // Modules only ever act on the local player in a singleplayer world.
        ModuleManager.setLocalPlayer(singleplayer ? player.getUUID() : null);
        if (!singleplayer) {
            nightVisionApplied = false;
            return;
        }

        // Glide Boost: small push along your look direction while gliding
        if (ModuleManager.GLIDE_BOOST.isEnabled() && player.isFallFlying()) {
            Vec3 velocity = player.getDeltaMovement();
            if (velocity.length() < 2.0) {
                player.setDeltaMovement(velocity.add(player.getLookAngle().scale(0.04)));
            }
        }

        // Night Vision: client-side effect, removed again when switched off
        if (ModuleManager.NIGHT_VISION.isEnabled()) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
            nightVisionApplied = true;
        } else if (nightVisionApplied) {
            player.removeEffect(MobEffects.NIGHT_VISION);
            nightVisionApplied = false;
        }
    }
}

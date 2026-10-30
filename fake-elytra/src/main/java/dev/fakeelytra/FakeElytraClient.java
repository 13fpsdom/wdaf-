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
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
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

    private volatile boolean openMenuNextTick = false;
    private boolean jumpWasDown = false;
    private int airborneTicks = 0;
    private boolean nightVisionApplied = false;
    private final Map<Long, SpawnerSim> spawners = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ModuleManager.load(CONFIG_PATH);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("fakeelytra")
                        .executes(ctx -> {
                            // The chat screen closes itself right after a command runs, which
                            // would close our menu too. So just flag it and open it next tick.
                            openMenuNextTick = true;
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
        if (openMenuNextTick) {
            openMenuNextTick = false;
            mc.setScreen(new ModulesScreen());
        }

        LocalPlayer player = mc.player;
        boolean singleplayer = player != null && mc.getSingleplayerServer() != null;

        // Modules only ever act on the local player in a singleplayer world.
        ModuleManager.setLocalPlayer(singleplayer ? player.getUUID() : null);
        if (!singleplayer) {
            nightVisionApplied = false;
            return;
        }

        // Fake Elytra: start gliding on a second jump press in mid-air.
        // (Backup for vanilla's own check, which may skip players without a real elytra.)
        boolean jumpDown = mc.options.keyJump.isDown();
        airborneTicks = player.onGround() ? 0 : airborneTicks + 1;
        if (ModuleManager.FAKE_ELYTRA.isEnabled() && jumpDown && !jumpWasDown && !player.onGround()) {
            boolean started = false;
            if (airborneTicks >= 2
                    && mc.screen == null
                    && !player.isFallFlying()
                    && !player.getAbilities().flying) {
                started = player.tryToStartFallFlying();
                if (started) {
                    player.connection.send(new ServerboundPlayerCommandPacket(
                            player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                }
            }
            // TEMPORARY debug line (shows above the hotbar) so we can see which step fails
            player.displayClientMessage(Component.literal(
                    "FE debug: air=" + airborneTicks
                            + " canGlide=" + player.canGlide()
                            + " started=" + started
                            + " gliding=" + player.isFallFlying()
                            + " flying=" + player.getAbilities().flying), true);
        }
        jumpWasDown = jumpDown;

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

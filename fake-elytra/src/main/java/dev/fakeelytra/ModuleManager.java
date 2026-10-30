package dev.fakeelytra;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

/**
 * Holds every module and which player (the local singleplayer player) they apply to.
 * Contains no Minecraft classes so it can be unit tested.
 */
public final class ModuleManager {

    public static final FeatureModule FAKE_ELYTRA = new FeatureModule(
            "fake_elytra", "Fake Elytra",
            "Glide and use fireworks without wearing an elytra.", FeatureModule.Category.MOVEMENT);

    public static final FeatureModule GLIDE_BOOST = new FeatureModule(
            "glide_boost", "Glide Boost",
            "Gentle speed boost while you are gliding.", FeatureModule.Category.MOVEMENT);

    public static final FeatureModule NIGHT_VISION = new FeatureModule(
            "night_vision", "Night Vision",
            "See in the dark (visual only).", FeatureModule.Category.VISUALS);

    public static final FeatureModule SPAWNER_DEMO = new FeatureModule(
            "spawner_demo", "Spawner Demo",
            "Right-click a spawner block for a skeleton spawner menu (demo).", FeatureModule.Category.VISUALS);

    public static final List<FeatureModule> ALL = List.of(FAKE_ELYTRA, GLIDE_BOOST, NIGHT_VISION, SPAWNER_DEMO);

    /** The local player, set only while in a singleplayer world. Modules never act on anyone else. */
    private static volatile UUID localPlayer = null;

    private ModuleManager() {}

    public static void setLocalPlayer(UUID player) {
        localPlayer = player;
    }

    public static UUID localPlayer() {
        return localPlayer;
    }

    /** True if the module is switched on AND this is the singleplayer local player. */
    public static boolean isActiveFor(FeatureModule module, UUID player) {
        UUID local = localPlayer;
        return module.isEnabled() && player != null && player.equals(local);
    }

    /** Modules matching the category (null = all) and the search text (case-insensitive). */
    public static List<FeatureModule> search(FeatureModule.Category category, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<FeatureModule> out = new ArrayList<>();
        for (FeatureModule m : ALL) {
            if (category != null && m.category != category) continue;
            if (!q.isEmpty()
                    && !m.name.toLowerCase(Locale.ROOT).contains(q)
                    && !m.description.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            out.add(m);
        }
        return out;
    }

    public static int installedCount() {
        return ALL.size();
    }

    public static int enabledCount() {
        int n = 0;
        for (FeatureModule m : ALL) {
            if (m.isEnabled()) n++;
        }
        return n;
    }

    public static void save(Path file) {
        Properties p = new Properties();
        for (FeatureModule m : ALL) {
            p.setProperty(m.id, Boolean.toString(m.isEnabled()));
        }
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "Fake Elytra module settings");
        } catch (IOException e) {
            System.err.println("[FakeElytra] Could not save settings: " + e.getMessage());
        }
    }

    public static void load(Path file) {
        if (!Files.exists(file)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            System.err.println("[FakeElytra] Could not load settings: " + e.getMessage());
            return;
        }
        for (FeatureModule m : ALL) {
            m.setEnabled(Boolean.parseBoolean(p.getProperty(m.id, "false")));
        }
    }
}

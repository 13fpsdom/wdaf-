package dev.fakeelytra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModuleManagerTest {
    private final UUID me = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @AfterEach
    void reset() {
        ModuleManager.ALL.forEach(m -> m.setEnabled(false));
        ModuleManager.setLocalPlayer(null);
    }

    @Test
    void everythingOffByDefault() {
        assertEquals(0, ModuleManager.enabledCount());
        assertEquals(4, ModuleManager.installedCount());
    }

    @Test
    void activeOnlyForLocalPlayerWhenEnabled() {
        ModuleManager.FAKE_ELYTRA.setEnabled(true);
        assertFalse(ModuleManager.isActiveFor(ModuleManager.FAKE_ELYTRA, me)); // no local player yet
        ModuleManager.setLocalPlayer(me);
        assertTrue(ModuleManager.isActiveFor(ModuleManager.FAKE_ELYTRA, me));
        assertFalse(ModuleManager.isActiveFor(ModuleManager.FAKE_ELYTRA, other));
        assertFalse(ModuleManager.isActiveFor(ModuleManager.GLIDE_BOOST, me)); // not enabled
    }

    @Test
    void leavingSingleplayerDeactivatesModules() {
        ModuleManager.FAKE_ELYTRA.setEnabled(true);
        ModuleManager.setLocalPlayer(me);
        ModuleManager.setLocalPlayer(null);
        assertFalse(ModuleManager.isActiveFor(ModuleManager.FAKE_ELYTRA, me));
    }

    @Test
    void searchFiltersByCategoryAndText() {
        assertEquals(4, ModuleManager.search(null, "").size());
        assertEquals(2, ModuleManager.search(FeatureModule.Category.MOVEMENT, "").size());
        assertEquals(2, ModuleManager.search(FeatureModule.Category.VISUALS, "").size());
        assertEquals(1, ModuleManager.search(null, "NIGHT").size());
        assertEquals(0, ModuleManager.search(null, "nothing like this").size());
    }

    @Test
    void togglesAreCounted() {
        ModuleManager.FAKE_ELYTRA.toggle();
        ModuleManager.NIGHT_VISION.toggle();
        assertEquals(2, ModuleManager.enabledCount());
        ModuleManager.NIGHT_VISION.toggle();
        assertEquals(1, ModuleManager.enabledCount());
    }

    @Test
    void settingsSurviveSaveAndLoad(@TempDir Path dir) {
        Path file = dir.resolve("fakeelytra.properties");
        ModuleManager.FAKE_ELYTRA.setEnabled(true);
        ModuleManager.save(file);
        ModuleManager.ALL.forEach(m -> m.setEnabled(false));
        ModuleManager.load(file);
        assertTrue(ModuleManager.FAKE_ELYTRA.isEnabled());
        assertFalse(ModuleManager.GLIDE_BOOST.isEnabled());
    }
}

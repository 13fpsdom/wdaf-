package dev.fakeelytra;

/** One toggleable feature shown in the menu. Contains no Minecraft classes. */
public final class FeatureModule {

    public enum Category {
        MOVEMENT("Movement"),
        VISUALS("Visuals");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    private volatile boolean enabled;

    public FeatureModule(String id, String name, String description, Category category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean toggle() {
        enabled = !enabled;
        return enabled;
    }
}

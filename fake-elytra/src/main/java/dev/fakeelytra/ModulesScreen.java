package dev.fakeelytra;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Dark module-list menu: search box, category tabs, one ON/OFF switch per module. */
public class ModulesScreen extends Screen {
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 230;
    private static final int ROW_H = 38;
    private static final int MAX_ROWS = 4;

    private static final int COL_PANEL = 0xF0121216;
    private static final int COL_SIDEBAR = 0xFF1A1A20;
    private static final int COL_ROW = 0xFF1E1E26;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_DIM = 0xFF8A8A96;
    private static final int COL_GREEN = 0xFF55FF55;

    private int left;
    private int top;
    private FeatureModule.Category category = null; // null = All
    private String query = "";

    private EditBox searchBox;
    private final List<Button> toggleButtons = new ArrayList<>();
    private final List<Button> categoryButtons = new ArrayList<>();
    private List<FeatureModule> visibleRows = new ArrayList<>();

    public ModulesScreen() {
        super(Component.literal("Fake Elytra"));
    }

    @Override
    protected void init() {
        left = (this.width - PANEL_W) / 2;
        top = (this.height - PANEL_H) / 2;
        toggleButtons.clear();
        categoryButtons.clear();

        // Search box
        searchBox = new EditBox(this.font, left + 100, top + 10, PANEL_W - 110, 18, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search modules..."));
        searchBox.setValue(query);
        searchBox.setResponder(text -> {
            query = text;
            refresh();
        });
        this.addRenderableWidget(searchBox);

        // Category tabs (All + each category)
        addCategoryButton("All", null, 0);
        int i = 1;
        for (FeatureModule.Category c : FeatureModule.Category.values()) {
            addCategoryButton(c.label, c, i++);
        }

        // One toggle button per module (positioned in refresh())
        for (FeatureModule m : ModuleManager.ALL) {
            Button b = Button.builder(stateLabel(m), button -> {
                m.toggle();
                button.setMessage(stateLabel(m));
                ModuleManager.save(FakeElytraClient.CONFIG_PATH);
            }).bounds(0, 0, 54, 20).build();
            toggleButtons.add(b);
            this.addRenderableWidget(b);
        }

        // Close
        this.addRenderableWidget(
                Button.builder(Component.literal("Close"), b -> this.onClose())
                        .bounds(left + 8, top + PANEL_H - 52, 80, 18)
                        .build());

        refresh();
    }

    private void addCategoryButton(String label, FeatureModule.Category c, int index) {
        Button b = Button.builder(Component.literal(label), button -> {
            category = c;
            refresh();
        }).bounds(left + 8, top + 38 + index * 22, 80, 20).build();
        categoryButtons.add(b);
        this.addRenderableWidget(b);
    }

    private Component stateLabel(FeatureModule m) {
        return m.isEnabled()
                ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                : Component.literal("OFF").withStyle(ChatFormatting.RED);
    }

    /** Re-applies the search/category filter: shows, hides and moves the toggle buttons. */
    private void refresh() {
        visibleRows = ModuleManager.search(category, query);
        if (visibleRows.size() > MAX_ROWS) {
            visibleRows = new ArrayList<>(visibleRows.subList(0, MAX_ROWS));
        }
        for (int i = 0; i < ModuleManager.ALL.size(); i++) {
            FeatureModule m = ModuleManager.ALL.get(i);
            Button b = toggleButtons.get(i);
            int row = visibleRows.indexOf(m);
            b.visible = row >= 0;
            b.active = row >= 0;
            if (row >= 0) {
                b.setX(left + PANEL_W - 54 - 12);
                b.setY(rowTop(row) + (ROW_H - 4 - 20) / 2 + 2);
            }
        }
    }

    private int rowTop(int row) {
        return top + 38 + row * ROW_H;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);

        // Panel + sidebar
        g.fill(left, top, left + PANEL_W, top + PANEL_H, COL_PANEL);
        g.fill(left, top, left + 96, top + PANEL_H, COL_SIDEBAR);

        // FeatureModule rows
        for (int i = 0; i < visibleRows.size(); i++) {
            int y = rowTop(i);
            g.fill(left + 104, y, left + PANEL_W - 8, y + ROW_H - 4, COL_ROW);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        g.drawString(this.font, "FAKE ELYTRA", left + 10, top + 14, COL_TEXT);
        g.drawString(this.font, "CATEGORIES", left + 10, top + 28, COL_DIM);

        for (int i = 0; i < visibleRows.size(); i++) {
            FeatureModule m = visibleRows.get(i);
            int y = rowTop(i);
            g.drawString(this.font, m.name, left + 112, y + 7, COL_TEXT);
            g.drawString(this.font, m.description, left + 112, y + 19, COL_DIM);
        }
        if (visibleRows.isEmpty()) {
            g.drawString(this.font, "No modules found", left + 112, top + 50, COL_DIM);
        }

        // Footer
        g.drawString(this.font,
                ModuleManager.installedCount() + " installed",
                left + 10, top + PANEL_H - 28, COL_TEXT);
        g.drawString(this.font,
                ModuleManager.enabledCount() + " enabled",
                left + 10, top + PANEL_H - 16, COL_GREEN);

        boolean single = Minecraft.getInstance().getSingleplayerServer() != null;
        g.drawString(this.font,
                single ? "Active in this world" : "Open a singleplayer world to use modules",
                left + 104, top + PANEL_H - 16, single ? COL_GREEN : COL_DIM);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

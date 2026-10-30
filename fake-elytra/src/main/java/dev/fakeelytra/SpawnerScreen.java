package dev.fakeelytra;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Singleplayer demo of a skeleton spawner menu (storage, collect items, collect XP, stack size).
 * It is labelled as a demo and only opens in singleplayer worlds.
 */
public class SpawnerScreen extends Screen {
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 164;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;

    private final SpawnerSim sim;
    private int left;
    private int top;
    private Button takeButton;
    private Button xpButton;

    public SpawnerScreen(SpawnerSim sim) {
        super(Component.literal("Skeleton Spawner"));
        this.sim = sim;
    }

    @Override
    protected void init() {
        left = (this.width - PANEL_W) / 2;
        top = (this.height - PANEL_H) / 2;

        int bw = 78;
        int y1 = top + 114;
        int y2 = top + 136;

        takeButton = Button.builder(Component.literal("Take items"), b -> giveItems())
                .bounds(left + 8, y1, bw, 20).build();
        xpButton = Button.builder(Component.literal("Collect XP"), b -> giveXp())
                .bounds(left + 8 + bw + 4, y1, bw, 20).build();
        this.addRenderableWidget(takeButton);
        this.addRenderableWidget(xpButton);

        // Stack size: -10, -1, +1, +10
        int[] deltas = {-10, -1, 1, 10};
        for (int i = 0; i < deltas.length; i++) {
            final int delta = deltas[i];
            String label = (delta > 0 ? "+" : "") + delta;
            this.addRenderableWidget(Button.builder(Component.literal(label),
                            b -> sim.setStackSize(sim.stackSize() + delta, System.currentTimeMillis()))
                    .bounds(left + 8 + i * 40, y2, 38, 20).build());
        }
    }

    @Override
    public void tick() {
        sim.update(System.currentTimeMillis());
    }

    // ---- giving things to the player (integrated server does the real work) ----

    private void giveItems() {
        sim.update(System.currentTimeMillis());
        int bones = sim.takeBones();
        int arrows = sim.takeArrows();
        giveToPlayer(Items.BONE, bones);
        giveToPlayer(Items.ARROW, arrows);
    }

    private void giveXp() {
        sim.update(System.currentTimeMillis());
        int xp = sim.takeXp();
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null || xp <= 0) return;
        var id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) sp.giveExperiencePoints(xp);
        });
    }

    private void giveToPlayer(net.minecraft.world.item.Item item, int amount) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null || amount <= 0) return;
        var id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp == null) return;
            int left = amount;
            while (left > 0) {
                int n = Math.min(64, left);
                left -= n;
                ItemStack stack = new ItemStack(item, n);
                if (!sp.getInventory().add(stack)) {
                    sp.drop(stack, false);
                }
            }
        });
    }

    // ---- drawing ----

    private List<ItemStack> storedStacks() {
        List<ItemStack> out = new ArrayList<>();
        addStacks(out, Items.BONE, sim.bones());
        addStacks(out, Items.ARROW, sim.arrows());
        return out;
    }

    private static void addStacks(List<ItemStack> out, net.minecraft.world.item.Item item, int amount) {
        int remaining = amount;
        while (remaining > 0 && out.size() < 45) {
            int n = Math.min(64, remaining);
            remaining -= n;
            out.add(new ItemStack(item, n));
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);

        // Vanilla-style grey panel with bevel
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFFC6C6C6);
        g.fill(left, top, left + PANEL_W, top + 2, 0xFFFFFFFF);
        g.fill(left, top, left + 2, top + PANEL_H, 0xFFFFFFFF);
        g.fill(left, top + PANEL_H - 2, left + PANEL_W, top + PANEL_H, 0xFF555555);
        g.fill(left + PANEL_W - 2, top, left + PANEL_W, top + PANEL_H, 0xFF555555);

        // 9 x 5 slots
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                int x = left + GRID_X + col * 18;
                int y = top + GRID_Y + row * 18;
                g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
                g.fill(x, y, x + 17, y + 1, 0xFF373737);
                g.fill(x, y, x + 1, y + 17, 0xFF373737);
                g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
                g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        g.drawString(this.font, sim.stackSize() + " SKELETON SPAWNER", left + 8, top + 6, 0xFF404040, false);

        // Items in the slots
        List<ItemStack> stacks = storedStacks();
        for (int i = 0; i < stacks.size(); i++) {
            int x = left + GRID_X + (i % 9) * 18 + 1;
            int y = top + GRID_Y + (i / 9) * 18 + 1;
            g.renderItem(stacks.get(i), x, y);
            g.renderItemDecorations(this.font, stacks.get(i), x, y);
        }

        // Info line
        g.drawString(this.font,
                "XP: " + sim.xp() + "   Items: " + sim.totalItems(),
                left + 8, top + PANEL_H - 12, 0xFF404040, false);

        // Always-visible label so nobody mistakes this for the real server menu
        g.drawString(this.font, "Singleplayer demo - not a real spawner",
                left, top + PANEL_H + 6, 0xFFCCCCCC, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

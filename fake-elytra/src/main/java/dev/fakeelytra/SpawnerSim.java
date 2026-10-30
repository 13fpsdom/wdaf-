package dev.fakeelytra;

/**
 * Pure-Java simulation of a skeleton spawner's storage for the singleplayer demo menu.
 * Contains no Minecraft classes so it can be unit tested.
 *
 * Every CYCLE_MS, each spawner in the stack adds 1 bone, 1 arrow and 5 xp.
 */
public final class SpawnerSim {
    public static final long CYCLE_MS = 10_000L;
    public static final int MAX_STACK = 1000;
    /** Storage is a 9x5 grid of 64-item stacks, shared between bones and arrows. */
    public static final int MAX_ITEMS = 9 * 5 * 64;

    private int stackSize = 1;
    private long lastUpdateMs;
    private long carryMs = 0;
    private int bones = 0;
    private int arrows = 0;
    private int xp = 0;

    public SpawnerSim(long nowMs) {
        this.lastUpdateMs = nowMs;
    }

    public int stackSize() {
        return stackSize;
    }

    public int bones() {
        return bones;
    }

    public int arrows() {
        return arrows;
    }

    public int xp() {
        return xp;
    }

    public int totalItems() {
        return bones + arrows;
    }

    /** Brings storage up to date for the current time. */
    public void update(long nowMs) {
        long elapsed = Math.max(0, nowMs - lastUpdateMs);
        lastUpdateMs = nowMs;
        carryMs += elapsed;
        long cycles = carryMs / CYCLE_MS;
        carryMs %= CYCLE_MS;
        if (cycles <= 0) return;

        long perCycle = stackSize;
        long newBones = cycles * perCycle;
        long newArrows = cycles * perCycle;
        long space = MAX_ITEMS - totalItems();
        if (space > 0) {
            long addBones = Math.min(newBones, space / 2 + space % 2);
            long addArrows = Math.min(newArrows, space - addBones);
            bones += (int) addBones;
            arrows += (int) addArrows;
        }
        xp = (int) Math.min(1_000_000L, xp + cycles * perCycle * 5);
    }

    public void setStackSize(int size, long nowMs) {
        update(nowMs); // settle what was earned at the old stack size first
        stackSize = Math.max(1, Math.min(MAX_STACK, size));
    }

    /** Empties and returns the stored bones. */
    public int takeBones() {
        int n = bones;
        bones = 0;
        return n;
    }

    public int takeArrows() {
        int n = arrows;
        arrows = 0;
        return n;
    }

    public int takeXp() {
        int n = xp;
        xp = 0;
        return n;
    }
}

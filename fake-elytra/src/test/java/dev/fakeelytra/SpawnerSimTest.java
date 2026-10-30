package dev.fakeelytra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpawnerSimTest {
    @Test
    void producesDropsEveryCycle() {
        SpawnerSim s = new SpawnerSim(0);
        s.update(SpawnerSim.CYCLE_MS * 3);
        assertEquals(3, s.bones());
        assertEquals(3, s.arrows());
        assertEquals(15, s.xp());
    }

    @Test
    void biggerStackProducesMore() {
        SpawnerSim s = new SpawnerSim(0);
        s.setStackSize(10, 0);
        s.update(SpawnerSim.CYCLE_MS * 2);
        assertEquals(20, s.bones());
    }

    @Test
    void partialCyclesAreKept() {
        SpawnerSim s = new SpawnerSim(0);
        s.update(SpawnerSim.CYCLE_MS / 2);
        assertEquals(0, s.bones());
        s.update(SpawnerSim.CYCLE_MS);
        assertEquals(1, s.bones());
    }

    @Test
    void takingEmptiesStorage() {
        SpawnerSim s = new SpawnerSim(0);
        s.update(SpawnerSim.CYCLE_MS * 4);
        assertEquals(4, s.takeBones());
        assertEquals(0, s.bones());
        assertEquals(4, s.takeArrows());
        assertEquals(20, s.takeXp());
        assertEquals(0, s.xp());
    }

    @Test
    void storageIsCapped() {
        SpawnerSim s = new SpawnerSim(0);
        s.setStackSize(SpawnerSim.MAX_STACK, 0);
        s.update(SpawnerSim.CYCLE_MS * 100_000L);
        assertTrue(s.totalItems() <= SpawnerSim.MAX_ITEMS);
    }

    @Test
    void stackSizeIsClamped() {
        SpawnerSim s = new SpawnerSim(0);
        s.setStackSize(0, 0);
        assertEquals(1, s.stackSize());
        s.setStackSize(99_999, 0);
        assertEquals(SpawnerSim.MAX_STACK, s.stackSize());
    }
}

package ca.lukegrahamlandry.smellsfishy.data;

import java.util.List;

public class EntityRainEvent {
    public int chance, spawnRate, radius, height;
    public List<EntitySpawnOption> spawn;
    public EventConditions when;
}

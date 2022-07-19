package ca.lukegrahamlandry.smellsfishy.data;

import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;

public class EntitySpawnOption implements WeightedEntry {
    public String entity;
    public int weight;

    @Override
    public Weight getWeight() {
        return Weight.of(weight);
    }
}

package ca.lukegrahamlandry.smellsfishy.data;

import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;

import java.util.List;

public class EntitySpawnOption implements WeightedEntry, IBiomeListHolder {
    public String entity;
    public int weight;
    public List<String> biomes;
    public boolean biomesIsBlacklist = false;

    @Override
    public Weight getWeight() {
        return Weight.of(weight);
    }

    @Override
    public List<String> getBiomes() {
        return this.biomes;
    }

    @Override
    public boolean isBlacklist() {
        return this.biomesIsBlacklist;
    }
}

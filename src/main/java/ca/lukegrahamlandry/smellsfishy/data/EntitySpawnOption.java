package ca.lukegrahamlandry.smellsfishy.data;

import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Entity;

import java.util.Random;

public class EntitySpawnOption implements WeightedEntry {
    public String entity;
    public int weight;

    @Override
    public Weight getWeight() {
        return Weight.of(weight);
    }
}

package ca.lukegrahamlandry.smellsfishy.data;

import com.google.gson.JsonElement;

import java.util.List;

public class EntitySpawnOption implements IBiomeListHolder {
    public String entity;
    public int weight;
    public List<String> biomes;
    public boolean biomesIsBlacklist = false;
    public JsonElement nbt = null;

    @Override
    public List<String> getBiomes() {
        return this.biomes;
    }

    @Override
    public boolean isBlacklist() {
        return this.biomesIsBlacklist;
    }
}

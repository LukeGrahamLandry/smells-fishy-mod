package ca.lukegrahamlandry.smellsfishy.data;

import java.util.List;

public class EventConditions implements IBiomeListHolder {
    public boolean raining=true, notRaining=true, night=true, day=true;
    public List<String> dimensions;
    public List<String> biomes;
    public boolean biomesIsBlacklist = false;

    @Override
    public List<String> getBiomes() {
        return this.biomes;
    }

    @Override
    public boolean isBlacklist() {
        return this.biomesIsBlacklist;
    }
}

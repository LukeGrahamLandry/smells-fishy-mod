package ca.lukegrahamlandry.smellsfishy.data;

import ca.lukegrahamlandry.smellsfishy.ModMain;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityRainLoader extends SimpleJsonResourceReloadListener {
    public static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    public Map<ResourceLocation, EntityRainEvent> events = new HashMap<>();

    public EntityRainLoader() {
        super(GSON, "entityrain");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager p_10794_, ProfilerFiller p_10795_) {
        ModMain.LOGGER.debug("loading " + files.size() + " entity rain events");
        for (ResourceLocation name : files.keySet()){
            try {
                JsonObject data = files.get(name).getAsJsonObject();
                if (events.containsKey(name)){
                    if (!handleReplace(name, data)) continue;
                }

                EntityRainEvent stats = GSON.fromJson(data, EntityRainEvent.class);
                stats.spawn.removeIf((option) -> {
                    boolean exists = ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(option.entity));
                    if (!exists) ModMain.LOGGER.debug("entity type " + option.entity + " in " + name + "[spawn] is not registered");
                    return !exists;
                });

                events.put(name, stats);
            } catch (JsonSyntaxException e){
                e.printStackTrace();
                ModMain.LOGGER.error("failed to parse entity rain definition: " + name);
            }
        }
    }

    private boolean handleReplace(ResourceLocation name, JsonObject data){
        boolean replace = !data.has("replace") || data.get("replace").getAsBoolean();
        if (!replace){
            EntityRainEvent stats = events.get(name);
            if (data.has("spawn")){
                for (JsonElement spawnData : data.get("spawn").getAsJsonArray()){
                    EntitySpawnOption option = GSON.fromJson(spawnData, EntitySpawnOption.class);
                    boolean exists = ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(option.entity));
                    if (!exists) {
                        ModMain.LOGGER.debug("entity type " + option.entity + " in " + name + "[spawn] is not registered");
                        continue;
                    }
                    stats.spawn.add(option);
                }
            }

            if (data.has("when") && data.getAsJsonObject("when").has("dimensions")){
                for (JsonElement dimension : data.getAsJsonObject("when").getAsJsonArray("dimensions")){
                    stats.when.dimensions.add(dimension.getAsString());
                }
            }
        }

        return replace;
    }


    @SubscribeEvent
    public static void initListeners(AddReloadListenerEvent event){
        ModMain.ENTITY_RAIN_LOADER = new EntityRainLoader();
        event.addListener(ModMain.ENTITY_RAIN_LOADER);
    }
}
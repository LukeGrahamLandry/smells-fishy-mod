package ca.lukegrahamlandry.smellsfishy.data;

import ca.lukegrahamlandry.smellsfishy.ModMain;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
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
                EntityRainEvent stats = GSON.fromJson(files.get(name), EntityRainEvent.class);
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


    @SubscribeEvent
    public static void initListeners(AddReloadListenerEvent event){
        ModMain.ENTITY_RAIN_LOADER = new EntityRainLoader();
        event.addListener(ModMain.ENTITY_RAIN_LOADER);
    }
}
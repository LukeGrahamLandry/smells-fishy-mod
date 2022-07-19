package ca.lukegrahamlandry.smellsfishy.event;


import ca.lukegrahamlandry.smellsfishy.ModMain;
import ca.lukegrahamlandry.smellsfishy.data.EntityRainEvent;
import ca.lukegrahamlandry.smellsfishy.data.EntitySpawnOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.checkerframework.checker.units.qual.C;

import java.util.*;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RainHandler extends SavedData {
    private static final Random rand = new Random();
    private EntityRainEvent currentEvent = null;
    private List<Entity> existingRainEntities = new ArrayList<>();
    private ResourceLocation currentEventKey = null;

    @SubscribeEvent
    public static void onTickPlayer(TickEvent.PlayerTickEvent event){
        if (event.player.level.isClientSide() || event.phase == TickEvent.Phase.END) return;

        if (get(event.player.getLevel()).currentEvent != null){
            tickRain(event.player, get(event.player.getLevel()).currentEvent);
        }
    }

    @SubscribeEvent
    public static void onTickWorld(TickEvent.WorldTickEvent event){
        if (event.world.isClientSide() || event.phase == TickEvent.Phase.END) return;
        tryStartRainEvents(event.world);
    }

    private static void tickRain(Player player, EntityRainEvent rainEvent) {
        if (rand.nextInt(rainEvent.spawnRate) == 0){
            EntitySpawnOption toSpawn = pickRandom(rainEvent.spawn);
            if (toSpawn == null) return;

            ResourceLocation entityTypeKey = new ResourceLocation(toSpawn.entity);
            EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(entityTypeKey);
            if (entityType == null) return;

            Entity spawn = entityType.create(player.getLevel());
            int x = (int) (player.blockPosition().getX() + rand.nextInt(rainEvent.radius * 2) - rainEvent.radius);
            int z = (int) (player.blockPosition().getZ() + rand.nextInt(rainEvent.radius * 2) - rainEvent.radius);
            int y = player.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + rainEvent.height;
            if (spawn instanceof LivingEntity) ((LivingEntity) spawn).addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));
            spawn.setPos(x, y, z);
            spawn.getPersistentData().putBoolean("entityrain", true);
            player.level.addFreshEntity(spawn);
            get(player.level).existingRainEntities.add(spawn);
            get(player.level).setDirty();
        }
    }

    private static EntitySpawnOption pickRandom(List<EntitySpawnOption> spawn) {
        WeightedRandomList<EntitySpawnOption> options = WeightedRandomList.create(spawn);
        Optional<EntitySpawnOption> result = options.getRandom(rand);
        return result.orElse(null);
    }


    public static boolean startRain(Level level, ResourceLocation rainType){
        stopRain(level);
        EntityRainEvent rain = ModMain.ENTITY_RAIN_LOADER.events.get(rainType);
        if (rain != null) {
            get(level).currentEvent = rain;
            get(level).currentEventKey = rainType;
            get(level).setDirty();
        }

        return rain != null;
    }

    public static void stopRain(Level level) {
        get(level).currentEvent = null;
        get(level).currentEventKey = null;

        for (Entity e : get(level).existingRainEntities){
            e.discard();
        }
        get(level).existingRainEntities.clear();
        get(level).setDirty();
    }

    private boolean wasRaining = false;
    private boolean wasDay = false;
    private static void tryStartRainEvents(Level world) {
        boolean alreadyChecked = false;
        RainHandler data = get(world);
        if (world.isRaining() && !data.wasRaining) {
            data.wasRaining = true;
            checkRainEvent(world);
            alreadyChecked = true;
        }
        if (!world.isRaining() && data.wasRaining) {
            data.wasRaining = false;
            if (!alreadyChecked) checkRainEvent(world);
            alreadyChecked = true;
        }
        if (world.isDay() && !data.wasDay) {
            data.wasDay = true;
            if (!alreadyChecked) checkRainEvent(world);
            alreadyChecked = true;
        }
        if (!world.isDay() && data.wasDay) {
            data.wasDay = false;
            if (!alreadyChecked) checkRainEvent(world);
        }
    }


    private static void checkRainEvent(Level world) {
        for (ResourceLocation rainType : ModMain.ENTITY_RAIN_LOADER.events.keySet()){
            if (get(world).currentEvent != null) stopRain(world);
            EntityRainEvent rainData = ModMain.ENTITY_RAIN_LOADER.events.get(rainType);

            if (!rainData.when.dimensions.contains(world.dimension().location().toString())) continue;
            if (!rainData.when.day && world.isDay()) continue;
            if (!rainData.when.night && !world.isDay()) continue;
            if (!rainData.when.raining && world.isRaining()) continue;
            if (!rainData.when.notRaining && !world.isRaining()) continue;

            if (rand.nextInt(rainData.chance) == 0){
                startRain(world, rainType);
                return;
            }
        }
    }

    // rain entities should not take fall damage
    @SubscribeEvent
    public static void onFall(LivingFallEvent event){
        if (event.getEntityLiving().level.isClientSide()) return;

        if (get(event.getEntityLiving().level).currentEvent != null){
            if (get(event.getEntityLiving().level).existingRainEntities.contains(event.getEntityLiving())){
                event.setDamageMultiplier(0);
            }
        }
    }

    // rain entities should not drop items unless hit by player
    @SubscribeEvent
    public static void onLoot(LivingDropsEvent event){
        if (event.getEntityLiving().level.isClientSide()) return;

        if (event.getEntityLiving().getPersistentData().contains("entityrain")){
            if (!event.isRecentlyHit()) event.setCanceled(true);
        }
    }

    public static RainHandler get(Level level){
        return ((ServerLevel) level).getDataStorage().computeIfAbsent(tag -> RainHandler.load(tag, level), RainHandler::new, ModMain.MOD_ID + ":rain_event_tracker");
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("wasDay", wasDay);
        tag.putBoolean("wasRaining", wasRaining);
        if (currentEventKey != null) tag.putString("event", currentEventKey.toString());

        CompoundTag entities = new CompoundTag();
        int i = 0;
        for (Entity e : existingRainEntities){
            if (!e.isAlive()) continue;
            entities.putUUID(String.valueOf(i), e.getUUID());
            i++;
        }
        tag.put("entities", entities);

        return tag;
    }

    private static RainHandler load(CompoundTag tag, Level level) {
        RainHandler self = new RainHandler();
        self.wasDay = tag.getBoolean("wasDay");
        self.wasRaining = tag.getBoolean("wasRaining");
        self.currentEventKey = tag.contains("event") ? new ResourceLocation(tag.getString("event")) : null;
        self.currentEvent = tag.contains("event") ? ModMain.ENTITY_RAIN_LOADER.events.getOrDefault(self.currentEventKey, null) : null;

        int i = 0;
        CompoundTag entities = tag.getCompound("entities");
        while (entities.contains(String.valueOf(i))){
            Entity e = ((ServerLevel)level).getEntity(entities.getUUID(String.valueOf(i)));
            if (e != null) self.existingRainEntities.add(e);
            i++;
        }

        return self;
    }
}

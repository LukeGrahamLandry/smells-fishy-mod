package ca.lukegrahamlandry.smellsfishy.event;


import ca.lukegrahamlandry.smellsfishy.ModMain;
import ca.lukegrahamlandry.smellsfishy.data.EntityRainEvent;
import ca.lukegrahamlandry.smellsfishy.data.EntitySpawnOption;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RainHandler {
    private static final Random rand = new Random();
    private static final Map<ResourceKey<Level>, EntityRainEvent> currentEvents = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<Entity>> existingRainEntities = new HashMap<>();

    @SubscribeEvent
    public static void onTickPlayer(TickEvent.PlayerTickEvent event){
        if (event.player.level.isClientSide() || event.phase == TickEvent.Phase.END) return;

        if (currentEvents.containsKey(event.player.level.dimension())){
            EntityRainEvent rainEvent = currentEvents.get(event.player.level.dimension());
            tickRain(event.player, rainEvent);
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
            player.level.addFreshEntity(spawn);
            existingRainEntities.get(player.level.dimension()).add(spawn);
        }
    }

    private static EntitySpawnOption pickRandom(List<EntitySpawnOption> spawn) {
        WeightedRandomList<EntitySpawnOption> options = WeightedRandomList.create(spawn);
        Optional<EntitySpawnOption> result = options.getRandom(rand);
        return result.orElse(null);
    }


    public static boolean startRain(Level level, ResourceLocation rainType){
        stopRain(level);
        ResourceKey<Level> dimension = level.dimension();
        EntityRainEvent rain = ModMain.ENTITY_RAIN_LOADER.events.get(rainType);
        if (rain != null) {
            currentEvents.put(dimension, rain);
            existingRainEntities.put(level.dimension(), new ArrayList<>());
        }

        return rain != null;
    }

    public static void stopRain(Level level) {
        currentEvents.remove(level.dimension());

        if (existingRainEntities.containsKey(level.dimension())){
            for (Entity e : existingRainEntities.get(level.dimension())){
                e.discard();
            }
            existingRainEntities.remove(level.dimension());
        }
    }

    private static Map<ResourceKey<Level>, Boolean> wasRaining = new HashMap<>();
    private static Map<ResourceKey<Level>, Boolean> wasDay = new HashMap<>();
    private static void tryStartRainEvents(Level world) {
        boolean alreadyChecked = false;
        if (world.isRaining() && !wasRaining.getOrDefault(world.dimension(), false)) {
            wasRaining.put(world.dimension(), true);
            checkRainEvent(world);
            alreadyChecked = true;
        }
        if (!world.isRaining() && wasRaining.getOrDefault(world.dimension(), false)) {
            wasRaining.put(world.dimension(), false);
            if (!alreadyChecked) checkRainEvent(world);
            alreadyChecked = true;
        }
        if (world.isDay() && !wasDay.getOrDefault(world.dimension(), false)) {
            wasDay.put(world.dimension(), true);
            if (!alreadyChecked) checkRainEvent(world);
            alreadyChecked = true;
        }
        if (!world.isDay() && wasDay.getOrDefault(world.dimension(), false)) {
            wasDay.put(world.dimension(), false);
            if (!alreadyChecked) checkRainEvent(world);
        }
    }


    private static void checkRainEvent(Level world) {
        for (ResourceLocation rainType : ModMain.ENTITY_RAIN_LOADER.events.keySet()){
            if (currentEvents.containsKey(world.dimension())) stopRain(world);
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

        if (existingRainEntities.containsKey(event.getEntityLiving().level.dimension())){
            if (existingRainEntities.get(event.getEntityLiving().level.dimension()).contains(event.getEntityLiving())){
                event.setDamageMultiplier(0);
            }
        }
    }

    // rain entities should not drop items unless hit by player
    @SubscribeEvent
    public static void onLoot(LivingDropsEvent event){
        if (event.getEntityLiving().level.isClientSide()) return;

        if (existingRainEntities.containsKey(event.getEntityLiving().level.dimension())){
            if (existingRainEntities.get(event.getEntityLiving().level.dimension()).contains(event.getEntityLiving())){
                if (!event.isRecentlyHit()) event.setCanceled(true);
            }
        }
    }
}

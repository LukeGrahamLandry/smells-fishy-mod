package ca.lukegrahamlandry.smellsfishy.event;


import ca.lukegrahamlandry.smellsfishy.ModMain;
import ca.lukegrahamlandry.smellsfishy.data.EntityRainEvent;
import ca.lukegrahamlandry.smellsfishy.data.EntitySpawnOption;
import ca.lukegrahamlandry.smellsfishy.data.IBiomeListHolder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RainHandler extends WorldSavedData {
    private static final Random rand = new Random();
    private final World levelForLoad;
    private EntityRainEvent currentEvent = null;
    private List<Entity> existingRainEntities = new ArrayList<>();
    private ResourceLocation currentEventKey = null;

    public RainHandler(String p_i2141_1_, World level) {
        super(p_i2141_1_);
        this.levelForLoad = level;
    }

    @SubscribeEvent
    public static void onTickPlayer(TickEvent.PlayerTickEvent event){
        if (event.player.level.isClientSide() || event.phase == TickEvent.Phase.END) return;

        if (get(event.player.level).currentEvent != null){
            tickRain(event.player, get(event.player.level).currentEvent);
        }
    }

    @SubscribeEvent
    public static void onTickWorld(TickEvent.WorldTickEvent event){
        if (event.world.isClientSide() || event.phase == TickEvent.Phase.END) return;
        tryStartRainEvents(event.world);
    }

    private static void tickRain(PlayerEntity player, EntityRainEvent rainEvent) {
        if (rand.nextInt(rainEvent.spawnRate) == 0){
            int x = (int) (player.blockPosition().getX() + rand.nextInt(rainEvent.radius * 2) - rainEvent.radius);
            int z = (int) (player.blockPosition().getZ() + rand.nextInt(rainEvent.radius * 2) - rainEvent.radius);
            int y = player.level.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) + rainEvent.height;

            Biome currentBiome = player.level.getBiome(new BlockPos(x, y, z));
            if (!filterBiome(rainEvent.when, currentBiome)) return;

            List<EntitySpawnOption> possibleSpawns = new ArrayList<>(rainEvent.spawn);
            possibleSpawns.removeIf((spawnData) -> !filterBiome(spawnData, currentBiome));
            EntitySpawnOption toSpawn = pickRandom(possibleSpawns);
            Entity spawn = createEntity(toSpawn, player.level);
            if (spawn == null) return;

            if (spawn instanceof LivingEntity) ((LivingEntity) spawn).addEffect(new EffectInstance(Effects.SLOW_FALLING, 40, 0, false, false, false));
            spawn.setPos(x, y, z);
            spawn.getPersistentData().putBoolean("entityrain", true);
            player.level.addFreshEntity(spawn);
            get(player.level).existingRainEntities.add(spawn);
            get(player.level).setDirty();
        }
    }

    private static Entity createEntity(EntitySpawnOption toSpawn, World level) {
        if (toSpawn == null) return null;

        ResourceLocation entityTypeKey = new ResourceLocation(toSpawn.entity);
        EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(entityTypeKey);
        if (entityType == null) return null;

        Entity spawn = entityType.create(level);
        if (toSpawn.nbt != null)  {
            try {
                String data = toSpawn.nbt.toString();
                CompoundNBT forcedNbt = (new JsonToNBT(new StringReader(data))).readStruct();
                CompoundNBT defaultNbt = spawn.saveWithoutId(new CompoundNBT());
                for (String key : forcedNbt.getAllKeys()){
                    defaultNbt.put(key, forcedNbt.get(key));
                }
                spawn.load(defaultNbt);
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
        }
        return spawn;
    }

    private static EntitySpawnOption pickRandom(List<EntitySpawnOption> spawn) {
        List<Pair<Integer, EntitySpawnOption>> choices = new ArrayList<>();
        int total = 0;
        for (EntitySpawnOption option : spawn){
            total += option.weight;
            choices.add(new Pair<>(total, option));
        }

        int value = rand.nextInt(total);
        for (Pair<Integer, EntitySpawnOption> check : choices){
            if (value < check.getFirst()) return check.getSecond();
        }

        return spawn.get(spawn.size() - 1);
    }


    public static boolean startRain(World level, ResourceLocation rainType){
        stopRain(level);
        EntityRainEvent rain = ModMain.ENTITY_RAIN_LOADER.events.get(rainType);
        if (rain != null) {
            get(level).currentEvent = rain;
            get(level).currentEventKey = rainType;
            get(level).setDirty();
        }

        return rain != null;
    }

    public static void stopRain(World level) {
        get(level).currentEvent = null;
        get(level).currentEventKey = null;

        for (Entity e : get(level).existingRainEntities){
            e.remove();
        }
        get(level).existingRainEntities.clear();
        get(level).setDirty();
    }

    private boolean wasRaining = false;
    private boolean wasDay = false;
    private static void tryStartRainEvents(World world) {
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


    private static void checkRainEvent(World world) {
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

    public static RainHandler get(World level){
        return ((ServerWorld) level).getDataStorage().computeIfAbsent(() -> new RainHandler(ModMain.MOD_ID + ":rain_event_tracker", level), ModMain.MOD_ID + ":rain_event_tracker");
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        tag.putBoolean("wasDay", wasDay);
        tag.putBoolean("wasRaining", wasRaining);
        if (currentEventKey != null) tag.putString("event", currentEventKey.toString());

        CompoundNBT entities = new CompoundNBT();
        int i = 0;
        for (Entity e : existingRainEntities){
            if (!e.isAlive()) continue;
            entities.putUUID(String.valueOf(i), e.getUUID());
            i++;
        }
        tag.put("entities", entities);

        return tag;
    }

    @Override
    public void load(CompoundNBT tag) {
        this.wasDay = tag.getBoolean("wasDay");
        this.wasRaining = tag.getBoolean("wasRaining");
        this.currentEventKey = tag.contains("event") ? new ResourceLocation(tag.getString("event")) : null;
        this.currentEvent = tag.contains("event") ? ModMain.ENTITY_RAIN_LOADER.events.getOrDefault(this.currentEventKey, null) : null;

        int i = 0;
        CompoundNBT entities = tag.getCompound("entities");
        while (entities.contains(String.valueOf(i))){
            Entity e = ((ServerWorld)this.levelForLoad).getEntity(entities.getUUID(String.valueOf(i)));
            if (e != null) this.existingRainEntities.add(e);
            i++;
        }
    }

    private static boolean filterBiome(IBiomeListHolder spawnData, Biome currentBiome) {
        if (spawnData.getBiomes() == null) return true;

        boolean matches = false;
        for (String checkBiome : spawnData.getBiomes()){
            if (checkBiome.startsWith("#")){
                Biome.Category category = Biome.Category.valueOf(checkBiome.substring(1));
                if (currentBiome.getBiomeCategory().equals(category)){
                    matches = true;
                    break;
                }
            } else {
                if (currentBiome.getRegistryName().equals(new ResourceLocation(checkBiome))){
                    matches = true;
                    break;
                }
            }
        }

        if (matches) return !spawnData.isBlacklist();
        else return spawnData.isBlacklist();
    }
}

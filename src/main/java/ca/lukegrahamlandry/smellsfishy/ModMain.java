package ca.lukegrahamlandry.smellsfishy;

import ca.lukegrahamlandry.smellsfishy.command.RainArgumentType;
import ca.lukegrahamlandry.smellsfishy.data.EntityRainLoader;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.commands.synchronization.brigadier.StringArgumentSerializer;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ModMain.MOD_ID)
public class ModMain {
    public static final String MOD_ID = "smellsfishy";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static EntityRainLoader ENTITY_RAIN_LOADER = null;

    public ModMain() {
        ArgumentTypes.register("tribe", RainArgumentType.class, new EmptyArgumentSerializer<>(RainArgumentType::new));
    }
}

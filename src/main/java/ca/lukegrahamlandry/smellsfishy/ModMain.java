package ca.lukegrahamlandry.smellsfishy;

import ca.lukegrahamlandry.smellsfishy.command.RainArgumentType;
import ca.lukegrahamlandry.smellsfishy.data.EntityRainLoader;
import net.minecraft.command.arguments.ArgumentSerializer;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ModMain.MOD_ID)
public class ModMain {
    public static final String MOD_ID = "smellsfishy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static EntityRainLoader ENTITY_RAIN_LOADER = null;

    public ModMain() {
        ArgumentTypes.register("entityrainevent", RainArgumentType.class, new ArgumentSerializer<>(RainArgumentType::new));
    }
}

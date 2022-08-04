package ca.lukegrahamlandry.smellsfishy.init;

import ca.lukegrahamlandry.smellsfishy.ModMain;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class NetworkInit {
    public static SimpleChannel INSTANCE;
    private static int ID = 0;

    public static int nextID() {
        return ID++;
    }

    public static void registerPackets() {
        INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(ModMain.MOD_ID, "packets"), () -> "1.0", s -> true, s -> true);

    }
}
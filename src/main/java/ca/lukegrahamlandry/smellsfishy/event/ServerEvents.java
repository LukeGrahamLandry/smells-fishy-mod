package ca.lukegrahamlandry.smellsfishy.event;


import ca.lukegrahamlandry.smellsfishy.ModMain;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event){
        if (event.player.level.isClientSide() || event.phase == TickEvent.Phase.END) return;


    }

}

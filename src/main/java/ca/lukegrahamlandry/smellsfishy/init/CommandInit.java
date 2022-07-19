package ca.lukegrahamlandry.smellsfishy.init;

import ca.lukegrahamlandry.smellsfishy.ModMain;
import ca.lukegrahamlandry.smellsfishy.command.RainEventCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandInit {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event){
        RainEventCommand.register(event.getDispatcher());
    }
}
package ca.lukegrahamlandry.smellsfishy.command;

import ca.lukegrahamlandry.smellsfishy.event.RainHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

public class RainEventCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(register());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("entityrain").requires((ctx) -> {
            return ctx.hasPermission(2);
        }).then(
                Commands.literal("start")
                        .requires(cs->cs.hasPermission(0)) //permission
                        .then(Commands.argument("event", new RainArgumentType())
                               .executes(RainEventCommand::handleStart))
                        ).then(Commands.literal("stop").executes(RainEventCommand::handleStop));

    }

    public static int handleStart(CommandContext<CommandSourceStack> source) throws CommandSyntaxException {
        ResourceLocation type = RainArgumentType.get(source, "event");
        boolean success = RainHandler.startRain(source.getSource().getLevel(), type);
        source.getSource().sendSuccess(new TranslatableComponent("success.smellsfishy." + (success ? "start_rain" : "invlaid_rain")).withStyle(ChatFormatting.AQUA), true);
        return Command.SINGLE_SUCCESS;
    }

    public static int handleStop(CommandContext<CommandSourceStack> source) throws CommandSyntaxException {
        RainHandler.stopRain(source.getSource().getLevel());
        source.getSource().sendSuccess(new TranslatableComponent("success.smellsfishy.stop_rain").withStyle(ChatFormatting.AQUA), true);
        return Command.SINGLE_SUCCESS;
    }
}

package ca.lukegrahamlandry.smellsfishy.command;

import ca.lukegrahamlandry.smellsfishy.event.RainHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class RainEventCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(register());
    }

    public static LiteralArgumentBuilder<CommandSource> register() {
        return Commands.literal("entityrain").requires((ctx) -> {
            return ctx.hasPermission(2);
        }).then(
                Commands.literal("start")
                        .requires(cs->cs.hasPermission(0)) //permission
                        .then(Commands.argument("event", new RainArgumentType())
                               .executes(RainEventCommand::handleStart))
                        ).then(Commands.literal("stop").executes(RainEventCommand::handleStop));

    }

    public static int handleStart(CommandContext<CommandSource> source) throws CommandSyntaxException {
        ResourceLocation type = RainArgumentType.get(source, "event");
        boolean success = RainHandler.startRain(source.getSource().getLevel(), type);
        source.getSource().sendSuccess(new TranslationTextComponent("success.smellsfishy." + (success ? "start_rain" : "invlaid_rain")).withStyle(TextFormatting.AQUA), true);
        return Command.SINGLE_SUCCESS;
    }

    public static int handleStop(CommandContext<CommandSource> source) throws CommandSyntaxException {
        RainHandler.stopRain(source.getSource().getLevel());
        source.getSource().sendSuccess(new TranslationTextComponent("success.smellsfishy.stop_rain").withStyle(TextFormatting.AQUA), true);
        return Command.SINGLE_SUCCESS;
    }
}

package ca.lukegrahamlandry.smellsfishy.command;

import ca.lukegrahamlandry.smellsfishy.ModMain;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class RainEventCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(register());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("rainevent").then(
                Commands.literal("start")
                        .requires(cs->cs.hasPermission(0)) //permission
                        .then(Commands.argument("event", new RainArgumentType())
                               .executes(RainEventCommand::handleStart))
                        ).then(Commands.literal("stop").executes(RainEventCommand::HandleStop));

    }

    public static int handleStart(CommandContext<CommandSourceStack> source) throws CommandSyntaxException {
        Player player = source.getSource().getPlayerOrException();
        ResourceLocation type = RainArgumentType.get(source, "event");

        return Command.SINGLE_SUCCESS;
    }

    public static int HandleStop(CommandContext<CommandSourceStack> source) throws CommandSyntaxException {


        return Command.SINGLE_SUCCESS;
    }
}

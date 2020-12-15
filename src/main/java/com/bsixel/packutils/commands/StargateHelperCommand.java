package com.bsixel.packutils.commands;

import com.bsixel.packutils.data.stargates.StargateData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.Map;

public class StargateHelperCommand extends CommandBase {

    private static final String name = "sghelper";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return name + "<subcommand>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) return;
        String subCommand = args[0];
        if (subCommand.equals("list")) {
            Map<String, String> addressNameMap = StargateData.getGateNameMap(server.getEntityWorld());
            TextComponentString text = new TextComponentString("Known Stargates:");
            for (String address : addressNameMap.keySet()) {
                 text.appendSibling(new TextComponentString(addressNameMap.get(address) + ": " + address));
            }
            sender.sendMessage(text);
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }
}

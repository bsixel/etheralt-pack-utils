package com.bsixel.packutils.commands;

import com.bsixel.packutils.EtheraltPackUtils;
import com.bsixel.packutils.data.stargates.StargateData;
import com.bsixel.packutils.utilities.BasicTeleporter;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import gcewing.sg.block.SGBaseBlock;
import gcewing.sg.block.SGBlock;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.util.GeneralAddressRegistry;
import gcewing.sg.util.SGAddressing;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class StargateHelperCommand extends CommandBase {

    private static final String name = "sghelper";
    private static final List<String> subCommands = Arrays.asList("check", "generateGateData", "list", "teleport");

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return name + " <subcommand>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            sender.sendMessage(new TextComponentString("Usage: /" + getUsage(sender)).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }
        String subCommand = args[0];
        switch (subCommand) {
            case "check":
                infoAddress(server, sender, args);
                break;
            case "generateGateData":
                generateMissingGateData(server, sender);
                break;
            case "list":
                listGates(server, sender);
                break;
            case "teleport":
                teleportToAddress(server, sender, args);
                break;
            default:
                sender.sendMessage(new TextComponentString("Usage: /" + getUsage(sender)).setStyle(new Style().setColor(TextFormatting.RED)));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> list = new ArrayList<>();
        if (args.length == 0 || args[0].equals("")) {
            return subCommands;
        } else {
            String subCommand = args[0];
            if (!subCommands.contains(subCommand)) {
                List<String> includes = subCommands.stream().filter(str -> str.startsWith(subCommand)).collect(Collectors.toList());
                return includes.size() == 0 ? subCommands : includes;
            } else {
                if (subCommand.equals("teleport")) {
                    Set<String> allAddresses = StargateData.getAllAddresses(server.getEntityWorld());
                    if (args.length < 2) { // They haven't even started typing a target address yet
                        return new ArrayList<>(allAddresses);
                    } else {
                        return allAddresses.stream().filter(addr -> addr.startsWith(args[1])).collect(Collectors.toList());
                    }
                }
            }

        }
        return list;
    }

    private void generateMissingGateData(MinecraftServer server, ICommandSender sender) {
        ConfigurationNode root = null;
        try {
            root = GeneralAddressRegistry.createRootNode(Paths.get(".", "config", "SGCraft", "general.yml"));
        } catch (IOException e) {
            EtheraltPackUtils.logger.error("Error accessing config directory to regenerate gate data!");
            e.printStackTrace();
            return;
        }
        checkNotNull(root);
        World currentWorld = server.getEntityWorld();
        final ConfigurationNode worldsRoot = root.getNode("worlds");
        try {
            List<String> addresses = worldsRoot.getChildrenMap().get(currentWorld.getWorldInfo().getWorldName().toLowerCase()).getList(TypeToken.of(String.class));
            Set<String> alreadyKnown = StargateData.getAllAddresses(currentWorld);
            int added = 0;
            for (String address : addresses) {
                if (alreadyKnown.contains(address)) {
                    break;
                } else {
                    EtheraltPackUtils.logger.info("Adding missing Stargate: " + address);
                    SGBaseTE supposedStargate = SGAddressing.findAddressedStargate(address, server.getWorld(SGAddressing.dimensionSymbolsOf(address)), false);
                    if (supposedStargate != null) {
                        StargateData.setAddressData(supposedStargate.getWorld(), supposedStargate.getPos(), address);
                        added++;
                    }
                }
            }
            sender.sendMessage(new TextComponentString("Successfully added " + added + " new gates. All gates will be considered world-generated and thus show up in lootable address books.").setStyle(new Style().setColor(TextFormatting.GREEN)));
        } catch (ObjectMappingException | SGAddressing.AddressingError e) {
            if (e instanceof  ObjectMappingException) {
                EtheraltPackUtils.logger.error("Error! Could not find stargate world data for this world.");
            } else {
                EtheraltPackUtils.logger.error("Error! Could not parse address!");
            }
            e.printStackTrace();
        }
    }

    private void listGates(MinecraftServer server, ICommandSender sender) {
        Map<String, String> addressNameMap = StargateData.getDetailedGateNameMap(server.getEntityWorld());
        sender.sendMessage(new TextComponentString("Known Stargates:").setStyle(new Style().setColor(TextFormatting.YELLOW)));
        for (String address : addressNameMap.keySet()) {
            ITextComponent msg = new TextComponentString(addressNameMap.get(address) + ": ").appendSibling(
                new TextComponentString("[" + address + "]").setStyle(new Style()
                        .setColor(TextFormatting.GREEN)
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Click to teleport")))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sghelper teleport " + address))));
            sender.sendMessage(msg);
        }
    }

    private void infoAddress(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString("Usage: /sghelper check <address>").setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }
        NBTTagCompound addressData = StargateData.getDataForAddress(server.getEntityWorld(), args[1]);
        if (addressData == null || addressData.isEmpty()) {
            sender.sendMessage(new TextComponentString("Error! No known gate with address " + args[1] + " to get coordinates for!").setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }
        double x = addressData.getDouble("xPos");
        double y = addressData.getDouble("yPos");
        double z = addressData.getDouble("zPos");
        int dimid;
        if (addressData.hasKey("dimid")) {
            dimid = addressData.getInteger("dimid");
        } else {
            dimid = 0; // Default to overworld if for some reason there isn't a dimid
        }
        TileEntity te = server.getWorld(dimid).getTileEntity(new BlockPos(x, y, z));
        boolean hasIris = false;
        if (te instanceof SGBaseTE) { // Also implicitly covers null check
            hasIris = ((SGBaseTE) te).irisIsClosed();
        }
        sender.sendMessage(new TextComponentString("" + x + " " + y + " " + z + " " + dimid + " " + hasIris).setStyle(new Style().setColor(TextFormatting.YELLOW)));
    }

    private void teleportToAddress(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString("Usage: /sghelper teleport <address>").setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }
        try {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            NBTTagCompound addressData = StargateData.getDataForAddress(server.getEntityWorld(), args[1]);
            if (addressData == null || addressData.isEmpty()) {
                sender.sendMessage(new TextComponentString("Error! No known gate with address " + args[1] + " to teleport to!").setStyle(new Style().setColor(TextFormatting.RED)));
                return;
            }
            double x = addressData.getDouble("xPos");
            double y = addressData.getDouble("yPos");
            double z = addressData.getDouble("zPos");
            int dimid;
            if (addressData.hasKey("dimid")) {
                dimid = addressData.getInteger("dimid");
            } else {
                dimid = 0; // Default to overworld if for some reason there isn't a dimid
            }
            player.changeDimension(dimid, new BasicTeleporter(sender.getEntityWorld(), x, y+1, z));
            sender.sendMessage(new TextComponentString("Successfully teleported player to " + args[1] + "!").setStyle(new Style().setColor(TextFormatting.GREEN)));
        } catch (PlayerNotFoundException e) {
            sender.sendMessage(new TextComponentString("Error! Only a player can use this command!"));
            EtheraltPackUtils.logger.error("Error! Command sender is not a player and thus cannot be teleported!");
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
}

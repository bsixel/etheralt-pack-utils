package com.bsixel.packutils.commands;

import com.bsixel.packutils.EtheraltPackUtils;
import com.bsixel.packutils.utilities.BasicTeleporter;
import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("NullableProblems")
public class UtilityCommand extends CommandBase {

    // For some reason the flying and noClip toggle are forced every tick... keep a list of who's got which here
    private static final Map<EntityPlayer, Boolean> flyingPlayerMap = new HashMap<>();
    private static final Map<EntityPlayer, Boolean> noClipPlayerMap = new HashMap<>();
    private static final String name = "utils";
    private static final List<String> subCommands = Arrays.asList("dispel", "feed", "fly", "noClip", "spawn");

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUsage(ICommandSender iCommandSender) {
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
            case "dispel":
                dispelEffects(server, sender, args);
                break;
            case "feed":
                feedPlayer(sender);
                break;
            case "fly":
                toggleFlightForPlayer(sender);
                break;
            case "noClip":
                toggleNoClip(sender);
                break;
            case "spawn":
                teleportToSpawn(sender);
                break;
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> list = new ArrayList<>();
        EtheraltPackUtils.logger.info("Trying command completion for Utils... " + args.length);
        if (args.length == 0 || args[0].equals("")) {
            return subCommands;
        } else { // This will likely be a mess, there's a lot going on... I'm sure there's a better way but I'm tired. TODO: Improve
            String subCommand = args[0];
            if (!subCommands.contains(subCommand)) {
                List<String> includes = subCommands.stream().filter(str -> str.startsWith(subCommand)).collect(Collectors.toList());
                return includes.size() == 0 ? subCommands : includes;
            } else {
                if (subCommand.equals("dispel")) {
                    if (args.length < 2) {
                        list.add("<entitySelector>");
                        list.addAll(Arrays.asList(server.getOnlinePlayerNames()));
                    } else if (args.length == 2) {
                        String selector = args[1];
                        List<String> includes = Arrays.asList(server.getOnlinePlayerNames());
                        if (includes.contains(selector)) {
                            return list;
                        } else {
                            return includes.stream().filter(str -> str.startsWith(selector)).collect(Collectors.toList());
                        }

                    }
                }
            }
        }
        return list;
    }

    public void dispelEffects(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        List<EntityLivingBase> targets = new ArrayList<>();
        if (args.length < 2) {
            try {
                targets.add(getCommandSenderAsPlayer(sender));
            } catch (PlayerNotFoundException ex) {
                sender.sendMessage(new TextComponentString("Error! You must provide a valid target to dispel!"));
                EtheraltPackUtils.logger.error("Error! No valid target provided to dispel!");
            }
        } else if (EntitySelector.isSelector(args[1])) {
            targets = EntitySelector.matchEntities(sender, args[1], EntityLivingBase.class);
        } else if (Arrays.asList(server.getOnlinePlayerNames()).contains(args[1])) {
            targets.add(server.getPlayerList().getPlayerByUsername(args[1]));
        } else {
            sender.sendMessage(new TextComponentString("Error! You must provide a valid target to dispel!"));
            EtheraltPackUtils.logger.error("Error! No valid target provided to dispel!");
            return;
        }
        if (args.length >= 2) {
            Potion potion = Potion.getPotionFromResourceLocation(args[2]);
            if (potion == null) {
                sender.sendMessage(new TextComponentString("Error! You must provide a valid potion to dispel!"));
                EtheraltPackUtils.logger.error("Error! No valid potion resource supplied!");
            } else {
                targets.forEach(target -> target.removePotionEffect(potion));
            }
        } else {
            targets.forEach(target -> target.getActivePotionEffects().forEach(effect -> target.removePotionEffect(effect.getPotion())));
        }
    }

    private void feedPlayer(ICommandSender sender) {
        try {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            player.getFoodStats().setFoodLevel(20);
            player.getFoodStats().setFoodSaturationLevel(20);
            sender.sendMessage(new TextComponentString("Successfully fed player!").setStyle(new Style().setColor(TextFormatting.GREEN)));
        } catch (PlayerNotFoundException ex) {
            sender.sendMessage(new TextComponentString("Error! Only a player can use this command!").setStyle(new Style().setColor(TextFormatting.RED)));
            EtheraltPackUtils.logger.error("Error! Command sender is not a player and thus cannot be fed!");
        }
    }

    private void teleportToSpawn(ICommandSender sender) {
        try {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            BlockPos spawnPos = player.getBedLocation();
            if (spawnPos == null) {
                spawnPos = player.world.getSpawnPoint();
            }
            double spawnHeight = Math.max(spawnPos.getY(), sender.getEntityWorld().getHeight(spawnPos).getY());
            player.changeDimension(player.getSpawnDimension(), new BasicTeleporter(player.world, spawnPos.getX(), spawnHeight, spawnPos.getZ()));
            sender.sendMessage(new TextComponentString("Successfully teleported player to spawn!").setStyle(new Style().setColor(TextFormatting.GREEN)));
        } catch (PlayerNotFoundException ex) {
            sender.sendMessage(new TextComponentString("Error! Only a player can use this command!").setStyle(new Style().setColor(TextFormatting.RED)));
            EtheraltPackUtils.logger.error("Error! Command sender is not a player and thus cannot be teleported!");
        }
    }

    private void toggleFlightForPlayer(ICommandSender sender) {
        try {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            if (flyingPlayerMap.containsKey(player)) {
                flyingPlayerMap.remove(player);
                player.capabilities.isFlying = false;
                player.capabilities.allowFlying = false;
                sender.sendMessage(new TextComponentString("Successfully set to fly to false").setStyle(new Style().setColor(TextFormatting.GREEN)));
            } else {
                flyingPlayerMap.put(player, true);
                player.capabilities.allowFlying = true;
                sender.sendMessage(new TextComponentString("Successfully set to fly to true").setStyle(new Style().setColor(TextFormatting.GREEN)));
            }
            player.sendPlayerAbilities();
        } catch (PlayerNotFoundException ex) {
            sender.sendMessage(new TextComponentString("Error! Only a player can use this command!"));
            EtheraltPackUtils.logger.error("Error! Command sender is not a player and thus cannot have flight!");
        }
    }

    private void toggleNoClip(ICommandSender sender) {
        try {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            if (player.isSpectator()) { // Don't do anything for players in spectator mode
                sender.sendMessage(new TextComponentString("Error! You can't toggle noClip in spectator mode!"));
                EtheraltPackUtils.logger.error("Error! Can't toggle noClip for a spectator!");
            } else {
                if (noClipPlayerMap.containsKey(player)) {
                    noClipPlayerMap.remove(player);
                    player.noClip = false;
                    player.capabilities.isFlying = false;
                    player.capabilities.allowFlying = false;
                    sender.sendMessage(new TextComponentString("Successfully set to noClip to false").setStyle(new Style().setColor(TextFormatting.GREEN)));
                } else {
                    noClipPlayerMap.put(player, true);
                    player.noClip = false;
                    player.capabilities.allowFlying = true;
                    sender.sendMessage(new TextComponentString("Successfully set to noClip to true").setStyle(new Style().setColor(TextFormatting.GREEN)));
                }
            }
            player.sendPlayerAbilities();
        } catch (PlayerNotFoundException ex) {
            sender.sendMessage(new TextComponentString("Error! Only a player can use this command!"));
            EtheraltPackUtils.logger.error("Error! Command sender is not a player and thus cannot have flight!");
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    // Silliness requires us to update on every event to keep the player flying or noclipped
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return; // Only matters on players
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (noClipPlayerMap.containsKey(player) && noClipPlayerMap.get(player)) {
            player.noClip = true;
            player.capabilities.allowFlying = true;
        } else {
            player.noClip = false;
            if (flyingPlayerMap.containsKey(player) && flyingPlayerMap.get(player)) {
                player.capabilities.allowFlying = true;
            } else {
                player.capabilities.isFlying = false;
                player.capabilities.allowFlying = false;
            }
        }
        player.sendPlayerAbilities();
    }

}

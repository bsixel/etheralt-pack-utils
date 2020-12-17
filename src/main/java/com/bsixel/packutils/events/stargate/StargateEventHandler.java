package com.bsixel.packutils.events.stargate;

import com.bsixel.packutils.EtheraltPackUtils;
import com.bsixel.packutils.data.stargates.StargateData;
import gcewing.sg.event.SGMergeEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class StargateEventHandler {

    @SubscribeEvent
    public void handleStargateCreation(SGMergeEvent event) {
        if (!event.isMerged()) { // It's a gate-removal event, for now we don't care about those
            StargateData.removeAddressData(event);
        } else {
            NBTTagCompound stargateEventData = StargateData.setAddressData(event);
            String infoString = "New stargate with address " + event.getAddress() + " , at position " + event.getGatePosition().toString() + " named " + stargateEventData.getString("locationName");
            EtheraltPackUtils.logger.info(infoString);
        }
    }

}

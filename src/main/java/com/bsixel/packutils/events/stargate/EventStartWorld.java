package com.bsixel.packutils.events.stargate;

import com.bsixel.packutils.data.stargates.StargateData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EventStartWorld {

    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void onWorldStarSetup(WorldEvent.Load event) {
        // For future reference:
        // event.getWorld().isRemote - check for true, if true, it's clientside

        StargateData.getData(event.getWorld()); // This will populate the liveAddress map for later use
    }

}

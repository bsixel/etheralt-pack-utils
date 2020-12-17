package com.bsixel.packutils.events.geneticscompat;

import com.theundertaker11.geneticsreborn.api.capability.genes.EnumGenes;
import com.theundertaker11.geneticsreborn.api.capability.genes.IGenes;
import com.theundertaker11.geneticsreborn.util.ModUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import slimeknights.tconstruct.library.events.TinkerToolEvent;
import slimeknights.tconstruct.library.tools.ranged.IAmmo;
import slimeknights.tconstruct.library.utils.ToolHelper;

public class EventFireSpecialArrowHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void fireTconstructBow(TinkerToolEvent.OnBowShoot event) {
        ItemStack usedItemStack = event.itemStack;

        // Check if player has infinity gene
        IGenes playerGenes = ModUtils.getIGenes(event.entityPlayer);
        if (playerGenes == null || !playerGenes.hasGene(EnumGenes.INFINITY)) {
            return;
        }

        // At this point we've got the gene and know we've shot a tinkers bow
        ToolHelper.repairTool(usedItemStack, 1); // Repair the bow
        ItemStack ammo = event.ammo;
        if(ammo != null && ammo.getItem() instanceof IAmmo) { // Re-add ammo and repair arrow
            for(int i = 0; i < event.projectileCount; i++) {
                ((IAmmo) ammo.getItem()).addAmmo(ammo, event.entityPlayer);
                ToolHelper.repairTool(ammo, 1);
            }
        }

    }

}

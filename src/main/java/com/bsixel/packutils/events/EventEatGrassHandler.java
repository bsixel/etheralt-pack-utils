package com.bsixel.packutils.events;

import ca.wescook.nutrition.capabilities.INutrientManager;
import ca.wescook.nutrition.nutrients.Nutrient;
import ca.wescook.nutrition.proxy.ClientProxy;
import ca.wescook.nutrition.utility.Config;
import com.bsixel.packutils.EtheraltPackUtils;
import com.theundertaker11.geneticsreborn.api.capability.genes.EnumGenes;
import com.theundertaker11.geneticsreborn.api.capability.genes.IGenes;
import com.theundertaker11.geneticsreborn.util.ModUtils;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventEatGrassHandler {
    @CapabilityInject(INutrientManager.class)
    private static final Capability<INutrientManager> NUTRITION_CAPABILITY = null;

    /*
    Add compat between WesCook's Nutrition and Genetics Reborn
    See https://github.com/WesCook/Nutrition/
     */
    @SubscribeEvent
    public void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        IBlockState blockInfo = world.getBlockState(event.getPos());
        EntityPlayer player = event.getEntityPlayer();

        // Only continue if it's grass we just right clicked and that gene is even allowed
        if (!(blockInfo.getBlock() instanceof BlockGrass) || !EnumGenes.EAT_GRASS.isActive()) {
            return;
        }
        // Check if player has grass eating gene
        IGenes playerGenes = ModUtils.getIGenes(player);
        if (playerGenes == null || !playerGenes.hasGene(EnumGenes.EAT_GRASS)) {
            return;
        }

        if (player.canEat(false) || Config.allowOverEating) {
            Map<Nutrient, Float> nutrientMap = NUTRITION_CAPABILITY.getDefaultInstance().get();
            List<Nutrient> nutrients = new ArrayList<Nutrient>(nutrientMap.keySet());
            for (Nutrient nut : nutrients) {
                float nutAmount = EtheraltPackUtils.valueForNutrient(nut.name);
                if (!player.getEntityWorld().isRemote) { // Server
                    player.getCapability(NUTRITION_CAPABILITY, null).add(nut, nutAmount);
                } else { // Client
                    ClientProxy.localNutrition.add(nutrients, nutAmount);
                }
            }
        }

    }

}

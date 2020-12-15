package com.bsixel.packutils.modhelpers;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import astrotibs.villagenames.village.*;

public class SafeVillageNamesLoader {
    // TODO: Add individual coord variant
    public static String villageNameForPosition(World world, int x, int y, int z) {
        return villageNameForPosition(world, new BlockPos(x, y, z));
    }

    public static String villageNameForPosition(World world, BlockPos pos) {
        NBTTagCompound villageNameNBT;
        try {
            villageNameNBT = StructureVillageVN.getOrMakeVNInfo(world, pos);
        } catch (Exception e) { // In case we somehow got in here without valid VillageNames data
            villageNameNBT = null;
        }
        if (villageNameNBT != null) {
            return villageNameNBT.getString("namePrefix") + villageNameNBT.getString("nameRoot") + villageNameNBT.getString("nameSuffix");
        } else {
            return "Unnamed Location";
        }
    }
}

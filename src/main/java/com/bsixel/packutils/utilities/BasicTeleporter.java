package com.bsixel.packutils.utilities;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;

public class BasicTeleporter implements ITeleporter {

    private double x, y, z;

    public BasicTeleporter(World world, double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void placeEntity(World world, Entity entity, float v) {
        entity.posX = this.x;
        entity.posY = this.y;
        entity.posZ = this.z;
        entity.rotationYaw = v;
    }

    @Override
    public boolean isVanilla() {
        return false;
    }
}

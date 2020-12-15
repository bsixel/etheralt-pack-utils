package com.bsixel.packutils.data.stargates;

import com.bsixel.packutils.ModInfo;
import com.bsixel.packutils.modhelpers.SafeVillageNamesLoader;
import gcewing.sg.event.SGMergeEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.Loader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StargateData extends WorldSavedData {
    private static final String DATA_NAME = ModInfo.MODID + "StargateData";

    // All the stargates - each stargate has a NBTTagCompound entry under its address
    private NBTTagCompound stargateData = new NBTTagCompound();

    public StargateData() {
        super(DATA_NAME);
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        stargateData = data;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        for (String address : stargateData.getKeySet()) {
            data.setTag(address, stargateData.getCompoundTag(address));
        }
//        data.setTag("locations", stargateLocations);
//        data.setTag("names", stargateNames); // TODO: Add names to stargates if we've loaded the VillageNames mod
        return data;
    }

    /**
     * Static getter for all Stargate location data.
     * @param world
     * @return a StargateData object containing the location data of every known Stargate
     */
    public static StargateData getData(World world) {
        MapStorage storage = world.getMapStorage();
        StargateData data = (StargateData) storage.getOrLoadData(StargateData.class, DATA_NAME);

        if (data == null) {
            data = new StargateData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    /**
     * Static method for removing an address from the server-wide registry
     * @param event A Stargate-Merged event, from when a stargate is converted to or (more likely) from a multiblock struct
     */
    public static void removeAddressData(SGMergeEvent event) {
        removeAddressData(event.getWorld(), event.getAddress());
    }

    /**
     * Static method for removing an address from the server-wide registry
     * @param world The world associated with the gate. Although this can be any dimension on the server, as stargate data is globa
     * @param address The address of the gate to be removed; should be unique across all dimensions
     */
    public static void removeAddressData(World world, String address) {
        StargateData data = getData(world);
        data.stargateData.removeTag(address);

        data.markDirty(); // Always last
    }

    /**
     * Static method for setting the global data of a single Stargate's location for future use
     * @param event A Stargate-merged event, from when a stargate is converted from or (more likely) to a multiblock struct
     * @return A NBTTagCompound containing the data for this specific address.
     * Keys on the return compound:
     * xPos, yPos, zPos: Integers with the position data for the stargate base
     * dimension: String of the dimname of the world the gate (should) be in. Note this may be wrong if someone did something odd with the initial set.
     * locationName: String of the name of the location this gate was spawned - for now, unless you have the Village Names mod this will just be "Unnamed Location"
     */
    public static NBTTagCompound setAddressData(SGMergeEvent event) {
        return setAddressData(event.getWorld(), event.getGatePosition(), event.getWorldName(), event.getAddress());
    }

    /**
     * Static method for setting the global data of a single Stargate's location for future use
     * @param world The world the Stargate is in
     * @param pos The position of the Stargate base
     * @param dimName The name of the dimension the Stargate is in. I guess this is useful if the dimname from world.getName isn't what you want.
     * @param address The address of the gate itself
     *  @return A NBTTagCompound containing the data for this specific address.
     * Keys on the return compound:
     * xPos, yPos, zPos: Integers with the position data for the stargate base
     * dimension: String of the dimname of the world the gate (should) be in. Note this may be wrong if someone did something odd with the initial set.
     * locationName: String of the name of the location this gate was spawned - for now, unless you have the Village Names mod this will just be "Unnamed Location"
     */
    public static NBTTagCompound setAddressData(World world, BlockPos pos, String dimName, String address) {
        StargateData worldData = getData(world);
        NBTTagCompound eventData = new NBTTagCompound();
        eventData.setInteger("xPos", pos.getX());
        eventData.setInteger("yPos", pos.getY());
        eventData.setInteger("zPos", pos.getZ());
        eventData.setString("dimension", dimName);
        String structName;
        if (Loader.isModLoaded("villagenames")) { // If we've got Village Names installed, use the name of the structure the Stargate spawned at if it exists
            structName = SafeVillageNamesLoader.villageNameForPosition(world, pos);
        } else {
            structName = "Unnamed Location";
        }
        eventData.setString("locationName", structName);

        worldData.stargateData.setTag(address, eventData);

        worldData.markDirty(); // Always last, except return
        return eventData;
    }

    /**
     * Static getter for retrieving the data of a single Stargate.
     * @param world The world the Stargate is in. Although technically any valid World object will do the trick.
     * @param address The address of the gate you're trying to find.
     * @return A NBTTagCompound containing the data for this specific address.
     * Keys on the return compound:
     * xPos, yPos, zPos: Integers with the position data for the stargate base
     * dimension: String of the dimname of the world the gate (should) be in. Note this may be wrong if someone did something odd with the initial set.
     * locationName: String of the name of the location this gate was spawned - for now, unless you have the Village Names mod this will just be "Unnamed Location"
     */
    public static NBTTagCompound getDataForAddress(World world, String address) {
        StargateData worldData = getData(world);
        return worldData.stargateData;
    }

    /**
     * Instanced getter for retrieving the data of a single Stargate.
     * @param address The address of the gate you're trying to find.
     * @return A NBTTagCompound containing the data for this specific address.
     * Keys on the return compound:
     * xPos, yPos, zPos: Integers with the position data for the stargate base
     * dimension: String of the dimname of the world the gate (should) be in. Note this may be wrong if someone did something odd with the initial set.
     * locationName: String of the name of the location this gate was spawned - for now, unless you have the Village Names mod this will just be "Unnamed Location"
     */
    public NBTTagCompound getDataForAddress(String address) {
        return this.stargateData.getCompoundTag(address);
    }

    /**
     * Static getter for a set of all unique addresses known in the global datastore.
     * @return a set of all unique known Stargate addresses
     */
    public static Set<String> getAllAddresses(World world) {
        return getData(world).stargateData.getKeySet();
    }

    public static Map<String, String> getGateNameMap(World world) {
        NBTTagCompound data = getData(world).stargateData;
        Map<String, String> map = new HashMap<>();
        for (String address : data.getKeySet()) {
            map.put(address, data.getCompoundTag(address).getString("locationName"));
        }
        return map;
    }

}

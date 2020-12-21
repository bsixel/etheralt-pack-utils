package com.bsixel.packutils.data.stargates;

import com.bsixel.packutils.EtheraltPackUtils;
import com.bsixel.packutils.ModInfo;
import com.bsixel.packutils.modhelpers.SafeVillageNamesLoader;
import gcewing.sg.event.SGMergeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.Loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StargateData extends WorldSavedData {
    private static final String DATA_NAME = ModInfo.MODID + "StargateData";
    private static Map<String, String> liveAddressMap = new HashMap<>();

    // All the stargates - each stargate has a NBTTagCompound entry under its address
    private NBTTagCompound stargateData = new NBTTagCompound();

    public StargateData(String tagName) {
        super(tagName);
        markDirty();
    }

    public StargateData() {
        super(DATA_NAME);
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        stargateData = data;
        EtheraltPackUtils.logger.debug("Loading Stargate data from NBT");
        liveAddressMap = new HashMap<>();
        for (String address : stargateData.getKeySet()) {
            liveAddressMap.put(address, stargateData.getCompoundTag(address).getString("locationName"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        for (String address : stargateData.getKeySet()) {
            data.setTag(address, stargateData.getCompoundTag(address));
        }
        markDirty();
        return data;
    }

    public static Set<String> getLiveAddresses() {
        return liveAddressMap.keySet();
    }

    public static String getLiveNameForAddress(String addr) {
        return liveAddressMap.getOrDefault(addr, "Unknown Structure");
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
        liveAddressMap.remove(address);

        data.markDirty(); // Always last
    }

    /**
     * Static method for setting the global data of a single Stargate's location for future use
     * @param event A Stargate-merged event, from when a stargate is converted from or (more likely) to a multiblock struct
     * @return A NBTTagCompound containing the data for this specific address.
     * Keys on the return compound:
     * xPos, yPos, zPos: Integers with the position data for the stargate base
     * dimension: String of the dimname of the world the gate (should) be in. Note this may be wrong if someone did something odd with the initial set.
     * locationName: String of the name of the location this gate was spawned - for now, unless you have the Village Names mod this will just be "Unknown Location"
     */
    public static NBTTagCompound setAddressData(SGMergeEvent event) {
        return setAddressData(event.getWorld(), event.getGatePosition(), event.getAddress());
    }

    /**
     * Static method for setting the global data of a single Stargate's location for future use
     * @param world The world the Stargate is in
     * @param pos The position of the Stargate base
     * @param address The address of the gate itself
     *  @return A NBTTagCompound containing the data for this specific address.
     * Keys on the return compound:
     * xPos, yPos, zPos: Integers with the position data for the stargate base
     * dimension: String of the dimname of the world the gate (should) be in. Note this may be wrong if someone did something odd with the initial set.
     * locationName: String of the name of the location this gate was spawned - for now, unless you have the Village Names mod this will just be "Unknown Location"
     */
    public static NBTTagCompound setAddressData(World world, BlockPos pos, String address) {
        StargateData worldData = getData(world);
        NBTTagCompound eventData = new NBTTagCompound();
        eventData.setInteger("xPos", pos.getX());
        eventData.setInteger("yPos", pos.getY());
        eventData.setInteger("zPos", pos.getZ());
        eventData.setString("dimension", world.provider.getDimensionType().name());
        eventData.setInteger("dimid", world.provider.getDimension());
        // Check if a player is close enough to have placed the final piece - otherwise it was worldgen that made the gate
        EntityPlayer closestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 15.0d, false);
        // If the closestPlayer is non-null then a player made the gate
        // Useful data in case we want to only give access to worldgen-made gates
        eventData.setBoolean("isPlayerMade", closestPlayer != null);
        String structName;
        if (Loader.isModLoaded("villagenames")) { // If we've got Village Names installed, use the name of the structure the Stargate spawned at if it exists
            structName = SafeVillageNamesLoader.villageNameForPosition(world, pos);
        } else {
            structName = "Unknown Location";
        }
        eventData.setString("locationName", structName);
        worldData.stargateData.setTag(address, eventData);
        liveAddressMap.put(address, structName);

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
     * locationName: String of the name of the location this gate was spawned - for now, unless you have the Village Names mod this will just be "Unknown Location"
     */
    public static NBTTagCompound getDataForAddress(World world, String address) {
        StargateData worldData = getData(world);
        return worldData.stargateData.getCompoundTag(address);
    }

    /**
     * Instanced getter for retrieving the data of a single Stargate.
     * @param address The address of the gate you're trying to find.
     * @return A NBTTagCompound containing the data for this specific address.
     * Keys on the return compound:
     * xPos, yPos, zPos: Integers with the position data for the stargate base
     * dimension: String of the dimname of the world the gate (should) be in. Note this may be wrong if someone did something odd with the initial set.
     * locationName: String of the name of the location this gate was spawned - for now, unless you have the Village Names mod this will just be "Unknown Location"
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

    /**
     * Static getter for a map of all addresses and whether they were made by a player
     * @param world World from which to get addresses
     * @return A Map <addr,isPlayerMade>
     */
    public static Map<String, Boolean> getGateMergetypeMap(World world) {
        NBTTagCompound data = getData(world).stargateData;
        Map<String, Boolean> map = new HashMap<>();
        for (String addr : data.getKeySet()) {
            map.put(addr, data.getCompoundTag(addr).getBoolean("isPlayerMade"));
        }
        return map;
    }

    /**
     * Static getter for a set of all gate addresses and their dimensions
     * @param world
     * @return
     */
    public static Map<String, String> getGateDimMap(World world) {
        NBTTagCompound data = getData(world).stargateData;
        Map<String, String> map = new HashMap<>();
        for (String address : data.getKeySet()) {
            map.put(address, data.getCompoundTag(address).getString("dimension"));
        }
        return map;
    }

    /**
     * Static getter for a map of all addresses -> location names - name will be empty if the Village Names mod is not installed or it's player made.
     * @param world The world to use to extract addresses from - grabs from the whole global world instance not just "nether" "overworld"
     * @return A map of all <address, locationName> pairs
     */
    public static Map<String, String> getGateNameMap(World world) {
        NBTTagCompound data = getData(world).stargateData;
        Map<String, String> map = new HashMap<>();
        for (String address : data.getKeySet()) {
            map.put(address, data.getCompoundTag(address).getString("locationName"));
        }
        return map;
    }

    /**
     * Same as getGateNameMap except that the name also includes the dimension the location is in
     * @param world The world to use to extract addresses from - grabs from the whole global world instance not just "nether" "overworld"
     * @return A map of all <address, locationName - [Dimension]> pairs
     */
    public static Map<String, String> getDetailedGateNameMap(World world) {
        NBTTagCompound data = getData(world).stargateData;
        Map<String, String> map = new HashMap<>();
        for (String address : data.getKeySet()) {
            String descr = data.getCompoundTag(address).getString("locationName") + " (" + data.getCompoundTag(address).getString("dimension") + ")";
            map.put(address, descr);
        }
        return map;
    }

}

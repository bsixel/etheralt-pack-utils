package com.bsixel.packutils.events.loot;

import com.bsixel.packutils.EtheraltPackUtils;
import com.bsixel.packutils.ModInfo;
import com.bsixel.packutils.data.stargates.StargateData;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;
import java.util.stream.Collectors;

public class SGLootHandler {

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        EtheraltPackUtils.logger.debug("Generating loot for tables - hopefully adding a book of addresses!");
        ResourceLocation eventName = event.getName();
        // For now just add the random address book to all tables for testing
        LootPool main = event.getTable().getPool("main");
        if (eventName.getPath().contains("chest") && main != null) {
            EtheraltPackUtils.logger.debug("Actually adding address table loot!");
            World world = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld();
            // Apparently this is the mess required to generate a different book each time... There's gotta be an easier way right?
            main.addEntry(new LootEntryItem(Items.WRITTEN_BOOK, 5, 0, new LootFunction[]{ new LootFunction(new LootCondition[0]) {
                @Override
                public ItemStack apply(ItemStack itemStack, Random random, LootContext lootContext) {
                    return genAddressBookForWorld(world, itemStack);
                }
            }}, new LootCondition[0], ModInfo.MODID + ":addressBook"));
        }
    }

    /**
     * Generates a book with up to 5 random SG addresses in it
     * @param world The global world to take addresses from
     * @return a Book item with addresses written in it
     */
    private static ItemStack genAddressBookForWorld(World world, ItemStack book) {
        EtheraltPackUtils.logger.debug("Made a new address book!");
        book.setTagInfo("author", new NBTTagString("Ancient Scholar"));
        NBTTagList nbtForPages = new NBTTagList();
        Map<String, Boolean> addrPlayermadeMap = StargateData.getGateMergetypeMap(world);
        // Filtered list of all non-playermade addresses
        List<String> addresses = new ArrayList<>(addrPlayermadeMap.keySet()).stream().filter(addr -> !addrPlayermadeMap.get(addr)).collect(Collectors.toList());
        // Randomize the list
        Collections.shuffle(addresses);
        // Limit to at most 5 addresses
        int numAddresses = Math.min(addresses.size(), 5);
        if (numAddresses <= 0) { // If there aren't any known addresses, return nothing
            return null;
        }
        for (int i = 0; i < numAddresses; i++) {
            ITextComponent pageContent = new TextComponentString(addresses.get(i));
            String serialized = ITextComponent.Serializer.componentToJson(pageContent);
            nbtForPages.appendTag(new NBTTagString(serialized));
        }
        book.setTagInfo("pages", nbtForPages);
        book.setTagInfo("title", new NBTTagString("Tablet of the Ancients"));

        return book;
    }

}

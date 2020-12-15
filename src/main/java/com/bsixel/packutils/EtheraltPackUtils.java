package com.bsixel.packutils;

import com.bsixel.packutils.commands.StargateHelperCommand;
import com.bsixel.packutils.events.EventEatGrassHandler;
import com.bsixel.packutils.events.EventFireSpecialArrowHandler;
import com.bsixel.packutils.events.StargateEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Mod(modid = ModInfo.MODID, version = ModInfo.VERSION, name = ModInfo.NAME, acceptedMinecraftVersions = ModInfo.ACCEPTED_MINECRAFT)
public class EtheraltPackUtils {

    @Mod.Instance
    public static EtheraltPackUtils instance;

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Preinitializing Etheralt Pack Utils version " + ModInfo.VERSION);
        Configuration config = new Configuration(new File(event.getModConfigurationDirectory().getAbsolutePath() + File.separator + "etheraltpackutils.cfg"));

        initConfig(config);

        if (Loader.isModLoaded("nutrition") && integrateNutrition) { // Compat between Genetics Reborn and Nutrition
            logger.info("Loading compat between genetics reborn and Nutrition");
            MinecraftForge.EVENT_BUS.register(new EventEatGrassHandler());
        }
        if (Loader.isModLoaded("tconstruct") && integrateTinkers) { // Compat between Genetics Reborn and Tinkers Construct
            logger.info("Loading compat between genetics reborn and Tinker's Construct");
            MinecraftForge.EVENT_BUS.register(new EventFireSpecialArrowHandler());
        }
        if (Loader.isModLoaded("sgcraft") && integrateStargate) { // TODO: Added event handling for Stargate Network
            logger.info("Loading integration for Stargate Network");
            MinecraftForge.EVENT_BUS.register(new StargateEventHandler());
        }
    }

    @EventHandler
    public void starting(FMLServerStartingEvent event) {
        logger.info("Starting Etheralt Pack Utils version " + ModInfo.VERSION);
        if (Loader.isModLoaded("sgcraft") && integrateStargate) {
            logger.info("Registering commands for sgcraft integration!");
            event.registerServerCommand(new StargateHelperCommand());
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Initializing Etheralt Pack Utils version " + ModInfo.VERSION);
    }

    // Configs from file
    public static boolean integrateNutrition;
    public static float defaultNutritionValue;
    public static Map<String, Float> grassNutritionValues = new HashMap<String, Float>();
    public static boolean integrateStargate;
    public static boolean integrateTinkers;

    public static Configuration initConfig(Configuration config) {
        config.load();
        logger.info("Loading configuration");
        String generalSettings = "General Pack Utils Settings";
        String nutritionSettings = "Nutrition Integration Settings";
        String stargateSettings = "Stargate Network Integration Settings";
        String tinkersSettings = "Tinkers Construct Integration Settings";

        // Nutrition settings
        integrateNutrition = config.getBoolean("integrateNutrition", nutritionSettings, true, "Whether the mod should provide integration between nutrition and GeneticsReborn. If true, players with the grass eating gene will gain nutritional value from grass.");
        defaultNutritionValue = config.getFloat("defaultNutritionValue", nutritionSettings, 0, 0, Float.MAX_VALUE, "The default nutritional value of a nutrient if not present in the list below.");
        String[] grassNutritionValuesArr = config.getStringList("grassNutritionValues", nutritionSettings, new String[]{"dairy,0.5", "fruit,0.5", "grain,0.5", "protein,0.5", "vegetable,0.5"},"A list of the nutrients that right-clicking a grass block with the grass-eating gene will give. Each line represent a nutrient and the amount restored, comma separated. Ex: 'dairy,0.5'");
        for (String s : grassNutritionValuesArr) {
            if (s == null || !s.contains(",")) { // Invalid formatting if there's no comma, just skip it
                continue;
            }
            String[] split = s.split(",");
            String nutrient = split[0];
            float nutValue;
            try  {
                nutValue = Float.parseFloat(split[1]);
                logger.info("Setting nutrient " + nutrient + " to " + nutValue);
            } catch (Exception e) { // It'll either be a NullPointer or a NumberFormatException, in which case just set the value to 1
                nutValue = defaultNutritionValue;
                logger.info("Setting nutrient " + nutrient + " to default");
            }
            grassNutritionValues.put(nutrient, nutValue);
        }

        // Stargate Network settings
        integrateStargate = config.getBoolean("integrateStargate", stargateSettings, true, "Whether this mod should provide integration for Stargate Network. WIP");

        // Tinkers Construct settings
        integrateTinkers = config.getBoolean("integrateTinkers", tinkersSettings, true, "Whether the mod should provide integration between Tinkers Construct and GeneticsReborn. If true, players with the infinity gene will have the effect applied to tinkers bows and crossbows, not just vanilla ones.");

        config.save();
        return config;
    }

    public static float valueForNutrient(String nutrient) {
        Float possible = grassNutritionValues.get(nutrient);
        return possible == null ? defaultNutritionValue : possible;
    }

}

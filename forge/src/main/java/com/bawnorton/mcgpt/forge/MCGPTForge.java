package com.bawnorton.mcgpt.forge;

import com.bawnorton.mcgpt.MCGPTClient;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MCGPTClient.MOD_ID)
public class MCGPTForge {
    public MCGPTForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(MCGPTClient.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        MCGPTClient.init();
    }
}

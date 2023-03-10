package com.bawnorton.mcgpt.fabric;

import com.bawnorton.mcgpt.MCGPTClient;
import net.fabricmc.api.ClientModInitializer;

public class MCGPTFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MCGPTClient.init();
    }
}

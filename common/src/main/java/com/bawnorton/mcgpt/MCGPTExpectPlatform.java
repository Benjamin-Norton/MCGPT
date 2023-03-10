package com.bawnorton.mcgpt;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class MCGPTExpectPlatform {

    @ExpectPlatform
    public static void registerCommands() {
        throw new AssertionError();
    }
}

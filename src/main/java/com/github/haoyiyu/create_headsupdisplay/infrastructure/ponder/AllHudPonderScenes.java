package com.github.haoyiyu.create_headsupdisplay.infrastructure.ponder;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.infrastructure.ponder.scenes.DisplayTerminalScenes;
import com.github.haoyiyu.create_headsupdisplay.infrastructure.ponder.scenes.OmniCoreScenes;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class AllHudPonderScenes {

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "display_terminal"))
            .addStoryBoard("display_terminal", DisplayTerminalScenes::render);

        helper.forComponents(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "omni_core"))
            .addStoryBoard("omni_core", OmniCoreScenes::network);
    }
}

package com.github.haoyiyu.create_headsupdisplay.infrastructure.ponder;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class HudPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return CreateHeadsUpDisplay.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        AllHudPonderScenes.register(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
    }
}

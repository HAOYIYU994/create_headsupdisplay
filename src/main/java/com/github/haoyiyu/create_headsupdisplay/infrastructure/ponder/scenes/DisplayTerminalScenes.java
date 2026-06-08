package com.github.haoyiyu.create_headsupdisplay.infrastructure.ponder.scenes;

import com.github.haoyiyu.create_headsupdisplay.registration.ModItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class DisplayTerminalScenes {

    public static void render(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("display_terminal", "图像的渲染");
        scene.configureBasePlate(0, 0, 5);
        scene.addKeyframe();

        // 揭示底座 + 终端(2,1,2)
        Selection terminalAndBase = util.select().fromTo(0, 0, 0, 4, 0, 4)
            .add(util.select().position(2, 1, 2));
        scene.world().showSection(terminalAndBase, Direction.DOWN);
        scene.idleSeconds(1);

        // text_1
        scene.overlay().showText(60)
            .text("text_1")
            .pointAt(util.vector().of(2.5, 2, 2.5))
            .placeNearTarget();
        scene.idle(65);

        // text_2 RED
        scene.overlay().showText(60)
            .text("text_2")
            .colored(PonderPalette.RED)
            .pointAt(util.vector().of(2.5, 2, 2.5))
            .placeNearTarget();
        scene.idle(65);

        scene.addKeyframe();

        // 揭示 4×显示器 [3,1,2],[4,1,2],[3,2,2],[4,2,2]
        Selection displays = util.select().fromTo(3, 1, 2, 4, 2, 2);
        scene.world().showSection(displays, Direction.WEST);
        scene.idleSeconds(1);

        // text_3
        scene.overlay().showText(60)
            .text("text_3")
            .pointAt(util.vector().of(3, 2.5, 2.5))
            .placeNearTarget();
        scene.idle(65);

        // showControls
        scene.overlay().showControls(util.vector().of(2.5, 2, 2.5), Pointing.DOWN, 40)
            .rightClick()
            .withItem(new ItemStack(ModItems.HEAD_MOUNT_DISPLAY.get()))
            .whileSneaking();
        scene.idle(45);

        // text_4
        scene.overlay().showText(60)
            .text("text_4")
            .pointAt(util.vector().of(2.5, 2, 2.5))
            .placeNearTarget();
        scene.idle(65);

        // text_5 OUTPUT
        scene.overlay().showText(80)
            .text("text_5")
            .colored(PonderPalette.OUTPUT)
            .pointAt(util.vector().of(2.5, 2, 2.5))
            .placeNearTarget();
        scene.idle(80);
    }
}

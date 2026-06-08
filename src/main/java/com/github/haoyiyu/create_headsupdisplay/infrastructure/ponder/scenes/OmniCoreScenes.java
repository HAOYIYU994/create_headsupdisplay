package com.github.haoyiyu.create_headsupdisplay.infrastructure.ponder.scenes;

import com.github.haoyiyu.create_headsupdisplay.registration.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class OmniCoreScenes {

    public static void network(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("omni_core", "显示器的基本连接网络");
        scene.configureBasePlate(0, 0, 6);
        scene.addKeyframe();

        // 展示 NBT 结构
        scene.world().showSection(util.select().everywhere(), Direction.DOWN);
        scene.idleSeconds(1);

        BlockPos corePos = util.grid().at(2, 1, 2);
        AABB coreBounds = new AABB(corePos);

        // 介绍文字  [2.5, 1.5, 2]
        scene.overlay().showText(60)
            .text("这是数据集成核心，在显示器网络中作为信息的统一接收器")
            .colored(PonderPalette.WHITE)
            .pointAt(util.vector().of(2.5, 1.5, 2))
            .placeNearTarget();
        scene.idle(70);

        // showControls  [2.5, 1, 2.5]  "up"  右键+显示连接器物品
        scene.overlay().showControls(util.vector().of(2.5, 1, 2.5), Pointing.UP, 40)
            .rightClick()
            .withItem(AllBlocks.DISPLAY_LINK.asStack());
        // highlight_section core (2,1,2)
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, new Object(), coreBounds, 40);
        scene.idleSeconds(1);
        scene.addKeyframe();

        // set_block display_link  [3, 2, 4]
        BlockPos linkPos = util.grid().at(3, 2, 4);
        BlockState displayLinkState = AllBlocks.DISPLAY_LINK.get().defaultBlockState()
            .setValue(DisplayLinkBlock.FACING, Direction.UP)
            .setValue(DisplayLinkBlock.POWERED, false);
        scene.world().setBlock(linkPos, displayLinkState, false);

        // 说明文字  [3.5, 2, 4.5]
        scene.overlay().showText(60)
            .text("以显示连接器为例，与核心建立连接")
            .colored(PonderPalette.WHITE)
            .pointAt(util.vector().of(3.5, 2, 4.5))
            .placeNearTarget();
        scene.idleSeconds(2);
        scene.addKeyframe();
        scene.idleSeconds(1);

        // set_block display_terminal  [4, 1, 1]
        BlockPos terminal1Pos = util.grid().at(4, 1, 1);
        scene.world().setBlock(terminal1Pos, ModBlocks.DISPLAY_TERMINAL.get().defaultBlockState(), false);

        // 说明文字  [4.5, 2, 1.5]
        scene.overlay().showText(50)
            .text("放下一个显示核心")
            .colored(PonderPalette.WHITE)
            .pointAt(util.vector().of(4.5, 2, 1.5))
            .placeNearTarget();
        scene.idleSeconds(3);

        // showControls  [2.5, 2, 2.5]  "down"  右键+链接方块物品
        scene.overlay().showControls(util.vector().of(2.5, 2, 2.5), Pointing.DOWN, 20)
            .rightClick()
            .withItem(new ItemStack(ModBlocks.LINK_BLOCK.get().asItem()));
        scene.idleSeconds(1);

        // set_block link_block  [3, 1, 1]
        BlockPos linkBlockPos = util.grid().at(3, 1, 1);
        BlockState linkBlockState = ModBlocks.LINK_BLOCK.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.WEST);
        scene.world().setBlock(linkBlockPos, linkBlockState, false);

        // 说明文字  [3.5, 2, 1.5]
        scene.overlay().showText(60)
            .text("使用和显示连接器类似的方法建立连接")
            .colored(PonderPalette.WHITE)
            .pointAt(util.vector().of(3.5, 2, 1.5))
            .placeNearTarget();
        // highlight_section ×2: core (2,1,2) + terminal1 (4,1,1)
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, new Object(), coreBounds, 40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, new Object(),
            new AABB(terminal1Pos), 40);
        scene.idleSeconds(2);
        scene.idleSeconds(1);

        // set_block display_terminal(紧贴)  [1, 1, 2]
        BlockPos terminal2Pos = util.grid().at(1, 1, 2);
        scene.world().setBlock(terminal2Pos, ModBlocks.DISPLAY_TERMINAL.get().defaultBlockState(), false);

        // 说明文字  [0, 1.5, 2.5]
        scene.overlay().showText(50)
            .text("让两者紧贴也可以直接建立连接")
            .pointAt(util.vector().of(0, 1.5, 2.5))
            .placeNearTarget();
        // highlight_section: core + terminal2
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, new Object(), coreBounds, 40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, new Object(),
            new AABB(terminal2Pos), 40);
        scene.idleSeconds(2);

        // destroy_block  [1, 1, 2]
        scene.world().destroyBlock(terminal2Pos);
        scene.idleSeconds(2);

        // showControls  [2.5, 2, 2.5]  "down"  右键
        scene.overlay().showControls(util.vector().of(2.5, 2, 2.5), Pointing.DOWN, 60)
            .rightClick();

        // 说明文字  [2.5, 2, 2.5]  attachKeyFrame
        scene.overlay().showText(50)
            .text("右键打开核心界面，选择显示器然后对数据点击发送")
            .pointAt(util.vector().of(2.5, 2, 2.5))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idleSeconds(3);

        // 说明文字  [4.5, 2, 1.5]
        scene.overlay().showText(60)
            .text("终端就可以接受这一条信息并让它可以被显示")
            .pointAt(util.vector().of(4.5, 2, 1.5))
            .placeNearTarget();
        scene.idleSeconds(3);

        // 说明文字  [2.5, 2, 2.5]
        scene.overlay().showText(80)
            .text("更多信息请查看核心的帮助界面")
            .pointAt(util.vector().of(2.5, 2, 2.5))
            .placeNearTarget();
        scene.idle(80);
    }
}

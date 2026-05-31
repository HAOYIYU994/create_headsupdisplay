package com.github.haoyiyu.create_headsupdisplay.block;

import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * OmniCore 显示目标：接收 Create 显示连接器推送的数据，作为核心的信息源。
 */
public class OmniCoreDisplayTarget extends DisplayTarget {

    @Override
    public void acceptText(int line, List<MutableComponent> text, DisplayLinkContext context) {
        BlockEntity be = context.getTargetBlockEntity();
        if (!(be instanceof OmniCoreBlockEntity core)) return;

        StringBuilder sb = new StringBuilder();
        for (MutableComponent c : text) {
            sb.append(c.getString());
            sb.append(" ");
        }
        String fullText = sb.toString().trim().replaceAll("§[0-9a-fk-or]", "");
        if (fullText.isEmpty()) return;

        BlockPos sourcePos = context.getSourcePos();

        // 解析标签和源名称
        String sourceName = null;
        String label = null;
        try {
            var config = context.blockEntity().getSourceConfig();
            if (config.contains("Label")) {
                label = config.getString("Label");
                if (!label.isEmpty()) sourceName = label;
            }
            if (sourceName == null) {
                var activeSource = context.blockEntity().activeSource;
                if (activeSource != null) {
                    sourceName = activeSource.getName().getString();
                }
            }
        } catch (Exception ignored) {}
        if (sourceName == null) sourceName = "Display Link";
        sourceName = sourceName.replaceAll("§[0-9a-fk-or]", "");

        // 仅在配有备注标签时，去掉第一个空格前的标签文本
        if (label != null && !label.isEmpty()) {
            int firstSpace = fullText.indexOf(' ');
            if (firstSpace > 0) {
                fullText = fullText.substring(firstSpace + 1);
            }
        }

        core.addOrUpdateDisplayLinkSource(sourcePos, sourceName, fullText);
    }

    @Override
    public DisplayTargetStats provideStats(DisplayLinkContext context) {
        return new DisplayTargetStats(3, 3, this);
    }
}
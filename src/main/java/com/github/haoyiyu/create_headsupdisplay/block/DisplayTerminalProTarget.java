package com.github.haoyiyu.create_headsupdisplay.block;

import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import java.util.List;

public class DisplayTerminalProTarget extends DisplayTarget {
    @Override
    public void acceptText(int line, List<MutableComponent> text, DisplayLinkContext context) {
        BlockEntity be = context.getTargetBlockEntity();
        if (!(be instanceof DisplayTerminalProBlockEntity terminal)) return;

        // 获取数据源位置（显示连接器连接的源方块）
        BlockPos sourcePos = context.getSourcePos();

        // 合并多行文本为单个字符串
        StringBuilder sb = new StringBuilder();
        for (MutableComponent component : text) {
            sb.append(component.getString());
            sb.append(" ");
        }
        String fullText = sb.toString().trim().replaceAll("§[0-9a-fk-or]", "");
        int firstSpace = fullText.indexOf(' ');
        if (firstSpace > 0) {
            fullText = fullText.substring(firstSpace + 1);
        }

        // 更新槽位数据和样式（行号）
        terminal.updateSlotDataAndStyle(sourcePos, fullText, line);
    }

    @Override
    public DisplayTargetStats provideStats(DisplayLinkContext context) {
        // 支持的行数：这里我们支持 0~2 分别对应不同样式，但显示连接器界面会显示可用的行数
        // 返回最大行数为 3，这样用户可以选择 0,1,2
        return new DisplayTargetStats(3, 3, this);
    }
}

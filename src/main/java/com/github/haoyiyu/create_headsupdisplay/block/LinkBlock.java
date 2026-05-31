package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 链接方块：贴在 Display Terminal 上，将 OmniCore 连接到终端。
 * 放置方向由玩家点击面决定。
 */
public class LinkBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    // 小方块碰撞箱（6x6x4 像素，贴在面上）
    private static final VoxelShape SHAPE_UP    = Block.box(5, 0, 5, 11, 4, 11);
    private static final VoxelShape SHAPE_DOWN  = Block.box(5, 12, 5, 11, 16, 11);
    private static final VoxelShape SHAPE_NORTH = Block.box(5, 5, 12, 11, 11, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(5, 5, 0, 11, 11, 4);
    private static final VoxelShape SHAPE_WEST  = Block.box(12, 5, 5, 16, 11, 11);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 5, 5, 4, 11, 11);

    public static final MapCodec<LinkBlock> CODEC = simpleCodec(LinkBlock::new);

    public LinkBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case UP    -> SHAPE_UP;
            case DOWN  -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.LINK.get().create(pos, state);
    }
}
package com.minecrafteft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ContainerPartBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty X = IntegerProperty.create("local_x", 0, 9);
    public static final IntegerProperty Y = IntegerProperty.create("local_y", 0, 5);
    public static final IntegerProperty Z = IntegerProperty.create("local_z", 0, 7);
    public static final BooleanProperty STAIR = BooleanProperty.create("stair");

    public ContainerPartBlock() {
        super(Properties.of().strength(4F).sound(SoundType.METAL).noOcclusion()
                .lightLevel(state->ContainerStructureLayout.decorationAt(state.getValue(X),state.getValue(Y),state.getValue(Z)).light()?13:0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(X, 0).setValue(Y, 0).setValue(Z, 0).setValue(STAIR, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, X, Y, Z, STAIR);
    }

    public static BlockPos anchor(BlockPos pos, BlockState state) {
        BlockPos offset = ContainerStructure.position(BlockPos.ZERO, state.getValue(FACING),
                state.getValue(X), state.getValue(Y), state.getValue(Z));
        return pos.subtract(offset);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var decoration=ContainerStructureLayout.decorationAt(state.getValue(X),state.getValue(Y),state.getValue(Z));
        if(decoration!=ContainerStructureLayout.Decoration.NONE) return StashRestAreaShapes.shape(decoration,state.getValue(FACING));
        if (!state.getValue(STAIR)) return Shapes.block();
        VoxelShape upper = switch (state.getValue(FACING)) {
            case EAST -> box(0, 8, 0, 8, 16, 16);
            case SOUTH -> box(0, 8, 0, 16, 16, 8);
            case WEST -> box(8, 8, 0, 16, 16, 16);
            default -> box(0, 8, 8, 16, 16, 16);
        };
        return Shapes.or(box(0, 0, 0, 16, 8, 16), upper);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        BlockPos anchor = anchor(pos, state);
        BlockState root = level.getBlockState(anchor);
        if (!(root.getBlock() instanceof PersonalStashBlock stash)) return InteractionResult.PASS;
        var decoration=ContainerStructureLayout.decorationAt(state.getValue(X),state.getValue(Y),state.getValue(Z));
        if(decoration.seat()) {
            if(hand!=InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            StashSeatEntity.sit(level,pos,player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(decoration.workbench()) {
            if(hand!=InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if(player instanceof net.minecraft.server.level.ServerPlayer server) {
                server.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,ignored)->
                        new net.minecraft.world.inventory.CraftingMenu(id,inventory,net.minecraft.world.inventory.ContainerLevelAccess.create(level,pos)) {
                            @Override public boolean stillValid(Player viewer) {
                                return viewer.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=64
                                        && level.getBlockState(pos)==state && level.getBlockState(anchor).is(root.getBlock());
                            }
                        },net.minecraft.network.chat.Component.translatable("container.crafting")));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(decoration!=ContainerStructureLayout.Decoration.NONE) return InteractionResult.PASS;
        var cell = new ContainerStructureLayout.Cell(state.getValue(X), state.getValue(Y), state.getValue(Z), state.getValue(STAIR));
        return stash.useDoor(root, level, anchor, cell, player, hand, hit.getDirection());
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !state.is(replacement.getBlock())) {
            BlockPos anchor = anchor(pos, state);
            if (!ContainerStructure.removing(anchor) && level.getBlockState(anchor).getBlock() instanceof PersonalStashBlock)
                level.destroyBlock(anchor, true);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()) {
            BlockPos root = anchor(pos, state);
            if (level.getBlockState(root).getBlock() instanceof PersonalStashBlock)
                level.destroyBlock(root, false);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
        return net.minecraft.world.level.material.PushReaction.BLOCK;
    }
}

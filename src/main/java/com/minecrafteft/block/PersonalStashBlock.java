package com.minecrafteft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.minecrafteft.stash.StashService;
import com.minecrafteft.stash.StashTier;

public final class PersonalStashBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty REST_AREA = BooleanProperty.create("rest_area");
    private final StashTier tier;

    public PersonalStashBlock(StashTier tier) {
        super(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL).noOcclusion());
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(REST_AREA,false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, REST_AREA);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
        return net.minecraft.world.level.material.PushReaction.BLOCK;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return ContainerStructure.canPlace(context, tier.level(), facing)
                ? defaultBlockState().setValue(FACING, facing) : null;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            ContainerStructure.build(level, pos, state, tier.level());
            if(tier==StashTier.FOUR) level.setBlock(pos,state.setValue(REST_AREA,true),3);
        }
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (world, pos, blockState, entity) -> {
            if (entity instanceof PersonalStashBlockEntity stash) stash.tickViewers();
        };
    }

    public StashTier tier() {
        return tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PersonalStashBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return useDoor(state, level, pos, new ContainerStructureLayout.Cell(0, 0, 0, false), player, hand, hit.getDirection());
    }

    public InteractionResult useDoor(BlockState state, Level level, BlockPos pos, ContainerStructureLayout.Cell cell,
                                     Player player, InteractionHand hand, Direction hitFace) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        Direction facing = state.getValue(FACING);
        int localFace = hitFace == facing ? 0 : hitFace == facing.getClockWise() ? 1 : -1;
        if (!ContainerStructureLayout.door(cell, localFace, tier.level())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            StashService.claimOrOpen(serverPlayer, (ServerLevel) level, pos, tier);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof PersonalStashBlockEntity blockEntity
                && blockEntity.getOwner() != null) {
            StashService.onAnchorDestroyed(serverLevel, pos, blockEntity.getOwner());
        }
        if (!state.is(newState.getBlock()) && !level.isClientSide)
            ContainerStructure.removeParts(level, pos, state, tier.level());
        super.onRemove(state, level, pos, newState, isMoving);
    }
}

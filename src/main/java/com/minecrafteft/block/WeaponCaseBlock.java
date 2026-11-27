package com.minecrafteft.block;

import com.minecrafteft.registry.ModContent;
import com.minecrafteft.stash.StashMenu;
import com.minecrafteft.stash.WeaponCaseInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;

public final class WeaponCaseBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    // Missing properties in old worlds resolve to false, preserving their corner-root layout.
    public static final BooleanProperty CENTERED = BooleanProperty.create("centered");
    private static final ThreadLocal<Set<BlockPos>> REMOVING = ThreadLocal.withInitial(HashSet::new);
    private final WeaponCaseKind kind;
    public WeaponCaseBlock() {
        this(WeaponCaseKind.LARGE);
    }
    public WeaponCaseBlock(WeaponCaseKind kind) {
        super(Properties.of().strength(3.5F).sound(SoundType.METAL).noOcclusion());
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(OPEN,false).setValue(CENTERED,kind.small()));
    }
    public WeaponCaseKind kind() { return kind; }
    private int heightCells() { return kind.small()?1:2; }
    private int depthCells() { return kind.small()?1:2; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) {
        return kind.small() ? WeaponCasePartBlock.shape(state.getValue(FACING), 0, 1, .4, .8) : Shapes.block();
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING,OPEN,CENTERED); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WeaponCaseBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return (world,pos,bs,entity)-> { if(entity instanceof WeaponCaseBlockEntity c) c.tick(); };
    }
    public static BlockPos at(BlockPos root,Direction facing,int x,int y,int z) { return ContainerStructure.position(root,facing,x,y,z); }
    public static BlockPos at(BlockPos root,BlockState state,int x,int y,int z) {
        return at(root,state.getValue(FACING),x - (state.getValue(CENTERED) ? 1 : 0),y,z);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var facing=context.getHorizontalDirection().getOpposite();
        var placed=defaultBlockState().setValue(FACING,facing).setValue(CENTERED,true);
        for(int x=0;x<3;x++) for(int y=0;y<heightCells();y++) for(int z=0;z<depthCells();z++) {
            var pos=at(context.getClickedPos(),placed,x,y,z);
            if(context.getLevel().isOutsideBuildHeight(pos) || !context.getLevel().getWorldBorder().isWithinBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
            if(!context.getLevel().isUnobstructed(part(placed,x,y,z),pos,CollisionContext.empty())) return null;
        }
        return placed;
    }
    private BlockState part(BlockState root,int x,int y,int z) {
        return ModContent.WEAPON_CASE_PART.get().defaultBlockState().setValue(WeaponCasePartBlock.FACING,root.getValue(FACING))
                .setValue(WeaponCasePartBlock.CENTERED,root.getValue(CENTERED))
                .setValue(WeaponCasePartBlock.SMALL,kind.small())
                .setValue(WeaponCasePartBlock.X,x).setValue(WeaponCasePartBlock.Y,y).setValue(WeaponCasePartBlock.Z,z);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack item) {
        if(!level.isClientSide) {
            if(level.getBlockEntity(pos) instanceof WeaponCaseBlockEntity c) c.loadItem(item);
            for(int x=0;x<3;x++) for(int y=0;y<heightCells();y++) for(int z=0;z<depthCells();z++) {
                if(x==(state.getValue(CENTERED)?1:0) && y==0 && z==0) continue;
                level.setBlock(at(pos,state,x,y,z),part(state,x,y,z),3);
            }
        }
        super.setPlacedBy(level,pos,state,placer,item);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(hand!=InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if(player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof WeaponCaseBlockEntity c) {
            NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,ignored)->new StashMenu(id,inventory,c),
                    Component.translatable("block.minecrafteft."+kind.id())),
                    buf->buf.writeInt(kind.slots()).writeBoolean(true));
            c.startViewing(player.getUUID());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder params) {
        var entity=params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return List.of(entity instanceof WeaponCaseBlockEntity c ? c.asItem() : new ItemStack(this));
    }
    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof WeaponCaseBlockEntity c
                && !c.inventory().write().getList("Items",10).isEmpty()) popResource(level,pos,c.asItem());
        super.playerWillDestroy(level,pos,state,player);
    }
    public static boolean removing(BlockPos pos) { return REMOVING.get().contains(pos); }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!level.isClientSide && !state.is(next.getBlock()) && REMOVING.get().add(pos)) {
            try {
                for(int x=0;x<3;x++) for(int y=0;y<heightCells();y++) for(int z=0;z<depthCells();z++) {
                    var p=at(pos,state,x,y,z);
                    var s=level.getBlockState(p);
                    if(s.is(ModContent.WEAPON_CASE_PART.get()) && WeaponCasePartBlock.anchor(p,s).equals(pos)) level.removeBlock(p,false);
                }
            } finally { REMOVING.get().remove(pos); }
        }
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
        return net.minecraft.world.level.material.PushReaction.BLOCK;
    }
}

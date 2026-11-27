package com.minecrafteft.block;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class WeaponCasePartBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty X=IntegerProperty.create("case_x",0,2);
    public static final IntegerProperty Y=IntegerProperty.create("case_y",0,1);
    public static final IntegerProperty Z=IntegerProperty.create("case_z",0,1);
    public static final BooleanProperty CENTERED=WeaponCaseBlock.CENTERED;
    public static final BooleanProperty SMALL=BooleanProperty.create("small");
    public WeaponCasePartBlock() {
        super(Properties.of().strength(3.5F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(X,0).setValue(Y,0).setValue(Z,0).setValue(CENTERED,false).setValue(SMALL,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,X,Y,Z,CENTERED,SMALL);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    public static BlockPos anchor(BlockPos p,BlockState s){
        return p.subtract(WeaponCaseBlock.at(BlockPos.ZERO,s.getValue(FACING),
                s.getValue(X)-(s.getValue(CENTERED)?1:0),s.getValue(Y),s.getValue(Z)));
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext c){
        boolean centered=s.getValue(CENTERED);
        boolean small=s.getValue(SMALL);
        double x1=centered && s.getValue(X)==0?(small?.65:.15):0;
        double x2=s.getValue(X)==2?(small?.35:centered?.85:.7):1;
        double h=small?.4:s.getValue(Y)==1?.2:1, d=small?.8:s.getValue(Z)==1?.6:1;
        return shape(s.getValue(FACING),x1,x2,h,d);
    }
    public static VoxelShape shape(Direction facing,double x1,double x2,double h,double d) {
        return switch(facing) {
            case EAST->Shapes.box(1-d,0,x1,1,h,x2);
            case SOUTH->Shapes.box(1-x2,0,1-d,1-x1,h,1);
            case WEST->Shapes.box(0,0,1-x2,d,h,1-x1);
            default->Shapes.box(x1,0,0,x2,h,d);
        };
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        var root=anchor(p,s); var state=l.getBlockState(root);
        return state.getBlock() instanceof WeaponCaseBlock block ? block.use(state,l,root,player,hand,hit) : InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(!l.isClientSide && !s.is(next.getBlock())) {
            var root=anchor(p,s);
            if(!WeaponCaseBlock.removing(root) && l.getBlockState(root).getBlock() instanceof WeaponCaseBlock) l.destroyBlock(root,true);
        }
        super.onRemove(s,l,p,next,moving);
    }
    @Override public void playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        if(!l.isClientSide && player.isCreative()) {
            var root=anchor(p,s);
            if(l.getBlockState(root).getBlock() instanceof WeaponCaseBlock) {
                if(l.getBlockEntity(root) instanceof WeaponCaseBlockEntity c && !c.inventory().write().getList("Items",10).isEmpty()) popResource(l,root,c.asItem());
                l.destroyBlock(root,false);
            }
        }
        super.playerWillDestroy(l,p,s,player);
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state){
        return net.minecraft.world.level.material.PushReaction.BLOCK;
    }
}

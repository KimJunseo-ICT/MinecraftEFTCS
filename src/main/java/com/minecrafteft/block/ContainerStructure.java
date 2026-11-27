package com.minecrafteft.block;

import com.minecrafteft.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.HashSet;
import java.util.Set;

public final class ContainerStructure {
    private static final ThreadLocal<Set<BlockPos>> REMOVING = ThreadLocal.withInitial(HashSet::new);
    private ContainerStructure() { }

    public static BlockPos position(BlockPos anchor, Direction facing, int x, int y, int z) {
        return switch (facing) {
            case EAST -> anchor.offset(-z, y, x);
            case SOUTH -> anchor.offset(-x, y, -z);
            case WEST -> anchor.offset(z, y, -x);
            default -> anchor.offset(x, y, z);
        };
    }

    public static boolean canPlace(BlockPlaceContext context, int tier, Direction facing) {
        Level level = context.getLevel();
        for (var cell : ContainerStructureLayout.cells(tier)) {
            BlockPos pos = position(context.getClickedPos(), facing, cell.x(), cell.y(), cell.z());
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos).canBeReplaced(context)) return false;
            BlockState part = partState(cell, facing);
            if (!level.isUnobstructed(part, pos, CollisionContext.empty())) return false;
        }
        return true;
    }

    private static BlockState partState(ContainerStructureLayout.Cell cell, Direction facing) {
        return ModContent.STASH_PART.get().defaultBlockState()
                .setValue(ContainerPartBlock.FACING, facing)
                .setValue(ContainerPartBlock.X, cell.x()).setValue(ContainerPartBlock.Y, cell.y())
                .setValue(ContainerPartBlock.Z, cell.z()).setValue(ContainerPartBlock.STAIR, cell.stair());
    }

    public static void build(Level level, BlockPos anchor, BlockState state, int tier) {
        Direction facing = state.getValue(PersonalStashBlock.FACING);
        for (var cell : ContainerStructureLayout.cells(tier)) {
            if (cell.x() == 0 && cell.y() == 0 && cell.z() == 0) continue;
            level.setBlock(position(anchor, facing, cell.x(), cell.y(), cell.z()), partState(cell, facing), 3);
        }
    }

    /** Upgrade existing level-4 structures only when all extra cells are clear. */
    public static boolean addRestArea(Level level,BlockPos anchor,BlockState state) {
        Direction facing=state.getValue(PersonalStashBlock.FACING);
        for(var cell:ContainerStructureLayout.restAreaCells()) {
            BlockPos pos=position(anchor,facing,cell.x(),cell.y(),cell.z());
            if(level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)) return false;
            BlockState existing=level.getBlockState(pos);
            if(existing.is(ModContent.STASH_PART.get()) && ContainerPartBlock.anchor(pos,existing).equals(anchor)) continue;
            if(!existing.isAir() || !level.isUnobstructed(partState(cell,facing),pos,CollisionContext.empty())) return false;
        }
        for(var cell:ContainerStructureLayout.restAreaCells())
            level.setBlock(position(anchor,facing,cell.x(),cell.y(),cell.z()),partState(cell,facing),3);
        level.setBlock(anchor,state.setValue(PersonalStashBlock.REST_AREA,true),3);
        return true;
    }

    public static void removeParts(Level level, BlockPos anchor, BlockState state, int tier) {
        if (!REMOVING.get().add(anchor)) return;
        try {
            Direction facing = state.getValue(PersonalStashBlock.FACING);
            for (var cell : ContainerStructureLayout.cells(tier)) {
                if (cell.x() == 0 && cell.y() == 0 && cell.z() == 0) continue;
                BlockPos pos = position(anchor, facing, cell.x(), cell.y(), cell.z());
                BlockState part = level.getBlockState(pos);
                if (part.is(ModContent.STASH_PART.get()) && ContainerPartBlock.anchor(pos, part).equals(anchor))
                    level.removeBlock(pos, false);
            }
        } finally {
            REMOVING.get().remove(anchor);
        }
    }

    public static boolean removing(BlockPos anchor) { return REMOVING.get().contains(anchor); }
}

package com.ferreusveritas.dynamictrees.systems;

import com.ferreusveritas.dynamictrees.DynamicTrees;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.util.function.TetraFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows branches connect to non-tree blocks
 *
 * @author Max Hyper
 */
public class BranchConnectables {

    private static final Map<Block, Map<ResourceLocation, TetraFunction<BlockState, BlockGetter, BlockPos, Direction, Integer>>> connectablesMap = new HashMap<>();

    //Direction can be null
    public static void makeBlockConnectable(Block block, TetraFunction<BlockState, BlockGetter, BlockPos, Direction, Integer> radiusFunction, ResourceLocation family) {
        var map = connectablesMap.computeIfAbsent(block, k -> new HashMap<>());
        map.putIfAbsent(family, radiusFunction);
    }
    public static void makeBlockConnectable(Block block, TetraFunction<BlockState, BlockGetter, BlockPos, Direction, Integer> radiusFunction) {
        makeBlockConnectable(block, radiusFunction, DynamicTrees.location("null"));
    }
    public static void replaceBlockConnectable(Block block, TetraFunction<BlockState, BlockGetter, BlockPos, Direction, Integer> radiusFunction, ResourceLocation family) {
        var map = connectablesMap.computeIfAbsent(block, k -> new HashMap<>());
        map.remove(family);
        map.put(family, radiusFunction);
    }

    public static boolean isBlockConnectable(Block block) {
        return connectablesMap.containsKey(block);
    }

    public static int getConnectionRadiusForBlock(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side, Family family) {
        final Block block = state.getBlock();
        if (isBlockConnectable(block)){
            var function = getFunctionForFamily(block, family);
            if (function == null) return 0;
            return function.apply(state, world, pos, side);
        } else {
            return 0;
        }
    }
    public static int getConnectionRadiusForBlock(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) {
        return getConnectionRadiusForBlock(state, world, pos, side, Family.NULL_FAMILY);
    }

    @Nullable
    private static TetraFunction<BlockState, BlockGetter, BlockPos, Direction, Integer> getFunctionForFamily(Block block, Family family){
        var familyMap = connectablesMap.get(block);
        if (familyMap == null) return null;
        var function = familyMap.get(family.getRegistryName());
        if (function == null){
            function = familyMap.get(DynamicTrees.location("null"));
        }
        return function;
    }
}

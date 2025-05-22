package com.ferreusveritas.dynamictrees.worldgen;

import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors;
import com.ferreusveritas.dynamictrees.api.worldgen.GroundFinder;
import com.ferreusveritas.dynamictrees.systems.poissondisc.PoissonDisc;
import com.ferreusveritas.dynamictrees.util.LevelContext;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class CaveRootedTreeFeature extends DynamicTreeFeature {

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ResourceLocation dimensionName = level.getLevel().dimension().location();

        // Do not generate if the current dimension is blacklisted.
        if (BiomeDatabases.isBlacklisted(dimensionName)) {
            return false;
        }

        BlockPos originPos = context.origin();
        ChunkPos chunkPos = level.getChunk(originPos).getPos();
        LevelContext levelContext = LevelContext.create(level);

        PoissonDisc disc = getDisc(levelContext, chunkPos, originPos).orElse(null);
        if (disc == null) {
            return false;
        }

        List<BlockPos> groundPositions = GroundFinder.getGroundFinder(level.getLevel()).findGround(level, originPos, null);
        if (groundPositions.isEmpty()) {
            return false;
        }

        BiomeDatabase.Entry biomeEntry = BiomeDatabases.getDefault().getEntry(level.getLevel().getBiome(originPos));
        if (biomeEntry.getCaveRootedData() == null)
            return false;

        BiomeDatabase.CaveRootedData caveRootedData = biomeEntry.getCaveRootedData();

        groundPositions = groundPositions.stream()
                .filter(pos-> pos != BlockPos.ZERO)
                //.filter(pos-> pos.getY() - originPos.getY() < caveRootedData.getMaxDistToSurface())
                .sorted(Comparator.comparingInt(Vec3i::getY)).toList();

        if (groundPositions.isEmpty()) return false;

        if (caveRootedData.shouldGenerateOnSurface()){
            groundPositions = List.of(groundPositions.get(groundPositions.size() - 1));
        }

        AtomicBoolean generated = new AtomicBoolean(false);
        groundPositions.forEach(groundPos -> {
                    GeneratorResult result = this.generateTree(levelContext, biomeEntry, disc, originPos, groundPos, SafeChunkBounds.ANY_WG);
                    if (result == GeneratorResult.GENERATED) generated.set(true);
                });

        return generated.get();
    }

    protected BiomePropertySelectors.SpeciesSelector getSpeciesSelector (BiomeDatabase.EntryReader biomeEntry){
        if (biomeEntry instanceof BiomeDatabase.Entry entry && entry.getCaveRootedData() != null)
            return entry.getCaveRootedData().getCaveRootedSpeciesSelector();
        return biomeEntry.getSpeciesSelector();
    }

    protected BiomePropertySelectors.ChanceSelector getChanceSelector (BiomeDatabase.EntryReader biomeEntry){
        if (biomeEntry instanceof BiomeDatabase.Entry entry && entry.getCaveRootedData() != null)
            return entry.getCaveRootedData().getCaveRootedChanceSelector();
        return biomeEntry.getChanceSelector();
    }

    private Optional<PoissonDisc> getDisc(LevelContext levelContext, ChunkPos chunkPos, BlockPos originPos) {
        return DISC_PROVIDER.getPoissonDiscs(levelContext, chunkPos).stream()
                .filter(disc -> disc.x == originPos.getX() && disc.z == originPos.getZ())
                .findFirst();
    }

    private Optional<BlockPos> getNextGroundPos(BlockPos originPos, List<BlockPos> groundPositions) {
        for (BlockPos groundPos: groundPositions) {
            if (groundPos.getY() > originPos.getY()) {
                return Optional.of(groundPos);
            }
        }
        return Optional.empty();
    }

}

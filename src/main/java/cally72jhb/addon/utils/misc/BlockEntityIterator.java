package cally72jhb.addon.utils.misc;

import meteordevelopment.meteorclient.mixin.ChunkAccessor;
import meteordevelopment.meteorclient.utils.world.ChunkIterator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.Iterator;
import java.util.Map;

public class BlockEntityIterator implements Iterator<BlockEntity> {
    private final Iterator<Chunk> chunks;
    private Iterator<BlockEntity> blockEntities;

    public BlockEntityIterator() {
        chunks = new ChunkIterator();

        nextChunk();
    }

    private void nextChunk() {
        while (true) {
            if (!chunks.hasNext()) break;

            Map<BlockPos, BlockEntity> blockEntityMap = ((ChunkAccessor) chunks.next()).getBlockEntities();

            if (blockEntityMap.size() > 0) {
                blockEntities = blockEntityMap.values().iterator();
                break;
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (blockEntities == null) return false;
        if (blockEntities.hasNext()) return true;

        nextChunk();

        return blockEntities.hasNext();
    }

    @Override
    public BlockEntity next() {
        return blockEntities.next();
    }
}

package org.pepsoft.worldpainter.minetest;

import com.google.common.collect.ImmutableSortedMap;
import org.pepsoft.minecraft.*;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Constants;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.WorldPainterChunkFactory;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.Theme;

import java.util.Collections;
import java.util.UUID;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Terrain.GRASS;

/**
 * Created by Pepijn Schmitz on 21-02-17.
 */
public class ChunkTest {
    public static void main(String[] args) {
        WPPluginManager.initialise(UUID.randomUUID());
        Theme theme = new SimpleTheme(1L, 52, ImmutableSortedMap.of(-1, GRASS), null, DEFAULT_MAX_HEIGHT_2, true, true);
        TileFactory tileFactory = new HeightMapTileFactory(1L, new NoiseHeightMap(20, 1.0, 1), DEFAULT_MAX_HEIGHT_2, false, theme);
        Dimension dimension = new World2(DefaultPlugin.JAVA_ANVIL, 1L, tileFactory, tileFactory.getMaxHeight()).getDimension(Constants.DIM_NORMAL);
        dimension.addTile(tileFactory.createTile(0, 0));
        WorldPainterChunkFactory mcChunkFactory = new WorldPainterChunkFactory(dimension, Collections.emptyMap(), DefaultPlugin.JAVA_ANVIL, dimension.getMaxHeight());
        WorldPainterChunkFactory mtChunkFactory = new WorldPainterChunkFactory(dimension, Collections.emptyMap(), MinetestPlatformProvider.MINETEST, dimension.getMaxHeight());
        Chunk mcChunk = mcChunkFactory.createChunk(0, 0).chunk;
        Chunk mtChunk = mtChunkFactory.createChunk(0, 0).chunk;
        for (int y = 0; y < DEFAULT_MAX_HEIGHT_2; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (mcChunk.getMaterial(x, y, z) != mtChunk.getMaterial(x, y, z)) {
                        System.out.printf("Different materials @ %d,%d,%d: %s and %s%n", x, y, z, mcChunk.getMaterial(x, y, z), mtChunk.getMaterial(x, y, z));
                    }
                }
            }
        }
    }
}
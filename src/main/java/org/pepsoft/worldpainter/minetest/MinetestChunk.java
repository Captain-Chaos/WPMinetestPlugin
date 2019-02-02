package org.pepsoft.worldpainter.minetest;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;

import java.awt.*;
import java.util.List;

import static org.pepsoft.minecraft.Constants.BLK_AIR;
import static org.pepsoft.minecraft.Material.AIR;

/**
 * This API's coordinate system is the Minecraft coordinate system (W <- x -> E,
 * down <- y -> up, N <- z -> S).
 *
 * <p>This implementation maintains the invariant that the map block at y == 0
 * ALWAYS exists, and that any blocks above and below it are contiguous; that
 * is: there are no gaps in the column other than below the lowest and above the
 * highest map block.
 *
 * <p>Created by Pepijn on 12-2-2017.
 */
public class MinetestChunk implements Chunk {
    public MinetestChunk(int x, int z) {
        this.x = x;
        this.z = z;
        mapBlocks = new MapBlock[] {new MapBlock(x, 0, -z)};
        lowestMapBlockY = 0;
    }

    public MinetestChunk(int x, int z, MapBlock[] mapBlocks) {
        this.x = x;
        this.z = z;
        this.mapBlocks = mapBlocks;
        lowestMapBlockY = mapBlocks[0].y;
    }

    // Chunk

    @Override
    public Material getMaterial(int x, int y, int z) {
        MapBlock mapBlock = getMapBlock(y >> 4);
        return (mapBlock != null) ? mapBlock.getMaterial(x, y & 0xf, 15 - z) : AIR;
    }

    @Override
    public void setMaterial(int x, int y, int z, Material material) {
        if (material != AIR) {
            getOrCreateMapBlock(y >> 4).setMaterial(x, y & 0xf, 15 - z, material);
        } else {
            MapBlock mapBlock = getMapBlock(y >> 4);
            if (mapBlock != null) {
                mapBlock.setMaterial(x, y & 0xf, 15 - z, material);
            }
        }
    }

    @Override
    public int getBlockType(int x, int y, int z) {
        MapBlock mapBlock = getMapBlock(y >> 4);
        return (mapBlock != null) ? mapBlock.getBlockType(x, y & 0xf, 15 - z) : BLK_AIR;
    }

    @Override
    public void setBlockType(int x, int y, int z, int blockType) {
        if (blockType != BLK_AIR) {
            getOrCreateMapBlock(y >> 4).setBlockType(x, y & 0xf, 15 - z, blockType);
        } else {
            MapBlock mapBlock = getMapBlock(y >> 4);
            if (mapBlock != null) {
                mapBlock.setBlockType(x, y & 0xf, 15 - z, blockType);
            }
        }
    }

    @Override
    public int getDataValue(int x, int y, int z) {
        MapBlock mapBlock = getMapBlock(y >> 4);
        return (mapBlock != null) ? mapBlock.getBlockType(x, y & 0xf, 15 - z) : 0;
    }

    @Override
    public void setDataValue(int x, int y, int z, int dataValue) {
        if (dataValue != 0) {
            getOrCreateMapBlock(y >> 4).setDataValue(x, y & 0xf, 15 - z, dataValue);
        } else {
            MapBlock mapBlock = getMapBlock(y >> 4);
            if (mapBlock != null) {
                mapBlock.setDataValue(x, y & 0xf, 15 - z, dataValue);
            }
        }
    }

    @Override
    public int getBlockLightLevel(int x, int y, int z) {
        MapBlock mapBlock = getMapBlock(y >> 4);
        return (mapBlock != null) ? mapBlock.getBlockLightLevel(x, y & 0xf, 15 - z) : 0;
    }

    @Override
    public void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {
        if (blockLightLevel != 0) {
            getOrCreateMapBlock(y >> 4).setBlockLightLevel(x, y & 0xf, 15 - z, blockLightLevel);
        } else {
            MapBlock mapBlock = getMapBlock(y >> 4);
            if (mapBlock != null) {
                mapBlock.setBlockLightLevel(x, y & 0xf, 15 - z, blockLightLevel);
            }
        }
    }

    @Override
    public int getHeight(int x, int z) {
        return 0;
    }

    @Override
    public void setHeight(int x, int z, int height) {
        // Do nothing
    }

    @Override
    public int getSkyLightLevel(int x, int y, int z) {
        MapBlock mapBlock = getMapBlock(y >> 4);
        return (mapBlock != null) ? mapBlock.getSkyLightLevel(x, y & 0xf, 15 - z) : 15;
    }

    @Override
    public void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {
        if (skyLightLevel != 15) {
            getOrCreateMapBlock(y >> 4).setSkyLightLevel(x, y & 0xf, 15 - z, skyLightLevel);
        } else {
            MapBlock mapBlock = getMapBlock(y >> 4);
            if (mapBlock != null) {
                mapBlock.setSkyLightLevel(x, y & 0xf, 15 - z, skyLightLevel);
            }
        }
    }

    @Override
    public int getxPos() {
        return x;
    }

    @Override
    public int getzPos() {
        return z;
    }

    @Override
    public Point getCoords() {
        return new Point(x, z);
    }

    @Override
    public boolean isTerrainPopulated() {
        return false;
    }

    @Override
    public void setTerrainPopulated(boolean terrainPopulated) {
        // Do nothing
    }

    @Override
    public List<Entity> getEntities() {
        return null;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        return null;
    }

    @Override
    public int getMaxHeight() {
        return 0;
    }

    @Override
    public boolean isBiomesAvailable() {
        return false;
    }

    @Override
    public int getBiome(int x, int z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBiome(int x, int z, int biome) {
        // Do nothing
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isLightPopulated() {
        return false;
    }

    @Override
    public void setLightPopulated(boolean lightPopulated) {
        // Do nothing
    }

    @Override
    public long getInhabitedTime() {
        return 0;
    }

    @Override
    public void setInhabitedTime(long inhabitedTime) {
        // Do nothing
    }

    @Override
    public int getHighestNonAirBlock(int x, int z) {
        return 0;
    }

    @Override
    public int getHighestNonAirBlock() {
        return 0;
    }

    private MapBlock getMapBlock(int y) {
        if ((y >= lowestMapBlockY) && (y < lowestMapBlockY + mapBlocks.length)) {
            return mapBlocks[y - lowestMapBlockY];
        } else {
            return null;
        }
    }

    private MapBlock getOrCreateMapBlock(int y) {
        MapBlock mapBlock = getMapBlock(y);
        if (mapBlock == null) {
            if (y < lowestMapBlockY) {
                MapBlock[] newMapBlocks = new MapBlock[lowestMapBlockY + mapBlocks.length - y];
                System.arraycopy(mapBlocks, 0, newMapBlocks, lowestMapBlockY - y, mapBlocks.length);
                for (int i = 0; i < lowestMapBlockY - y; i++) {
                    newMapBlocks[i] = new MapBlock(x, y + i, -z);
                    newMapBlocks[i].setUnderground(true);
                }
                mapBlocks = newMapBlocks;
                lowestMapBlockY = y;
                mapBlock = mapBlocks[0];
            } else {
                MapBlock[] newMapBlocks = new MapBlock[y - lowestMapBlockY + 1];
                System.arraycopy(mapBlocks, 0, newMapBlocks, 0, mapBlocks.length);
                for (int i = mapBlocks.length; i < newMapBlocks.length; i++) {
                    newMapBlocks[i] = new MapBlock(x, i + lowestMapBlockY, -z);
                    // Make sure all map blocks except the topmost one are flagged as "underground"
                    newMapBlocks[i - 1].setUnderground(true);
                }
                mapBlocks = newMapBlocks;
                mapBlock = mapBlocks[mapBlocks.length - 1];
            }
        }
        return mapBlock;
    }

    final int x, z;
    MapBlock[] mapBlocks;
    int lowestMapBlockY;
}
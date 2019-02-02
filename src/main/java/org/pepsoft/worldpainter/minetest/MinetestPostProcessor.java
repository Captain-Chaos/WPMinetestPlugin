package org.pepsoft.worldpainter.minetest;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.PostProcessor;

import static org.pepsoft.minecraft.Block.BLOCKS;
import static org.pepsoft.minecraft.Block.BLOCK_TRANSPARENCY;
import static org.pepsoft.minecraft.Constants.*;

/**
 * Created by Pepijn on 25-2-2017.
 */
public class MinetestPostProcessor extends PostProcessor {
    @Override
    public void postProcess(MinecraftWorld minecraftWorld, Box volume, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        if (! enabled) {
            return;
        }
        if (progressReceiver != null) {
            progressReceiver.setMessage("Enforcing Minetest rules on exported blocks");
        }
        final int worldMaxZ = minecraftWorld.getMaxHeight() - 1;
        final int x1, y1, x2, y2, minZ, maxZ;
        // TODO: make these configurable:
        final FloatMode sandMode = "false".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.supportSand")) ? FloatMode.LEAVE_FLOATING : FloatMode.SUPPORT;
        final FloatMode gravelMode = FloatMode.LEAVE_FLOATING;
        x1 = volume.getX1();
        y1 = volume.getY1();
        x2 = volume.getX2() - 1;
        y2 = volume.getY2() - 1;
        minZ = volume.getZ1();
        maxZ = volume.getZ2() - 1;
        for (int x = x1; x <= x2; x ++) {
            for (int y = y1; y <= y2; y++) {
                int blockTypeBelow = BLK_AIR;
                int blockTypeAbove = minecraftWorld.getBlockTypeAt(x, y, minZ);
                for (int z = minZ; z <= maxZ; z++) {
                    int blockType = blockTypeAbove;
                    blockTypeAbove = (z < worldMaxZ) ? minecraftWorld.getBlockTypeAt(x, y, z + 1) : BLK_AIR;
                    if (((blockTypeBelow == BLK_GRASS) || (blockTypeBelow == BLK_TILLED_DIRT)) && ((blockType == BLK_WATER) || (blockType == BLK_STATIONARY_WATER) || (blockType == BLK_ICE) || ((blockType <= HIGHEST_KNOWN_BLOCK_ID) && (BLOCK_TRANSPARENCY[blockType] == 15)))) {
                        // Covered grass, mycelium or tilled earth block, should
                        // be dirt. Note that unknown blocks are treated as
                        // transparent for this check so that grass underneath
                        // custom plants doesn't turn to dirt, for instance
                        minecraftWorld.setMaterialAt(x, y, z - 1, Material.DIRT);
                        blockTypeBelow = BLK_DIRT;
                    }
                    switch (blockType) {
                        case BLK_GRASS:
                            if (blockTypeAbove == BLK_SNOW) {
                                // This is not a Minecraft block, but it will be
                                // mapped to "dirt with snow" by the default
                                // mapping:
                                minecraftWorld.setDataAt(x, y, z, 15);
                            }
                            break;
                        case BLK_SAND:
                            if (BLOCKS[blockTypeBelow].veryInsubstantial) {
                                switch (sandMode) {
                                    case DROP:
                                        dropBlock(minecraftWorld, x, y, z);
                                        blockType = BLK_AIR;
                                        break;
                                    case SUPPORT:
                                        // All unsupported sand should be supported by sandstone
                                        minecraftWorld.setMaterialAt(x, y, z, (minecraftWorld.getDataAt(x, y, z) == 1) ? Material.RED_SANDSTONE : Material.SANDSTONE);
                                        blockType = minecraftWorld.getBlockTypeAt(x, y, z);
                                        break;
                                    default:
                                        // Do nothing
                                        break;
                                }
                            }
                            break;
                        case BLK_GRAVEL:
                            if (BLOCKS[blockTypeBelow].veryInsubstantial) {
                                switch (gravelMode) {
                                    case DROP:
                                        dropBlock(minecraftWorld, x, y, z);
                                        blockType = BLK_AIR;
                                        break;
                                    case SUPPORT:
                                        // All unsupported gravel should be supported by stone
                                        minecraftWorld.setMaterialAt(x, y, z, Material.STONE);
                                        blockType = BLK_STONE;
                                        break;
                                    default:
                                        // Do nothing
                                        break;
                                }
                            }
                            break;
                        // TODO: find out what the most appropriate rules are;
                        // for now just check that they aren't floating:
                        case BLK_DEAD_SHRUBS:
                        case BLK_TALL_GRASS:
                        case BLK_ROSE:
                        case BLK_DANDELION:
                        case BLK_RED_MUSHROOM:
                        case BLK_BROWN_MUSHROOM:
                        case BLK_LARGE_FLOWERS: // Assuming only bottom halves
                            if (BLOCKS[blockTypeBelow].veryInsubstantial) {
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_SNOW:
                            if ((blockTypeBelow == BLK_ICE) || (blockTypeBelow == BLK_SNOW) || (blockTypeBelow == BLK_AIR) || (blockTypeBelow == BLK_PACKED_ICE)) {
                                // Snow can't be on ice, or another snow block, or air
                                // (well it could be, but it makes no sense, would
                                // disappear when touched, and it makes this algorithm
                                // remove stacks of snow blocks correctly)
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_WHEAT:
                            if (blockTypeBelow != BLK_TILLED_DIRT) {
                                // Wheat can only exist on Tilled Dirt blocks // TODO: true for Minetest?
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_CACTUS:
                            if ((blockTypeBelow != BLK_SAND) && (blockTypeBelow != BLK_CACTUS)) {
                                // Cactus blocks can only be on top of sand or other cactus blocks // TODO: true for Minetest?
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_SUGAR_CANE:
                            if ((blockTypeBelow != BLK_GRASS) && (blockTypeBelow != BLK_DIRT) && (blockTypeBelow != BLK_SAND) && (blockTypeBelow != BLK_SUGAR_CANE)) {
                                // Sugar cane blocks can only be on top of grass, dirt, sand or other sugar cane blocks // TODO: true for Minetest?
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_FIRE:
                            // We don't know which blocks are flammable, but at
                            // least check whether the fire is not floating in
                            // the air
                            if ((blockTypeBelow == BLK_AIR)
                                    && (blockTypeAbove == BLK_AIR)
                                    && (minecraftWorld.getBlockTypeAt(x - 1, y, z) == BLK_AIR)
                                    && (minecraftWorld.getBlockTypeAt(x + 1, y, z) == BLK_AIR)
                                    && (minecraftWorld.getBlockTypeAt(x, y - 1, z) == BLK_AIR)
                                    && (minecraftWorld.getBlockTypeAt(x, y + 1, z) == BLK_AIR)) {
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                    }
                    blockTypeBelow = blockType;
                }
            }
            if (progressReceiver != null) {
                progressReceiver.setProgress((float) (x - x1 + 1) / (x2 - x1 + 1));
            }
        }

    }
}
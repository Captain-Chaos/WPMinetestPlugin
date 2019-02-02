package org.pepsoft.worldpainter.minetest;

import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.objects.NamedObjectWithAttributes;

import javax.vecmath.Point3i;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.pepsoft.util.DataUtils.readUnsignedInt;
import static org.pepsoft.worldpainter.minetest.MinetestUtils.deserialiseContents;
import static org.pepsoft.worldpainter.minetest.MinetestUtils.deserialiseString;

/**
 * Created by Pepijn on 9-3-2017.
 */
public class Schematic extends NamedObjectWithAttributes {
    private Schematic(String name, short dimX, short dimY, short dimZ, byte[] sliceProbs, short[] content, byte[] param1, byte[] param2, String[] nodeNames) {
        super(name);
        // Minetest coords:
        this.dimX = dimX;
        this.dimY = dimY;
        this.dimZ = dimZ;
        this.sliceProbs = sliceProbs;
        this.content = content;
        this.param1 = param1;
        this.param2 = param2;
        this.nodeNames = nodeNames;
        mapping = new Material[nodeNames.length];
        int expectedLength = dimX * dimY * dimZ;
        if ((content.length != expectedLength) || (param1.length != expectedLength) || (param2.length != expectedLength)) {
            throw new IllegalArgumentException("Array size does not match dimensions");
        }
        setAttribute(ATTRIBUTE_OFFSET, guestimateOffset());
    }

    @Override
    public Point3i getDimensions() {
        return new Point3i(dimX, dimZ, dimY);
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        // API is in WorldPainter coords; data is in Minetest coords
        return mapping[content[wpToMtsIndex(x, y, z)] & 0xffff];
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        // API is in WorldPainter coords; data is in Minetest coords
        return mapping[content[wpToMtsIndex(x, y, z)] & 0xffff] != Material.AIR;
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
    public void prepareForExport(Dimension dimension) {
        BlockMapping blockMapping = BlockMapping.get(dimension);
        for (int i = 0; i < mapping.length; i++) {
            mapping[i] = blockMapping.getMaterial(nodeNames[i], true);
        }
    }

    /**
     * Convert coordinates in WorldPainter coordinate space to an index into a
     * Minetest node array.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param z The Z coordinate.
     * @return The corresponding index into a Minetest node array.
     */
    private int wpToMtsIndex(int x, int y, int z) {
        return y * dimX * dimY + z * dimX + x;
    }

    static Schematic load(File file) throws IOException {
        short dimX, dimY, dimZ;
        byte[] sliceProbs, param1, param2;
        String[] nodeNames;
        short[] content;
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            long signature = readUnsignedInt(in);
            if (signature != MTSCHEM_FILE_SIGNATURE) {
                throw new IllegalArgumentException("Not a Minetest schematic: \"" + file + "\"");
            }
            int version = in.readUnsignedShort();
            if (version != MTSCHEM_FILE_VER_HIGHEST_READ) {
                throw new IllegalArgumentException("Unsupported Minetest schematic format version: " + version);
            }
            dimX = in.readShort();
            dimY = in.readShort();
            dimZ = in.readShort();
            sliceProbs = new byte[dimY];
            for (int y = 0; y < dimY; y++) {
                // Coercing unsigned bytes to signed here; take this into
                // account when processing them later:
                sliceProbs[y] = in.readByte();
            }
            int nameCount = in.readUnsignedShort();
            nodeNames = new String[nameCount];
            for (int i = 0; i < nameCount; i++) {
                nodeNames[i] = deserialiseString(in);
            }
            int size = dimX * dimY * dimZ;
            content = new short[size];
            param1 = new byte[size];
            param2 = new byte[size];
            deserialiseContents(in, content, param1, param2);
            String name = file.getName();
            int p = name.lastIndexOf('.');
            if (p != -1) {
                name = name.substring(0, p);
            }
            return new Schematic(name, dimX, dimY, dimZ, sliceProbs, content, param1, param2, nodeNames);
        }
    }

    private final short dimX, dimY, dimZ;
    private final byte[] sliceProbs, param1, param2;
    private final String[] nodeNames;
    private final short[] content;
    private final Material[] mapping;

    private static final long MTSCHEM_FILE_SIGNATURE = 0x4d54534dL;
    private static final int MTSCHEM_FILE_VER_HIGHEST_READ = 4;
    private static final long serialVersionUID = 1L;
}
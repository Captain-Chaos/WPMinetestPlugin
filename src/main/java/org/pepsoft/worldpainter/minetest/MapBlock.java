package org.pepsoft.worldpainter.minetest;

import org.pepsoft.minecraft.Material;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static org.pepsoft.util.DataUtils.readUnsignedInt;
import static org.pepsoft.worldpainter.minetest.MinetestUtils.*;

/**
 * This API's coordinate system is the Minetest coordinate system
 * (W <- x -> E, down <- y -> up, S <- z -> N).
 *
 * <p>This implementation assumes that the {@link BlockMapping} maps materials
 * onto content ids one-on-one. That may have to change to support reading
 * Minetest-generated map blocks.
 *
 * <p>This implementation supports only version 25 of the serialization format.
 * That may have to change to support reading Minetest-generated map blocks.
 *
 * <p>Created by Pepijn Schmitz on 14-02-17.
 */
class MapBlock {
    MapBlock(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        content = new short[4096];
        param1 = new byte[4096];
        param2 = new byte[4096];
        flags = FLAG_DAY_NIGHT_DIFFERENT /* probably anyway */;
    }

    MapBlock(int x, int y, int z, byte[] data) {
        this(x, y, z);
        loadData(data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append("{MapBlock; coords: ").append(x).append(",").append(y).append(",").append(z);
        if (metadata != null) {
            sb.append("; metadata: ").append(metadata);
        }
        if (staticObjects != null) {
            sb.append("; static objects: ").append(staticObjects);
        }
        if (nameIdMappings != null) {
            sb.append("; name-ID mappings: ").append(nameIdMappings);
        }
        if (nodeTimers != null) {
            sb.append("; node timers: ").append(nodeTimers);
        }
        return sb.append("}").toString();
    }

    Material getMaterial(int x, int y, int z) {
        return Material.getByCombinedIndex(content[(z << 8) | (y << 4) | x] & 0xffff);
    }

    void setMaterial(int x, int y, int z, Material material) {
        content[(z << 8) | (y << 4) | x] = (short) material.index;
    }

    int getBlockType(int x, int y, int z) {
        return (content[(z << 8) | (y << 4) | x] & 0xffff) >> 4;
    }

    void setBlockType(int x, int y, int z, int blockType) {
        int contentId = content[(z << 8) | (y << 4) | x];
        content[(z << 8) | (y << 4) | x] = (short) ((blockType << 4) | (contentId & 0xf));
    }

    int getDataValue(int x, int y, int z) {
        return content[(z << 8) | (y << 4) | x] & 0xf;
    }

    void setDataValue(int x, int y, int z, int dataValue) {
        int contentId = content[(z << 8) | (y << 4) | x];
        content[(z << 8) | (y << 4) | x] = (short) ((contentId & 0xfff0) | dataValue);
    }

    int getBlockLightLevel(int x, int y, int z) {
        return (param1[(z << 8) | (y << 4) | x] & 0xf0) >> 4;
    }

    void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {
        int param1 = this.param1[(z << 8) | (y << 4) | x] & 0xff;
        this.param1[(z << 8) | (y << 4) | x] = (byte) ((blockLightLevel << 4) | (param1 & 0xf));
    }

    int getSkyLightLevel(int x, int y, int z) {
        return param1[(z << 8) | (y << 4) | x] & 0xf;
    }

    void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {
        int param1 = this.param1[(z << 8) | (y << 4) | x] & 0xff;
        this.param1[(z << 8) | (y << 4) | x] = (byte) ((param1 & 0xf0) | skyLightLevel);
    }

    int getVersion() {
        return version;
    }

    int getFlags() {
        return flags;
    }

    void setFlags(int flags) {
        this.flags = flags;
    }

    boolean isUnderground() {
        return (flags & FLAG_UNDERGROUND) != 0;
    }

    void setUnderground(boolean underground) {
        flags = underground ? (flags | FLAG_UNDERGROUND) : (flags & ~FLAG_UNDERGROUND);
    }

    boolean isDayNightDifference() {
        return (flags & FLAG_DAY_NIGHT_DIFFERENT) != 0;
    }

    void setDayNightDifference(boolean dayNightDifference) {
        flags = dayNightDifference ? (flags | FLAG_DAY_NIGHT_DIFFERENT) : (flags & ~FLAG_DAY_NIGHT_DIFFERENT);
    }

    boolean isLightingExpired() {
        return (flags & FLAG_LIGHTING_EXPIRED) != 0;
    }

    void setLightingExpired(boolean lightingExpired) {
        flags = lightingExpired ? (flags | FLAG_LIGHTING_EXPIRED) : (flags & ~FLAG_LIGHTING_EXPIRED);
    }

    boolean isGenerated() {
        return (flags & FLAG_GENERATED) != 0;
    }

    void setGenerated(boolean generated) {
        flags = generated ? (flags | FLAG_GENERATED) : (flags & ~FLAG_GENERATED);
    }

    boolean isAllAir() {
        for (short contentId: content) {
            if (contentId != 0) {
                return false;
            }
        }
        return true;
    }

    byte[] getData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeByte(version);
            out.writeByte(flags); // TODO: do we need to set/reset any?
            out.writeByte(2); // contentWidth
            out.writeByte(2); // paramsWidth

            // Blocks
            BitSet nodeIdsInUse = new BitSet(65536);
            try (DataOutputStream nestedOut = new DataOutputStream(new DeflaterOutputStream(out))) {
                for (short s: content) {
                    nestedOut.writeShort(s);
                    nodeIdsInUse.set(s & 0xffff);
                }
                // We have the light in sky/block configuration as per
                // Minecraft; write it out in the day/night configuration which
                // Minetest expects; TODO: upon loading this distinction is lost, how should we handle that?
                for (byte b: param1) {
                    int blockNibble = (b & 0xf0) >> 4;
                    nestedOut.write((blockNibble << 4) | Math.max(blockNibble, b & 0xf));
                }
                nestedOut.write(param2);
            }

            // Metadata
            try (DataOutputStream nestedOut = new DataOutputStream(new DeflaterOutputStream(out))) {
                if ((metadata != null) && (! metadata.isEmpty())) {
                    nestedOut.write(1); // Version
                    nestedOut.writeShort(metadata.size());
                    for (Map.Entry<Integer, Metadata> nodeEntry: metadata.entrySet()) {
                        nestedOut.writeShort(nodeEntry.getKey());
                        Metadata metadata = nodeEntry.getValue();

                        if ((metadata.variables != null) && (! metadata.variables.isEmpty())) {
                            nestedOut.writeInt(metadata.variables.size());
                            for (Map.Entry<String, String> varEntry: metadata.variables.entrySet()) {
                                serialiseString(nestedOut, varEntry.getKey());
                                serialiseLongString(nestedOut, varEntry.getValue());
                            }
                        } else {
                            nestedOut.writeInt(0); // Count
                        }

                        if ((metadata.inventories != null) && (! metadata.inventories.isEmpty())) {
                            for (Map.Entry<String, Inventory> invEntry: metadata.inventories.entrySet()) {
                                Inventory inventory = invEntry.getValue();
                                writeLine(nestedOut, "List " + inventory.name + " " + inventory.items.size());
                                for (String item: inventory.items) {
                                    writeLine(nestedOut, item);
                                }
                                writeLine(nestedOut, "EndInventoryList");
                            }
                        }
                        writeLine(nestedOut, "EndInventory");
                    }
                } else {
                    nestedOut.write(0); // Version
                }
            }

            // Static objects
            out.write(0); // Version
            if ((staticObjects != null) && (! staticObjects.isEmpty())) {
                out.writeShort(staticObjects.size());
                for (StaticObject staticObject: staticObjects) {
                    out.write(staticObject.type);
                    out.write(staticObject.x);
                    out.write(staticObject.y);
                    out.write(staticObject.z);
                    out.writeShort(staticObject.data.length);
                    out.write(staticObject.data);
                }
            } else {
                out.writeShort(0); // Count
            }

            out.writeInt((int) timestamp);

            // Name ID mappings
            // TODO: standard mappings
            out.write(0); // Version
            out.writeShort(nodeIdsInUse.cardinality());
//            System.out.print("Name-ID mappings");
            nameIdMappings = new HashMap<>();
            nodeIdsInUse.stream().forEach(node -> {
                try {
//                    System.out.print("; " + Material.getByCombinedIndex(node) + " -> " + nodeName);
                    out.writeShort(node);
                    serialiseString(out, BLOCK_MAPPING.getNodeName(node));
                    nameIdMappings.put((short) node, BLOCK_MAPPING.getNodeName(node));
                } catch (IOException e) {
                    throw new RuntimeException("I/O error while writing name-ID mapping", e);
                }
            });
            // Sanity check:
//            for (short s: content) {
//                if (! nameIdMappings.containsKey(s)) {
//                    throw new RuntimeException("Missing mapping for content ID " + s + "!");
//                }
//            }
//            System.out.println();
//            if ((nameIdMappings != null) && (! nameIdMappings.isEmpty())) {
//                out.writeShort(nameIdMappings.size());
//                for (Map.Entry<Integer, String> entry: nameIdMappings.entrySet()) {
//                    out.writeShort(entry.getKey());
//                    serialiseString(out, entry.getValue());
//                }
//            } else {
//                out.writeShort(0); // Count
//            }

            // Node timers
            out.write(10); // Timer data length
            if ((nodeTimers != null) && (! nodeTimers.isEmpty())) {
                out.writeShort(nodeTimers.size());
                for (NodeTimer nodeTimer: nodeTimers.values()) {
                    out.writeShort(nodeTimer.position);
                    out.writeInt(nodeTimer.timeout);
                    out.writeInt(nodeTimer.elapsed);
                }
            } else {
                out.writeShort(0); // Count
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while writing map block data", e);
        }
        return baos.toByteArray();
    }

    void dumpContents() {
        for (int y = 0; y < 16; y++) {
            System.out.printf("y == %d%n", y);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    short contentId = content[(z << 8) | (y << 4) | x];
                    int param1 = this.param1[(z << 8) | (y << 4) | x] & 0xff;
                    int param2 = this.param2[(z << 8) | (y << 4) | x] & 0xff;
                    System.out.printf("[%s:%d:%d] ", nameIdMappings.get(contentId), param1, param2);
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    private void loadData(byte[] data) {
//        dump(data);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            version = in.read();
            if (version != SUPPORTED_VERSION) {
                throw new IllegalArgumentException("Version " + version + " not (yet) supported by the Minetest platform");
            }
            flags = in.read();
            int contentWidth = in.read();
            int paramsWidth = in.read();
            if ((contentWidth != 2) || (paramsWidth != 2)) {
                throw new IllegalArgumentException("Illegal data in map block");
            }

            // Blocks
            deserialiseContents(in, content, param1, param2);
//            dump(in);

            // Metadata
            try (DataInputStream nestedIn = new DataInputStream(new InflaterInputStream(in, new Inflater(), 1))) {
                int metadataVersion = nestedIn.read();
                if (metadataVersion != 0) {
                    if (metadataVersion != 1) {
                        throw new IllegalArgumentException("Illegal data in map block");
                    }
                    int count = nestedIn.readUnsignedShort();
                    if (count > 0) {
                        metadata = new HashMap<>();
                        for (int i = 0; i < count; i++) {
                            int position = nestedIn.readUnsignedShort();
                            Metadata nodeMetadata = new Metadata();
                            metadata.put(position, nodeMetadata);

                            long numVars = readUnsignedInt(nestedIn);
                            if (numVars > 0) {
                                nodeMetadata.variables = new HashMap<>();
                                for (int j = 0; j < numVars; j++) {
                                    String key = deserialiseString(nestedIn);
                                    String value = deserialiseLongString(nestedIn);
                                    nodeMetadata.variables.put(key, value);
                                }
                            }

                            String line;
                            while ((line = nestedIn.readLine()) != null) {
//                                System.out.println(line);
                                if (line.equals("EndInventory")) {
                                    break;
                                } else {
                                    Matcher matcher = LIST_START_PATTERN.matcher(line);
                                    if (matcher.matches()) {
                                        if (nodeMetadata.inventories == null) {
                                            nodeMetadata.inventories = new HashMap<>();
                                        }
                                        String name = matcher.group(1);
                                        int size = Integer.parseInt(matcher.group(2));
                                        Matcher widthMatcher = WIDTH_PATTERN.matcher(nestedIn.readLine());
                                        widthMatcher.find();
                                        int width = Integer.parseInt(widthMatcher.group(1));
                                        List<String> items = new ArrayList<>(size);
                                        for (int j = 0; j < size; j++) {
                                            items.add(nestedIn.readLine());
                                        }
                                        nodeMetadata.inventories.put(name, new Inventory(name, width, items));
                                    } else {
                                        throw new IllegalArgumentException("Illegal data in map block");
                                    }
                                    String endListLine = nestedIn.readLine();
                                    if (! endListLine.equals("EndInventoryList")) {
                                        throw new IllegalArgumentException("Illegal data in map block");
                                    }
                                }
                            }
                        }
                    }
                    // TODO
                }
                while (nestedIn.read() != -1) {
                    // Read until end of compressed stream to ensure checksum is
                    // properly skipped
                }
            }

            // Static objects
            int staticObjectVersion = in.read();
            if (staticObjectVersion != 0) {
                throw new IllegalArgumentException("Illegal data in map block");
            }
            int staticObjectCount = in.readUnsignedShort();
            if (staticObjectCount > 0) {
                staticObjects = new ArrayList<>(staticObjectCount);
                for (int i = 0; i < staticObjectCount; i++) {
                    int type = in.read();
                    int posXNodes = in.readInt(); // TODO: really some kind of floating point?
                    int posYNodes = in.readInt(); // TODO: really some kind of floating point?
                    int posZNodes = in.readInt(); // TODO: really some kind of floating point?
                    int objDataSize = in.readUnsignedShort();
                    byte[] objData = new byte[objDataSize];
                    in.readFully(objData);
                    staticObjects.add(new StaticObject(type, posXNodes, posYNodes, posZNodes, objData));
                }
            }

            timestamp = readUnsignedInt(in);

            // Name ID mappings
            int nameIdMappingVersion = in.read();
            if (nameIdMappingVersion != 0) {
                throw new IllegalArgumentException("Illegal data in map block");
            }
            int nameIdMappingCount = in.readUnsignedShort();
            if (nameIdMappingCount > 0) {
                nameIdMappings = new HashMap<>();
                for (int i = 0; i < nameIdMappingCount; i++) {
                    short id = in.readShort();
                    String name = deserialiseString(in);
                    nameIdMappings.put(id, name);
                }
            }
            // TODO normalise these to the standard mapping

            // Node timers
            int timerDataLength = in.read();
            if (timerDataLength != 10) {
                throw new IllegalArgumentException("Illegal data in map block");
            }
            int numOfTimers = in.readUnsignedShort();
            if (numOfTimers > 0) {
                nodeTimers = new HashMap<>();
                for (int i = 0; i < numOfTimers; i++) {
                    int position = in.readUnsignedShort();
                    int timeout = in.readInt(); // TODO: really some kind of floating point?
                    int elapsed = in.readInt(); // TODO: really some kind of floating point?
                    nodeTimers.put(position, new NodeTimer(position, timeout, elapsed));
                }
            }

            // Verify that there is no extraneous data
            if (in.read() != -1) {
                throw new IllegalArgumentException("Illegal data in map block");
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading map block data", e);
        }
    }

    private static void writeLine(OutputStream out, String str) throws IOException {
        out.write(str.getBytes("US-ASCII")); // TODO: assumes ASCII
        out.write('\n');
    }

    private static void dump(byte[] data) {
        for (byte b: data) {
            System.out.printf("%02x ", b);
        }
        System.out.println();
    }

    private static void dump(InputStream in) throws IOException {
        int b;
        while((b = in.read()) != -1) {
            System.out.printf("%02x ", b);
        }
        System.out.println();
    }

    final int x, y, z;
    final short[] content;
    final byte[] param1, param2;
    int version = SUPPORTED_VERSION, flags;
    long timestamp;
    Map<Integer, Metadata> metadata;
    List<StaticObject> staticObjects;
    Map<Short, String> nameIdMappings;
    Map<Integer, NodeTimer> nodeTimers;

    static final int FLAG_UNDERGROUND         = 0x01;
    static final int FLAG_DAY_NIGHT_DIFFERENT = 0x02;
    static final int FLAG_LIGHTING_EXPIRED    = 0x04;
    static final int FLAG_GENERATED           = 0x08;

    private static final int SUPPORTED_VERSION = 25;
    private static final Pattern LIST_START_PATTERN = Pattern.compile("List (\\w+) (\\d+)");
    private static final Pattern WIDTH_PATTERN = Pattern.compile("Width (\\d+)");
    private static final BlockMapping BLOCK_MAPPING = new BlockMapping();

    class Metadata {
        Map<String, String> variables;
        Map<String, Inventory> inventories;
    }

    class Inventory {
        Inventory(String name, int width, List<String> items) {
            this.name = name;
            this.width = width;
            this.items = items;
        }

        @Override
        public String toString() {
            return "{Inventory; name: " + name + "; width: " + width + "; items: " + items + "}";
        }

        String name;
        int width;
        List<String> items;
    }

    class StaticObject {
        StaticObject(int type, int x, int y, int z, byte[] data) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.data = data;
        }

        @Override
        public String toString() {
            return "{StaticObject; type: " + type + "; position: " + x + "," + y + "," + z + "; data: " + data.length + " bytes}";
        }

        int type, x, y, z;
        byte[] data;
    }

    class NodeTimer {
        NodeTimer(int position, int timeout, int elapsed) {
            this.position = position;
            this.timeout = timeout;
            this.elapsed = elapsed;
        }

        @Override
        public String toString() {
            return "{NodeTimer; position: " + position + "; timeout: " + timeout + "; elapsed: " + elapsed + "}";
        }

        int position, timeout, elapsed;
    }
}
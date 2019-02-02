package org.pepsoft.worldpainter.minetest;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.World2;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.pepsoft.worldpainter.minetest.Constants.ATTRIBUTE_BLOCK_ID_MAPPING;

/**
 * A persistent three way mapping between WorldPainter/Minecraft
 * {@link Material materials}, Minetest dynamic content ids and Minetest node
 * names.
 *
 * <p>This implementation simply uses the material's combined index, which is
 * already a u16, as the content id and vice versa. This will do for generating
 * new maps with WorldPainter, but will have to be modified for importing
 * existing Minetest maps.
 *
 * <p>The mapping to node names <em>is</em> dynamic. For unmapped materials, the
 * node name <code>wp:mat_<em>index</em></code> will be used, where
 * <em>index</em> is the material's {@link Material#index combined index}.
 *
 * <p>Created by Pepijn Schmitz on 15-02-17.
 */
public final class BlockMapping implements Serializable {
    public BlockMapping() {
        compileMapping();
    }

    public BlockMapping(Map<Object, String> mapping) {
        this.mapping.putAll(mapping);
        compileMapping();
    }

    /**
     * Get the Minetest content ID corresponding to a WorldPainter/Minecraft
     * {@link Material material}.
     *
     * @param material The material for which to get the content ID.
     * @return The content ID to use for the specified material.
     */
    public synchronized int getContentId(Material material) {
        return material.index;
    }

    /**
     * Get the WorldPainter/Minecraft {@link Material material} corresponding to
     * a Minetest content ID.
     *
     * @param contentId The content ID for which to get the material.
     * @return The material to use for the specified content ID.
     */
    public synchronized Material getMaterial(int contentId) {
        return Material.getByCombinedIndex(contentId);
    }

    /**
     * Get the Minetest node name corresponding to a Minetest content ID.
     *
     * <p>This always returns a value. If the content ID is not explicitly
     * mapped to a node name, this will return
     * <code>wp:mat_<em>xxxx</em></code>, where <em>xxxx</em> is the content ID
     * as a four digit zero padded hexadecimal number.
     *
     * @param contentId The content ID for which to get the node name.
     * @return The Minetest node name to use for the specified content ID.
     */
    public synchronized String getNodeName(int contentId) {
        return (compiledMapping[contentId] == null) ? (String.format("wp:mat_%04x", contentId)) : compiledMapping[contentId];
    }

    /**
     * Get the WorldPainter/Minecraft {@link Material material} corresponding to
     * a Minetest node name.
     *
     * <p>If more than one material is mapped to the same node name, the first
     * one will be returned.
     *
     * <p>This method can optionally automatically add a mapping for node names
     * for which a mapping does not yet exist. It will select an as yet unmapped
     * material starting with 1024:0.
     *
     * @param nodeName The node name for which to get the material.
     * @param autoMapUnknownNames Whether or not to automatically add a mapping
     *                            for the node name if one does not already
     *                            exist.
     * @return The material to use for the specified node name, or
     * <code>null</code> if the node name is not mapped and
     * <code>autoMapUnknownNames</code> was <code>false</code>.
     */
    public synchronized Material getMaterial(String nodeName, boolean autoMapUnknownNames) {
        Material[] materials = compiledReverseMapping.get(nodeName);
        if (materials != null) {
            return materials[0];
        }
        if (autoMapUnknownNames) {
            // Find the next available material to map the unknown node name to
            // TODO: this will hang if all 4096 block ids ever get mapped; that's unlikely to happen any time soon but we really should deal with it
            while (mapping.containsKey(nextUnknownNodeNameMaterial) || mapping.containsKey(nextUnknownNodeNameMaterial.blockType) || DEFAULT_MAPPING.containsKey(nextUnknownNodeNameMaterial) || DEFAULT_MAPPING.containsKey(nextUnknownNodeNameMaterial.blockType)) {
                nextUnknownNodeNameMaterial = Material.getByCombinedIndex(nextUnknownNodeNameMaterial.index + 1);
            }
            mapping.put(nextUnknownNodeNameMaterial, nodeName);
            compiledReverseMapping.put(nodeName, new Material[] {nextUnknownNodeNameMaterial});
            compiledMapping[nextUnknownNodeNameMaterial.index] = nodeName;
            return nextUnknownNodeNameMaterial;
        } else {
            return null;
        }
    }

    /**
     * Get all WorldPainter/Minecraft {@link Material materials} corresponding
     * to a Minetest node name.
     *
     * @param nodeName The node name for which to get the material.
     * @return The materials mapped to the specified node name, or
     * <code>null</code> if the node name is not mapped.
     */
    public synchronized Material[] getAllMaterials(String nodeName) {
        return compiledReverseMapping.get(nodeName);
    }

    /**
     * Get the additional node name mapping. The keys are either Minecraft block
     * ids (as {@link Integer}s) or {@link Material}s; the values are Minetest
     * node name strings. For a block id, all data values of that block will be
     * mapped on the specified node name. If a block id is mapped for which one
     * or more materials are also present, the materials will take precedence
     * for their corresponding data values.
     *
     * <p>Note that these mappings are in addition to the default mapping, which
     * already maps most of the common blocks between Minecraft and the
     * minetest_game subgame. This mapping may override the default mapping.
     *
     * @return The additional node name mapping. (Additional to the default
     * mapping.)
     */
    public synchronized Map<Object, String> getMapping() {
        return mapping;
    }

    /**
     * Set the additional node name mapping. See {@link #getMapping()} for
     * details.
     *
     * @param mapping The new additional node name mapping.
     */
    public synchronized void setMapping(Map<Object, String> mapping) {
        this.mapping.clear();
        this.mapping.putAll(mapping);
        compileMapping();
    }

    private void compileMapping() {
        compiledMapping = new String[65536];
        for (int matIndex = 0; matIndex < 65536; matIndex++) {
            Material material = Material.getByCombinedIndex(matIndex);
            if (mapping.containsKey(material)) {
                compiledMapping[matIndex] = mapping.get(material);
            } else if (mapping.containsKey(matIndex >> 4)) {
                compiledMapping[matIndex] = mapping.get(matIndex >> 4);
            } else if (DEFAULT_MAPPING.containsKey(material)) {
                compiledMapping[matIndex] = DEFAULT_MAPPING.get(material);
            } else if (DEFAULT_MAPPING.containsKey(matIndex >> 4)) {
                compiledMapping[matIndex] = DEFAULT_MAPPING.get(matIndex >> 4);
            }
        }
        Map<String, List<Material>> tmpMap = new HashMap<>();
        for (int matIndex = 0; matIndex < 65536; matIndex++) {
            String nodeName = compiledMapping[matIndex];
            if (nodeName != null) {
                List<Material> mappedMats = tmpMap.get(nodeName);
                if (mappedMats == null) {
                    tmpMap.put(nodeName, Lists.newArrayList(Material.getByCombinedIndex(matIndex)));
                } else {
                    mappedMats.add(Material.getByCombinedIndex(matIndex));
                }
            }
        }
        compiledReverseMapping = tmpMap.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> {
                List<Material> list = e.getValue();
                return list.toArray(new Material[list.size()]);
            }));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        compileMapping();
        nextUnknownNodeNameMaterial = Material.get(1024, 0);
    }

    /**
     * Get the persistent, shared block mapping for a dimension, initialising
     * one if it does not yet exist.
     *
     * @param dimension The dimension for which to get the mapping.
     * @return The persistent, shared block mapping for the dimension.
     */
    static BlockMapping get(Dimension dimension) {
        synchronized (dimension) {
            Map<Object, String> mapping;
            World2 world = dimension.getWorld();
            synchronized (world) {
                mapping = world.getAttribute(ATTRIBUTE_BLOCK_ID_MAPPING);
                if (mapping == null) {
                    mapping = new HashMap<>();
                    world.setAttribute(ATTRIBUTE_BLOCK_ID_MAPPING, mapping);
                }
            }
            BlockMapping blockMapping = LOADED_MAPPINGS.get(mapping);
            if (blockMapping == null) {
                blockMapping = new BlockMapping(mapping);
                LOADED_MAPPINGS.put(mapping, blockMapping);
            }
            return blockMapping;
        }
    }

    public static void main(String[] args) {
        // Do nothing (but test the class initializer)
    }

    private static int decodeIdentifier(String id) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return findMinecraftConstant(id);
        }
    }

    private static int findMinecraftConstant(String name) {
        try {
            Field field = Constants.class.getField(name);
            return field.getInt(null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Unknown block or data name encountered: \"" + name + "\"");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final Map<Object, String> mapping = new HashMap<>();
    private transient String[] compiledMapping;
    private transient Map<String, Material[]> compiledReverseMapping;
    private transient Material nextUnknownNodeNameMaterial = Material.get(1024, 0);

    private static final Map<Map<Object, String>, BlockMapping> LOADED_MAPPINGS = new MapMaker().weakKeys().makeMap();
    private static final Map<Object, String> DEFAULT_MAPPING;

    static {
        try {
            Set<String> allNodeNames = new HashSet<>();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(BlockMapping.class.getResourceAsStream("/minetest_game_blocks.txt"), "US-ASCII"))) {
                String line;
                while ((line = in.readLine()) != null) {
                    allNodeNames.add(line);
                }
            }
            Map<Object, String> map = new HashMap<>();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(BlockMapping.class.getResourceAsStream("/default_mapping.csv"), "US-ASCII"))) {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] tokens = line.split(",");
                    if (tokens.length != 2) {
                        throw new RuntimeException("Wrong number of tokens encountered on line: \"" + line + "\"");
                    }
                    String blockKey = tokens[0].trim();
                    String nodeName = tokens[1].trim();
                    if (nodeName.isEmpty()) {
                        throw new RuntimeException("Empty node name encountered on line: \"" + line + "\"");
                    } else if (! allNodeNames.contains(nodeName)) {
                        throw new RuntimeException("Unknown node name encountered on line: \"" + line + "\"");
                    }
                    int p = blockKey.indexOf(':');
                    if (p != -1) {
                        int blockId = decodeIdentifier(blockKey.substring(0, p));
                        int blockData = decodeIdentifier(blockKey.substring(p + 1));
                        map.put(Material.get(blockId, blockData), nodeName);
                    } else {
                        int blockId = decodeIdentifier(blockKey);
                        map.put(blockId, nodeName);
                    }
                }
            }
            DEFAULT_MAPPING = Collections.unmodifiableMap(map);
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("VM does not support mandatory encoding US-ASCII");
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading Minetest block names or mapping from classpath", e);
        }
    }

    private static final long serialVersionUID = 1L;
}
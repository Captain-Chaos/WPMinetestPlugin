package org.pepsoft.worldpainter.minetest;

import org.pepsoft.util.AttributeKey;

import java.util.Map;

/**
 * Created by Pepijn on 10-3-2017.
 */
class Constants {
    private Constants() {
        // Prevent instantiation
    }

    static final AttributeKey<Map<Object, String>> ATTRIBUTE_BLOCK_ID_MAPPING = new AttributeKey<>("org.pepsoft.minetest.blockIdMapping");
}

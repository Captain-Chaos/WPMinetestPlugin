package org.pepsoft.worldpainter.minetest.lua;

import com.google.common.collect.ImmutableMap;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LibFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by Pepijn Schmitz on 22-02-17.
 */
public class MinetestLibrary extends LuaTable {
    @Override
    public LuaValue get(LuaValue key) {
        if (logger.isTraceEnabled()) {
            logger.trace("get({})", key);
        }
        String functionName = key.checkjstring();
        if (FUNCTIONS.containsKey(functionName)) {
            return FUNCTIONS.get(functionName);
        } else {
            return Dummy.INSTANCE;
        }
    }

    private static final Map<String, LuaValue> FUNCTIONS = ImmutableMap.<String, LuaValue>builder()
        .put("get_modpath", new GetModpath())
        .put("item_eat", new ItemEat())
        .put("registered_entities", tableOf())
        .put("get_mapgen_setting", new GetMapgenSetting())
        .put("registered_nodes", new RegisteredNodes())
        .put("register_node", new RegisterNode())
        .build();
    private static final Logger logger = LoggerFactory.getLogger(MinetestLibrary.class);
}
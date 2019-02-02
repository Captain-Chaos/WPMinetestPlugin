package org.pepsoft.worldpainter.minetest.lua;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Pepijn Schmitz on 22-02-17.
 */
public class RegisterNode extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue nameLua, LuaValue nodeDefinitionLua) {
        String name = nameLua.checkjstring();
        logger.info("Node {} registered: {}", name, toMap((LuaTable) nodeDefinitionLua));
        return null;
    }

    private Map<?, ?> toMap(LuaTable luaTable) {
        return new AbstractMap<Object, Object>() {
            @Override
            public Set<Entry<Object, Object>> entrySet() {
                return null;
            }
        };
    }

    private static final Logger logger = LoggerFactory.getLogger(MinetestLibrary.class);
}
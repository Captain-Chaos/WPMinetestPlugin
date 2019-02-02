package org.pepsoft.worldpainter.minetest.lua;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

/**
 * Created by Pepijn Schmitz on 22-02-17.
 */
public class Dummy extends OneArgFunction {
    @Override
    public LuaValue call(LuaValue arg) {
        return null;
    }

    public static final Dummy INSTANCE = new Dummy();
}
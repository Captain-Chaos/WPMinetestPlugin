package org.pepsoft.worldpainter.minetest.lua;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.pepsoft.worldpainter.Configuration;

import java.io.File;

/**
 * Created by Pepijn Schmitz on 22-02-17.
 */
public class GetModpath extends OneArgFunction {
    public LuaValue call(LuaValue mod) {
        File pluginDir = new File(Configuration.getConfigDir(), "plugins");
        File modDir = new File(pluginDir, mod.checkjstring());
        return LuaValue.valueOf(modDir.getAbsolutePath());
    }
}
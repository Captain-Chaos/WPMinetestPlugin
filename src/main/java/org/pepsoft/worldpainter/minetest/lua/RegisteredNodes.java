package org.pepsoft.worldpainter.minetest.lua;

import org.luaj.vm2.LuaNil;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

/**
 * Created by Pepijn Schmitz on 22-02-17.
 */
public class RegisteredNodes extends LuaTable {
    public RegisteredNodes() {
        set("default:water_source", tableOf());
        set("default:lava_source", tableOf());
    }
}

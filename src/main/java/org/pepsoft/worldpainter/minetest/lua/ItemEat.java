package org.pepsoft.worldpainter.minetest.lua;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

/**
 * Created by Pepijn Schmitz on 22-02-17.
 */
public class ItemEat extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        return Dummy.INSTANCE;
    }
}

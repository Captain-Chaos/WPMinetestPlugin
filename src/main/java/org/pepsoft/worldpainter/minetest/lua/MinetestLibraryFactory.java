package org.pepsoft.worldpainter.minetest.lua;

import org.luaj.vm2.LuaValue;

import static org.luaj.vm2.LuaValue.tableOf;

/**
 * Created by Pepijn Schmitz on 22-02-17.
 */
public class MinetestLibraryFactory {
    public static LuaValue createLibrary() {
        LuaValue library = tableOf();
        library.set("get_modpath", new GetModpath());
        library.set("register_entity", new Dummy());
        library.set("register_abm", new Dummy());
        library.set("register_node", new Dummy());
        library.set("register_tool", new Dummy());
        library.set("register_craftitem", new Dummy());
        library.set("register_alias", new Dummy());
        library.set("register_craft", new Dummy());
        library.set("register_ore", new Dummy());
        library.set("register_decoration", new Dummy());
        library.set("override_item", new Dummy());
        library.set("clear_registered_ores", new Dummy());
        library.set("clear_registered_decorations", new Dummy());
        library.set("register_globalstep", new Dummy());
        library.set("register_on_shutdown", new Dummy());
        library.set("register_on_placenode", new Dummy());
        library.set("register_on_dignode", new Dummy());
        library.set("register_on_punchnode", new Dummy());
        library.set("register_on_generated", new Dummy());
        library.set("register_on_newplayer", new Dummy());
        library.set("register_on_dieplayer", new Dummy());
        library.set("register_on_punchplayer", new Dummy());
        library.set("register_on_player_hpchange", new Dummy());
        library.set("register_on_respawnplayer", new Dummy());
        library.set("register_on_prejoinplayer", new Dummy());
        library.set("register_on_joinplayer", new Dummy());
        library.set("register_on_leaveplayer", new Dummy());
        library.set("register_on_cheat", new Dummy());
        library.set("register_on_chat_message", new Dummy());
        library.set("register_on_player_receive_fields", new Dummy());
        library.set("register_on_craft", new Dummy());
        library.set("register_craft_predict", new Dummy());
        library.set("register_on_protection_violation", new Dummy());
        library.set("register_on_item_eat", new Dummy());
        library.set("register_lbm", new Dummy());
        return library;
    }
}
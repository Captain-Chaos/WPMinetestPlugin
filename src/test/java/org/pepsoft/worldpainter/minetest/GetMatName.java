package org.pepsoft.worldpainter.minetest;

import org.pepsoft.minecraft.Material;

/**
 * Created by Pepijn on 24-2-2017.
 */
public class GetMatName {
    public static void main(String[] args) {
        String name = args[0];
        if (name.startsWith("mat_")) {
            name = name.substring(4);
        }
        int index = Integer.parseInt(name, 16);
        System.out.println(Material.getByCombinedIndex(index));
    }
}
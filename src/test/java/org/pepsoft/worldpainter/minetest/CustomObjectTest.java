package org.pepsoft.worldpainter.minetest;

import java.io.File;
import java.io.IOException;

/**
 * Created by Pepijn on 30-6-2017.
 */
public class CustomObjectTest {
    public static void main(String[] args) throws IOException {
        Schematic.load(new File(args[0]));
    }
}
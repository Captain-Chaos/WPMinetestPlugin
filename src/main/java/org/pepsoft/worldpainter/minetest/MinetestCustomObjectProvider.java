package org.pepsoft.worldpainter.minetest;

import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.CustomObjectProvider;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Created by Pepijn on 9-3-2017.
 */
public class MinetestCustomObjectProvider extends AbstractPlugin implements CustomObjectProvider {
    public MinetestCustomObjectProvider() {
        super("MinetestCustomObjects", "1.0.0");
    }

    @Override
    public List<String> getSupportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public WPObject loadObject(File file) throws IOException {
        if (file.getName().toLowerCase().endsWith(".mts")) {
            return Schematic.load(file);
        } else {
            throw new IllegalArgumentException("Not a supported filename extension: \"" + file.getName() + "\"");
        }
    }

    @Override
    public List<String> getKeys() {
        return TYPES;
    }

    private static final List<String> TYPES = singletonList(Schematic.class.getName());
    private static final List<String> EXTENSIONS = singletonList("mts");
}
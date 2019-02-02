package org.pepsoft.worldpainter.minetest;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.util.SystemUtils;
import org.pepsoft.util.XDG;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.PostProcessor;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;
import org.pepsoft.worldpainter.minetest.lua.MinetestLibrary;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.BlockBasedPlatformProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.GameType.CREATIVE;
import static org.pepsoft.worldpainter.GameType.SURVIVAL;
import static org.pepsoft.worldpainter.Generator.DEFAULT;
import static org.pepsoft.worldpainter.Platform.Capability.BLOCK_BASED;

/**
 * Created by Pepijn on 12-2-2017.
 */
public class MinetestPlatformProvider extends AbstractPlugin implements BlockBasedPlatformProvider {
    public MinetestPlatformProvider() {
        super("MinetestPlatform", "1.0.0platfo");
        init();
    }

    // PlatformProvider

    @Override
    public List<Platform> getKeys() {
        return singletonList(MINETEST);
    }

    @Override
    public Chunk createChunk(Platform platform, int x, int z, int maxHeight) {
        if (! platform.equals(MINETEST)) {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
        return new MinetestChunk(x, z);
    }

    @Override
    public ChunkStore getChunkStore(Platform platform, File worldDir, int dimension) {
        if (! platform.equals(MINETEST)) {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
        return new MinetestChunkStore(worldDir, dimension);
    }

    @Override
    public WorldExporter getExporter(World2 world) {
        Platform platform = world.getPlatform();
        if (! platform.equals(MINETEST)) {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
        return new MinetestWorldExporter(world);
    }

    @Override
    public File getDefaultExportDir(Platform platform) {
        if (! platform.equals(MINETEST)) {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
        File potentialDir;
        String minetestWorldPath = System.getenv("MINETEST_WORLD_PATH");
        if (minetestWorldPath != null) {
            for (String path: minetestWorldPath.split(File.pathSeparator)) {
                potentialDir = new File(path);
                if (potentialDir.isDirectory()) {
                    return potentialDir;
                }
            }
        }
        switch (SystemUtils.getOS()) {
            case WINDOWS:
                String appData = System.getenv("APPDATA");
                if (appData != null) {
                    potentialDir = new File(appData, "minetest/worlds");
                    if (potentialDir.isDirectory()) {
                        return potentialDir;
                    }
                }
                break;
            case MAC:
                potentialDir = new File(System.getProperty("user.home"), "Library/Application Support/minetest/worlds");
                if (potentialDir.isDirectory()) {
                    return potentialDir;
                }
                break;
        }
        potentialDir = new File(XDG.XDG_DATA_HOME, "minetest/worlds");
        if (potentialDir.isDirectory()) {
            return potentialDir;
        }
        potentialDir = new File(XDG.HOME, ".minetest/worlds");
        if (potentialDir.isDirectory()) {
            return potentialDir;
        }
        return null;
    }

    @Override
    public PostProcessor getPostProcessor(Platform platform) {
        if (! platform.equals(MINETEST)) {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
        return new MinetestPostProcessor();
    }

    @Override
    public MapRecognizer getMapRecognizer() {
        return new MinetestMapRecognizer();
    }

    private void init() {
        // Look for plugins and load them in the backgroun
        new Thread("Minetest Mod Initialiser") {
            {
                // Not much point in continuing to look for plugins if
                // WorldPainter is shutting down
                setDaemon(true);
            }

            @SuppressWarnings("ConstantConditions") // Can't realistically happen since we just checked that the directory exists
            @Override
            public void run() {
                File pluginDir = new File(Configuration.getConfigDir(), "plugins");
                if (pluginDir.isDirectory()) {
                    for (File subDir: pluginDir.listFiles(File::isDirectory)) {
                        File initFile = new File(subDir, "init.lua");
                        if (initFile.isFile()) {
                            // Presumable Minetest mod found
                            try {
                                Globals globals = JsePlatform.standardGlobals();
                                globals.set("minetest", new MinetestLibrary());
                                LuaValue chunk = globals.loadfile(initFile.getAbsolutePath());
                                chunk.call();
                            } catch (RuntimeException e) {
                                logger.error("Exception while initialising Minetest mod " + subDir.getName(), e);
                            }
                        }
                    }
                }
            }
        }.start();
    }

    static final Platform MINETEST = new Platform(
            "org.pepsoft.minetest",
            "Minetest",
            256, 256, 32768,
            -30912, 30927, -30912, 30927,
            Arrays.asList(SURVIVAL, CREATIVE),
            singletonList(DEFAULT),
            singletonList(DIM_NORMAL),
            EnumSet.of(BLOCK_BASED));

    private static final Logger logger = LoggerFactory.getLogger(MinetestPlatformProvider.class);
}
package org.pepsoft.worldpainter.minetest;

import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.exporting.AbstractWorldExporter;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.pepsoft.worldpainter.vo.EventVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.pepsoft.worldpainter.Constants.*;

/**
 * Created by Pepijn on 12-2-2017.
 */
public class MinetestWorldExporter extends AbstractWorldExporter {
    protected MinetestWorldExporter(World2 world) {
        super(world);
    }

    @Override
    public Map<Integer, ChunkFactory.Stats> export(File baseDir, String name, File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        Dimension dimension;
        int selectedDimension;
        if ((selectedDimensions != null) && (selectedDimensions.size() == 1)) {
            selectedDimension = world.getDimensionsToExport().iterator().next();
            dimension = world.getDimension(selectedDimension);
        } else if (world.getDimensions().length == 1) {
            dimension = world.getDimensions()[0];
            selectedDimension = dimension.getDim();
        } else {
            throw new IllegalArgumentException("Minetest exporter only supports exporting one dimension");
        }

        // Get the block ID to name mapping
        BlockMapping blockIdMapping = BlockMapping.get(dimension);

        // Backup existing level
        File worldDir = new File(baseDir, FileUtils.sanitiseName(name));
        logger.info("Exporting world " + world.getName() + " to map at " + worldDir);
        if (worldDir.isDirectory()) {
            logger.info("Directory already exists; backing up to " + backupDir);
            if (! worldDir.renameTo(backupDir)) {
                throw new FileInUseException("Could not move " + worldDir + " to " + backupDir);
            }
        }

        // Record start of export
        long start = System.currentTimeMillis();

        parallelExportRegions(dimension, MinetestPlatformProvider.MINETEST, worldDir, progressReceiver);

        // Write metadata
        File file = new File(worldDir, "world.mt");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(("gameid = minetest\n" +
                "backend = sqlite3\n" +
                "creative_mode = " + ((world.getGameType() == GameType.SURVIVAL) ? "false" : "true") + "\n" +
                "enable_damage = " + ((world.getGameType() == GameType.SURVIVAL) ? "true" : "false")).getBytes());
        }
        file = new File(worldDir, "map_meta.txt");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(("chunksize = 1\n" +
                "mg_name = singlenode\n" +
                "seed = " + dimension.getMinecraftSeed() + "\n" +
                "water_level = " + ((HeightMapTileFactory) dimension.getTileFactory()).getWaterHeight() + "\n" +
                "[end_of_params]").getBytes());
        }

        // Mark the dimension as dirty just in case the block mapping has changed and should be saved
        // TODO: make this more sophisticated
        dimension.changed();

        // Record the export in the world history
        if (selectedTiles == null) {
            world.addHistoryEntry(HistoryEntry.WORLD_EXPORTED_FULL, name, worldDir);
        } else {
            world.addHistoryEntry(HistoryEntry.WORLD_EXPORTED_PARTIAL, name, worldDir, dimension.getName());
        }

        // Log an event
        Configuration config = Configuration.getInstance();
        if (config != null) {
            EventVO event = new EventVO(EVENT_KEY_ACTION_EXPORT_WORLD).duration(System.currentTimeMillis() - start);
            event.setAttribute(EventVO.ATTRIBUTE_TIMESTAMP, new Date(start));
            event.setAttribute(ATTRIBUTE_KEY_MAX_HEIGHT, world.getMaxHeight());
            event.setAttribute(ATTRIBUTE_KEY_PLATFORM, world.getPlatform().displayName);
            event.setAttribute(ATTRIBUTE_KEY_MAP_FEATURES, world.isMapFeatures());
            event.setAttribute(ATTRIBUTE_KEY_GAME_TYPE_NAME, world.getGameType().name());
            event.setAttribute(ATTRIBUTE_KEY_ALLOW_CHEATS, world.isAllowCheats());
            event.setAttribute(ATTRIBUTE_KEY_GENERATOR, world.getGenerator().name());
            event.setAttribute(ATTRIBUTE_KEY_TILES, dimension.getTiles().size());
            logLayers(dimension, event, "");
            event.setAttribute(ATTRIBUTE_KEY_EXPORTED_DIMENSION, selectedDimension);
            if (selectedTiles != null) {
                event.setAttribute(ATTRIBUTE_KEY_EXPORTED_DIMENSION_TILES, selectedTiles.size());
            }
            if (world.getImportedFrom() != null) {
                event.setAttribute(ATTRIBUTE_KEY_IMPORTED_WORLD, true);
            }
            config.logEvent(event);
        }

        return new HashMap<>();
    }

    private static final Logger logger = LoggerFactory.getLogger(MinetestWorldExporter.class);
}
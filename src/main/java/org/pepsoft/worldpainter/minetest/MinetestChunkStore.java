package org.pepsoft.worldpainter.minetest;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.util.Pair;
import org.pepsoft.worldpainter.Constants;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.tmatesoft.sqljet.core.SqlJetTransactionMode.READ_ONLY;
import static org.tmatesoft.sqljet.core.SqlJetTransactionMode.WRITE;
import static org.tmatesoft.sqljet.core.schema.SqlJetConflictAction.REPLACE;

// TODO: I'm sure this can all be done massively more efficiently; this is just a first correct but naive implementation

/**
 * This platform maintains the invariant that the map block at y == 0 ALWAYS
 * exists for columns in which any map blocks are present, and that any blocks
 * above and below it are contiguous; that is: there are no gaps in the column
 * other than below the lowest and above the highest map block.
 *
 * <p>Created by Pepijn on 12-2-2017.
 */
public class MinetestChunkStore implements ChunkStore {
    public MinetestChunkStore(File worldDir, int dimension) {
        if (dimension != Constants.DIM_NORMAL) {
            throw new IllegalArgumentException("Dimensions other than Surface are not (yet) supported by the Minetest platform");
        }
        try {
            this.worldDir = worldDir.getCanonicalFile();
            synchronized (OPEN_DBS) {
                Descriptor desc = OPEN_DBS.get(worldDir);
                if (desc == null) {
                    // Not yet open
                    File dbFile = new File(worldDir, "map.sqlite");
                    if (dbFile.isFile()) {
                        // Existing DB
                        db = SqlJetDb.open(dbFile, true);
                    } else {
                        // New DB
                        db = SqlJetDb.open(dbFile, true);
//                        db.getOptions().setAutovacuum(true); // From SqlJet tutorial; TODO: appropriate for Minetest?
                        db.beginTransaction(WRITE);
                        try {
                            // From Minetest world_format.txt:
                            db.createTable("CREATE TABLE `blocks` (`pos` INT NOT NULL PRIMARY KEY,`data` BLOB);");
                        } finally {
                            db.commit();
                        }
                    }
                    OPEN_DBS.put(worldDir, new Descriptor(db, 1));
                } else {
                    // Already open; increase usage count
                    db = desc.getDb();
                    OPEN_DBS.put(worldDir, new Descriptor(db, desc.getCount() + 1));
                }
            }
            blocks = db.getTable("blocks");
        } catch (IOException e) {
            throw new RuntimeException("I/O error while making worldDir canonical", e);
        } catch (SqlJetException e) {
            throw new RuntimeException("SqlJet error while opening database", e);
        }
    }

    @Override
    public void saveChunk(Chunk chunk) {
        // TODO: this assumes we're creating this map from scratch and there not yet any map blocks there!
//        System.out.println("Saving chunk @ " + chunk.getxPos() + "," + chunk.getzPos() + " which has map blocks from y = " + ((MinetestChunk) chunk).lowestMapBlockY + " through " + ((((MinetestChunk) chunk).lowestMapBlockY) + ((MinetestChunk) chunk).mapBlocks.length - 1));
        synchronized (db) {
            try {
                if (!inTransaction.get().get()) {
                    db.beginTransaction(WRITE);
                }
                try {
                    //                int highestY = MIN_Y;
                    for (MapBlock mapBlock : ((MinetestChunk) chunk).mapBlocks) {
                        if ((mapBlock.y != 0) && mapBlock.isAllAir()) {
                            // Skip all air map blocks
                            continue;
                        }
                        // TODO: this allows us to update records, but it will not *remove* records for which the chunk no longer contains mapblocks:
                        blocks.insertOr(REPLACE, getKey(mapBlock.x, mapBlock.y, mapBlock.z), mapBlock.getData());
                        //                    if (mapBlock.y > highestY) {
                        //                        highestY = mapBlock.y;
                        //                    }
                    }
                    // Top up with empty map blocks to prevent Minetest from generating them
                    //                for (int y = highestY + 1; y <= MAX_Y; y++) {
                    //                    blocks.insert(getKey(chunk.getxPos(), y, chunk.getzPos()), EMPTY_MAP_BLOCK);
                    //                }
                } finally {
                    if (!inTransaction.get().get()) {
                        db.commit();
                    }
                }
            } catch (SqlJetException e) {
                throw new RuntimeException("SqlJet error while writing to database", e);
            }
        }
    }

    @Override
    public void doInTransaction(Runnable task) {
        synchronized (db) {
            try {
                if (!inTransaction.get().compareAndSet(false, true)) {
                    throw new IllegalStateException("Already in transaction");
                }
                try {
                    db.beginTransaction(WRITE);
                    try {
                        task.run();
                        db.commit();
                    } catch (Throwable e) {
                        db.rollback();
                        if (e instanceof RuntimeException) {
                            throw (RuntimeException) e;
                        } else if (e instanceof Error) {
                            throw (Error) e;
                        } else {
                            throw (SqlJetException) e;
                        }
                    }
                } finally {
                    inTransaction.get().set(false);
                }
            } catch (SqlJetException e) {
                throw new RuntimeException("SqlJet error while writing to database", e);
            }
        }
    }

    @Override
    public void flush() {
        // Do nothing
    }

    @Override
    public boolean isChunkPresent(int x, int z) {
        synchronized (db) {
            try {
                db.beginTransaction(READ_ONLY);
                try {
                    ISqlJetCursor cursor = blocks.lookup(null, getKey(x, -z, 0));
                    try {
                        return !cursor.eof();
                    } finally {
                        cursor.close();
                    }
                } finally {
                    db.commit();
                }
            } catch (SqlJetException e) {
                throw new RuntimeException("SqlJet error while reading from database", e);
            }
        }
    }

    @Override
    public Chunk getChunk(int x, int z) {
        synchronized (db) {
            try {
                db.beginTransaction(READ_ONLY);
                try {
                    List<MapBlock> mapBlocks = null;
                    ISqlJetCursor cursor = blocks.lookup(null, getKey(x, 0, -z));
                    try {
                        if (!cursor.eof()) {
                            mapBlocks = new ArrayList<>();
                            mapBlocks.add(new MapBlock(x, 0, z, cursor.getBlobAsArray("data")));
                        }
                    } finally {
                        cursor.close();
                    }
                    if (!mapBlocks.isEmpty()) {
                        for (int y = 1; y < 2048; y++) {
                            cursor = blocks.lookup(null, getKey(x, y, -z));
                            try {
                                if (!cursor.eof()) {
                                    mapBlocks.add(new MapBlock(x, y, -z, cursor.getBlobAsArray("data")));
                                } else {
                                    break;
                                }
                            } finally {
                                cursor.close();
                            }
                        }
                        for (int y = -1; y >= -2048; y--) {
                            cursor = blocks.lookup(null, getKey(x, y, -z));
                            try {
                                if (!cursor.eof()) {
                                    mapBlocks.add(0, new MapBlock(x, y, -z, cursor.getBlobAsArray("data"))); // TODO: this would perform better with a linked list
                                } else {
                                    break;
                                }
                            } finally {
                                cursor.close();
                            }
                        }
                        return new MinetestChunk(x, z, mapBlocks.toArray(new MapBlock[mapBlocks.size()]));
                    } else {
                        return null;
                    }
                } finally {
                    db.commit();
                }
            } catch (SqlJetException e) {
                throw new RuntimeException("SqlJet error while reading from database", e);
            }
        }
    }

    @Override
    public Chunk getChunkForEditing(int x, int z) {
        return getChunk(x, z);
    }

    @Override
    public void close() {
        synchronized (db) {
            flush();
            synchronized (OPEN_DBS) {
                Descriptor desc = OPEN_DBS.get(worldDir);
                if (desc.getCount() == 1) {
                    // We're the last user; close the database
                    try {
                        db.close();
                    } catch (SqlJetException e) {
                        throw new RuntimeException("SqlJet error closing the database", e);
                    }
                    OPEN_DBS.remove(worldDir);
                } else {
                    // Decrement usage count
                    OPEN_DBS.put(worldDir, new Descriptor(db, desc.getCount() - 1));
                }
            }
        }
    }

    private long getKey(int x, int y, int z) {
        return z * 16777216L + y * 4096 + x;
//        return ((long) (z & 0xfff) << 24) | ((y & 0xfff) << 12) | (x & 0xfff); // TODO: is this right?
    }

    private final File worldDir;
    private final SqlJetDb db;
    private final ISqlJetTable blocks;
    private final ThreadLocal<AtomicBoolean> inTransaction = ThreadLocal.withInitial(AtomicBoolean::new);

    private static final Map<File, Descriptor> OPEN_DBS = new HashMap<>();
//    private static final byte[] EMPTY_MAP_BLOCK = new MapBlock(0, 0, 0).getData();
//    private static final int MIN_Y = 0, MAX_Y = 15;

    static class Descriptor extends Pair<SqlJetDb, Integer> {
        public Descriptor(SqlJetDb value1, Integer value2) {
            super(value1, value2);
        }

        public SqlJetDb getDb() {
            return getValue1();
        }

        public Integer getCount() {
            return getValue2();
        }
    }
}
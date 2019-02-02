package org.pepsoft.worldpainter.minetest;

import org.pepsoft.minecraft.mapexplorer.DirectoryNode;
import org.pepsoft.minecraft.mapexplorer.FileSystemNode;
import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;
import org.pepsoft.worldpainter.mapexplorer.Node;
import org.pepsoft.util.IconUtils;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import javax.swing.*;
import java.io.File;
import java.util.*;

/**
 * Created by Pepijn on 25-2-2017.
 */
public class MinetestMapRecognizer implements MapRecognizer {
    @Override
    public boolean isMap(File dir) {
        File dbFile = new File(dir, "map.sqlite");
        return dbFile.isFile();
    }

    @Override
    public synchronized Node getMapNode(File mapDir) {
        if (MAP_NODES.containsKey(mapDir)) {
            return MAP_NODES.get(mapDir);
        } else {
            MinetestMapNode mapNode = new MinetestMapNode(mapDir);
            MAP_NODES.put(mapDir, mapNode);
            return mapNode;
        }
    }

    public static class MinetestMapNode extends DirectoryNode {
        public MinetestMapNode(File mapDir) {
            super(mapDir, true);
        }

        @Override
        public Icon getIcon() {
            return MINETEST_ICON;
        }

        @Override
        protected Node[] loadChildren() {
            Node[] children = super.loadChildren();
            for (int i = 0; i < children.length; i++) {
                if (children[i].getName().equals("map.sqlite")) {
                    children[i] = new SqliteNode(new File(file, "map.sqlite"));
                }
            }
            return children;
        }
    }

    public static class SqliteNode extends FileSystemNode {
        public SqliteNode(File dbFile) {
            super(dbFile);
            try {
                db = SqlJetDb.open(dbFile, false);
                blocks = db.getTable("blocks");
                indexName = blocks.getPrimaryKeyIndexName();
            } catch (SqlJetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Icon getIcon() {
            return SQLITE_ICON;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        protected Node[] loadChildren() {
            List<MapBlockNode> children = new ArrayList<>();
            synchronized (db) {
                try {
                    db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
                    try {
                        ISqlJetCursor cursor = blocks.open();
                        try {
                            while (!cursor.eof()) {
                                int[] coords = getCoords(cursor.getInteger("pos"));
                                children.add(new MapBlockNode(db, blocks, indexName, coords[0], coords[1], coords[2]));
                                cursor.next();
                            }
                        } finally {
                            cursor.close();
                        }
                        db.commit();
                    } catch (RuntimeException | SqlJetException e) {
                        db.rollback();
                        throw e;
                    }
                } catch (SqlJetException e) {
                    throw new RuntimeException(e);
                }
            }
            children.sort((n1, n2) -> {
                if (n1.x < n2.x) {
                    return -1;
                } else if (n1.x > n2.x) {
                    return 1;
                } else if (n1.z < n2.z) {
                    return -1;
                } else if (n1.z > n2.z) {
                    return 1;
                } else if (n1.y < n2.y) {
                    return -1;
                } else if (n1.y > n2.y) {
                    return 1;
                } else {
                    return 0;
                }
            });
            return children.toArray(new Node[children.size()]);
        }

        private static int[] getCoords(long key) {
            return new int[] {
                    utos((int) (key & 0xfffL)),
                    utos((int) ((key & 0xfff000L) >> 12)),
                    utos((int) ((key & 0xfff000000L) >> 24))
            };
        }

        private static int utos(int value) {
            return (value > 2047) ? value - 4096 : value;
        }

        private final SqlJetDb db;
        private final ISqlJetTable blocks;
        private final String indexName;
    }

    public static class MapBlockNode extends Node {
        public MapBlockNode(SqlJetDb db, ISqlJetTable blocks, String indexName, int x, int y, int z) {
            this.db = db;
            this.blocks = blocks;
            this.indexName = indexName;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String getName() {
            return "(" + x + "," + y + "," + z + ")";
        }

        @Override
        public Icon getIcon() {
            return MAP_BLOCK_ICON;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        protected Node[] loadChildren() {
            return new Node[0];
        }

        @Override
        public void doubleClicked() {
            if (mapBlock == null) {
                synchronized (db) {
                    try {
                        db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
                        try {
                            ISqlJetCursor cursor = blocks.lookup(indexName, getKey(x, y, z));
                            try {
                                mapBlock = new MapBlock(x, y, z, cursor.getBlobAsArray("data"));
                            } finally {
                                cursor.close();
                            }
                            db.commit();
                        } catch (RuntimeException | SqlJetException e) {
                            db.rollback();
                            throw e;
                        }
                    } catch (SqlJetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            System.out.println(mapBlock);
            mapBlock.dumpContents();
        }

        private long getKey(int x, int y, int z) {
            return z * 16777216L + y * 4096 + x;
        }

        private final SqlJetDb db;
        private final ISqlJetTable blocks;
        private final String indexName;
        private final int x, y, z;
        private MapBlock mapBlock;
    }

    private static final Map<File, MinetestMapNode> MAP_NODES = new HashMap<>();
    private static final Icon MINETEST_ICON = IconUtils.scaleIcon(IconUtils.loadScaledIcon(MinetestMapRecognizer.class.getClassLoader(), "Minetest_logo.png"), 16);
    private static final Icon SQLITE_ICON = IconUtils.scaleIcon(IconUtils.loadScaledIcon(MinetestMapRecognizer.class.getClassLoader(), "Minetest_logo.png"), 16);
    private static final Icon MAP_BLOCK_ICON = IconUtils.scaleIcon(IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/grass.png"), 16);
}
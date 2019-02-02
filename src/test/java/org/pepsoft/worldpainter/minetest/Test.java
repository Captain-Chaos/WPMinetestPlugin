package org.pepsoft.worldpainter.minetest;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import java.io.File;

/**
 * Created by Pepijn on 19-2-2017.
 */
public class Test {
    public static void main(String[] args) throws SqlJetException {
        File dbFile = new File(args[0], "map.sqlite");
        SqlJetDb db = SqlJetDb.open(dbFile, false);
        try {
            ISqlJetTable blocks = db.getTable("blocks");
            db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
            try {
                ISqlJetCursor cursor = blocks.open();
                try {
                    int count = 0;
                    while (!cursor.eof()) {
                        int[] coords = getCoords(cursor.getInteger("pos"));
                        byte[] data = cursor.getBlobAsArray("data");
                        MapBlock mapBlock = new MapBlock(coords[0], coords[1], coords[2], data);
                        System.out.println(mapBlock);
                        count++;
                        cursor.next();
                    }
                    System.out.println("Read " + count + " map blocks");
                } finally {
                    cursor.close();
                }
            } finally {
                db.commit();
            }
        } finally {
            db.close();
        }
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
}
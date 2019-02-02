package org.pepsoft.worldpainter.minetest;

import java.io.*;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Utility methods for working with Minetest files.
 *
 * <p>Created by Pepijn on 9-3-2017.
 */
class MinetestUtils {
    static String deserialiseString(DataInput in) throws IOException {
        int length = in.readUnsignedShort();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) in.readByte(); // TODO: assumes ASCII
        }
        return new String(chars);
    }

    static String deserialiseLongString(DataInput in) throws IOException {
        int length = in.readInt(); // TODO: should be un unsigned int, but unlikely to ever cause problems in real world
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) in.readByte(); // TODO: assumes ASCII
        }
        return new String(chars);
    }

    static void serialiseString(DataOutput out, String str) throws IOException {
        byte[] bytes = str.getBytes("US-ASCII"); // TODO: assumes ASCII
        if (bytes.length > 4095) {
            throw new IllegalArgumentException("String too long");
        }
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    static void serialiseLongString(DataOutput out, String str) throws IOException {
        byte[] bytes = str.getBytes("US-ASCII"); // TODO: assumes ASCII
        out.writeInt(bytes.length); // TODO: we don't support strings longer than Integer.MAX_VALUE, but unlikely to ever cause problems in real world
        out.write(bytes);
    }

    static void deserialiseContents(InputStream in, short[] content, byte[] param1, byte[] param2) throws IOException {
        try (DataInputStream nestedIn = new DataInputStream(new InflaterInputStream(in, new Inflater(), 1))) {
            // Note: we're coercing unsigned short values into signed short
            // values here, which we must take into account wherever we read
            // from or write to these arrays
            for (int i = 0; i < content.length; i++) {
                content[i] = nestedIn.readShort();
            }
            nestedIn.readFully(param1);
            nestedIn.readFully(param2);
            while (nestedIn.read() != -1) {
                // Read until end of compressed stream to ensure checksum is
                // properly skipped
            }
        }
    }
}
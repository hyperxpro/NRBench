package com.aayushatharva.nrbench;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public final class MemoryMappedCachedFile {
    private static final Map<String, ByteBuffer> CACHE = new HashMap<>();

    public static ByteBuffer load(String path) {
        return CACHE.computeIfAbsent(path, requestedPath -> {
            try (RandomAccessFile file = new RandomAccessFile(requestedPath, "r")) {
                FileChannel channel = file.getChannel();

                // Map the file into memory
                return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}

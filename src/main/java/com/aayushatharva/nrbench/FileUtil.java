package com.aayushatharva.nrbench;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileUtil {

    public static String readFileAsString(String path) throws IOException {
//        return Files.readString(Path.of(path));
        try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
            FileChannel channel = file.getChannel();

            // Map the file into memory
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

            return StandardCharsets.UTF_8.decode(buffer).toString();
        }
    }
}

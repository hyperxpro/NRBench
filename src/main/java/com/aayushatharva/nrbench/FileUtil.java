package com.aayushatharva.nrbench;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileUtil {

    public static String readFileAsString(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}

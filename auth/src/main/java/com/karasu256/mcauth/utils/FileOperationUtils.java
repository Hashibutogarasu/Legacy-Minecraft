package com.karasu256.mcauth.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileOperationUtils {
    public static void write(String content, Path path) {
        try {
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

package com.pyojan.eDastakhat.utils;

import net.sf.oval.constraint.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class FileUtil {

    // Check if a file exists using java.nio.file
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    public static boolean fileExists(Path filePath) {
        return Files.exists(filePath);
    }

    // Check if a directory exists
    public static boolean directoryExists(String dirPath) {
        Path path = Paths.get(dirPath);
        return Files.exists(path) && Files.isDirectory(path);
    }

    public static boolean directoryExists(Path dirPath) {
        return Files.exists(dirPath) && Files.isDirectory(dirPath);
    }

    // Validate that a file exists; throw an exception if not
    public static void isFileExists(String filePath) throws NoSuchFileException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new NoSuchFileException("File does not exist - " + filePath);
        }
    }

    public static void isFileExists(Path filePath) throws NoSuchFileException {
        if (!Files.exists(filePath)) {
            throw new NoSuchFileException("File does not exist - " + filePath);
        }
    }

    // Ensure file exists, create if not, along with parent directories
    public static void ensureFileExists(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();

        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir); // Ensure parent directories exist
        }

        if (!Files.exists(path)) {
            Files.createFile(path); // Create file if it does not exist
        }
    }

    public static void ensureDirectoryExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public static String writePdfToDisk(String filePath, String base64Content) throws IOException {
        // Convert the file path string to a Path object
        Path path = Paths.get(filePath);

        // Create parent directories if they don't exist
        Path parentDirectory = path.getParent();
        if (parentDirectory != null && !Files.exists(parentDirectory)) {
            Files.createDirectories(parentDirectory);
        }

        // Decode the Base64 content
        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);

        // Write the decoded bytes to the specified file path
        Files.write(path, decodedBytes);

        // Return the path as a string
        return path.toString();
    }

    public static String generateSignedFilePath(@NotNull  String inputPath) {
        File inputFile = new File(inputPath);
        String parent = inputFile.getParent();
        String fileName = inputFile.getName();

        int dotIndex = fileName.lastIndexOf('.');
        String baseName = (dotIndex != -1) ? fileName.substring(0, dotIndex) : fileName;
        String extension = (dotIndex != -1) ? fileName.substring(dotIndex) : ".pdf";

        String signedFileName = baseName + "_signed" + extension;

        return new File(parent, signedFileName).getPath();
    }
    public static String generateTimestampedFilename() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yy_hh-mm-a");
        String timestamp = LocalDateTime.now().format(formatter);
        return timestamp + ".pdf";
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

}


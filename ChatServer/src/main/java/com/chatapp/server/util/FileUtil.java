package com.chatapp.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    private static final String UPLOAD_DIR = "uploads/";
    private static final String IMAGE_DIR = UPLOAD_DIR + "images/";
    private static final String FILE_DIR = UPLOAD_DIR + "files/";
    private static final String VIDEO_DIR = UPLOAD_DIR + "videos/";
    private static final String AUDIO_DIR = UPLOAD_DIR + "audio/";

    static {
        createDirectories();
    }

    private static void createDirectories() {
        try {
            Files.createDirectories(Paths.get(IMAGE_DIR));
            Files.createDirectories(Paths.get(FILE_DIR));
            Files.createDirectories(Paths.get(VIDEO_DIR));
            Files.createDirectories(Paths.get(AUDIO_DIR));
        } catch (IOException e) {
            logger.error("Error creating upload directories", e);
        }
    }

    /**
     * Save file from base64 string
     */
    public static String saveFile(String base64Data, String fileName, String fileType) {
        try {
            byte[] fileData = Base64.getDecoder().decode(base64Data);

            String directory = getDirectoryForType(fileType);
            String uniqueFileName = generateUniqueFileName(fileName);
            String filePath = directory + uniqueFileName;

            Files.write(Paths.get(filePath), fileData);

            logger.info("File saved: {}", filePath);
            return filePath;
        } catch (Exception e) {
            logger.error("Error saving file", e);
            return null;
        }
    }

    /**
     * Read file to base64 string
     */
    public static String readFileAsBase64(String filePath) {
        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            return Base64.getEncoder().encodeToString(fileData);
        } catch (IOException e) {
            logger.error("Error reading file: {}", filePath, e);
            return null;
        }
    }

    /**
     * Delete file
     */
    public static boolean deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            logger.info("File deleted: {}", filePath);
            return true;
        } catch (IOException e) {
            logger.error("Error deleting file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Get file size
     */
    public static long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            logger.error("Error getting file size: {}", filePath, e);
            return 0;
        }
    }

    private static String getDirectoryForType(String fileType) {
        switch (fileType.toUpperCase()) {
            case "IMAGE":
                return IMAGE_DIR;
            case "VIDEO":
                return VIDEO_DIR;
            case "AUDIO":
                return AUDIO_DIR;
            default:
                return FILE_DIR;
        }
    }

    private static String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }

    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
}

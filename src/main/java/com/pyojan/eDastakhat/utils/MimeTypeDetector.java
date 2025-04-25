package com.pyojan.eDastakhat.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MimeTypeDetector {
    // Common MIME type constants
    private static final String PDF_MIME = "application/pdf";
    private static final String XML_MIME = "application/xml";
    private static final String JSON_MIME = "application/json";
    private static final String TEXT_MIME = "text/plain";
    private static final String OCTET_STREAM = "application/octet-stream";

    // Magic numbers for common file types
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};
    private static final byte[] XML_MAGIC = {'<', '?', 'x', 'm', 'l'};
    private static final byte[] ZIP_MAGIC = {'P', 'K', 0x03, 0x04};

    // Extension to MIME type mapping
    private static final Map<String, String> EXTENSION_TO_MIME = new HashMap<>();

    static {
        // Initialize common MIME type mappings
        EXTENSION_TO_MIME.put("pdf", PDF_MIME);
        EXTENSION_TO_MIME.put("xml", XML_MIME);
        EXTENSION_TO_MIME.put("json", JSON_MIME);
        EXTENSION_TO_MIME.put("txt", TEXT_MIME);
        EXTENSION_TO_MIME.put("csv", "text/csv");
        EXTENSION_TO_MIME.put("png", "image/png");
        EXTENSION_TO_MIME.put("jpg", "image/jpeg");
        EXTENSION_TO_MIME.put("jpeg", "image/jpeg");
        EXTENSION_TO_MIME.put("gif", "image/gif");
        EXTENSION_TO_MIME.put("zip", "application/zip");
        EXTENSION_TO_MIME.put("doc", "application/msword");
        EXTENSION_TO_MIME.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        EXTENSION_TO_MIME.put("xls", "application/vnd.ms-excel");
        EXTENSION_TO_MIME.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        EXTENSION_TO_MIME.put("ppt", "application/vnd.ms-powerpoint");
        EXTENSION_TO_MIME.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }

    /**
     * Detects MIME type of a file using both extension and content detection
     * @param file The file to analyze
     * @return MIME type as String
     * @throws IOException If there's an error reading the file
     */
    public static String detectMimeType(File file) throws IOException {
        // First try content detection
        try (InputStream is = Files.newInputStream(file.toPath())) {
            String contentType = detectFromContent(is);
            if (!contentType.equals(OCTET_STREAM)) {
                return contentType;
            }
        }

        // Fall back to extension detection
        return detectFromExtension(file.getName());
    }

    /**
     * Detects MIME type from file path
     * @param filePath Path to the file
     * @return MIME type as String
     * @throws IOException If there's an error reading the file
     */
    public static String detectMimeType(Path filePath) throws IOException {
        return detectMimeType(filePath.toFile());
    }

    /**
     * Detects MIME type from file name (extension-based detection)
     * @param fileName The name of the file
     * @return MIME type as String
     */
    public static String detectMimeTypeFromName(String fileName) {
        return detectFromExtension(fileName);
    }

    /**
     * Detects MIME type from file content (magic number detection)
     * @param inputStream InputStream of the file content
     * @return MIME type as String
     * @throws IOException If there's an error reading the stream
     */
    public static String detectMimeType(InputStream inputStream) throws IOException {
        return detectFromContent(inputStream);
    }

    /**
     * Detects MIME type from byte array
     * @param data Byte array containing file content
     * @return MIME type as String
     */
    public static String detectMimeType(byte[] data) {
        if (data == null || data.length == 0) {
            return OCTET_STREAM;
        }

        // Check for PDF
        if (startsWith(data, PDF_MAGIC)) {
            return PDF_MIME;
        }

        // Check for XML
        if (startsWith(data, XML_MAGIC)) {
            return XML_MIME;
        }

        // Check for ZIP-based formats (like docx, xlsx, etc.)
        if (startsWith(data, ZIP_MAGIC)) {
            return "application/zip";
        }

        // Check for text (first 100 bytes)
        if (isLikelyText(data)) {
            return TEXT_MIME;
        }

        return OCTET_STREAM;
    }

    /**
     * Checks if the file is a PDF document
     * @param file The file to check
     * @return true if the file is a PDF
     * @throws IOException If there's an error reading the file
     */
    public static boolean isPdf(File file) throws IOException {
        return PDF_MIME.equals(detectMimeType(file));
    }

    /**
     * Checks if the input stream contains PDF content
     * @param inputStream The input stream to check
     * @return true if the content is PDF
     * @throws IOException If there's an error reading the stream
     */
    public static boolean isPdf(InputStream inputStream) throws IOException {
        return PDF_MIME.equals(detectMimeType(inputStream));
    }

    /**
     * Check if the file path is a PDF document
     * @param filePath The path to the file
     * @return true if the file is a PDF
     * @throws IOException If there's an error reading the file
     */
    public static boolean isPdf(Path filePath) throws IOException {
        return PDF_MIME.equals(detectMimeType(filePath));
    }

    /**
     * Checks if the file is an XML document
     * @param file The file to check
     * @return true if the file is XML
     * @throws IOException If there's an error reading the file
     */
    public static boolean isXml(File file) throws IOException {
        return XML_MIME.equals(detectMimeType(file));
    }

    /**
     * Checks if the input stream contains XML content
     * @param inputStream The input stream to check
     * @return true if the content is XML
     * @throws IOException If there's an error reading the stream
     */
    public static boolean isXml(InputStream inputStream) throws IOException {
        return XML_MIME.equals(detectMimeType(inputStream));
    }

    /**
     * Checks if the file is a JSON document
     * @param file The file to check
     * @return true if the file is JSON
     * @throws IOException If there's an error reading the file
     */
    public static boolean isJson(File file) throws IOException {
        return JSON_MIME.equals(detectMimeType(file));
    }

    /**
     * Checks if the input stream contains JSON content
     * @param inputStream The input stream to check
     * @return true if the content is JSON
     * @throws IOException If there's an error reading the stream
     */
    public static boolean isJson(InputStream inputStream) throws IOException {
        return JSON_MIME.equals(detectMimeType(inputStream));
    }

    /**
     * Checks if the file is a plain text document
     * @param file The file to check
     * @return true if the file is plain text
     * @throws IOException If there's an error reading the file
     */
    public static boolean isText(File file) throws IOException {
        return TEXT_MIME.equals(detectMimeType(file));
    }

    /**
     * Checks if the input stream contains plain text content
     * @param inputStream The input stream to check
     * @return true if the content is plain text
     * @throws IOException If there's an error reading the stream
     */
    public static boolean isText(InputStream inputStream) throws IOException {
        return TEXT_MIME.equals(detectMimeType(inputStream));
    }

    /**
     * Gets the default MIME type (application/octet-stream)
     * @return Default MIME type
     */
    public static String getDefaultMimeType() {
        return OCTET_STREAM;
    }

    // Helper methods
    private static String detectFromExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            String extension = fileName.substring(dotIndex + 1).toLowerCase();
            return EXTENSION_TO_MIME.getOrDefault(extension, OCTET_STREAM);
        }
        return OCTET_STREAM;
    }

    private static String detectFromContent(InputStream is) throws IOException {
        byte[] header = new byte[1024];
        int bytesRead = is.read(header);
        if (bytesRead <= 0) {
            return OCTET_STREAM;
        }
        return detectMimeType(header);
    }

    private static boolean startsWith(byte[] data, byte[] magic) {
        if (data.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLikelyText(byte[] data) {
        int checkLength = Math.min(data.length, 100);
        for (int i = 0; i < checkLength; i++) {
            byte b = data[i];
            // Check for non-text characters (control chars except common ones like \n, \r, \t)
            if (b < 0x09 || (b > 0x0D && b < 0x20) || b == 0x7F) {
                return false;
            }
        }
        return true;
    }
}
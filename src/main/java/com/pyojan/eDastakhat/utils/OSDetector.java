package com.pyojan.eDastakhat.utils;

public class OSDetector {

    // Private constructor to prevent instantiation
    private OSDetector() {
        throw new UnsupportedOperationException("Utility class. Do not instantiate.");
    }

    /**
     * Checks if the operating system is Windows.
     *
     * @return true if the OS is Windows, false otherwise.
     */
    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    /**
     * Checks if the operating system is Linux.
     *
     * @return true if the OS is Linux, false otherwise.
     */
    public static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("aix");
    }

    /**
     * Checks if the operating system is macOS.
     *
     * @return true if the OS is macOS, false otherwise.
     */
    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    /**
     * Returns the name of the operating system.
     *
     * @return the OS name (e.g., "Windows 10", "Linux", "Mac OS X").
     */
    public static String getOSName() {
        return System.getProperty("os.name");
    }
}
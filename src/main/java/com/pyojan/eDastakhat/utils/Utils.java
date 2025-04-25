package com.pyojan.eDastakhat.utils;

import com.pyojan.eDastakhat.exceptions.TsaException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final String[] tsaUrls = {
            "http://timestamp.digicert.com",
            "http://timestamp.comodoca.com",
            "http://timestamp.entrust.net/TSS/RFC3161sha2TS",
            "http://timestamp.digicert.com"
    };

    /**
     * Gets a random Time Stamp Authority (TSA) URL from the predefined list
     * @return A randomly selected TSA URL string
     */
    public static String getRandomTsaUrl() {
        Random random = new Random();
        int index = random.nextInt(tsaUrls.length);
        return tsaUrls[index];
    }

    /**
     * Validates if the provided TSA URL is reachable and operational
     * @param tsaUrl The Time Stamp Authority URL to validate
     * @throws TsaException If the URL is not reachable or invalid
     * @throws IllegalArgumentException If the URL is null or empty
     */
    public static void validateTsaUrlUpdated(String tsaUrl) throws TsaException {
        if (tsaUrl == null || tsaUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("TSA URL cannot be null or empty.");
        }

        try {
            if (getResponseCode(tsaUrl)) {
                throw new IllegalArgumentException("TSA URL is not reachable.");
            }
        } catch (Exception e) {
            throw new TsaException("Unable to connect to the Time Stamp Authority at: " + tsaUrl, e);
        }
    }

    private static boolean getResponseCode(String tsaUrl) throws IOException {
        URL url = new URL(tsaUrl);
        if (!("http".equalsIgnoreCase(url.getProtocol()) || "https".equalsIgnoreCase(url.getProtocol()))) {
            throw new IllegalArgumentException("TSA URL must use HTTP or HTTPS.");
        }

        // Optional: Check if URL is reachable
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.setConnectTimeout(5000); // 5 seconds timeout
        connection.setReadTimeout(5000);
        int responseCode = connection.getResponseCode();
        return (responseCode >= 200 && responseCode < 400);
    }

    /**
     * Gets the signature page number based on the input string and total pages
     * @param page The page specification ("F" for first, "L" for last, or a number)
     * @param totalPages The total number of pages in the document
     * @return The page number where signature should be placed
     * @throws IllegalArgumentException If page number is invalid or out of range
     */
    public static int getSignaturePageNumber(String page, int totalPages) {
        if ("L".equalsIgnoreCase(page)) return totalPages;
        if ("F".equalsIgnoreCase(page)) return 1;

        try {
            int pageNumber = Integer.parseInt(page);
            if (pageNumber < 1 || pageNumber > totalPages) {
                throw new IllegalArgumentException(
                        String.format("Invalid page number %s. Document has %d pages.", page, totalPages));
            }
            return pageNumber;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid page option: " + page, ex);
        }
    }

    /**
     * Prints concise help information about the application usage and options
     */
    public static void printConciseHelp() {
        String helpText =
                "eDastakhat - PDF Digital Signing Tool\n" +
                        "Usage: java -jar eDastakhat.jar [options]\n" +
                        "\n" +
                        "REQUIRED OPTIONS:\n" +
                        "\t-i, --input <file>        Input PDF file to sign (required)\n" +
                        "\t-c, --config <file>       Signature configuration JSON file (required)\n" +
                        "\n" +
                        "SECURITY OPTIONS (choose one):\n" +
                        "\t-t,  --token <file>       PKCS#11 library file (requires --pin, --certificateSerial, and --tokenSerial [if multiple tokens])\n" +
                        "\t-pf, --pfx <file>         PFX/PKCS#12 file (requires --pin)\n" +
                        "\t(No option needed for Windows KeyStore. Certificate must be present in Windows-MY, use --certificateSerial to specify one)\n" +
                        "\n" +
                        "ADDITIONAL OPTIONS:\n" +
                        "\t-o,  --output <file>      Output signed file (default: input_signed.pdf)\n" +
                        "\t-p,  --pin <pin>          PIN for token or PFX\n" +
                        "\t-ts, --tokenSerial <id>   Serial number of the PKCS#11 token (required only if multiple tokens are available)\n" +
                        "\t-cs, --certificateSerial <serial>  Serial number of the certificate (required for PKCS#11 and Windows KeyStore)\n" +
                        "\t-pw, --password <pwd>     PDF password if encrypted\n" +
                        "\n" +
                        "NETWORK OPTIONS (for timestamping):\n" +
                        "\t--pxh <host>        HTTP/HTTPS proxy host\n" +
                        "\t--pxp <port>        Proxy port\n" +
                        "\t--pxu <user>        (Optional) Proxy username\n" +
                        "\t--pxw <pass>        (Optional) Proxy password\n" +
                        "\t--pxs               (Optional) Use HTTPS for proxy connection\n" +
                        "\n" +
                        "INFORMATION:\n" +
                        "\t-v,  --version            Show application version\n" +
                        "\t-h,  --help               Show this help message\n" +
                        "\n" +
                        "VALIDATION RULES:\n" +
                        "\t* --input and --config are always required\n" +
                        "\t* --token requires --pin, --certificateSerial and --tokenSerial (if multiple tokens)\n" +
                        "\t* --pfx requires --pin (no need for --certificateSerial)\n" +
                        "\t* Windows KeyStore requires --certificateSerial to identify the certificate from Windows-MY\n" +
                        "\t* Proxy options are only needed if timestamping and LTV is enabled and system requires a proxy\n" +
                        "\n" +
                        "EXAMPLES:\n" +
                        "\t1. PKCS#11 signing:\n" +
                        "\t   java -jar eDastakhat.jar -i doc.pdf -c config.json -t libpkcs11.so -p 1234 -ts A1B2C3 -cs 1234567890\n" +
                        "\n" +
                        "\t2. PFX signing:\n" +
                        "\t   java -jar eDastakhat.jar -i doc.pdf -c config.json -pf cert.pfx -p 5678\n" +
                        "\n" +
                        "\t3. Encrypted PDF:\n" +
                        "\t   java -jar eDastakhat.jar -i secure.pdf -pw pdfpass -c config.json -pf cert.pfx -p 5678\n" +
                        "\n" +
                        "\t4. Windows KeyStore signing:\n" +
                        "\t   java -jar eDastakhat.jar -i doc.pdf -c config.json -cs 1234567890\n" +
                        "\n" +
                        "\t5. With proxy for timestamping:\n" +
                        "\t   java -jar eDastakhat.jar -i doc.pdf -c config.json -pf cert.pfx -p 5678 \\\n" +
                        "\t        -pxh 192.168.0.1 -pxp 8080 -pxu user -pxw pass -pxs\n";

        System.out.println(helpText);
    }
}

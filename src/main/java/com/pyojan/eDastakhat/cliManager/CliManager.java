package com.pyojan.eDastakhat.cliManager;

import com.pyojan.eDastakhat.utils.FileUtil;
import com.pyojan.eDastakhat.utils.OSDetector;
import lombok.Getter;
import org.apache.commons.cli.*;

import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

public class CliManager {

    private final String[] cliArgs;
    @Getter
    private final CommandLineParser parser = new DefaultParser();

    public CliManager(String[] args) {
        this.cliArgs = args;
    }

    public CommandLine cliOption() throws ParseException, NoSuchFileException {
        Options options = new Options();

        // Info options
        options.addOption("v", "version", false, "Display current version of the application");
        options.addOption("h", "help", false, "Display help message");

        // Required input options
        options.addOption(Option.builder("i").longOpt("input").hasArg().desc("Path or URL of the input PDF file to be signed (required)").build());
        options.addOption(Option.builder("c").longOpt("config").hasArg().desc("Path to the signature configuration JSON file (required)").build());

        // Output options
        options.addOption(Option.builder("o").longOpt("output").hasArg().desc("Path to save the signed PDF (default: input filename with '_signed' suffix)").build());

        // PDF password
        options.addOption(Option.builder("pw").longOpt("password").hasArg().desc("Password for encrypted PDF (if required)").build());

        // Security options
        OptionGroup securityGroup = new OptionGroup();
        securityGroup.addOption(Option.builder("t").longOpt("token").hasArg().desc("Path to the PKCS#11 library file").build());
        securityGroup.addOption(Option.builder("pf").longOpt("pfx").hasArg().desc("Path to the PFX/PKCS#12 file").build());
        options.addOptionGroup(securityGroup);

        options.addOption(Option.builder("p").longOpt("pin").hasArg().desc("PIN for the security token or PFX file").build());
        options.addOption(Option.builder("ts").longOpt("tokenSerial").hasArg().desc("Serial number of the PKCS#11 token (required if --token is used)").build());
        options.addOption(Option.builder("nw").longOpt("no-watermark").desc("Do NOT apply a watermark to the signed PDF").build());
        options.addOption(Option.builder("cs").longOpt("certificateSerial").hasArg().desc("Serial number of the certificate to sign with").build());

        // Proxy options
        options.addOption(Option.builder("pxh").longOpt("proxyHost").hasArg().desc("Proxy host (e.g., proxy.company.com)").build());
        options.addOption(Option.builder("pxp").longOpt("proxyPort").hasArg().desc("Proxy port (e.g., 8080)").build());
        options.addOption(Option.builder("pxu").longOpt("proxyUser").hasArg().desc("Proxy username (if authentication required)").build());
        options.addOption(Option.builder("pxw").longOpt("proxyPassword").hasArg().desc("Proxy password (if authentication required)").build());
        options.addOption(Option.builder("pxs").longOpt("proxySecure").desc("Use HTTPS proxy instead of HTTP").build());


        // Parse arguments
        CommandLine commandLine = parser.parse(options, this.cliArgs);

        if (!(commandLine.hasOption("v") || commandLine.hasOption("h"))) {

            // General-required options
            if (!commandLine.hasOption("i")) throw new IllegalArgumentException("Input file (-i/--input) is required.");

            String inputPath = commandLine.getOptionValue("i");
            if (inputPath == null || inputPath.trim().isEmpty()) throw new IllegalArgumentException("Input file (-i/--input) path is empty or blank.");
            FileUtil.isFileExists(inputPath, String.format("Input file path [ %s ] does not exist.", inputPath));

            // Config only required if an input file is PDF
            if (commandLine.getOptionValue("i").endsWith(".pdf")) {
                if (!commandLine.hasOption("c")) throw new IllegalArgumentException("Configuration file (-c/--config) is required.");

                String configPath = commandLine.getOptionValue("c");
                if (configPath == null || configPath.trim().isEmpty()) throw new IllegalArgumentException("Configuration file (-c/--config) path is empty or blank.");

                FileUtil.isFileExists(configPath, String.format("Configuration file path [ %s ] does not exist.", configPath));
            }


            boolean isPfx = commandLine.hasOption("pf");
            boolean isToken = commandLine.hasOption("t");

            // this rules out the case where both --pfx and --token are specified
            if(isPfx || isToken) {
                if (isPfx && isToken) throw new IllegalArgumentException("Only one of (-t/--token) or (-pf/--pfx) can be specified");

                if (!commandLine.hasOption("p")) throw new IllegalArgumentException("PIN (-p/--pin) is required when using any one of (-pf/--pfx) or (-t/--token)");

                String tokenPath = commandLine.getOptionValue("t"); // if token is specified and tokenPath is null
                if (isToken && (tokenPath == null || tokenPath.trim().isEmpty())) throw new IllegalArgumentException("Token file (-t/--token) path is empty or blank.");
                if (isToken) FileUtil.isFileExists(tokenPath,  String.format("Token file path [ %s ] does not exist.", tokenPath));

                String pfxPath = commandLine.getOptionValue("pf"); // if pfx is specified and pfxPath is null
                if (isPfx && (pfxPath == null || pfxPath.trim().isEmpty())) throw new IllegalArgumentException("PFX file (-pf/--pfx) path is empty or blank.");
                if (isPfx) FileUtil.isFileExists(pfxPath, String.format("PFX file path [ %s ] does not exist.", pfxPath));

                boolean isTokenSerial = commandLine.hasOption("ts");
                if (isToken && isTokenSerial) { // if token is specified and tokenSerial is null
                    String tokenSerialValue = commandLine.getOptionValue("ts");
                    if (tokenSerialValue == null || tokenSerialValue.trim().isEmpty()) {
                        throw new IllegalArgumentException("Token serial number (-ts/--tokenSerial) is empty or blank.");
                    }
                }
            }

            // Certificate Serial required if NOT using pfx
            if (!isPfx && !commandLine.hasOption("cs")) throw new IllegalArgumentException("Certificate serial number (-cs/--certificateSerial) is required unless using (-pf/--pfx)");
            boolean isCertificateSerial = commandLine.hasOption("cs");
            if(isCertificateSerial) {
                String certificateSerialValue = commandLine.getOptionValue("cs");
                if (certificateSerialValue == null || certificateSerialValue.trim().isEmpty()) {
                    throw new IllegalArgumentException("Certificate serial number (-cs/--certificateSerial) is empty or blank.");
                }
            }

            // Proxy validation
            boolean hasProxyHost = commandLine.hasOption("pxh");
            boolean hasProxyPort = commandLine.hasOption("pxp");
            boolean hasProxyUser = commandLine.hasOption("pxu");
            boolean hasProxyPassword = commandLine.hasOption("pxw");

            if (hasProxyHost && !hasProxyPort) throw new IllegalArgumentException("Proxy port (-pxp/--proxyPort) is required when proxy host is provided");
            if (hasProxyUser && !hasProxyPassword) throw new IllegalArgumentException("Proxy password (--proxyPassword) is required when proxy username is provided");

            if (hasProxyPort && !hasProxyHost) throw new IllegalArgumentException("Proxy host (-pxh/--proxyHost) is required when proxy port is provided");
            if (hasProxyPassword && !hasProxyUser) throw new IllegalArgumentException("Proxy username (-pxu/--proxyUser) is required when proxy password is provided");

            // validate all proxy options if provided should not be empty
            if (hasProxyHost) {
                if (commandLine.getOptionValue("pxh") == null || commandLine.getOptionValue("pxh").trim().isEmpty())
                    throw new IllegalArgumentException("Proxy host (-pxh/--proxyHost) is empty or blank.");
            }
            if (hasProxyPort) {
                if (commandLine.getOptionValue("pxp") == null || commandLine.getOptionValue("pxp").trim().isEmpty())
                    throw new IllegalArgumentException("Proxy port (-pxp/--proxyPort) is empty or blank.");
            }
            if (hasProxyUser) {
                if (commandLine.getOptionValue("pxu") == null || commandLine.getOptionValue("pxu").trim().isEmpty())
                    throw new IllegalArgumentException("Proxy username (-pxu/--proxyUser) is empty or blank.");
            }
            if (hasProxyPassword) {
                if (commandLine.getOptionValue("pxw") == null || commandLine.getOptionValue("pxw").trim().isEmpty())
                    throw new IllegalArgumentException("Proxy password (-pxw/--proxyPassword) is empty or blank.");
            }
        }

        return commandLine;
    }
}

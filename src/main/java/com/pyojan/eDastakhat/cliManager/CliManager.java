package com.pyojan.eDastakhat.cliManager;

import com.pyojan.eDastakhat.utils.OSDetector;
import lombok.Getter;
import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.List;

public class CliManager {

    private final String[] cliArgs;
    @Getter
    private final CommandLineParser parser = new DefaultParser();

    public CliManager(String[] args) {
        this.cliArgs = args;
    }

    public CommandLine cliOption() throws ParseException {
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

        if (OSDetector.isWindows() && !(commandLine.hasOption("v") || commandLine.hasOption("h"))) {
            List<String> missingOptions = new ArrayList<>();

            // General required options
            if (!commandLine.hasOption("i")) missingOptions.add("Input PDF file (-i/--input)");

            // config only required if file type is PDF
            if (commandLine.getOptionValue("i").endsWith(".pdf") && !commandLine.hasOption("c"))
                missingOptions.add("Configuration file (-c/--config)");

            boolean usingPfx = commandLine.hasOption("pf");
            boolean usingToken = commandLine.hasOption("t");

            // PIN required if using token or pfx
            if ((usingToken || usingPfx) && !commandLine.hasOption("p")) missingOptions.add("PIN (--pin) is required when using --token or --pfx");

            // Certificate Serial required if NOT using pfx
            if (!usingPfx && !commandLine.hasOption("cs")) missingOptions.add("Certificate serial number (-cs/--certificateSerial) is required unless using --pfx");

            // Proxy validation
            boolean hasProxyHost = commandLine.hasOption("pxh");
            boolean hasProxyPort = commandLine.hasOption("pxp");
            boolean hasProxyUser = commandLine.hasOption("pxu");
            boolean hasProxyPassword = commandLine.hasOption("pxw");

            if (hasProxyHost && !hasProxyPort) missingOptions.add("Proxy port (--proxyPort) is required when proxy host is provided");
            if (hasProxyUser && !hasProxyPassword) missingOptions.add("Proxy password (--proxyPassword) is required when proxy username is provided");

            if (!missingOptions.isEmpty()) {
                throw new MissingOptionException(
                        "Missing required arguments:\n" +
                                String.join("\n", missingOptions) +
                                "\n\nUse -h or --help for usage information."
                );
            }
        }

        return commandLine;
    }
}

package com.pyojan.eDastakhat;

import com.pyojan.eDastakhat.cliManager.CliManager;
import com.pyojan.eDastakhat.libs.ProxyConfig;
import com.pyojan.eDastakhat.libs.Response;
import com.pyojan.eDastakhat.utils.OSDetector;
import com.pyojan.eDastakhat.utils.Utils;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;

import static com.pyojan.eDastakhat.libs.Response.generateErrorResponse;
import static com.pyojan.eDastakhat.libs.Response.generateSuccessResponse;

public class EDastakhatApplication {

    public static void main(String[] args) {
        try {
            // Ensure the OS is supported
            ensureSupportedOS();
            validateArguments(args);

            CommandLine commandLine = initializeCLI(args);
            EDastakhatApplication.configureProxyIfPresent(commandLine);

            // Handle version and help options early
            handleHelpAndVersion(commandLine);

            // Handle sign request
            SignerController.handleExecuteSigningRequest(commandLine);

        } catch (Exception e) {
            Response.generateErrorResponse(e);
        }
    }


    private static void ensureSupportedOS() {
        if (!OSDetector.isWindows() && !OSDetector.isLinux() && !OSDetector.isMac()) {
            throw new RuntimeException("OS is not supported: " + OSDetector.getOSName());
        }
    }

    private static void validateArguments(String[] args) throws IOException {
        if (args.length == 0) {
            generateErrorResponse(new IOException("No arguments provided. Use -h or --help to see usage instructions."));
            System.exit(0);
        }
    }

    private static CommandLine initializeCLI(String[] args) throws ParseException, NoSuchFileException {
        CliManager cliManager = new CliManager(args);
        return cliManager.cliOption();
    }

    private static void handleHelpAndVersion(CommandLine commandLine) throws IOException {
        if (commandLine.hasOption("v")) {
            LinkedHashMap<String, String> versionInfo = new LinkedHashMap<>();
            versionInfo.put("version", "1.0.0");
            generateSuccessResponse(versionInfo);
            System.exit(0);
        }

        if (commandLine.hasOption("h")) {
            Utils.printConciseHelp();
            copyExampleFiles();
            System.exit(0);
        }
    }


    /**
     * Copies example payload files from resources to the current working directory, converting extensions from .txt to .json.
     */
    private static void copyExampleFiles() throws IOException {
        String[] payloadFileNames = {"config.txt"};
        for (String sourceFilename : payloadFileNames) {
            URL resource = EDastakhatApplication.class.getClassLoader().getResource("examples/" + sourceFilename);
            if (resource == null) return;

            // Convert extension from .txt to .json and copy the file
            String destFilename = sourceFilename.replace(".txt", ".json");
            Files.copy(resource.openStream(), Paths.get(destFilename), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("------------------< Example Signature Configuration File saved successfully on: >------------------");
            System.out.println("\t" + destFilename);
            System.out.println("\n\n\n");
        }
    }

    private static void configureProxyIfPresent(CommandLine commandLine) throws IOException {
        if (commandLine.hasOption("pxh") && commandLine.hasOption("pxp")) {
            String host = commandLine.getOptionValue("pxh");
            int port = Integer.parseInt(commandLine.getOptionValue("pxp"));
            String user = commandLine.getOptionValue("pxu");
            String password = commandLine.getOptionValue("pxw");
            boolean isHttps = commandLine.hasOption("pxs");

            ProxyConfig proxyConfig = new ProxyConfig(host, port, user, password, isHttps);
            proxyConfig.setProxy();
        }
    }
}

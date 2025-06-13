//package com.pyojan.eDastakhat.cliManager;
//
//import com.pyojan.eDastakhat.utils.FileUtil;
//import com.pyojan.eDastakhat.utils.OSDetector;
//import lombok.Getter;
//import org.apache.commons.cli.*;
//
//import java.nio.file.NoSuchFileException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class CliManager {
//
//    private final String[] cliArgs;
//    @Getter
//    private final CommandLineParser parser = new DefaultParser();
//
//    public CliManager(String[] args) {
//        this.cliArgs = args;
//    }
//
//    public CommandLine cliOption() throws ParseException, NoSuchFileException {
//        Options options = new Options();
//
//        // Info options
//        options.addOption("v", "version", false, "Display current version of the application");
//        options.addOption("h", "help", false, "Display help message");
//
//        // Signed PDF or XML verification
//        options.addOption(Option.builder("vf")
//                .longOpt("verify")
//                .hasArg()
//                .desc("Verify digital signatures in the specified PDF file (e.g., -vf filePath.pdf)")
//                .build());
//
//        // Input options for Signing PDF or Enveloped XML
//        options.addOption(Option.builder("i").longOpt("input").hasArg().desc("Path or URL of the input PDF file to be signed (required)").build());
//        options.addOption(Option.builder("c").longOpt("config").hasArg().desc("Path to the signature configuration JSON file (required)").build());
//
//        // Output options
//        options.addOption(Option.builder("o").longOpt("output").hasArg().desc("Path to save the signed PDF (default: input filename with '_signed' suffix)").build());
//
//        // PDF password
//        options.addOption(Option.builder("pw").longOpt("password").hasArg().desc("Password for encrypted PDF (if required)").build());
//
//        // Security options
//        OptionGroup securityGroup = new OptionGroup();
//        securityGroup.addOption(Option.builder("t").longOpt("token").hasArg().desc("Path to the PKCS#11 library file").build());
//        securityGroup.addOption(Option.builder("pf").longOpt("pfx").hasArg().desc("Path to the PFX/PKCS#12 file").build());
//        options.addOptionGroup(securityGroup);
//
//        options.addOption(Option.builder("p").longOpt("pin").hasArg().desc("PIN for the security token or PFX file").build());
//        options.addOption(Option.builder("ts").longOpt("tokenSerial").hasArg().desc("Serial number of the PKCS#11 token (required if --token is used)").build());
//        options.addOption(Option.builder("nw").longOpt("no-watermark").desc("Do NOT apply a watermark to the signed PDF").build());
//        options.addOption(Option.builder("cs").longOpt("certificateSerial").hasArg().desc("Serial number of the certificate to sign with").build());
//
//        // Proxy options
//        options.addOption(Option.builder("pxh").longOpt("proxyHost").hasArg().desc("Proxy host (e.g., proxy.company.com)").build());
//        options.addOption(Option.builder("pxp").longOpt("proxyPort").hasArg().desc("Proxy port (e.g., 8080)").build());
//        options.addOption(Option.builder("pxu").longOpt("proxyUser").hasArg().desc("Proxy username (if authentication required)").build());
//        options.addOption(Option.builder("pxw").longOpt("proxyPassword").hasArg().desc("Proxy password (if authentication required)").build());
//        options.addOption(Option.builder("pxs").longOpt("proxySecure").desc("Use HTTPS proxy instead of HTTP").build());
//
//
//        // Parse arguments
//        CommandLine commandLine = parser.parse(options, this.cliArgs);
//
//        if (!(commandLine.hasOption("v") || commandLine.hasOption("h"))) {
//
//            // General-required options
//            boolean hasInput = commandLine.hasOption("i");
//            boolean hasVerify = commandLine.hasOption("vf");
//
//            if (!hasInput && !hasVerify) {
//                throw new IllegalArgumentException("Either the input PDF file (-i/--input) for signing or the verification file (-vf/--verify) must be provided.");
//            } else if (hasInput && hasVerify) {
//                throw new IllegalArgumentException("Please provide only one of: input PDF for signing (-i/--input) or file for verification (-vf/--verify). Both cannot be used together.");
//            }
//
//            if(hasInput) {
//                String inputPath = commandLine.getOptionValue("i");
//                if (inputPath == null || inputPath.trim().isEmpty()) throw new IllegalArgumentException("Input file (-i/--input) path is empty or blank.");
//                FileUtil.isFileExists(inputPath, String.format("Input file path [ %s ] does not exist.", inputPath));
//
//                // Config only required if an input file is PDF
//                if (commandLine.getOptionValue("i").endsWith(".pdf")) {
//                    if (!commandLine.hasOption("c")) throw new IllegalArgumentException("Configuration file (-c/--config) is required.");
//
//                    String configPath = commandLine.getOptionValue("c");
//                    if (configPath == null || configPath.trim().isEmpty()) throw new IllegalArgumentException("Configuration file (-c/--config) path is empty or blank.");
//
//                    FileUtil.isFileExists(configPath, String.format("Configuration file path [ %s ] does not exist.", configPath));
//                }
//            }
//
//
//            boolean isPfx = commandLine.hasOption("pf");
//            boolean isToken = commandLine.hasOption("t");
//
//            // this rules out the case where both --pfx and --token are specified
//            if(isPfx || isToken) {
//                if (isPfx && isToken) throw new IllegalArgumentException("Only one of (-t/--token) or (-pf/--pfx) can be specified");
//
//                if (!commandLine.hasOption("p")) throw new IllegalArgumentException("PIN (-p/--pin) is required when using any one of (-pf/--pfx) or (-t/--token)");
//
//                String tokenPath = commandLine.getOptionValue("t"); // if token is specified and tokenPath is null
//                if (isToken && (tokenPath == null || tokenPath.trim().isEmpty())) throw new IllegalArgumentException("Token file (-t/--token) path is empty or blank.");
//                if (isToken) FileUtil.isFileExists(tokenPath,  String.format("Token file path [ %s ] does not exist.", tokenPath));
//
//                String pfxPath = commandLine.getOptionValue("pf"); // if pfx is specified and pfxPath is null
//                if (isPfx && (pfxPath == null || pfxPath.trim().isEmpty())) throw new IllegalArgumentException("PFX file (-pf/--pfx) path is empty or blank.");
//                if (isPfx) FileUtil.isFileExists(pfxPath, String.format("PFX file path [ %s ] does not exist.", pfxPath));
//
//                boolean isTokenSerial = commandLine.hasOption("ts");
//                if (isToken && isTokenSerial) { // if token is specified and tokenSerial is null
//                    String tokenSerialValue = commandLine.getOptionValue("ts");
//                    if (tokenSerialValue == null || tokenSerialValue.trim().isEmpty()) {
//                        throw new IllegalArgumentException("Token serial number (-ts/--tokenSerial) is empty or blank.");
//                    }
//                }
//            }
//
//            // Certificate Serial required if NOT using pfx
//            if(hasInput) {
//                if (!isPfx && !commandLine.hasOption("cs")) throw new IllegalArgumentException("Certificate serial number (-cs/--certificateSerial) is required unless using (-pf/--pfx)");
//                boolean isCertificateSerial = commandLine.hasOption("cs");
//                if(isCertificateSerial) {
//                    String certificateSerialValue = commandLine.getOptionValue("cs");
//                    if (certificateSerialValue == null || certificateSerialValue.trim().isEmpty()) {
//                        throw new IllegalArgumentException("Certificate serial number (-cs/--certificateSerial) is empty or blank.");
//                    }
//                }
//            }
//
//            // Proxy validation
//            boolean hasProxyHost = commandLine.hasOption("pxh");
//            boolean hasProxyPort = commandLine.hasOption("pxp");
//            boolean hasProxyUser = commandLine.hasOption("pxu");
//            boolean hasProxyPassword = commandLine.hasOption("pxw");
//
//            if (hasProxyHost && !hasProxyPort) throw new IllegalArgumentException("Proxy port (-pxp/--proxyPort) is required when proxy host is provided");
//            if (hasProxyUser && !hasProxyPassword) throw new IllegalArgumentException("Proxy password (--proxyPassword) is required when proxy username is provided");
//
//            if (hasProxyPort && !hasProxyHost) throw new IllegalArgumentException("Proxy host (-pxh/--proxyHost) is required when proxy port is provided");
//            if (hasProxyPassword && !hasProxyUser) throw new IllegalArgumentException("Proxy username (-pxu/--proxyUser) is required when proxy password is provided");
//
//            // validate all proxy options if provided should not be empty
//            if (hasProxyHost) {
//                if (commandLine.getOptionValue("pxh") == null || commandLine.getOptionValue("pxh").trim().isEmpty())
//                    throw new IllegalArgumentException("Proxy host (-pxh/--proxyHost) is empty or blank.");
//            }
//            if (hasProxyPort) {
//                if (commandLine.getOptionValue("pxp") == null || commandLine.getOptionValue("pxp").trim().isEmpty())
//                    throw new IllegalArgumentException("Proxy port (-pxp/--proxyPort) is empty or blank.");
//            }
//            if (hasProxyUser) {
//                if (commandLine.getOptionValue("pxu") == null || commandLine.getOptionValue("pxu").trim().isEmpty())
//                    throw new IllegalArgumentException("Proxy username (-pxu/--proxyUser) is empty or blank.");
//            }
//            if (hasProxyPassword) {
//                if (commandLine.getOptionValue("pxw") == null || commandLine.getOptionValue("pxw").trim().isEmpty())
//                    throw new IllegalArgumentException("Proxy password (-pxw/--proxyPassword) is empty or blank.");
//            }
//        }
//
//        return commandLine;
//    }
//}



package com.pyojan.eDastakhat.cliManager;

import com.pyojan.eDastakhat.utils.FileUtil;
import lombok.Getter;
import org.apache.commons.cli.*;

import java.nio.file.NoSuchFileException;

public class CliManager {

    private final String[] cliArgs;
    @Getter
    private final CommandLineParser parser = new DefaultParser();

    public CliManager(String[] args) {
        this.cliArgs = args;
    }

    public CommandLine cliOption() throws ParseException, NoSuchFileException {
        Options options = defineOptions();
        CommandLine commandLine = parser.parse(options, this.cliArgs);

        if (!(commandLine.hasOption("v") || commandLine.hasOption("h"))) {
            validateVerifyOptions(commandLine);
            validateInputOrVerifyOptions(commandLine);
            validateInputAndConfig(commandLine);
            validateSecurityOptions(commandLine);
            validateCertificateRequirement(commandLine);
            validateProxyOptions(commandLine);
        }

        return commandLine;
    }

    private Options defineOptions() {
        Options options = new Options();

        options.addOption("v", "version", false, "Display current version of the application");
        options.addOption("h", "help", false, "Display help message");
        options.addOption(Option.builder("vf").longOpt("verify").hasArg().desc("Verify digital signatures in the specified PDF file").build());
        options.addOption(Option.builder("i").longOpt("input").hasArg().desc("Input PDF file to be signed").build());
        options.addOption(Option.builder("c").longOpt("config").hasArg().desc("Path to the signature configuration JSON file").build());
        options.addOption(Option.builder("o").longOpt("output").hasArg().desc("Path to save the signed PDF").build());
        options.addOption(Option.builder("pw").longOpt("password").hasArg().desc("Password for encrypted PDF").build());

        OptionGroup securityGroup = new OptionGroup();
        securityGroup.addOption(Option.builder("t").longOpt("token").hasArg().desc("Path to the PKCS#11 library file").build());
        securityGroup.addOption(Option.builder("pf").longOpt("pfx").hasArg().desc("Path to the PFX/PKCS#12 file").build());
        options.addOptionGroup(securityGroup);

        options.addOption(Option.builder("p").longOpt("pin").hasArg().desc("PIN for the security token or PFX file").build());
        options.addOption(Option.builder("ts").longOpt("tokenSerial").hasArg().desc("Serial number of the PKCS#11 token").build());
        options.addOption(Option.builder("nw").longOpt("no-watermark").desc("Do NOT apply a watermark").build());
        options.addOption(Option.builder("cs").longOpt("certificateSerial").hasArg().desc("Serial number of the certificate").build());

        options.addOption(Option.builder("pxh").longOpt("proxyHost").hasArg().desc("Proxy host").build());
        options.addOption(Option.builder("pxp").longOpt("proxyPort").hasArg().desc("Proxy port").build());
        options.addOption(Option.builder("pxu").longOpt("proxyUser").hasArg().desc("Proxy username").build());
        options.addOption(Option.builder("pxw").longOpt("proxyPassword").hasArg().desc("Proxy password").build());
        options.addOption(Option.builder("pxs").longOpt("proxySecure").desc("Use HTTPS proxy").build());

        return options;
    }


    private void validateVerifyOptions(CommandLine cmd) throws NoSuchFileException {
        if(isBlank(cmd.getOptionValue("vf"))) throw new IllegalArgumentException("Verify file path is empty or blank.");
        FileUtil.isFileExists(cmd.getOptionValue("vf"), String.format("Verify file [ %s ] does not exist.", cmd.getOptionValue("vf")));
    }

    private void validateInputOrVerifyOptions(CommandLine cmd) {
        boolean hasInput = cmd.hasOption("i");
        boolean hasVerify = cmd.hasOption("vf");

        if (!hasInput && !hasVerify) {
            throw new IllegalArgumentException("Either -(i/--input) or (-vf/--verify) must be provided.");
        }
        if (hasInput && hasVerify) {
            throw new IllegalArgumentException("Only one of (-i/--input) or (-vf/--verify) should be provided.");
        }
    }

    private void validateInputAndConfig(CommandLine cmd) throws NoSuchFileException {
        if (!cmd.hasOption("i")) return;

        String inputPath = cmd.getOptionValue("i");
        if (isBlank(inputPath)) throw new IllegalArgumentException("Input file path is empty or blank.");
        FileUtil.isFileExists(inputPath, String.format("Input file [ %s ] does not exist.", inputPath));

        if (inputPath.endsWith(".pdf")) {
            if (!cmd.hasOption("c")) throw new IllegalArgumentException("(-c/--config) is required for PDF input.");

            String configPath = cmd.getOptionValue("c");
            if (isBlank(configPath)) throw new IllegalArgumentException("Configuration path is empty or blank.");
            FileUtil.isFileExists(configPath, String.format("Configuration file [ %s ] does not exist.", configPath));
        }
    }

    private void validateSecurityOptions(CommandLine cmd) throws NoSuchFileException {
        boolean isToken = cmd.hasOption("t");
        boolean isPfx = cmd.hasOption("pf");

        if (isToken && isPfx) {
            throw new IllegalArgumentException("Only one of (-t/--token) or (pf/--pfx) can be specified.");
        }

        if (isToken || isPfx) {
            if (!cmd.hasOption("p")) throw new IllegalArgumentException("PIN (-p/--pin) is required for (-t/--token) or (-pf/--pfx).");

            if (isToken) {
                String tokenPath = cmd.getOptionValue("t");
                if (isBlank(tokenPath)) throw new IllegalArgumentException("Token path is empty or blank.");
                FileUtil.isFileExists(tokenPath, String.format("Token file [ %s ] does not exist.", tokenPath));

                if (cmd.hasOption("ts")) {
                    String ts = cmd.getOptionValue("ts");
                    if (isBlank(ts)) throw new IllegalArgumentException("Token serial number is empty or blank.");
                }
            }

            if (isPfx) {
                String pfxPath = cmd.getOptionValue("pf");
                if (isBlank(pfxPath)) throw new IllegalArgumentException("PFX path is empty or blank.");
                FileUtil.isFileExists(pfxPath, String.format("PFX file [ %s ] does not exist.", pfxPath));
            }
        }
    }

    private void validateCertificateRequirement(CommandLine cmd) {
        if (!cmd.hasOption("i") || cmd.hasOption("pf")) return;

        if (!cmd.hasOption("cs")) throw new IllegalArgumentException("(-cs/--certificateSerial) is required unless (-pf/--pfx) is used.");

        String certSerial = cmd.getOptionValue("cs");
        if (isBlank(certSerial)) throw new IllegalArgumentException("Certificate serial is empty or blank.");
    }

    private void validateProxyOptions(CommandLine cmd) {
        validateProxyPair(cmd, "pxh", "pxp", "Proxy port is required when host is provided.");
        validateProxyPair(cmd, "pxp", "pxh", "Proxy host is required when port is provided.");
        validateProxyPair(cmd, "pxu", "pxw", "Proxy password is required when username is provided.");
        validateProxyPair(cmd, "pxw", "pxu", "Proxy username is required when password is provided.");

        validateOptionNotBlank(cmd, "pxh", "Proxy host is empty or blank.");
        validateOptionNotBlank(cmd, "pxp", "Proxy port is empty or blank.");
        validateOptionNotBlank(cmd, "pxu", "Proxy username is empty or blank.");
        validateOptionNotBlank(cmd, "pxw", "Proxy password is empty or blank.");
    }

    private void validateProxyPair(CommandLine cmd, String first, String second, String message) {
        if (cmd.hasOption(first) && !cmd.hasOption(second)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateOptionNotBlank(CommandLine cmd, String option, String message) {
        if (cmd.hasOption(option)) {
            String value = cmd.getOptionValue(option);
            if (isBlank(value)) throw new IllegalArgumentException(message);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

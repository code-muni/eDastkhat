//package com.pyojan.eDastakhat;
//
//import com.pyojan.eDastakhat.libs.Response;
//import com.pyojan.eDastakhat.libs.keyStore.PKCS11KeyStore;
//import com.pyojan.eDastakhat.libs.keyStore.PKCS12KeyStore;
//import com.pyojan.eDastakhat.libs.keyStore.WindowKeyStore;
//import com.pyojan.eDastakhat.models.ModelValidator;
//import com.pyojan.eDastakhat.models.SignatureOptions;
//import com.pyojan.eDastakhat.services.pdf.PdfSigner;
//import com.pyojan.eDastakhat.services.xml.XMLSigner;
//import com.pyojan.eDastakhat.utils.FileUtil;
//import com.pyojan.eDastakhat.utils.MimeTypeDetector;
//import org.apache.commons.cli.CommandLine;
//
//import java.io.File;
//import java.nio.file.Paths;
//import java.security.PrivateKey;
//import java.security.cert.X509Certificate;
//import java.util.LinkedHashMap;
//
//public class ExecutorController {
//
//    public static void handleExecuteRequest(CommandLine commandLine) throws Exception {
//
//        boolean hasVerifyOption = commandLine.hasOption("vf");
//
//        if(hasVerifyOption) {
//            System.out.println();
//            System.out.println("Verifying digital signatures in the specified PDF file...");
//            System.out.println();
//        } else  {
//
//            String inputFile = commandLine.getOptionValue("i");
//            File file = new File(inputFile);
//            // For PDF signing
//            if(MimeTypeDetector.isPdf(file)) {
//
//                // Load and validate the signing model from a config file
//                String configOption = commandLine.getOptionValue("c");
//                ModelValidator modelValidator = new ModelValidator(Paths.get(configOption));
//                modelValidator.validatePdfPayloadModel();
//                SignatureOptions signatureOptions = modelValidator.getModal();
//
//                // Execute the signing process
//                PdfSigner pdfSigner = new PdfSigner();
//                pdfSigner.executeSign(commandLine, signatureOptions);
//            }
//
//            // For XML signing
//            if(MimeTypeDetector.isXml(file)) {
//
//                X509Certificate certificate = null;
//                PrivateKey privateKey = null;
//
//                if(commandLine.hasOption("pf")) {
//                    String pfxPath = commandLine.getOptionValue("pf");
//                    String pfxPassword = commandLine.getOptionValue("p");
//                    boolean isCertificateSerial = commandLine.hasOption("cs");
//
//                    PKCS12KeyStore pkcs12KeyStore = new PKCS12KeyStore(pfxPath, pfxPassword);
//
//                    certificate = isCertificateSerial
//                            ? pkcs12KeyStore.getCertificate(commandLine.getOptionValue("cs"))
//                            : pkcs12KeyStore.getCertificate();
//                    privateKey = isCertificateSerial
//                            ? pkcs12KeyStore.getPrivateKey(commandLine.getOptionValue("cs"))
//                            : pkcs12KeyStore.getPrivateKey();
//                } else if (commandLine.hasOption("t")) {
//                    String pkcs11Path = commandLine.getOptionValue("t");
//                    String pin = commandLine.getOptionValue("p");
//                    boolean isCertificateSerial = commandLine.hasOption("cs");
//                    String tokenSerialNumber = commandLine.getOptionValue("ts");
//
//                    PKCS11KeyStore pkcs11KeyStore = new PKCS11KeyStore();
//                    pkcs11KeyStore.init(pkcs11Path, pin);
//
//                    if(tokenSerialNumber != null) pkcs11KeyStore.setTokenSerial(tokenSerialNumber);
//
//                    certificate = isCertificateSerial
//                            ? pkcs11KeyStore.getCertificate(commandLine.getOptionValue("cs"))
//                            : pkcs11KeyStore.getCertificate();
//                    privateKey = isCertificateSerial
//                            ? pkcs11KeyStore.getPrivateKey(commandLine.getOptionValue("cs"))
//                            : pkcs11KeyStore.getPrivateKey();
//                } else {
//
//                    WindowKeyStore windowKeyStore = new WindowKeyStore();
//                    windowKeyStore.setSerialHex(commandLine.getOptionValue("cs")); // Windows allows certificate serial number
//                    certificate = windowKeyStore.getCertificate();
//                    privateKey = windowKeyStore.getPrivateKey();
//                }
//
//                XMLSigner xmlSigner = new XMLSigner(certificate, privateKey);
//                String signedXml = xmlSigner.signXmlFromFile(inputFile);
//
//                String outputPath = FileUtil.prepareDistPath(commandLine.getOptionValue("o"), inputFile, FileUtil.Extension.XML);
//                FileUtil.writeToDisk(signedXml, outputPath);
//
//                LinkedHashMap<String, String> signDataMap = new LinkedHashMap<>();
//                signDataMap.put("signedFilePath", outputPath);
//                Response.generateSuccessResponse(signDataMap);
//            }
//        }
//
//
//    }
//}


package com.pyojan.eDastakhat;

import com.pyojan.eDastakhat.libs.Response;
import com.pyojan.eDastakhat.libs.keyStore.PKCS11KeyStore;
import com.pyojan.eDastakhat.libs.keyStore.PKCS12KeyStore;
import com.pyojan.eDastakhat.libs.keyStore.WindowKeyStore;
import com.pyojan.eDastakhat.models.ModelValidator;
import com.pyojan.eDastakhat.models.PdfSignatureVerificationResult;
import com.pyojan.eDastakhat.models.SignatureOptions;
import com.pyojan.eDastakhat.services.pdf.PdfSignatureVerifier;
import com.pyojan.eDastakhat.services.pdf.PdfSigner;
import com.pyojan.eDastakhat.services.xml.XMLSigner;
import com.pyojan.eDastakhat.utils.FileUtil;
import com.pyojan.eDastakhat.utils.MimeTypeDetector;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;

public class ExecutorController {

    public static void handleExecuteRequest(CommandLine commandLine) throws Exception {
        if (commandLine.hasOption("vf")) {
            executeSignatureVerification(commandLine);
            return;
        }

        String inputFile = commandLine.getOptionValue("i");
        File file = new File(inputFile);

        if (MimeTypeDetector.isPdf(file)) {
            executePdfSigning(commandLine, inputFile);
        } else if (MimeTypeDetector.isXml(file)) {
            executeXmlSigning(commandLine, inputFile);
        } else {
            throw new IllegalArgumentException("Unsupported file type: only PDF and XML are allowed.");
        }
    }

    private static void executeSignatureVerification(CommandLine commandLine) throws IOException, GeneralSecurityException {

        String inputFile = commandLine.getOptionValue("vf");
        File file = new File(inputFile);

        if(MimeTypeDetector.isPdf(file)) {
            PdfSignatureVerifier pdfSignatureVerifier = new PdfSignatureVerifier();
            PdfSignatureVerificationResult result = pdfSignatureVerifier.verifySignatures(inputFile);

            Response.generateSuccessResponse(result);

        } else if (MimeTypeDetector.isXml(file)) {
            throw new IllegalArgumentException("XML signature verification is not supported yet.");
        } else  {
            throw new IllegalArgumentException("Unsupported file type: only PDF and XML are allowed.");
        }


    }

    private static void executePdfSigning(CommandLine commandLine, String inputFile) throws Exception {
        String configPath = commandLine.getOptionValue("c");
        ModelValidator modelValidator = new ModelValidator(Paths.get(configPath));
        modelValidator.validatePdfPayloadModel();

        SignatureOptions signatureOptions = modelValidator.getModal();
        PdfSigner pdfSigner = new PdfSigner();
        pdfSigner.executeSign(commandLine, signatureOptions);
    }

    private static void executeXmlSigning(CommandLine commandLine, String inputFile) throws Exception {
        CertKeyPair certKeyPair = loadCertificateAndKey(commandLine);

        XMLSigner xmlSigner = new XMLSigner(certKeyPair.certificate, certKeyPair.privateKey);
        String signedXml = xmlSigner.signXmlFromFile(inputFile);

        String outputPath = FileUtil.prepareDistPath(
                commandLine.getOptionValue("o"),
                inputFile,
                FileUtil.Extension.XML
        );

        FileUtil.writeToDisk(signedXml, outputPath);

        LinkedHashMap<String, String> signDataMap = new LinkedHashMap<>();
        signDataMap.put("signedFilePath", outputPath);
        Response.generateSuccessResponse(signDataMap);
    }

    private static CertKeyPair loadCertificateAndKey(CommandLine cmd) throws Exception {
        X509Certificate certificate;
        PrivateKey privateKey;
        boolean hasSerial = cmd.hasOption("cs");

        if (cmd.hasOption("pf")) {
            PKCS12KeyStore pkcs12 = new PKCS12KeyStore(cmd.getOptionValue("pf"), cmd.getOptionValue("p"));
            certificate = hasSerial
                    ? pkcs12.getCertificate(cmd.getOptionValue("cs"))
                    : pkcs12.getCertificate();
            privateKey = hasSerial
                    ? pkcs12.getPrivateKey(cmd.getOptionValue("cs"))
                    : pkcs12.getPrivateKey();

        } else if (cmd.hasOption("t")) {
            PKCS11KeyStore pkcs11 = new PKCS11KeyStore();
            pkcs11.init(cmd.getOptionValue("t"), cmd.getOptionValue("p"));

            if (cmd.hasOption("ts")) {
                pkcs11.setTokenSerial(cmd.getOptionValue("ts"));
            }

            certificate = hasSerial
                    ? pkcs11.getCertificate(cmd.getOptionValue("cs"))
                    : pkcs11.getCertificate();
            privateKey = hasSerial
                    ? pkcs11.getPrivateKey(cmd.getOptionValue("cs"))
                    : pkcs11.getPrivateKey();

        } else {
            WindowKeyStore winKeyStore = new WindowKeyStore();
            winKeyStore.setSerialHex(cmd.getOptionValue("cs"));
            certificate = winKeyStore.getCertificate();
            privateKey = winKeyStore.getPrivateKey();
        }

        return new CertKeyPair(certificate, privateKey);
    }

    private static class CertKeyPair {
        final X509Certificate certificate;
        final PrivateKey privateKey;

        CertKeyPair(X509Certificate certificate, PrivateKey privateKey) {
            this.certificate = certificate;
            this.privateKey = privateKey;
        }
    }
}

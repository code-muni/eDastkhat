package com.pyojan.eDastakhat;

import com.pyojan.eDastakhat.libs.Response;
import com.pyojan.eDastakhat.libs.keyStore.PKCS11KeyStore;
import com.pyojan.eDastakhat.libs.keyStore.PKCS12KeyStore;
import com.pyojan.eDastakhat.libs.keyStore.WindowKeyStore;
import com.pyojan.eDastakhat.models.ModelValidator;
import com.pyojan.eDastakhat.models.SignatureOptions;
import com.pyojan.eDastakhat.services.PdfSigner;
import com.pyojan.eDastakhat.services.XMLSigner;
import com.pyojan.eDastakhat.utils.FileUtil;
import com.pyojan.eDastakhat.utils.MimeTypeDetector;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;

public class SignerController {

    public static void handleExecuteSigningRequest(CommandLine commandLine) throws Exception {

        String inputFile = commandLine.getOptionValue("i");
        File file = new File(inputFile);

        // For PDF signing
        if(MimeTypeDetector.isPdf(file)) {

            // Load and validate the signing model from a config file
            String configOption = commandLine.getOptionValue("c");
            ModelValidator modelValidator = new ModelValidator(Paths.get(configOption));
            modelValidator.validatePdfPayloadModel();
            SignatureOptions signatureOptions = modelValidator.getModal();

            // Execute the signing process
            PdfSigner pdfSigner = new PdfSigner();
            pdfSigner.executeSign(commandLine, signatureOptions);
        }

        // For XML signing
        if(MimeTypeDetector.isXml(file)) {

            X509Certificate certificate = null;
            PrivateKey privateKey = null;

            if(commandLine.hasOption("pf")) {
                String pfxPath = commandLine.getOptionValue("pf");
                String pfxPassword = commandLine.getOptionValue("p");
                boolean isCertificateSerial = commandLine.hasOption("cs");

                PKCS12KeyStore pkcs12KeyStore = new PKCS12KeyStore(pfxPath, pfxPassword);

                certificate = isCertificateSerial
                        ? pkcs12KeyStore.getCertificate(commandLine.getOptionValue("cs"))
                        : pkcs12KeyStore.getCertificate();
                privateKey = isCertificateSerial
                        ? pkcs12KeyStore.getPrivateKey(commandLine.getOptionValue("cs"))
                        : pkcs12KeyStore.getPrivateKey();
            } else if (commandLine.hasOption("t")) {
                String pkcs11Path = commandLine.getOptionValue("t");
                String pin = commandLine.getOptionValue("p");
                boolean isCertificateSerial = commandLine.hasOption("cs");
                String tokenSerialNumber = commandLine.getOptionValue("ts");

                PKCS11KeyStore pkcs11KeyStore = new PKCS11KeyStore();
                pkcs11KeyStore.init(pkcs11Path, pin);

                if(tokenSerialNumber != null) pkcs11KeyStore.setTokenSerial(tokenSerialNumber);

                certificate = isCertificateSerial
                        ? pkcs11KeyStore.getCertificate(commandLine.getOptionValue("cs"))
                        : pkcs11KeyStore.getCertificate();
                privateKey = isCertificateSerial
                        ? pkcs11KeyStore.getPrivateKey(commandLine.getOptionValue("cs"))
                        : pkcs11KeyStore.getPrivateKey();
            } else {

                WindowKeyStore windowKeyStore = new WindowKeyStore();
                windowKeyStore.setSerialHex(commandLine.getOptionValue("cs")); // Windows allows certificate serial number
                certificate = windowKeyStore.getCertificate();
                privateKey = windowKeyStore.getPrivateKey();
            }

            XMLSigner xmlSigner = new XMLSigner(certificate, privateKey);
            String signedXml = xmlSigner.signXmlFromFile(inputFile);

            String outputPath = FileUtil.prepareDistPath(commandLine.getOptionValue("o"), inputFile, FileUtil.Extension.XML);
            FileUtil.writeToDisk(signedXml, outputPath);

            LinkedHashMap<String, String> signDataMap = new LinkedHashMap<>();
            signDataMap.put("signedFilePath", outputPath);
            Response.generateSuccessResponse(signDataMap);
        }

    }
}

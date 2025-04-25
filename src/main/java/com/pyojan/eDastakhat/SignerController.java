package com.pyojan.eDastakhat;

import com.pyojan.eDastakhat.libs.ProxyConfig;
import com.pyojan.eDastakhat.libs.keyStore.WindowKeyStore;
import com.pyojan.eDastakhat.models.SignatureOptions;
import com.pyojan.eDastakhat.services.PdfSigner;
import com.pyojan.eDastakhat.services.XMLSigner;
import com.pyojan.eDastakhat.utils.FileUtil;
import com.pyojan.eDastakhat.utils.MimeTypeDetector;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class SignerController {

    public static void handleExecuteSigningRequest(CommandLine commandLine) throws Exception {

        String inputFile = commandLine.getOptionValue("i");
        File file = new File(inputFile);

        // For PDF signing
        if(MimeTypeDetector.isPdf(file)) {
            PdfSigner pdfSigner = new PdfSigner();
            SignatureOptions signatureOptions = pdfSigner.prepareAndEnsureSigningOption(commandLine);
            pdfSigner.executeSign(commandLine, signatureOptions);
        }

        // For XML signing
        if(MimeTypeDetector.isXml(file)) {
            WindowKeyStore windowKeyStore = new WindowKeyStore();
            windowKeyStore.setSerialHex(commandLine.getOptionValue("cs"));
            X509Certificate certificate = windowKeyStore.getCertificate();
            PrivateKey privateKey = windowKeyStore.getPrivateKey();

            XMLSigner xmlSigner = new XMLSigner(certificate, privateKey);
            String signedXml = xmlSigner.signXmlFromFile(inputFile);
            FileUtil.writeToDisk(signedXml, FileUtil.prepareDistPath(commandLine.getOptionValue("o"), inputFile, FileUtil.Extension.XML));
        }


    }

    public static void configureProxyIfPresent(CommandLine commandLine) {
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

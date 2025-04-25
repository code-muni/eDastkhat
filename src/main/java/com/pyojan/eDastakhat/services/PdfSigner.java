package com.pyojan.eDastakhat.services;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.security.TSAClient;
import com.itextpdf.text.pdf.security.TSAClientBouncyCastle;
import com.pyojan.eDastakhat.exceptions.SignerException;
import com.pyojan.eDastakhat.exceptions.TsaException;
import com.pyojan.eDastakhat.exceptions.UserCancelledException;
import com.pyojan.eDastakhat.libs.PdfWaterMarker;
import com.pyojan.eDastakhat.libs.Response;
import com.pyojan.eDastakhat.libs.keyStore.PKCS11KeyStore;
import com.pyojan.eDastakhat.libs.keyStore.PKCS12KeyStore;
import com.pyojan.eDastakhat.libs.keyStore.WindowKeyStore;
import com.pyojan.eDastakhat.models.ModelValidator;
import com.pyojan.eDastakhat.models.SignatureOptions;
import com.pyojan.eDastakhat.utils.FileUtil;
import com.pyojan.eDastakhat.utils.OSDetector;
import com.pyojan.eDastakhat.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import net.sf.oval.constraint.NotNull;
import org.apache.commons.cli.CommandLine;

import java.io.*;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;

@Getter
@Setter
public class PdfSigner {

    private final Signer signer = new Signer();

    public SignatureOptions prepareAndEnsureSigningOption(@NotNull CommandLine commandLine) throws IOException {
        String pdfInput = commandLine.getOptionValue("i");
        String configOption = commandLine.getOptionValue("c");

        // Optional keystore options
        String pkcs11Path = commandLine.getOptionValue("t"); // PKCS#11
        String pfxPath = commandLine.getOptionValue("pf");   // PKCS#12

        // Validate that provided file paths actually exist
        FileUtil.isFileExists(pdfInput);
        FileUtil.isFileExists(configOption);
        if (pkcs11Path != null) FileUtil.isFileExists(pkcs11Path);
        if (pfxPath != null) FileUtil.isFileExists(pfxPath);

        // Load and validate the signing model from config file
        ModelValidator modelValidator = new ModelValidator(Paths.get(configOption));
        modelValidator.validatePdfPayloadModel();

        return modelValidator.getModal();
    }


    /**
     * Execute the signing process.
     *
     * @param commandLine the {@link CommandLine} containing all the command line options
     * @param options     the {@link SignatureOptions} which contains all the signature options
     * @throws Exception if any error occurs during the signing process
     */
    public void executeSign(@NotNull CommandLine commandLine, SignatureOptions options) throws Exception {

        String pdfPath = commandLine.getOptionValue("i");
        String pdfPassword = commandLine.getOptionValue("pw");
        String dist = commandLine.getOptionValue("o");
        boolean notApplyWatermark = commandLine.hasOption("nw");

        // Load the input PDF as byte array (with or without watermark)
        byte[] inputPdfBytes = applyWaterMark(options, notApplyWatermark, pdfPath);

        String outputPath = FileUtil.prepareDistPath(dist, pdfPath, FileUtil.Extension.PDF);
        PdfReader reader = (pdfPassword == null || pdfPassword.isEmpty()) ?
                new PdfReader(new ByteArrayInputStream(inputPdfBytes)) :
                new PdfReader(new ByteArrayInputStream(inputPdfBytes), pdfPassword.getBytes());


        PrivateKey privateKey = null;
        String provider = null;
        X509Certificate[] certificateChain = null;


        // if Token argument is provided then use PKCS11 and if PFX is provided then use PFX and if these are not provided then use Windows
        if(commandLine.hasOption("t")) {
            PKCS11KeyStore pkcs11KeyStore = new PKCS11KeyStore();
            if(commandLine.hasOption("ts")) pkcs11KeyStore.setTokenSerial(commandLine.getOptionValue("ts")); // Token Serial is optional
            pkcs11KeyStore.setCertSerialHex(commandLine.getOptionValue("cs"));

            pkcs11KeyStore.init(commandLine.getOptionValue("t"), commandLine.getOptionValue('p'));

            privateKey = pkcs11KeyStore.getPrivateKey();
            provider = pkcs11KeyStore.getProvider().getName();
            certificateChain = pkcs11KeyStore.getCertificateChain();
        } else if (commandLine.hasOption("pf")) {
            PKCS12KeyStore pkcs12KeyStore = new PKCS12KeyStore();
            pkcs12KeyStore.setPkcs12FilePath(commandLine.getOptionValue("pf"));
            pkcs12KeyStore.setPkcs12Password(commandLine.getOptionValue("p"));

            pkcs12KeyStore.loadKeyStore();

            privateKey = pkcs12KeyStore.getPrivateKey();
            provider = pkcs12KeyStore.getProvider().getName();
            certificateChain = pkcs12KeyStore.getCertificateChain();


        } else {
            if(!OSDetector.isWindows()) throw new SignerException("Only Windows is supported for this operation. Please use --t or --pf argument.");

            WindowKeyStore windowKeyStore = new WindowKeyStore();
            windowKeyStore.setSerialHex(commandLine.getOptionValue("cs"));

            privateKey = windowKeyStore.getPrivateKey();
            certificateChain = windowKeyStore.getCertificateChain();
            provider = windowKeyStore.getProvider();
        }

        if (privateKey == null) throw new SignerException("PrivateKey cannot be null");
        if (certificateChain == null) throw new SignerException("CertificateChain cannot be null");
        if (provider == null) throw new SignerException("Provider cannot be null");

        TSAClient tsaClient = getTsaClient(options);
        int[] pagesToSign = Signer.parsePageSpecification(options.getPage(), reader.getNumberOfPages());

        String signedPdfBase64 = signSelectedPages(
                reader,
                options,
                provider,
                privateKey,
                certificateChain,
                tsaClient,
                pagesToSign
        );

        FileUtil.writePdfToDisk(outputPath, signedPdfBase64);

        LinkedHashMap<String, String> signDataMap = new LinkedHashMap<>();
        signDataMap.put("signedPdfPath", outputPath);
        Response.generateSuccessResponse(signDataMap);
    }

    private String signSelectedPages(PdfReader originalReader, SignatureOptions options,
                                     String provider, PrivateKey privateKey, Certificate[] certChain,
                                     TSAClient tsaClient, int[] pagesToSign)
            throws IOException, UserCancelledException, SignerException {

        PdfReader reader = originalReader;
        String lastSignedBase64 = null;

        // Sort the pages to ensure we process them in order
        Arrays.sort(pagesToSign);

        for (int i = 0; i < pagesToSign.length; i++) {
            int page = pagesToSign[i];
            // Check if page number is valid
            if (page < 1 || page > reader.getNumberOfPages()) {
                throw new IllegalArgumentException("Invalid page number: " + page);
            }

            // Only allow changes if this isn't the last page to be signed
            boolean allowChanges = (i != pagesToSign.length - 1);

            lastSignedBase64 = signer.sign(
                    reader, provider, privateKey, certChain, page, options.getCoord(),
                    options.isEnableLtv(), tsaClient, allowChanges, options.isGreenTick(),
                    options.getReason(), options.getLocation(), options.getCustomText()
            );

            // Close the previous reader and create a new one from the signed version
            if (reader != originalReader) {
                reader.close();
            }
            reader = new PdfReader(Base64.getDecoder().decode(lastSignedBase64));
        }

        // Clean up
        if (reader != originalReader) {
            reader.close();
        }
        return lastSignedBase64;
    }

    private TSAClient getTsaClient(SignatureOptions options) throws TsaException {

        String tsaUrl = (options.getTimestamp().getUrl() == null || options.getTimestamp().getUrl().isEmpty())
                ? Utils.getRandomTsaUrl()
                : options.getTimestamp().getUrl();

        Utils.validateTsaUrlUpdated(tsaUrl);

        return (options.getTimestamp().isEnabled()) ?
                new TSAClientBouncyCastle(
                        tsaUrl,
                        options.getTimestamp().getUsername(),
                        options.getTimestamp().getPassword(),
                        8192,
                        "SHA-256"
                ) : null;
    }



    private static byte[] applyWaterMark(SignatureOptions options, boolean notApplyWatermark, String pdfPath) throws IOException, DocumentException {
        byte[] inputPdfBytes;

        if (!notApplyWatermark) {
            int[] coord = options.getCoord();
            ByteArrayInputStream watermarkedStream;

            String watermarkText = "eDastakhat - Signature Applied";
            watermarkedStream = PdfWaterMarker.applyWatermarkToSelectedPages(pdfPath, watermarkText, coord, options.getPage());

            inputPdfBytes = FileUtil.toByteArray(watermarkedStream);
            watermarkedStream.close();
        } else {
            try (FileInputStream fis = new FileInputStream(pdfPath)) {
                inputPdfBytes = FileUtil.toByteArray(fis);
            }
        }

        return inputPdfBytes;
    }

}

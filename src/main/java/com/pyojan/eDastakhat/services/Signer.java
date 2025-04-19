package com.pyojan.eDastakhat.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.*;
import com.pyojan.eDastakhat.exceptions.SignerException;
import com.pyojan.eDastakhat.exceptions.UserCancelledException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static com.pyojan.eDastakhat.libs.Response.generateErrorResponse;

public class Signer {
    private static final int BASE_SIGNATURE_SIZE = 8000;
    private static final int CERTIFICATE_SIZE_ESTIMATE = 1500;
    private static final int TIMESTAMP_SIZE_ESTIMATE = 20000;
    private static final int LTV_SIZE_ESTIMATE = 400000;
    private static final int CMS_OVERHEAD = 2000;
    private static final int SAFETY_MARGIN = 2000;

    /**
     * Sign a PDF with the given parameters.
     *
     * @param reader              the original PDF
     * @param provider            the provider for the digital signature
     * @param privateKey          the private key to use for the digital signature
     * @param certChain           the certificate chain to use for the digital signature
     * @param pageNumber          the page number to sign
     * @param coord               the coordinates to place the signature on the page
     * @param isLtv               whether to include Long Term Validation (LTV) information
     * @param tsaClient           the timestamping authority (TSA) client
     * @param isChangesAllowed    whether changes are allowed after signing
     * @param isGreenTrick        whether to include a green tick in the signature
     * @param reason              the reason for signing the document
     * @param location            the location where the document is being signed
     * @param customText          custom text to include in the signature
     * @return the signed PDF as a Base64-encoded string
     */
    public String sign(
            PdfReader reader,
            String provider,
            PrivateKey privateKey,
            Certificate[] certChain,
            int pageNumber,
            int[] coord,
            boolean isLtv,
            TSAClient tsaClient,
            boolean isChangesAllowed,
            boolean isGreenTrick,
            String reason,
            String location,
            String customText
    ) throws UserCancelledException, SignerException {
        ByteArrayOutputStream signedPdfOutputStream = new ByteArrayOutputStream();
        PdfStamper stamper = null;

        try {

            String fieldName = generateSignatureFieldName(pageNumber);
            stamper = PdfStamper.createSignature(reader, signedPdfOutputStream, '\0', null, true);

            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();

            configureSignatureAppearance(
                    appearance,
                    pageNumber,
                    coord,
                    fieldName,
                    isChangesAllowed,
                    isGreenTrick,
                    reason,
                    location
            );

            ExternalDigest digest = new BouncyCastleDigest();
            ExternalSignature signature = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, provider);

            List<CrlClient> crlList = isLtv ? prepareLtvComponents(certChain) : new ArrayList<>();
            OcspClient ocspClient = isLtv ? new OcspClientBouncyCastle() : null;

            int estimatedSize = calculateEstimatedSignatureSize(certChain.length, tsaClient != null, isLtv);

            MakeSignature.signDetached(
                    appearance,
                    digest,
                    signature,
                    certChain,
                    crlList,
                    ocspClient,
                    tsaClient,
                    estimatedSize,
                    MakeSignature.CryptoStandard.CADES
            );

        } catch (DocumentException | IOException | GeneralSecurityException e) {
            if (e instanceof SignatureException) {
                throw new UserCancelledException("Signing was cancelled by the user", e);
            }
            throw new SignerException("PDF signing failed in sign method : " + e.getMessage(), e);
        } finally {
            String errorMessage = closeResources(reader, stamper);
            if (errorMessage != null) {
                generateErrorResponse(new Exception(errorMessage)); // You may want to log or throw instead
            }
        }

        return Base64.getEncoder().encodeToString(signedPdfOutputStream.toByteArray());
    }

    /**
     * Generates a unique field name for the signature on the given page.
     * The format is "eDastakhat__P_{pageNumber}_{randomNumber}".
     *
     * @param pageNumber the page number for the signature
     * @return a unique field name for the signature
     */
    private String generateSignatureFieldName(int pageNumber) {
        return String.format("eDastakhat__P_%d_%d", pageNumber, new Random().nextInt(900000));
    }

    /**
     * Prepares the list of CRL (Certificate Revocation List) clients for Long Term Validation (LTV).
     *
     * @param certChain the certificate chain used for signing the PDF
     * @return a list of CRL clients to be used in the signature process
     */
    private List<CrlClient> prepareLtvComponents(Certificate[] certChain) {
        List<CrlClient> crlList = new ArrayList<>();
        crlList.add(new CrlClientOnline(certChain));
        return crlList;
    }

    /**
     * Estimates the size of the signature based on the given parameters.
     * This method is used to calculate the maximum allowed size for the signature.
     *
     * @param certChainLength the number of certificates in the chain
     * @param withTimestamp whether a timestamp is included in the signature
     * @param withLTV whether Long Term Validation (LTV) is included in the signature
     * @return an estimate of the signature size
     */
    private int calculateEstimatedSignatureSize(int certChainLength, boolean withTimestamp, boolean withLTV) {

        return BASE_SIGNATURE_SIZE +
                (certChainLength * CERTIFICATE_SIZE_ESTIMATE) +
                (withTimestamp ? TIMESTAMP_SIZE_ESTIMATE : 0) +
                (withLTV ? LTV_SIZE_ESTIMATE : 0) +
                CMS_OVERHEAD +
                SAFETY_MARGIN;
    }

    /**
     * Closes the given PDF reader and stamper, and returns null if successful.
     * If an error occurs, returns a string in the format "ERROR: {error message}".
     *
     * @param reader the PDF reader to close, or null if there is no reader
     * @param stamper the PDF stamper to close, or null if there is no stamper
     * @return null if successful, or an error message if an error occurs
     */
    private String closeResources(PdfReader reader, PdfStamper stamper) {
        try {
            if (stamper != null) stamper.close();
            if (reader != null) reader.close();
            return null;
        } catch (DocumentException | IOException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Configures the appearance of the digital signature on a PDF.
     *
     * @param appearance       the PdfSignatureAppearance object to configure
     * @param pageNumber       the page number on which the signature will appear
     * @param coord            an array of coordinates specifying the rectangle where the signature will be placed
     * @param fieldName        the name of the signature field
     * @param isChangesAllowed whether changes are allowed after signing
     * @param isGreenTrick     whether to include a green tick in the signature
     * @param reason           the reason for signing the document
     * @param location         the location where the document is being signed
     * @throws DocumentException if an error occurs while processing the document
     * @throws IOException       if an I/O error occurs
     */
    private void configureSignatureAppearance(
            PdfSignatureAppearance appearance,
            int pageNumber,
            int[] coord,
            String fieldName,
            boolean isChangesAllowed,
            boolean isGreenTrick,
            String reason,
            String location
    ) throws DocumentException, IOException {
        if (coord != null && coord.length == 4) {
            appearance.setVisibleSignature(new Rectangle(coord[0], coord[1], coord[2], coord[3]), pageNumber, fieldName);
        }

        appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.NAME_AND_DESCRIPTION);
        appearance.setAcro6Layers(!isGreenTrick);

        int certificationStatus = isChangesAllowed ? PdfSignatureAppearance.NOT_CERTIFIED : PdfSignatureAppearance.CERTIFIED_FORM_FILLING;
        appearance.setCertificationLevel(certificationStatus)
        ;
        appearance.setReason(reason);
        appearance.setLocation(location);
    }

}

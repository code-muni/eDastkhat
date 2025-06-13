package com.pyojan.eDastakhat.services.pdf;

import com.google.gson.GsonBuilder;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.security.PdfPKCS7;
import com.pyojan.eDastakhat.models.PdfSignatureVerificationResult;
import com.pyojan.eDastakhat.models.PdfSignatureVerificationResult.*;
import jdk.nashorn.internal.objects.annotations.Getter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * A utility class for verifying digital signatures in PDF documents using iText and BouncyCastle.
 * This class provides methods to validate signatures, extract certificate details, and generate
 * a comprehensive verification report in JSON format.
 */
public class PdfSignatureVerifier {
    private static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Verifies digital signatures in a PDF provided as a byte array.
     *
     * @param pdfData The byte array containing the PDF document.
     * @return A JSON string representing the signature verification results.
     * @throws IOException If an error occurs while reading the PDF data.
     * @throws GeneralSecurityException If a security-related error occurs during verification.
     */
    public PdfSignatureVerificationResult verifySignatures(byte[] pdfData) throws IOException, GeneralSecurityException {
        PdfReader reader = new PdfReader(pdfData);

        return verifyDocumentSignatures(reader);
    }

    /**
     * Verifies digital signatures in a PDF file located at the specified file path.
     *
     * @param pdfFilePath The file path to the PDF document.
     * @return A JSON string representing the signature verification results.
     * @throws GeneralSecurityException If a security-related error occurs during verification.
     * @throws IOException If an error occurs while reading the PDF file.
     */
    public PdfSignatureVerificationResult verifySignatures(String pdfFilePath) throws GeneralSecurityException, IOException {
        return verifySignatures(Files.readAllBytes(Paths.get(pdfFilePath)));
    }

    /**
     * Verifies all digital signatures in the provided PDF document.
     *
     * @param reader The PdfReader instance for the PDF document.
     * @return A PdfSignatureVerificationResult containing the verification details.
     * @throws GeneralSecurityException If a security-related error occurs during verification.
     * @throws IOException If an error occurs while processing the PDF.
     * @throws IllegalArgumentException If no signatures are found in the PDF.
     */
    private PdfSignatureVerificationResult verifyDocumentSignatures(PdfReader reader) throws GeneralSecurityException, IOException {
        AcroFields acroFields = reader.getAcroFields();
        List<String> signatureNames = acroFields.getSignatureNames();

        if (signatureNames == null || signatureNames.isEmpty()) {
            throw new IllegalArgumentException("No signatures found in the PDF.");
        }

        List<SignatureInfo> signatures = verifyAllSignatures(acroFields, signatureNames);

        return PdfSignatureVerificationResult.builder()
                .document(buildDocumentInfo(reader, signatureNames, signatures))
                .signatures(signatures)
                .build();
    }

    /**
     * Verifies all signatures in the PDF and collects their verification details.
     *
     * @param acroFields The AcroFields instance containing signature information.
     * @param signatureNames The list of signature names in the PDF.
     * @return A list of SignatureInfo objects detailing each signature's verification status.
     * @throws GeneralSecurityException If a security-related error occurs during verification.
     */
    private List<SignatureInfo> verifyAllSignatures(AcroFields acroFields, List<String> signatureNames) throws GeneralSecurityException {
        List<SignatureInfo> signatures = new ArrayList<>();

        for (int i = 0; i < signatureNames.size(); i++) {
            String sigName = signatureNames.get(i);
            signatures.add(verifySignature(acroFields, sigName, i, signatureNames.size()));
        }

        return signatures;
    }

    /**
     * Verifies a single digital signature in the PDF.
     *
     * @param acroFields The AcroFields instance containing signature information.
     * @param sigName The name of the signature to verify.
     * @param index The index of the signature (0-based).
     * @param totalSignatures The total number of signatures in the PDF.
     * @return A SignatureInfo object containing the verification details for the signature.
     * @throws GeneralSecurityException If a security-related error occurs during verification.
     */
    private SignatureInfo verifySignature(AcroFields acroFields, String sigName, int index, int totalSignatures)
            throws GeneralSecurityException {
        PdfPKCS7 pkcs7 = acroFields.verifySignature(sigName, PROVIDER_NAME);
        X509Certificate cert = pkcs7.getSigningCertificate();

        boolean isValid = pkcs7.verify();
        boolean coversEntireDoc = acroFields.signatureCoversWholeDocument(sigName);

        return SignatureInfo.builder()
                .signatureIndex(index + 1)
                .signatureName(sigName)
                .pageNumber(1) // iText 5 doesn't map signatures to pages directly
                .signingTime(pkcs7.getSignDate().getTime())
                .reason(pkcs7.getReason())
                .location(pkcs7.getLocation())
                .signatureValid(isValid)
                .coversEntireDocument(coversEntireDoc)
                .coversRevision(coversEntireDoc) // Same as coversEntireDoc in iText5
                .certificate(buildCertificateInfo(cert))
                .revocationInfo(buildRevocationInfo(pkcs7))
                .warnings(generateSignatureWarnings(isValid, coversEntireDoc, index, totalSignatures))
                .build();
    }

    /**
     * Builds certificate information for a given X509 certificate.
     *
     * @param cert The X509 certificate to process.
     * @return A CertificateInfo object containing certificate details.
     */
    private CertificateInfo buildCertificateInfo(X509Certificate cert) {
        return CertificateInfo.builder()
                .subjectDN(cert.getSubjectDN().toString())
                .issuerDN(cert.getIssuerDN().toString())
                .validFrom(cert.getNotBefore())
                .validTo(cert.getNotAfter())
                .serialNumber(cert.getSerialNumber().toString(16))
                .certificateType("Document Signer Certificate")
                .build();
    }

    /**
     * Builds revocation information for a signature.
     *
     * @param pkcs7 The PdfPKCS7 instance containing signature details.
     * @return A RevocationInfo object containing revocation and timestamp details.
     */
    private RevocationInfo buildRevocationInfo(PdfPKCS7 pkcs7) {
        boolean isTsp = pkcs7.isTsp();
        boolean isTimestampPresent = pkcs7.getTimeStampToken() != null;

        // Revocation check
        boolean isOcspPresent = pkcs7.getOcsp() != null;
        boolean hasCRL = pkcs7.getCRLs() != null && !pkcs7.getCRLs().isEmpty();
        boolean isRevocationCheckPassed = pkcs7.isRevocationValid();

        // Long-Term Validation (LTV) inference
        boolean isLongTermValidation = (isTimestampPresent || isTsp) && (isOcspPresent || hasCRL);

        return RevocationInfo.builder()
                .isTimeStampPresent(isTsp || isTimestampPresent)
                .isRevocationCheckPassed(isRevocationCheckPassed)
                .isLongTermValidation(isLongTermValidation)
                .build();
    }

    /**
     * Generates warnings for a signature based on its verification status.
     *
     * @param isValid Whether the signature is valid.
     * @param coversEntireDoc Whether the signature covers the entire document.
     * @param currentIndex The index of the current signature (0-based).
     * @param totalSignatures The total number of signatures in the PDF.
     * @return A list of warning messages for the signature.
     */
    private List<String> generateSignatureWarnings(boolean isValid, boolean coversEntireDoc,
                                                   int currentIndex, int totalSignatures) {
        List<String> warnings = new ArrayList<>();

        if (!isValid) warnings.add("Signature is invalid");
        if (!coversEntireDoc) warnings.add("Signature does not cover the entire document");
        if (totalSignatures > 1 && currentIndex < totalSignatures - 1) {
            warnings.add("Document has additional signatures after this one - possible tampering!");
        }

        return warnings;
    }

    /**
     * Builds document information for the PDF, including signature and integrity details.
     *
     * @param reader The PdfReader instance for the PDF document.
     * @param signatureNames The list of signature names in the PDF.
     * @param signatures The list of verified SignatureInfo objects.
     * @return A DocumentInfo object containing document-level verification details.
     */
    private DocumentInfo buildDocumentInfo(PdfReader reader, List<String> signatureNames, List<SignatureInfo> signatures) {
        int validCount = (int) signatures.stream().filter(SignatureInfo::isSignatureValid).count();
        String lastSigName = signatureNames.get(signatureNames.size() - 1);

        return DocumentInfo.builder()
                .totalPages(reader.getNumberOfPages())
                .verificationTime(new Date())
                .certification(buildCertificationInfo(reader))
                .integrityChecks(buildIntegrityCheck(reader, signatureNames, validCount))
                .verificationSummary(buildVerificationSummary(signatureNames.size(), validCount))
                .lastSignerName(lastSigName)
                .build();
    }

    /**
     * Builds certification information for the PDF document.
     *
     * @param reader The PdfReader instance for the PDF document.
     * @return A CertificationInfo object containing certification details.
     */
    private CertificationInfo buildCertificationInfo(PdfReader reader) {
        int certificationLevel = reader.getCertificationLevel();

        return CertificationInfo.builder()
                .isCertified(certificationLevel != PdfSignatureAppearance.NOT_CERTIFIED)
                .certificationType(mapCertificationType(certificationLevel))
                .modificationAllowed(certificationLevel != PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED)
                .build();
    }

    /**
     * Maps the certification level to a human-readable certification type.
     *
     * @param certificationLevel The certification level from PdfSignatureAppearance.
     * @return A string representing the certification type.
     */
    private String mapCertificationType(int certificationLevel) {
        switch (certificationLevel) {
            case PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED:
                return "CERTIFIED_CHANGES_NOT_ALLOWED";
            case PdfSignatureAppearance.CERTIFIED_FORM_FILLING:
                return "CERTIFIED_FORM_FILLING_ALLOWED";
            case PdfSignatureAppearance.CERTIFIED_FORM_FILLING_AND_ANNOTATIONS:
                return "CERTIFIED_FORM_FILLING_AND_ANNOTATIONS_ALLOWED";
            default:
                return "NOT_CERTIFIED";
        }
    }

    /**
     * Builds integrity check details for the PDF document.
     *
     * @param reader The PdfReader instance for the PDF document.
     * @param signatureNames The list of signature names in the PDF.
     * @param validCount The number of valid signatures.
     * @return An IntegrityCheck object containing integrity details.
     */
    private IntegrityCheck buildIntegrityCheck(PdfReader reader, List<String> signatureNames, int validCount) {
        String lastSigName = signatureNames.get(signatureNames.size() - 1);

        return IntegrityCheck.builder()
                .coversEntireDocument(reader.getAcroFields().signatureCoversWholeDocument(lastSigName))
                .coversRevision(true) // Placeholder
                .revisionNumber(reader.getAcroFields().getRevision(lastSigName))
                .hasAdditionalSignaturesAfter(signatureNames.size() > 1)
                .hasTampering(validCount < signatureNames.size())
                .warnings(new ArrayList<>())
                .build();
    }

    /**
     * Builds a summary of the signature verification results.
     *
     * @param totalSignatures The total number of signatures in the PDF.
     * @param validCount The number of valid signatures.
     * @return A VerificationSummary object containing summary details.
     */
    private VerificationSummary buildVerificationSummary(int totalSignatures, int validCount) {
        return VerificationSummary.builder()
                .totalSignatures(totalSignatures)
                .validSignatures(validCount)
                .invalidSignatures(totalSignatures - validCount)
                .allSignaturesValid(validCount == totalSignatures)
                .build();
    }

    /**
     * Serializes the verification result to a JSON string.
     *
     * @param result The PdfSignatureVerificationResult to serialize.
     * @return A pretty-printed JSON string representing the verification result.
     */
    public String serializeToJson(PdfSignatureVerificationResult result) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(result);
    }

    /**
     * Main method for testing the PdfSignatureVerifier with a sample PDF file.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        try {
            String filePath = "C:\\Users\\Pintu\\Desktop\\PDFs\\enableLtv_withRevoke_locked_signed.pdf";
            PdfSignatureVerifier pdfSignatureVerifier = new PdfSignatureVerifier();
            PdfSignatureVerificationResult result = pdfSignatureVerifier.verifySignatures(filePath);

            System.out.println(pdfSignatureVerifier.serializeToJson(result));
        } catch (GeneralSecurityException | IOException e) {
            System.err.println("Error processing PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
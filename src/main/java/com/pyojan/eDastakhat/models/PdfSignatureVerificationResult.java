package com.pyojan.eDastakhat.models;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class PdfSignatureVerificationResult {
    private DocumentInfo document;
    private List<SignatureInfo> signatures;

    @Data
    @Builder
    public static class DocumentInfo {
        private int totalPages;
        private Date verificationTime;
        private CertificationInfo certification;
        private IntegrityCheck integrityChecks;
        private VerificationSummary verificationSummary;
        private String lastSignerName;
    }

    @Data
    @Builder
    public static class CertificationInfo {
        private boolean isCertified;
        private String certificationType;
        private boolean modificationAllowed;
    }

    @Data
    @Builder
    public static class IntegrityCheck {
        private boolean hasTampering;
        private int revisionNumber;
        private boolean hasAdditionalSignaturesAfter;
        private boolean coversEntireDocument;
        private boolean coversRevision;
        private List<String> warnings;
    }

    @Data
    @Builder
    public static class VerificationSummary {
        private int totalSignatures;
        private int validSignatures;
        private int invalidSignatures;
        private boolean allSignaturesValid;
    }

    @Data
    @Builder
    public static class SignatureInfo {
        private int signatureIndex;
        private String signatureName;
        private int pageNumber;
        private Date signingTime;
        private String reason;
        private String location;
        private boolean signatureValid;
        private boolean coversEntireDocument;
        private boolean coversRevision;
        private CertificateInfo certificate;
        private RevocationInfo revocationInfo;
        private List<String> warnings;
    }

    @Data
    @Builder
    public static class CertificateInfo {
        private String subjectDN;
        private String issuerDN;
        private Date validFrom;
        private Date validTo;
        private String serialNumber;
        private String certificateType;
    }

    @Data
    @Builder
    public static class RevocationInfo {
        private boolean isTimeStampPresent;
        private boolean isRevocationCheckPassed;
        private boolean isLongTermValidation;
    }
}

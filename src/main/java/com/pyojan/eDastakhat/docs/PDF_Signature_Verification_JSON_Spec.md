# PDF Signature Verification Documentation

This document describes the structure and details of the JSON output for verifying a digitally signed PDF using `eDastakhat`.

---

## 📜 `Root Object`

The top-level object containing the results of verifying a digitally signed PDF.

| Key          | Type           | Description                              | Why It's Important                                                         |
|--------------|----------------|------------------------------------------|---------------------------------------------------------------------------|
| `document`   | Object         | Metadata about the entire document.      | Provides high-level details like page count, integrity, and certification. |
| `signatures` | Array of Objects | List of all digital signatures detected. | Allows individual validation of each signature for detailed analysis.      |

### Example
```json
{
  "document": { ... },
  "signatures": [ { ... }, { ... }, ... ]
}
```

---

## 📄 `Document Object`

Contains general information about the PDF and a summary of its verification status.

| Key                   | Type           | Description                               | Why It's Important                                                  |
|-----------------------|----------------|-------------------------------------------|-------------------------------------------------------------------|
| `totalPages`          | Number         | Total number of pages in the PDF.         | Ensures the document hasn’t been truncated or altered.             |
| `verificationTime`    | String         | Timestamp of when verification occurred.  | Validates time-sensitive checks, like certificate validity.        |
| `certification`       | Object         | Details about document certification.     | Indicates if the PDF restricts post-signing changes.               |
| `integrityChecks`     | Object         | Results of tampering/integrity checks.    | Detects unauthorized modifications or tampering.                   |
| `verificationSummary` | Object         | Summary of signature validity counts.     | Provides a quick overview for audits and reporting.                |
| `lastSignerName`      | String         | Name or ID of the last signer.           | Identifies the final signer for tracking or audit purposes.        |

### Example
```json
{
  "totalPages": 5,
  "verificationTime": "Jun 13, 2025 11:24:48 AM",
  "certification": { ... },
  "integrityChecks": { ... },
  "verificationSummary": { ... },
  "lastSignerName": "eDastakhat__P_5_866942"
}
```

---

## 🔐 `Certification Object`

Describes whether the document is certified and what changes are allowed.

| Key                  | Type    | Description                                                | Why It's Important                                              |
|----------------------|---------|------------------------------------------------------------|----------------------------------------------------------------|
| `isCertified`        | Boolean | True if the PDF is digitally certified.                    | Certified PDFs ensure trust and restrict unauthorized changes. |
| `certificationType`  | String  | Certification level (e.g., "NOT_CERTIFIED").                | Defines permissible changes after signing.                     |
| `modificationAllowed`| Boolean | Whether modifications are allowed per certification type.  | Ensures compliance with document modification restrictions.    |

### Example
```json
{
  "isCertified": false,
  "certificationType": "NOT_CERTIFIED",
  "modificationAllowed": true
}
```

---

## 🛡️ `IntegrityChecks Object`

Tracks the document’s structural and content integrity.

| Key                           | Type           | Description                                         | Why It's Important                                                         |
|-------------------------------|----------------|-----------------------------------------------------|---------------------------------------------------------------------------|
| `hasTampering`                | Boolean        | True if content was altered after signing.          | Flags potential security breaches or unauthorized changes.                |
| `revisionNumber`              | Number         | Revision number the signature applies to.           | Tracks the document version at the time of signing.                       |
| `hasAdditionalSignaturesAfter`| Boolean        | True if later signatures exist.                     | Indicates potential unauthorized signatures added later.                  |
| `coversEntireDocument`        | Boolean        | Whether the signature protects the entire document. | Ensures comprehensive protection against tampering.                       |
| `coversRevision`              | Boolean        | Whether the signature matches the revision number.  | Verifies alignment between signature and document version.                |
| `warnings`                    | Array of Strings | Validation warnings.                               | Highlights non-critical issues for further investigation.                 |

### Example
```json
{
  "hasTampering": false,
  "revisionNumber": 5,
  "hasAdditionalSignaturesAfter": true,
  "coversEntireDocument": true,
  "coversRevision": true,
  "warnings": []
}
```

---

## 📊 `VerificationSummary Object`

Summarizes the verification results for all signatures in the document.

| Key                 | Type    | Description                                      | Why It's Important                                         |
|---------------------|---------|--------------------------------------------------|-----------------------------------------------------------|
| `totalSignatures`   | Number  | Total number of signatures found.                | Assesses the completeness of the signing workflow.        |
| `validSignatures`   | Number  | Number of signatures that passed validation.     | Indicates cryptographically trusted signatures.           |
| `invalidSignatures` | Number  | Number of signatures that failed validation.     | Flags tampered or revoked signatures.                     |
| `allSignaturesValid`| Boolean | True if all signatures are valid.                | Provides a quick pass/fail for the document’s signatures. |

### Example
```json
{
  "totalSignatures": 5,
  "validSignatures": 5,
  "invalidSignatures": 0,
  "allSignaturesValid": true
}
```

---

## ✍️ `Signature Object`

Details for a specific digital signature in the document. Each signature is an object in the `signatures` array.

| Key                   | Type           | Description                                     | Why It's Important                                    |
|-----------------------|----------------|-------------------------------------------------|------------------------------------------------------|
| `signatureIndex`      | Number         | Order of the signature in the document.         | Tracks the sequence of signing events.               |
| `signatureName`       | String         | Unique identifier for the signature field.      | Used for referencing or debugging in PDF forms.      |
| `pageNumber`          | Number         | Page where the signature appears.               | Helps locate the visible signature for review.       |
| `signingTime`         | String         | Timestamp when the signature was applied.       | Ensures certificate validity at the time of signing. |
| `reason`              | String         | Signer’s stated reason (e.g., "Approved").      | Provides context for the signature’s purpose.        |
| `location`            | String         | Signer’s location (optional).                   | Indicates where the signing occurred.                |
| `signatureValid`      | Boolean        | Whether the signature is cryptographically valid.| Confirms the signature’s trustworthiness.            |
| `coversEntireDocument`| Boolean        | Whether the signature protects the entire PDF.  | Ensures full document protection.                    |
| `coversRevision`      | Boolean        | Whether the signature matches the revision.     | Verifies alignment with document version.            |
| `certificate`         | Object         | Details of the signer’s certificate.            | Identifies the signer and their certificate’s trust. |
| `revocationInfo`      | Object         | Results of revocation and timestamp checks.     | Ensures the certificate wasn’t revoked at signing.   |
| `warnings`            | Array of Strings | Signature-specific issues or warnings.         | Highlights potential issues for further review.      |

### Example
```json
{
  "signatureIndex": 1,
  "signatureName": "eDastakhat__P_1_449953",
  "pageNumber": 1,
  "signingTime": "Jun 12, 2025 1:27:21 PM",
  "reason": "Approved",
  "location": "Delhi",
  "signatureValid": true,
  "coversEntireDocument": false,
  "coversRevision": false,
  "certificate": { ... },
  "revocationInfo": { ... },
  "warnings": [
    "Signature does not cover the entire document",
    "Document has additional signatures after this one - possible tampering!"
  ]
}
```

---

## 📜 `Certificate Object`

Details of the signer’s digital certificate (e.g., X.509).

| Key              | Type   | Description                             | Why It's Important                         |
|------------------|--------|-----------------------------------------|-------------------------------------------|
| `subjectDN`      | String | Signer’s Distinguished Name (identity). | Identifies the signer.                    |
| `issuerDN`       | String | Certificate Authority (CA) issuer.      | Confirms the certificate’s issuing authority. |
| `validFrom`      | String | Start of certificate validity period.   | Ensures the certificate was valid at signing. |
| `validTo`        | String | End of certificate validity period.     | Verifies the certificate hasn’t expired.  |
| `serialNumber`   | String | Unique certificate identifier.          | Used for revocation checks (e.g., CRL/OCSP). |
| `certificateType`| String | Type of certificate (e.g., "Document Signer Certificate"). | Indicates the certificate’s purpose.     |

### Example
```json
{
  "subjectDN": "C=IN,O=Capricorn Identity Services Pvt. Ltd.,OU=Technical support,TelephoneNumber=...,PostalCode=110092,ST=Delhi,SERIALNUMBER=...,CN=Pintu Prajapati",
  "issuerDN": "C=IN,O=Capricorn Identity Services Pvt Ltd.,OU=Certifying Authority,CN=Capricorn Sub CA for Organisation DSC 2022",
  "validFrom": "Jun 9, 2025 11:58:46 AM",
  "validTo": "Jun 9, 2026 11:58:46 AM",
  "serialNumber": "5d877912d2",
  "certificateType": "Document Signer Certificate"
}
```

---

## 🔐 `RevocationInfo Object`

Results of certificate revocation and timestamp validation.

| Key                      | Type    | Description                                 | Why It's Important                                                      |
|--------------------------|---------|---------------------------------------------|-----------------------------------------------------------------------|
| `isTimeStampPresent`     | Boolean | Whether a cryptographic timestamp exists.   | Locks the signature time for audit and legal purposes.                |
| `isRevocationCheckPassed`| Boolean | Whether CRL/OCSP revocation checks passed. | Confirms the certificate wasn’t revoked at the time of signing.       |
| `isLongTermValidation`   | Boolean | Whether long-term validation (LTV) is supported. | Ensures future verifiability even if certificates expire.            |

### Example
```json
{
  "isTimeStampPresent": false,
  "isRevocationCheckPassed": true,
  "isLongTermValidation": false
}
```

---

## Notes
- The structure is designed to be generic and applicable to any system processing PDF signature verification results.
- Examples are directly derived from the provided JSON, reflecting realistic data for a 5-page PDF with five signatures.
- Timestamps are stored as strings (e.g., `"Jun 12, 2025 1:27:21 PM"`) for human readability and compatibility.
- Warnings in `integrityChecks` and `signatures` highlight potential issues, such as partial document coverage or additional signatures that could indicate tampering.
- `certification` and `revocationInfo` provide information about the signer's certificate and revocation status, respectively.
- `verificationSummary` and `lastSignerName` provide quick overview and tracking information for audits and reporting purposes.
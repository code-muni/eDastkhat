# eDastakhat CLI User Guide

## Table of Contents
- [Overview](#-overview)
- [Basic Usage](#-basic-usage)
- [Command-Line Options](#-command-line-options)
  - [General Options](#general-options)
  - [Input/Output Options](#inputoutput-options)
  - [Configuration & Security Options](#configuration--security-options)
  - [Certificate Options](#certificate-options)
  - [Proxy Options](#proxy-options)
- [Validation Rules](#validation-rules)
- [JSON Configuration File](#-json-configuration-file)
- [Signature Verification](#-signature-verification)
  - [Verification Options](#verification-options)
  - [Example – Verify a PDF Signature](#example--verify-a-pdf-signature)
  - [Output Structure for Verification](#output-structure-for-verification)
    - [STDOUT – Verification Success](#stdout--verification-success)
    - [STDERR – Verification Failure](#stderr--verification-failure)
- [Output Structure](#-output-structure)
  - [STDOUT – Signing Success](#stdout--signing-success)
  - [STDERR – Signing Failure](#stderr--signing-failure)
- [Examples](#-examples)


## Overview
---
`eDastakhat` CLI (Command-Line Interface) is a powerful tool for digitally signing and verifying PDF and XML documents using digital certificates. Built for automation, scripting, and enterprise workflows, it supports:

- **Signing PDFs and Enveloped XML**: Apply digital signatures that comply with legal and compliance requirements.
- **Signature Verification**: Validate digital signatures in PDF files for authenticity and integrity.

### Key Features:
- **Multiple certificate sources**:
  - PFX/PKCS#12 files
  - Hardware tokens (PKCS#11 compatible)
  - Windows Certificate Store (Windows only)
- **Flexible PDF signature configuration** via JSON.
- **Supports encrypted PDFs**.
- **LTV (Long-Term Validation) and timestamping**.
- **Proxy support** for signing/validation behind corporate firewalls.

The CLI is structured to produce clear and structured output on **STDOUT** for successful operations and **STDERR** for errors, 

---

## Prerequisites
Before using **eDastakhat CLI**, ensure your system meets the following requirements:

### 1. **Java Runtime Environment (JRE)**
- **Supported Versions**: Only **Java LTS (Long-Term Support) versions 8 or 11** are officially supported.
- **How to Check**:
  ```bash
  java -version
  ```

### 2. **Operating System**
- **Windows**: Windows 7 or later (64-bit).
- **Linux**: Most modern distributions (tested on Ubuntu 20.04+, RHEL/CentOS 7+).
- **macOS**: 10.15 (Catalina) or later.

### 4. **Additional Dependencies**
- **For Hardware Tokens**:
  - PKCS#11 driver installed (e.g., [eToken](https://www.globalsign.com/en-sg/support/etoken-drivers), [SafeNet](https://supportportal.thalesgroup.com/)).
- **For Timestamping (LTV)**:
  - Internet access to TSA servers (if configured in JSON).
- **Proxy Support**: Configure firewall/proxy to allow connections to:
  - CRL/OCSP servers (for certificate validation).
  - TSA servers (if timestamping is enabled).

### Verification Steps
1. Confirm Java is installed:
   ```bash
   java -version  # Should show "1.8" or "11.x.x"
   ```

---

## Basic Usage
-------------
Run the CLI using the `java -jar eDastakhat.jar` command followed by the necessary options.

## Command-Line Options
------------------------

### General Options
| Option | Long Option | Description | Example |
|--------|-------------|-------------|---------|
| `-v`   | `--version` | Display application version. | `java -jar eDastakhat.jar -v` |
| `-h`   | `--help`    | Display help message. | `java -jar eDastakhat.jar -h` |

### Input/Output Options
| Option | Long Option | Description                                                           | Example |
|--------|-------------|-----------------------------------------------------------------------|---------|
| `-i`   | `--input`   | Path of the input **PDF** or **XML** file to be signed (required).    | `-i input.pdf` |
| `-o`   | `--output`  | Output path for the signed file. Defaults to "input_signed.pdf" or "input_signed.xml".     | `-o signed.pdf` |

### Configuration & Security Options
| Option | Long Option | Description | Example |
|--------|-------------|-------------|---------|
| `-c`   | `--config`  | Path to signature configuration JSON file. **Required only for PDF signing.** | `-c config.json` |
| `-pw`  | `--password`| Password for encrypted PDF (optional). | `-pw secret` |

### Certificate Options
| Option | Long Option | Description                                                                 | Example |
|--------|-------------|-----------------------------------------------------------------------------|---------|
| `-pf`  | `--pfx`     | Path to PFX/PKCS#12 file.                                                   | `-pf cert.pfx` |
| `-t`   | `--token`   | Path to PKCS#11 library (for hardware tokens).                              | `-t /usr/local/lib/xyz.so` |
| `-ts`  | `--tokenSerial` | Serial number of the PKCS#11 token. (Optional)                                | `-ts 12345678` |
| `-p`   | `--pin`     | PIN for PFX or PKCS#11 token (required if `--pfx` or `--token` is used).    | `-p 1234` |
| `-cs`  | `--certificateSerial` | Serial number of certificate to sign with. Required unless `--pfx` is used. | `-cs 789ABC` |

### Proxy Options
| Option | Long Option | Description | Example                                             |
|--------|-------------|-------------|-----------------------------------------------------|
| `-pxh` | `--proxyHost` | Proxy hostname or IP.                               | `--proxyHost proxy.example.com` |
| `-pxp` | `--proxyPort` | Proxy port number.                                  | `--proxyPort 8080` |
| `-pxu` | `--proxyUser` | Proxy username (if authentication is needed).       | `--proxyUser user1` |
| `-pxw` | `--proxyPass` | Proxy password (if authentication is needed).       | `--proxyPass pass123` |
| `-pxs` | `--proxySecure` | Use HTTPS for proxy connection. (if HTTPS required) | `--proxySecure true` |

## Validation Rules
If you're on Windows and not using `-v` or `-h`, the following apply:
- `-i` and `-c` are **required** in case of the PDF signing.
- If using `--pfx` or `--token`, `--pin` is **required**.
- If not using `--pfx`, `-cs/--certificateSerial` is **required**.
- `--tokenSerial` is optional. **If multiple tokens are available**, and it is not provided, the first available token will be selected automatically.

## JSON Configuration File
---------------------------

The `--config` option expects a `JSON` file containing parameters that define how the digital signature will appear on the PDF document. **Note: The keys in the JSON file are case-sensitive**.

### Example
```json
{
  "page": "F,L,2-4",
  "coord": [10, 10, 250, 100],
  "reason": "Approval of Document",
  "location": "New York, USA",
  "customText": "Digitally Approved by John Doe",
  "greenTick": true,
  "changesAllowed": false,
  "enableLtv": true,
  "timestamp": {
    "enabled": true,
    "url": "https://tsa.example.com/tsa",
    "username": "tsa_user",
    "password": "tsa_password"
  }
}
```

### Fields:

- `options.page`: Specifies where to place the signature(s) on the PDF document:
  - `"F"` = First page
  - `"L"` = Last page
  - `"A"` = All pages
  - `<number>`: The specific page number (e.g., "3").
  - `<number>,<number>,...`: A comma-separated list of page numbers (e.g., "1,3,5").
  - `<start>-<end>`: A range of pages (inclusive) (e.g., "1–5").
  - Combined formats are supported (e.g., "F,3,5-7,L").
- `options.reason`: An array `[x, y, width, height]` defining the coordinates and dimensions of the signature appearance on the page, **typically in user space units (points)**.
- `options.reason`: The reason for signing the document.
- `options.location`:  The geographical location where the signing occurred.
- `options.customText`:  Additional text to be displayed along with the signature.
- `options.greenTick`: A boolean value (`true` or `false`) indicating whether to display a` green checkmark symbol` with the signature, often used as a visual cue for a valid signature.
- `options.changesAllowed`: A boolean value indicating whether changes to the document should be allowed after signing. Setting this to `false` invalidate the signature upon modification.
- `options.enableLtv`: A boolean value to enable `Long-Term Validation (LTV)`. LTV embeds necessary information (like revocation data) to ensure the signature remains verifiable over time, even if the signing certificate expires or is revoked.
- `options.timestamp`: Configuration for adding a timestamp from a `Time Stamping Authority (TSA)`, which provides a trusted record of when the document was signed:
  - `enabled`:  Set to `true` to enable timestamping.
  - `url`: The URL of the Time Stamping Authority server.
  - `username`: Optional username for authenticating with the TSA server.
  - `password`:  Optional password for authenticating with the TSA server.


## Signature Verification
---------------------------

You can verify digital signatures present in a PDF document using the `--verify` (`-vf`) option. This is particularly useful for audit, compliance, and validation workflows.

### Verification Options
| Option | Long Option | Description | Example |
|--------|-------------|-------------|---------|
| `-vf`  | `--verify`  | Path of the **PDF** file to verify for digital signatures. | `-vf signed.pdf` |

When this option is used, the tool will:
- Detect and parse all digital signatures embedded in the given PDF.
- Display verification results, including whether each signature is valid, who signed the document, and the certificate details.

### Example – Verify a PDF Signature
```bash
java -jar eDastakhat.jar -vf signed.pdf
```

### Output Structure for Verification

#### STDOUT – Verification Success
```json
{
  "status": "SUCCESS",
  "data": {
    "signatureCount": 2,
    "signatures": [
      {
        "name": "John Doe",
        "reason": "Approval",
        "location": "New York",
        "signingTime": "2025-06-12T15:25:43Z",
        "valid": true,
        "certificate": {
          "subject": "CN=John Doe, O=Company, C=US",
          "issuer": "CN=Root CA, O=Trusted Org, C=US",
          "serialNumber": "5A7D981C",
          "validFrom": "2023-01-01T00:00:00Z",
          "validTo": "2026-01-01T00:00:00Z"
        }
      }
    ]
  }
}
```
For the more information about the `certificate` object, refer to the [Certificate Object](/src/main/java/com/pyojan/eDastakhat/docs/PDF_Signature_Verification_JSON_Spec.md) section.

#### STDERR – Verification Failure
```json
{
  "status": "ERROR",
  "data": {
    "message": "No valid digital signatures found in the PDF.",
    "stackTrace": "com.pyojan.eDastakhat.exceptions.SignatureNotFoundException: No digital signatures found in the PDF.\n\tat ..."
  }
}
```

## Output Structure
-------------------

### STDOUT – Signing Success
```json
{
  "status": "SUCCESS",
  "data": {
    "message": "Signed file successfully.",
    "signedFilePath": "C:\\Users\\Pintu\\Desktop\\PDFs\\PDF test for search By text_signed.pdf"
  }
}
```

### STDERR – Signing Failure
```json
{
  "status": "ERROR",
  "data": {
    "message": "Certificate with serial 5d877912d2s not found",
    "stackTrace": "com.pyojan.eDastakhat.exceptions.CertificateNotFoundException: ..."
  }
}
```

## Examples
-----------

### Sign PDF with PFX
```bash
java -jar eDastakhat.jar -pf cert.pfx -p 1234 -c config.json -i input.pdf -o signed.pdf
```

### Sign PDF with Hardware Token
```bash
java -jar eDastakhat.jar -t /path/to/pkcs11.so -ts 12345 -p 1234 -c config.json -i input.pdf -o signed.pdf
```

### Sign Encrypted PDF
```bash
java -jar eDastakhat.jar -pf cert.pfx -p 1234 -pw secret -c config.json -i input.pdf -o signed.pdf
```

### Sign with Windows Certificate Store
```bash
java -jar eDastakhat.jar -c config.json -i input.pdf -o signed.pdf
```

### Sign PDF via Proxy
```bash
java -jar eDastakhat.jar -pf cert.pfx -p 1234 -c config.json -i input.pdf -o signed.pdf --proxyHost proxy.example.com --proxyPort 8080 --proxyUser user1 --proxyPass pass123
```

### Sign XML with PFX Certificate
```bash
java -jar eDastakhat.jar -pf cert.pfx -p 1234 -i input.xml -o signed.xml
```

### Sign XML with Hardware Token and Token Serial
```bash
java -jar eDastakhat.jar -t /usr/local/lib/libpkcs11.so -ts 12345678 -p 1234 -i input.xml -o signed.xml
```

### Sign XML with Windows Certificate Store
```bash
java -jar eDastakhat.jar -cs 89ABCD1234 -i input.xml -o signed.xml
```

### Sign XML Behind a Proxy
```bash
java -jar eDastakhat.jar -pf cert.pfx -p 1234 -i input.xml -o signed.xml --proxyHost proxy.example.com --proxyPort 8080
```

### Verify PDF Signature
```bash
java -jar eDastakhat.jar -vf signed_contract.pdf
```
This command will check all embedded signatures in `signed_contract.pdf`, and output detailed JSON results to STDOUT.
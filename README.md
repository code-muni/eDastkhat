eDastakhat CLI User Guide
=====================================

## Overview
---
The `eDastakhat` CLI (Command-Line Interface) empowers you to digitally sign PDF and enveloped XML documents using digital certificates. This command-line tool is particularly useful for automation, scripting, and batch processing of document signing. For XML documents, eDastakhat supports the standard practice of embedding digital signatures directly within the XML structure.

**It supports certificate sources such as:**
 - PFX/PKCS#12 files 
 - Hardware tokens (PKCS#11)
 - Windows Certificate Store (Windows only)

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
| `-pxh` | `--proxyHost` |             | Proxy hostname or IP.                               | `--proxyHost proxy.example.com` |
| `-pxp` | `--proxyPort` |             | Proxy port number.                                  | `--proxyPort 8080` |
| `-pxu` | `--proxyUser` |             | Proxy username (if authentication is needed).       | `--proxyUser user1` |
| `-pxw` | `--proxyPass` |             | Proxy password (if authentication is needed).       | `--proxyPass pass123` |
| `-pxs` | `--proxySecure` |            | Use HTTPS for proxy connection. (if HTTPS required) | `--proxySecure true` |

## Validation Rules (Windows Only)
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
    - `<start>-<end>`: A range of pages (inclusive) (e.g., "1-5"). 
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


### Output Structure
The eDastakhat CLI returns structured JSON output to standard output (STDOUT) and standard error (STDERROR) for easy integration with other tools, scripts, or monitoring systems.

##### STDOUT – Success Output
If the operation completes successfully, the CLI prints a JSON object to STDOUT:
```bash
{
  "status": "SUCCESS",
  "data": {
    "message": "Signed file successfully.",
    "signedFilePath": "C:\\Users\\Pintu\\Desktop\\PDFs\\PDF test for search By text_signed.pdf"
  }
}
```
**Description**:<br />
Indicates the signing process was completed successfully. Includes a success message and the full path to the signed output file.

##### STDERR – Error Output
If the operation fails, the CLI prints a JSON object to STDERR:
```bash
{
  "status": "ERROR",
  "data": {
    "message": "Certificate with serial 5d877912d2s not found",
    "stackTrace": "com.pyojan.eDastakhat.exceptions.CertificateNotFoundException: Certificate with serial 5d877912d2s not found\r\n\tat com.pyojan.eDastakhat.libs.keyStore.WindowKeyStore.findAliasByCertSerial(WindowKeyStore.java:90)\r\n\tat com.pyojan.eDastakhat.libs.keyStore.WindowKeyStore.getPrivateKey(WindowKeyStore.java:64)\r\n\tat com.pyojan.eDastakhat.services.PdfSigner.executeSign(PdfSigner.java:95)\r\n\tat com.pyojan.eDastakhat.SignerController.handleExecuteSigningRequest(SignerController.java:39)\r\n\tat com.pyojan.eDastakhat.EDastakhatApplication.main(EDastakhatApplication.java:36)\r\n"
  }
}
```
**Description:** <br />
Indicates that an error occurred during execution. Includes a human-readable error message and a technical stack trace for debugging purposes.


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
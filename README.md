[//]: # (# eDastakhat CLI User Guide)

[//]: # (=====================================)

[//]: # ()
[//]: # (## Overview)

[//]: # (------------)

[//]: # ()
[//]: # (The `eDastakhat` CLI &#40;Command-Line Interface&#41; is a tool that allows you to digitally sign PDF documents using digital certificates. You can use certificates stored in files &#40;like PFX&#41; or hardware tokens.)

[//]: # ()
[//]: # (## Basic Usage)

[//]: # (-------------)

[//]: # ()
[//]: # (To use the `eDastakhat` CLI, you will need to run the `java eDastakhat.jar` command in your terminal or command prompt, followed by options that specify what you want to do.)

[//]: # ()
[//]: # (### Command-Line Options)

[//]: # ()
[//]: # (Here is a breakdown of the available options:)

[//]: # ()
[//]: # (#### **General Options**)

[//]: # ()
[//]: # (| **Option** | **Long Option** | **Description** | **Example** |)

[//]: # (| --- | --- | --- | --- |)

[//]: # (| `-v` | `--version` | Display the application's version. | `java eDastakhat.jar -v` or `java eDastakhat.jar --version` |)

[//]: # (| `-h` | `--help` | Display help information. | `java eDastakhat.jar -h` or `java eDastakhat.jar --help` |)

[//]: # ()
[//]: # (#### **Input and Output Options**)

[//]: # ()
[//]: # (| **Option** | **Long Option** | **Description** | **Example** |)

[//]: # (| --- | --- | --- | --- |)

[//]: # (| `-i` | `--input` | Path to the PDF file you want to sign. | `java eDastakhat.jar -i document.pdf` |)

[//]: # (| `-o` | `--output` | Path to save the signed PDF. If not provided, it saves as "input_signed.pdf". | `java eDastakhat.jar -o signed_document.pdf` |)

[//]: # ()
[//]: # (#### **Certificate and Token Options**)

[//]: # ()
[//]: # (| **Option** | **Long Option** | **Description**                                                        | **Example** |)

[//]: # (| --- | --- |------------------------------------------------------------------------| --- |)

[//]: # (| `-pf` | `--pfx` | Path to your PFX/PKCS#12 certificate file.                             | `java eDastakhat.jar -pf mycert.pfx` |)

[//]: # (| `-t` | `--token` | Path to the PKCS#11 library &#40;for hardware tokens&#41;.                     | `java eDastakhat.jar -t /path/to/pkcs11.so` |)

[//]: # (| `-ts` | `--tokenSerial` | Serial number of your PKCS#11 token. If multiple tokens are available, specify the one you want to use. | `java eDastakhat.jar -ts 12345` |)

[//]: # (| `-p` | `--pin` | PIN for your security token or PFX file.                               | `java eDastakhat.jar -p 1234` |)

[//]: # ()
[//]: # (#### **Password and Configuration Options**)

[//]: # ()
[//]: # (| **Option** | **Long Option** | **Description** | **Example** |)

[//]: # (| --- | --- | --- | --- |)

[//]: # (| `-pw` | `--password` | Password for the PDF file, if it's encrypted. | `java eDastakhat.jar -pw secret` |)

[//]: # (| `-c` | `--config` | Path to the JSON configuration file for the signature. | `java eDastakhat.jar -c config.json` |)

[//]: # ()
[//]: # (### JSON Configuration File)

[//]: # (---------------------------)

[//]: # ()
[//]: # (The `-c` option requires a JSON configuration file. This file contains settings for the digital signature.)

[//]: # ()
[//]: # (#### **JSON Object Structure**)

[//]: # ()
[//]: # (The JSON object has the following structure:)

[//]: # (```json)

[//]: # ({)

[//]: # (  "certInfo": "572b399b25",)

[//]: # (  "options": {)

[//]: # (    "page": "F",)

[//]: # (    "coord": [10, 10, 250, 100],)

[//]: # (    "reason": "Approval",)

[//]: # (    "location": "New York",)

[//]: # (    "customText": "Approved by John Doe",)

[//]: # (    "greenTick": true,)

[//]: # (    "changesAllowed": false,)

[//]: # (    "enableLtv": false,)

[//]: # (    "timestamp": {)

[//]: # (      "enabled": false,)

[//]: # (      "url": "http://timestamp.comodoca.com",)

[//]: # (      "username": "",)

[//]: # (      "password": "")

[//]: # (    })

[//]: # (  })

[//]: # (})

[//]: # (```)

[//]: # (#### **JSON Object Properties**)

[//]: # ()
[//]: # (* **certInfo**: A unique identifier for the certificate.)

[//]: # (* **options**: Contains settings for the digital signature.)

[//]: # (    + **page**: Specifies where the signature will be placed in the PDF. Acceptable values are:)

[//]: # (        - `"F"` for the first page)

[//]: # (        - `"L"` for the last page)

[//]: # (        - `"A"` for all pages)

[//]: # (        - a specific page number)

[//]: # (    + **coord**: The coordinates for the signature &#40;x, y, width, height&#41;.)

[//]: # (    + **reason**: The reason for signing the document.)

[//]: # (    + **location**: The location where the document was signed.)

[//]: # (    + **customText**: Custom text to be displayed with the signature.)

[//]: # (    + **greenTick**: A boolean indicating whether to display a green tick with the signature.)

[//]: # (    + **changesAllowed**: A boolean indicating whether changes are allowed to the document after signing.)

[//]: # (    + **enableLtv**: A boolean indicating whether to enable Long Term Validation &#40;LTV&#41; for the signature.)

[//]: # (    + **timestamp**: Contains settings for applying a timestamp to the signature.)

[//]: # (      - **enabled**: A boolean indicating whether timestamping is enabled.)

[//]: # (      - **url**: &#40;Optional&#41; The Time Stamp Authority &#40;TSA&#41; URL to be used.)

[//]: # (      - **username**: *&#40;Optional&#41;* Username for TSA authentication, if required.)

[//]: # (      - **password**: *&#40;Optional&#41;* Password for TSA authentication, if required.)

[//]: # ()
[//]: # ()
[//]: # (### Examples)

[//]: # (------------)

[//]: # ()
[//]: # (Here are some examples of how to use the `eDastakhat` command with different options:)

[//]: # ()
[//]: # (#### Signing a PDF with a PFX Certificate)

[//]: # ()
[//]: # (```bash)

[//]: # (java eDastakhat.jar -pf mycert.pfx -c config.json -i input.pdf -o output.pdf)

[//]: # (```)

[//]: # ()
[//]: # (#### Signing a PDF with a Hardware Token)

[//]: # ()
[//]: # (```bash)

[//]: # (java eDastakhat.jar -t /path/to/pkcs11.so -ts 12345 -p 1234 -c config.json -i input.pdf -o output.pdf)

[//]: # (```)

[//]: # ()
[//]: # (#### Signing an Encrypted PDF)

[//]: # ()
[//]: # (```bash)

[//]: # (java eDastakhat.jar -t /path/to/pkcs11.so -ts 12345 -p 1234 -c config.json -i input.pdf -o output.pdf -pw secret)

[//]: # (```)

[//]: # ()
[//]: # (#### Signing a PDF with Windows keystore)

[//]: # (```bash)

[//]: # (java eDastakhat.jar -c config.json -i input.pdf -o output.pdf -pw secret)

[//]: # (```)

[//]: # ()
[//]: # (Note: Replace the placeholders &#40;`mycert.pfx`, `config.json`, `input.pdf`, `output.pdf`, etc.&#41; with the actual file paths and values for your specific use case.)



eDastakhat CLI User Guide
=====================================

## Overview
------------
The `eDastakhat` CLI (Command-Line Interface) allows you to digitally sign PDF documents using digital certificates. You can use certificates stored in files (like PFX) or hardware tokens. Windows users can also use certificates from the Windows Certificate Store.

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
| Option | Long Option | Description | Example |
|--------|-------------|-------------|---------|
| `-i`   | `--input`   | Path or URL of the input PDF file to be signed (required on Windows). | `-i input.pdf` |
| `-o`   | `--output`  | Output path for the signed PDF. Defaults to "input_signed.pdf". | `-o signed.pdf` |

### Configuration & Security Options
| Option | Long Option | Description | Example |
|--------|-------------|-------------|---------|
| `-c`   | `--config`  | Path to signature configuration JSON file (required on Windows). | `-c config.json` |
| `-pw`  | `--password`| Password for encrypted PDF (optional). | `-pw secret` |

### Certificate Options
| Option | Long Option | Description | Example |
|--------|-------------|-------------|---------|
| `-pf`  | `--pfx`     | Path to PFX/PKCS#12 file. | `-pf cert.pfx` |
| `-t`   | `--token`   | Path to PKCS#11 library (for hardware tokens). | `-t /usr/local/lib/xyz.so` |
| `-ts`  | `--tokenSerial` | Serial number of the PKCS#11 token. Optional. | `-ts 12345678` |
| `-p`   | `--pin`     | PIN for PFX or PKCS#11 token (required if `--pfx` or `--token` is used). | `-p 1234` |
| `-cs`  | `--certificateSerial` | Serial number of certificate to sign with. Required unless `--pfx` is used. | `-cs 789ABC` |


### Proxy Options
| Option | Long Option | Description | Example                                             |
|--------|-------------|-------------|-----------------------------------------------------|
| `-pxh` | `--proxyHost` |             | Proxy hostname or IP.                               | `--proxyHost proxy.example.com` |
| `-pxp` | `--proxyPort` |             | Proxy port number.                                  | `--proxyPort 8080` |
| `-pxu` | `--proxyUser` |             | Proxy username (if authentication is needed).       | `--proxyUser user1` |
| `-pxw` | `--proxyPass` |             | Proxy password (if authentication is needed).       | `--proxyPass pass123` |
| `-pxs` | `--proxySecure` |            | Use HTTPS for proxy connection. (if HTTPS required) | `--proxySecure true` |

### Watermark Option
| Option | Long Option | Description | Example |
|--------|-------------|-------------|---------|
| `-nw`  | `--no-watermark` | Disable watermark in the signed PDF. | `-nw` |

## Validation Rules (Windows Only)
If you're on Windows and not using `-v` or `-h`, the following apply:
- `-i` and `-c` are **required**.
- If using `--pfx` or `--token`, `--pin` is **required**.
- If not using `--pfx`, `--certificateSerial` is **required**.
- `--tokenSerial` is optional if only one token is present.

## JSON Configuration File
---------------------------

The `--config` option expects a JSON file specifying signature parameters.

### Example
```json
{
  "page":"A",
  "coord":[10,10,250,100],
  "reason":"Approval",
  "location":"New York",
  "customText":"Approved by John Doe",
  "greenTick":true,
  "changesAllowed":false,
  "enableLtv":true,
  "timestamp":{
    "enabled":true,
    "url":"",
    "username":"",
    "password":""
  }
}
```

### Fields:

- `options.page`: Where to place the signature:
    - `"F"` = First page
    - `"L"` = Last page
    - `"A"` = All pages
    - Page number (e.g., `3`)
- `options.coord`: Coordinates `[x, y, width, height]` of the signature box.
- `options.reason`: Reason for signing.
- `options.location`: Location where the signing took place.
- `options.customText`: Additional text shown with the signature.
- `options.greenTick`: Show green tick symbol (true/false).
- `options.changesAllowed`: Allow changes after signing (true/false).
- `options.enableLtv`: Enable Long-Term Validation (LTV) (true/false).
- `options.timestamp`: Configure timestamping:
    - `enabled`: Enable TSA timestamping.
    - `url`: URL of the Time Stamping Authority.
    - `username`: Optional TSA username.
    - `password`: Optional TSA password.


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

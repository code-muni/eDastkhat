package com.pyojan.eDastakhat.services.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.*;
import com.pyojan.eDastakhat.exceptions.SignerException;
import com.pyojan.eDastakhat.exceptions.UserCancelledException;
import net.sf.oval.constraint.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
     * @param isLtv               whether to include Long-Term Validation (LTV) information
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
                    location,
                    customText,
                    (X509Certificate) certChain[0]
            );

            ExternalDigest digest = new BouncyCastleDigest();
            ExternalSignature signature = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, provider);

            List<CrlClient> crlList = isLtv ? prepareLtvComponents(certChain) : new ArrayList<>();
            OcspClient ocspClient = isLtv ? new OcspClientBouncyCastle(null) : null;

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

        } catch (Exception e) {
            if (e instanceof SignatureException) {
                throw new UserCancelledException("Signing was cancelled by the user", e);
            }
            throw new SignerException("PDF signing failed in sign method : " + e.getMessage(), e);
        } finally {
            String errorMessage = closeResources(reader, stamper);
            if (errorMessage != null) {
                generateErrorResponse(new SignerException(errorMessage)); // You may want to log or throw instead
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
     * Prepares the list of CRL (Certificate Revocation List) clients for Long-Term Validation (LTV).
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
     * @param withLTV whether Long-Term Validation (LTV) is included in the signature
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
        } catch (Exception e) {
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
            String location,
            String customText,
            X509Certificate cert
    ) throws DocumentException {
        appearance.setSignatureCreator("codemuni-eDastakhat");

        // Set coordinates only if they are provided
        if (coord != null && coord.length == 4) {
            Rectangle rectangle = new Rectangle(coord[0], coord[1], coord[2], coord[3]);
            appearance.setVisibleSignature(rectangle, pageNumber, fieldName);
            this.setSignatureBackgroundColor(appearance, new BaseColor(252, 252, 252));
        }

        appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.NAME_AND_DESCRIPTION);
        appearance.setAcro6Layers(!isGreenTrick);

        int certificationStatus = isChangesAllowed ? PdfSignatureAppearance.NOT_CERTIFIED : PdfSignatureAppearance.CERTIFIED_FORM_FILLING;
        appearance.setCertificationLevel(certificationStatus);

        if(reason != null && !reason.isEmpty()) appearance.setReason(reason);
        if(location != null && !location.isEmpty()) appearance.setLocation(location);

        this.setCustomAppearance(appearance, customText, cert, isChangesAllowed, reason, location);
    }

    /**
     * Sets a custom appearance for the digital signature, with dynamic font sizing and bottom-to-top text placement.
     * The font size is calculated to ensure all text fits within the signature rectangle, regardless of box size or text length.
     *
     * @param appearance        the PdfSignatureAppearance object to customize
     * @param customText        additional custom text to include in the signature
     * @param cert              the X509 certificate used for signing
     * @param isChangesAllowed  flag indicating if changes are allowed after signing
     * @param reason            reason for signing
     * @param location          location of signing
     * @throws DocumentException if any document-related error occurs
     */
    private void setCustomAppearance(
            PdfSignatureAppearance appearance,
            String customText,
            X509Certificate cert,
            boolean isChangesAllowed,
            String reason,
            String location
    ) throws DocumentException {
        // Get the layer for text and content display (Layer 2)
        PdfTemplate layer2 = appearance.getLayer(2);

        // Define the rectangle area with padding
        float padding = 1f;
        float paddingTop = 20f; // Increased top padding to avoid overlap with "Signature Valid"
        Rectangle rect = new Rectangle(
                padding, padding,
                appearance.getAppearance().getWidth(),
                appearance.getAppearance().getHeight() - paddingTop
        );

        // Format the current timestamp
        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a (xxx)");
        String formattedDate = now.format(formatter);

        // Extract the common name (CN) from the certificate subject
        String commonName = "Unknown Signer";
        if (cert != null && cert.getSubjectDN() != null) {
            String subject = cert.getSubjectDN().getName();
            if (subject.contains("CN=")) {
                commonName = subject.split("CN=")[1].split(",")[0];
            }
        }

        // Prepare text lines based on provided values
        List<String> lines = new ArrayList<>();
        if (isChangesAllowed) {
            lines.add("Signed By: " + commonName);
        } else {
            // Count non-empty fields
            int nonEmptyCount = 0;
            if (isNullOrEmpty(reason)) nonEmptyCount++;
            if (isNullOrEmpty(location)) nonEmptyCount++;
            if (isNullOrEmpty(customText)) nonEmptyCount++;

            // Add "Signed By" only if one or fewer fields are non-empty
            if (nonEmptyCount <= 1) {
                lines.add("Signed By: " + commonName);
            }
        }

        // Add other fields if provided
        if (isNullOrEmpty(reason)) lines.add("Reason: " + reason);
        if (isNullOrEmpty(location)) lines.add("Location: " + location);
        if (isNullOrEmpty(customText)) lines.add(customText);
        lines.add("Date: " + formattedDate);

        // Calculate optimal font size
        float fontSize = calculateOptimalFontSize(lines, rect);
        if (fontSize < 4f) fontSize = 4f; // Minimum readable font size

        // Create font
        Font font;
        try {
            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            font = new Font(baseFont, fontSize);
        } catch (IOException e) {
            throw new DocumentException("Failed to load Helvetica font: " + e.getMessage());
        }

        // Create ColumnText for rendering
        ColumnText ct = new ColumnText(layer2);
        ct.setSimpleColumn(rect);
        ct.setRunDirection(PdfWriter.RUN_DIRECTION_LTR);

        // Create paragraph with adjusted leading
        Paragraph paragraph = new Paragraph();
        paragraph.setLeading(fontSize * 1.2f); // 20% line spacing
        for (String line : lines) {
            paragraph.add(new Chunk(line, font));
            paragraph.add(Chunk.NEWLINE);
        }

        // Adjust for bottom alignment
        ct.setYLine(rect.getBottom() + fontSize * lines.size() * 1.2f);
        ct.addElement(paragraph);
        ct.go();
    }

    /**
     * Checks if a string is null or empty after trimming.
     *
     * @param s the string to check
     * @return true if null or empty; false otherwise
     */
    private boolean isNullOrEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * Calculates the optimal font size to fit all lines within the rectangle.
     * Iteratively tests font sizes to find the largest that fits both width and height.
     *
     * @param lines the list of text lines
     * @param rect  the rectangle for text placement
     * @return the optimal font size
     */
    private float calculateOptimalFontSize(List<String> lines, Rectangle rect) {
        if (lines.isEmpty()) return 8f; // Default font size for empty content

        BaseFont baseFont;
        try {
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        } catch (Exception e) {
            return 8f; // Fallback font size
        }

        float boxWidth = rect.getWidth();
        float boxHeight = rect.getHeight();
        int lineCount = lines.size();
        float maxFontSize = 16f; // Upper limit for readability
        float minFontSize = 4f;  // Lower limit for readability
        float testFontSize = maxFontSize;

        while (testFontSize >= minFontSize) {
            boolean fits = true;

            // Check the width for each line
            for (String line : lines) {
                float textWidth = baseFont.getWidthPoint(line, testFontSize);
                if (textWidth > boxWidth) {
                    fits = false;
                    break;
                }
            }

            // Check total height
            float totalHeight = lineCount * testFontSize * 1.2f; // Include line spacing
            if (totalHeight > boxHeight) {
                fits = false;
            }

            if (fits) {
                return testFontSize;
            }

            testFontSize -= 0.5f; // Decrease font size incrementally
        }

        return minFontSize; // Return minimum if no fit found
    }

//
//    /**
//     * Sets a custom appearance for the digital signature, including signer info, reason,
//     * location, and date. The font size is dynamically calculated based on the text content
//     * and available signature rectangle space to ensure the text fits well.
//     *
//     * @param appearance        the PdfSignatureAppearance object to customize
//     * @param customText        additional custom text to include in the signature
//     * @param cert              the X509 certificate used for signing
//     * @param isChangesAllowed flag indicating if changes are allowed after signing
//     * @param reason            reason for signing
//     * @param location          location of signing
//     * @throws DocumentException if any document-related error occurs
//     */
//    private void setCustomAppearance(
//            PdfSignatureAppearance appearance,
//            String customText,
//            X509Certificate cert,
//            boolean isChangesAllowed,
//            String reason,
//            String location
//    ) throws DocumentException {
//
//        // Get the layer for text and content display (Layer 2)
//        PdfTemplate layer2 = appearance.getLayer(2);
//
//        // Create ColumnText for laying out paragraph content inside the layer
//        ColumnText ct = new ColumnText(layer2);
//
//        // Define the rectangle area (with padding) inside the signature box
//        Rectangle rect = new Rectangle(2, 1, layer2.getWidth() - 2, layer2.getHeight() - 17);
//        ct.setSimpleColumn(rect);
//        ct.setRunDirection(PdfWriter.RUN_DIRECTION_LTR);
//
//        // Format the current timestamp for display
//        LocalDateTime now = LocalDateTime.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        String formatted = now.format(formatter);
//
//        // Extract the common name (CN) from the certificate subject
//        String commonName = cert.getSubjectDN().getName().split("CN=")[1].split(",")[0];
//
//        // Prepare text lines to display based on provided values
//        List<String> lines = new ArrayList<>();
//
//        if (isChangesAllowed) lines.add("Signed By " + commonName);
//        else  {
//            // Count how many fields are non-empty
//            int nonEmptyCount = 0;
//            if (isNullOrEmpty(reason)) nonEmptyCount++;
//            if (isNullOrEmpty(location)) nonEmptyCount++;
//            if (isNullOrEmpty(customText)) nonEmptyCount++;
//
//            // Add "Signed By" only if exactly one field has a value
//            if ( nonEmptyCount <= 1) lines.add("Signed By " + commonName);
//        }
//        // Add reason, location, and custom text always if provided
//        if (isNullOrEmpty(reason)) lines.add("Reason: " + reason);
//        if (isNullOrEmpty(location)) lines.add("Location: " + location);
//        if (isNullOrEmpty(customText)) lines.add(customText);
//        lines.add("Date: " + formatted);
//
//        // Calculate available box dimensions
//        int maxLines = lines.size();
//        float boxHeight = rect.getHeight();
//        float boxWidth = rect.getWidth();
//
//        // Determine the max font size that fits vertically and horizontally
//        float verticalFontSize = boxHeight / (maxLines * 1.2f); // Adjusting for line spacing
//        float horizontalFontSize = getMaxFontSizeForWidth(lines, boxWidth);
//
//        // Pick the smallest size to ensure it fits both height and width
//        float finalFontSize = Math.min(verticalFontSize, horizontalFontSize);
//        Font font = new Font(Font.FontFamily.HELVETICA, finalFontSize);
//
//        // Create paragraph with adjusted leading
//        Paragraph paragraph = new Paragraph();
//        paragraph.setLeading(finalFontSize * 1.2f); // 20% line spacing
//        for (String line : lines) {
//            paragraph.add(new Chunk(line, font));
//            paragraph.add(Chunk.NEWLINE);
//        }
//        // Add paragraph to column and render
//        ct.addElement(paragraph);
//        ct.go();
//    }
//
//    /**
//     * Checks if a given string is null or empty after trimming.
//     *
//     * @param s the string to check
//     * @return true if null or empty; false otherwise
//     */
//    private boolean isNullOrEmpty(String s) {
//        return s != null && !s.trim().isEmpty();
//    }
//
//    /**
//     * Estimates the maximum font size that can be used without exceeding the box width.
//     * It calculates the width of each line at 1pt and scales it to the actual box width.
//     *
//     * @param lines    list of text lines to measure
//     * @param boxWidth the width of the available box
//     * @return the maximum font size that ensures all lines fit within box width
//     */
//    private float getMaxFontSizeForWidth(List<String> lines, float boxWidth) {
//        BaseFont baseFont;
//        try {
//            // Load base font (Helvetica)
//            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
//        } catch (Exception e) {
//            return 8f; // Fallback font size in case of error
//        }
//
//        float maxFontSize = Float.MAX_VALUE;
//
//        // For each line, calculate the max possible font size that fits in the box width
//        for (String line : lines) {
//            float fontSize = boxWidth / baseFont.getWidthPoint(line, 1f); // Width at 1 pt
//            if (fontSize < maxFontSize) {
//                maxFontSize = fontSize;
//            }
//        }
//
//        return Math.min(maxFontSize, 12f); // Optional cap on font size
//    }

    /**
     * Fills the background color of the signature appearance (Layer 0).
     *
     * @param appearance the signature appearance to modify
     * @param color      the color to use for background fill
     */
    private void setSignatureBackgroundColor(@NotNull PdfSignatureAppearance appearance, BaseColor color) {
        PdfTemplate layer0 = appearance.getLayer(0);
        PdfContentByte canvas = layer0;

        float width = layer0.getWidth();
        float height = layer0.getHeight();

        // Draw a filled rectangle covering the entire background
        canvas.setColorFill(color);
        canvas.rectangle(0, 0, width, height);
        canvas.fill();
    }



    /**
     * Parses a page specification string into an array of page numbers.
     *
     * @param pageSpec The page specification string (e.g., "A", "L", "F", "1,3,5", "1-5", "F,L,2", "F-L", "1-3,5,L")
     * @param totalPages Total number of pages in the document (needed for 'A' and 'L' options)
     * @return Array of page numbers (1-based)
     * @throws IllegalArgumentException if the specification is invalid
     */
    public static int[] parsePageSpecification(String pageSpec, int totalPages) {
        if (pageSpec == null || pageSpec.trim().isEmpty()) {
            throw new IllegalArgumentException("Page specification cannot be empty");
        }

        String spec = pageSpec.trim().toUpperCase();

        // Handle special single-character cases first
        if (spec.equals("A")) {
            // All pages
            int[] allPages = new int[totalPages];
            for (int i = 0; i < totalPages; i++) {
                allPages[i] = i + 1;
            }
            return allPages;
        } else if (spec.equals("F")) {
            // First page
            return new int[]{1};
        } else if (spec.equals("L")) {
            // Last page
            return new int[]{totalPages};
        }

        // Process comma-separated list that may contain ranges and special codes
        String[] parts = spec.split(",");
        List<Integer> pageList = new ArrayList<>();

        for (String part : parts) {
            part = part.trim();

            if (part.contains("-")) {
                // Handle page ranges (e.g., "2-5", "F-L", "1-L")
                String[] range = part.split("-");
                if (range.length != 2) {
                    throw new IllegalArgumentException("Invalid range format: " + part);
                }

                int start = parsePageNumber(range[0].trim(), totalPages);
                int end = parsePageNumber(range[1].trim(), totalPages);

                if (start > end) {
                    throw new IllegalArgumentException("Range start must be <= end: " + part);
                }

                for (int i = start; i <= end; i++) {
                    pageList.add(i);
                }
            } else {
                // Single page number or special code
                pageList.add(parsePageNumber(part, totalPages));
            }
        }

        // Convert to array, remove duplicates, and sort
        return pageList.stream()
                .distinct()
                .sorted()
                .mapToInt(i -> i)
                .toArray();
    }

    private static int parsePageNumber(String pageStr, int totalPages) {
        switch (pageStr) {
            case "F":
                return 1;
            case "L":
                return totalPages;
            case "A":
                throw new IllegalArgumentException("'A' can only be used alone, not in ranges or lists");
            default:
                try {
                    int page = Integer.parseInt(pageStr);
                    if (page < 1 || page > totalPages) {
                        throw new IllegalArgumentException("Page number out of range (1-" + totalPages + "): " + page);
                    }
                    return page;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid page number: " + pageStr);
                }
        }
    }

}

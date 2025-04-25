package com.pyojan.eDastakhat.libs;

import com.itextpdf.text.pdf.*;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.DocumentException;
import com.pyojan.eDastakhat.services.Signer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PdfWaterMarker {

    private static final int FONT_SIZE = 8;

    private static BaseFont getBaseFont() {
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ByteArrayInputStream applyWatermarkToAllPages(String pdfFilePath, String watermarkText, int[] coord) throws IOException, DocumentException {
        try (InputStream inputStream = Files.newInputStream(Paths.get(pdfFilePath))) {
            PdfReader reader = new PdfReader(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfStamper stamper = new PdfStamper(reader, outputStream);

            int totalPages = reader.getNumberOfPages();
            for (int page = 1; page <= totalPages; page++) {
                addWatermark(stamper, reader, page, coord, watermarkText);
            }

            stamper.close();
            reader.close();

            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    public static ByteArrayInputStream applyWatermarkToSelectedPages(String pdfFilePath, String watermarkText, int[] coord, String pagesInfo) throws IOException, DocumentException {
        try (InputStream inputStream = Files.newInputStream(Paths.get(pdfFilePath))) {
            PdfReader reader = new PdfReader(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfStamper stamper = new PdfStamper(reader, outputStream);

            int[] selectedPages = Signer.parsePageSpecification(pagesInfo, reader.getNumberOfPages());
            for (int page : selectedPages) {
                if (page < 1 || page > reader.getNumberOfPages()) {
                    throw new IllegalArgumentException("Invalid page number: " + page);
                }
                addWatermark(stamper, reader, page, coord, watermarkText);
            }
            stamper.close();
            reader.close();
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    public static ByteArrayInputStream applyWatermarkToPage(String pdfFilePath, String watermarkText, int[] coord, int targetPage) throws IOException, DocumentException {
        try (InputStream inputStream = Files.newInputStream(Paths.get(pdfFilePath))) {
            PdfReader reader = new PdfReader(inputStream);
            if (targetPage < 1 || targetPage > reader.getNumberOfPages()) {
                throw new IllegalArgumentException("Invalid page number: " + targetPage);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfStamper stamper = new PdfStamper(reader, outputStream);

            addWatermark(stamper, reader, targetPage, coord, watermarkText);

            stamper.close();
            reader.close();

            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    private static void addWatermark(PdfStamper stamper, PdfReader reader, int pageNumber, int[] coord, String watermarkText) {
        PdfContentByte over = stamper.getOverContent(pageNumber);
        Rectangle pageSize = reader.getPageSize(pageNumber);

        BaseFont font = getBaseFont();
        if (font == null) {
            // Font loading failed; abort watermarking
            return;
        }

        float watermarkX, watermarkY;
        float textWidth = font.getWidthPoint(watermarkText, FONT_SIZE);

        // Calculate coordinates
        if (coord == null || coord.length != 4) {
            float margin = 20;
            watermarkX = pageSize.getRight() - margin - textWidth;
            watermarkY = pageSize.getBottom() + margin;
        } else {
            Rectangle signatureRect = new Rectangle(coord[0], coord[1], coord[2], coord[3]);
            watermarkY = signatureRect.getTop() + 3;
            watermarkX = signatureRect.getLeft() + (signatureRect.getWidth() - textWidth) / 2;
        }

        // Draw text
        over.beginText();
        over.setFontAndSize(font, FONT_SIZE);
        over.setRGBColorFill(112, 128, 144); // slate Gray
        over.setTextMatrix(watermarkX, watermarkY);
        over.showText(watermarkText);
        over.endText();
    }

}

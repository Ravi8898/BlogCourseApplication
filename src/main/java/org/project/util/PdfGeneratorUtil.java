package org.project.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.project.model.Article;
import org.project.model.ArticleSection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class PdfGeneratorUtil {

    @Value("${article.pdf.upload.path}")
    private String pdfUploadPath;

    @Value("${amx.logo.path}")
    private String logoPath;

    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 12;
    private static final float LEADING = 16;
    private static final int MAX_LINE_LENGTH = 90;

    public String generateOrUpdatePdf(Article article, List<ArticleSection> sections) throws IOException {

        Files.createDirectories(Paths.get(pdfUploadPath));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = article.getCreatedAt().format(formatter);

        String fileName = "Article_" + article.getId() + "_" + timestamp + ".pdf";
        Path filePath = Paths.get(pdfUploadPath, fileName);

        // overwrite existing file
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        try (PDDocument document = new PDDocument()) {

            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float yPosition = 750;
            float pageWidth = page.getMediaBox().getWidth();

            // ===================== TITLE (CENTER) =====================
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 22);

            String title = article.getTitle();
            float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 22;
            float titleX = (pageWidth - titleWidth) / 2;

            contentStream.beginText();
            contentStream.newLineAtOffset(titleX, yPosition);
            contentStream.showText(title);
            contentStream.endText();

            yPosition -= 50;

            // ===================== DESCRIPTION =====================
            contentStream.setFont(PDType1Font.HELVETICA, 14);
            contentStream.beginText();
            contentStream.setLeading(LEADING);
            contentStream.newLineAtOffset(MARGIN, yPosition);

            for (String line : wrapText(article.getDescription())) {
                contentStream.showText(line);
                contentStream.newLine();
                yPosition -= LEADING;
            }

            contentStream.endText();
            yPosition -= 30;

            // ===================== SEPARATOR LINE =====================
            float startX = MARGIN;
            float endX = page.getMediaBox().getWidth() - MARGIN;

            contentStream.moveTo(startX, yPosition);
            contentStream.lineTo(endX, yPosition);
            contentStream.setLineWidth(1.5f);
            contentStream.stroke();

            yPosition -= 40;

            // ===================== SECTIONS =====================
            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
            int index = 1;

            for (ArticleSection section : sections) {
                // ========= SECTION TITLE =========
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText(index + ". " + section.getSectionKey().toUpperCase());
                contentStream.endText();

                yPosition -= 30;

                // ========= IMAGE =========
                if (section.getImageUrl() != null && !section.getImageUrl().isBlank()) {

                    PDImageXObject image =
                            PDImageXObject.createFromFile(section.getImageUrl(), document);

                    float imageWidth = 400;
                    float imageHeight = (image.getHeight() * imageWidth) / image.getWidth();

                    if (yPosition - imageHeight < MARGIN) {
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = 750;
                    }

                    contentStream.drawImage(image, MARGIN, yPosition - imageHeight,
                            imageWidth, imageHeight);

                    yPosition -= imageHeight + 20;
                }

                // ========= TEXT =========
                contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                contentStream.beginText();
                contentStream.setLeading(LEADING);
                contentStream.newLineAtOffset(MARGIN, yPosition);

                for (String line : wrapText(section.getExplanation())) {

                    if (yPosition < MARGIN) {
                        contentStream.endText();
                        contentStream.close();

                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);

                        yPosition = 750;
                        contentStream.beginText();
                        contentStream.setLeading(LEADING);
                        contentStream.newLineAtOffset(MARGIN, yPosition);
                    }
                    contentStream.showText(line);
                    contentStream.newLine();
                    yPosition -= LEADING;
                }
                contentStream.endText();

                yPosition -= 30;
                index++;
            }
            contentStream.close();

            // Add header & footer to all pages
            int pageNumber = 1;
            for (PDPage pdfPage : document.getPages()) {

                addWatermark(document, pdfPage, logoPath);
                addHeaderAndFooter(document, pdfPage, article.getTitle(), pageNumber++);
                drawPageBorder(document, pdfPage);
            }

            document.save(filePath.toFile());
        }

        return filePath.toString();
    }


    private List<String> wrapText(String text) {

        List<String> lines = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (line.length() + word.length() > PdfGeneratorUtil.MAX_LINE_LENGTH) {
                lines.add(line.toString());
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        return lines;
    }

    private void addHeaderAndFooter(PDDocument document, PDPage page, String title, int pageNumber) throws IOException {

        PDPageContentStream contentStream = new PDPageContentStream(
                document,
                page,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true
        );

        float pageWidth = page.getMediaBox().getWidth();

        // ===== HEADER =====
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, 800);
        contentStream.showText(title);
        contentStream.endText();

        // ===== FOOTER =====
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.beginText();
        contentStream.newLineAtOffset(pageWidth - 120, 30);
        contentStream.showText("Page " + pageNumber);
        contentStream.endText();

        // ===== FOOTER TEXT (LEFT) =====
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, 30);
        contentStream.showText("Generated by AMX System");
        contentStream.endText();

        // Footer line
        contentStream.moveTo(MARGIN, 45);
        contentStream.lineTo(pageWidth - MARGIN, 45);
        contentStream.stroke();

        contentStream.close();
    }

    private void drawPageBorder(PDDocument document, PDPage page) throws IOException {

        PDPageContentStream contentStream = new PDPageContentStream(
                document,
                page,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true
        );

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        float borderMargin = 20;   // distance from page edge

        contentStream.setLineWidth(0.8f);  // thin border

        contentStream.addRect(
                borderMargin,
                borderMargin,
                pageWidth - 2 * borderMargin,
                pageHeight - 2 * borderMargin
        );

        contentStream.stroke();
        contentStream.close();
    }

    private void addWatermark(PDDocument document, PDPage page, String logoPath) throws IOException {

        PDPageContentStream contentStream = new PDPageContentStream(
                document,
                page,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true
        );

        PDImageXObject logo = PDImageXObject.createFromFile(logoPath, document);

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        float logoWidth = 300;
        float logoHeight = (logo.getHeight() * logoWidth) / logo.getWidth();

        float x = (pageWidth - logoWidth) / 2;
        float y = (pageHeight - logoHeight) / 2;

        // Set transparency
        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        graphicsState.setNonStrokingAlphaConstant(0.1f);  // light watermark

        // Apply graphics state
        contentStream.setGraphicsStateParameters(graphicsState);
        // Draw watermark image
        contentStream.drawImage(logo, x, y, logoWidth, logoHeight);

        contentStream.close();
    }


}
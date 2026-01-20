package org.project.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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
            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);

            float yPosition = 750;
            int index = 1;

            for (ArticleSection section : sections) {

                // ========= SECTION TITLE =========
                contentStream.beginText();
                contentStream.setLeading(LEADING);
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
                        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                        yPosition = 750;
                    }

                    contentStream.drawImage(image, MARGIN, yPosition - imageHeight,
                            imageWidth, imageHeight);

                    yPosition -= imageHeight + 20;
                }

                // ========= TEXT =========
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
                        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);

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

}
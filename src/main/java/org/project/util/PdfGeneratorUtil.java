package org.project.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.project.model.Article;
import org.project.model.ArticleSection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Component
@Slf4j
public class PdfGeneratorUtil {

    @Value("${article.pdf.upload.path}")
    private String pdfUploadPath;
    public String generateOrUpdatePdf(Article article, List<ArticleSection> sections) throws IOException {
        Files.createDirectories(Paths.get(pdfUploadPath));

        String fileName = "Article_" + article.getId() + ".pdf";
        Path filePath = Paths.get(pdfUploadPath, fileName);

        // If file exists, delete it first
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        try (PDDocument document = new PDDocument()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream =
                         new PDPageContentStream(document, page)) {

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.setLeading(16f);
                contentStream.newLineAtOffset(50, 750);

                int index = 1;

                for (ArticleSection section : sections) {

                    contentStream.showText(index + ". " + section.getSectionKey().toUpperCase());
                    contentStream.newLine();

                    for (String line : section.getExplanation().split("\n")) {
                        contentStream.showText(line);
                        contentStream.newLine();
                    }

                    contentStream.newLine();
                    index++;
                }

                contentStream.endText();
            }
            document.save(filePath.toFile());
        }
        return filePath.toString();  // same path every time
    }

}
package org.project.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.project.enums.ArticleStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArticleResponse {

    private Long articleId;
    private String title;
    private String description;
    private String pdfPath;
    private ArticleStatus articleStatus;
    private Long authorId;
    private String reviewMessage;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    // CMS-style content
    private ContentResponse content;
}


package org.project.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.project.enums.ArticleStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateArticleRequest {

    private Long articleId;

    private String title;

    private String description;

    // structured CMS-style content
    private ContentRequest content;

    // use enum, not String
    private ArticleStatus articleStatus;
}


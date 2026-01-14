package org.project.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateArticleRequest {

    private Long articleId;
    private String title;
    private String description;
    private String content;
    private String articleStatus;

}

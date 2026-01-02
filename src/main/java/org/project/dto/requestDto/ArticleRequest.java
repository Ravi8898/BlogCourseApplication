package org.project.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArticleRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String content;

}

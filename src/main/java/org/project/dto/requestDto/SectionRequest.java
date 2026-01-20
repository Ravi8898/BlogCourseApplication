package org.project.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SectionRequest {

    @NotBlank
    private String explanation;

    // optional image file name
    private String imageUrl;
}


package org.project.dto.responseDto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SectionResponse {

    private String explanation;
    private String imageUrl;   // Azure/S3 URL
}


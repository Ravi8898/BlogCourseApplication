package org.project.dto.responseDto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContentResponse {

    // dynamic sections
    private Map<String, SectionResponse> sections;
}


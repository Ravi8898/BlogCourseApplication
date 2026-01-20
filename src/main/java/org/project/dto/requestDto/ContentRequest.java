package org.project.dto.requestDto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContentRequest {

    private Map<String, SectionRequest> sections;
}

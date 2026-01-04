package org.project.model;

import jakarta.persistence.*;
import lombok.*;
import org.project.enums.ArticleStatus;
import org.project.model.audit.BaseAuditEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "article")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "articleId")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String pdfPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArticleStatus articleStatus;

    @Column(name = "authorId",nullable = false)
    private Long authorId;

    @Column(length = 1000) // may be null
    private String reviewMessage;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    private String isActive;
}

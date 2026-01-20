package org.project.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "article_section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // frontend key like: intro, setup, architecture
    @Column(name = "section_key", nullable = false)
    private String sectionKey;

    // actual text explanation
    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation;

    // optional image
    private String imageUrl;

    // order of section in article
    private Integer position;

    // many sections belong to one article
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
}


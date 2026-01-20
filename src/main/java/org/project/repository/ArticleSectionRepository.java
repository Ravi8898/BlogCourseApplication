package org.project.repository;

import org.project.model.ArticleSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleSectionRepository extends JpaRepository<ArticleSection, Long> {

    List<ArticleSection> findByArticleIdOrderByPosition(Long articleId);
    void deleteByArticleId(Long articleId);
}


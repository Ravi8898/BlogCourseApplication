package org.project.serviceImpl;

import static org.project.constants.MessageConstants.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.coyote.BadRequestException;
import org.project.dto.requestDto.ArticleRequest;
import org.project.dto.requestDto.SectionRequest;
import org.project.dto.requestDto.UpdateArticleRequest;
import org.project.dto.responseDto.*;
import org.project.enums.ArticleStatus;
import org.project.model.Article;
import org.project.model.ArticleSection;
import org.project.model.User;
import org.project.model.UserToken;
import org.project.repository.ArticleRepository;
import org.project.repository.ArticleSectionRepository;
import org.project.repository.UserRepository;
import org.project.repository.UserTokenRepository;
import org.project.service.ArticleService;
import org.project.util.ImageStorageUtil;
import org.project.util.PdfGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ArticleServiceImpl implements ArticleService {


    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private ArticleSectionRepository articleSectionRepository;

    @Autowired
    private ImageStorageUtil imageStorageUtil;

    @Autowired
    private PdfGeneratorUtil pdfGeneratorUtil;

    @Value("${article.file.upload.path}")
    private String articleUploadPath;


    private static final Logger log =
            LoggerFactory.getLogger(ArticleServiceImpl.class);

    /**
     * Creates a new article for the currently logged-in user.
     * The authorId is extracted from the JWT token present in the Authorization header.
     * Article content is saved to a file, and only the file path is stored in the database.
     *
     * @param request        Article creation request containing title, description, and content
     * @param servletRequest HTTP request used to extract Authorization header
     * @return ApiResponse containing created article details
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ApiResponse<ArticleResponse> createArticle(ArticleRequest request,
                                                      HttpServletRequest servletRequest) {

        try {
            // 1. Extract JWT token
            String authHeader = servletRequest.getHeader("Authorization");
            String token = authHeader.substring(7);

            Optional<UserToken> userTokenOptional = userTokenRepository.findByToken(token);

            Long authorId = userTokenOptional
                    .map(UserToken::getUserId)
                    .orElseThrow(() -> new RuntimeException("Invalid JWT token"));

            log.info("Author identified with userId={}", authorId);

            // 2. Save article metadata
            Article article = Article.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .articleStatus(ArticleStatus.DRAFT)
                    .authorId(authorId)
                    .isActive("Y")
                    .build();

            Article savedArticle = articleRepository.save(article);

            // 3. Save sections
            int position = 1;
            List<ArticleSection> savedSections = new ArrayList<>();

            for (Map.Entry<String, SectionRequest> entry :
                    request.getContent().getSections().entrySet()) {

                String sectionKey = entry.getKey();
                SectionRequest sectionRequest = entry.getValue();

                ArticleSection section = new ArticleSection();
                section.setArticle(savedArticle);
                section.setSectionKey(sectionKey);
                section.setExplanation(sectionRequest.getExplanation());
                section.setImageUrl(sectionRequest.getImageUrl()); // already uploaded
                section.setPosition(position++);

                savedSections.add(articleSectionRepository.save(section));
            }

            // 4. Generate PDF from sections
            String pdfPath = pdfGeneratorUtil.generateOrUpdatePdf(savedArticle, savedSections);
            savedArticle.setPdfPath(pdfPath);

            articleRepository.save(savedArticle);

            // 5. Prepare response
            ArticleResponse response = mapToResponse(savedArticle, savedSections);

            return new ApiResponse<>(
                    SUCCESS,
                    ARTICLE_CREATED_SUCCESS,
                    HttpStatus.CREATED.value(),
                    response
            );

        } catch (Exception ex) {
            log.error("Error while creating article", ex);

            throw new RuntimeException(ARTICLE_CREATED_FAILED, ex.getCause());
//            return new ApiResponse<>(
//                    FAILED,
//                    ARTICLE_CREATED_FAILED,
//                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
//                    null
//            );
        }
    }

    private ArticleResponse mapToResponse(Article article, List<ArticleSection> sections) {

        Map<String, SectionResponse> sectionMap = new LinkedHashMap<>();

        for (ArticleSection section : sections) {
            sectionMap.put(
                    section.getSectionKey(),
                    new SectionResponse(
                            section.getExplanation(),
                            section.getImageUrl()
                    )
            );
        }

        ContentResponse content = new ContentResponse(sectionMap);

        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getDescription(),
                article.getPdfPath(),
                article.getArticleStatus(),
                article.getAuthorId(),
                article.getReviewMessage(),
                article.getReviewedBy(),
                article.getReviewedAt(),
                content
        );
    }

    /**
     * Fetch all active articles.
     */
    @Override
    public ApiResponse<List<ArticleResponse>> getAllArticles() {

        log.info("getAllArticles method called");

        ApiResponse<List<ArticleResponse>> response;

        try {
            // Fetch all active articles
            List<Article> articleList = articleRepository.findByIsActive("Y");

            log.info("Articles fetched from repository: {}", articleList.size());

            if (articleList.isEmpty()) {
                log.info("No active articles found");
                return new ApiResponse<>(
                        SUCCESS,
                        DATA_NOT_FOUND,
                        HttpStatus.OK.value(),
                        List.of()
                );
            }

            // Map each article entity to response DTO with sections
            List<ArticleResponse> articleResponses = articleList.stream()
                    .map(article -> {

                        // Fetch sections for this article
                        List<ArticleSection> sections =
                                articleSectionRepository.findByArticleIdOrderByPosition(article.getId());

                        // Build section map
                        Map<String, SectionResponse> sectionMap = new LinkedHashMap<>();

                        for (ArticleSection section : sections) {
                            sectionMap.put(
                                    section.getSectionKey(),
                                    new SectionResponse(
                                            section.getExplanation(),
                                            section.getImageUrl()
                                    )
                            );
                        }

                        ContentResponse content = new ContentResponse(sectionMap);

                        return new ArticleResponse(
                                article.getId(),
                                article.getTitle(),
                                article.getDescription(),
                                article.getPdfPath(),
                                article.getArticleStatus(),
                                article.getAuthorId(),
                                article.getReviewMessage(),
                                article.getReviewedBy(),
                                article.getReviewedAt(),
                                content
                        );
                    })
                    .toList();

            log.info("Successfully mapped {} articles to ArticleResponse list", articleResponses.size());

            response = new ApiResponse<>(
                    SUCCESS,
                    FETCH_ARTICLE_SUCCESS,
                    HttpStatus.OK.value(),
                    articleResponses
            );

        } catch (Exception ex) {

            log.error("Error while fetching articles", ex);

            response = new ApiResponse<>(
                    FAILED,
                    FETCH_ARTICLE_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );
        }

        return response;
    }


    /**
     * Fetch article details by articleId.
     *
     * @param articleId ID of the article to be fetched
     * @return ApiResponse containing ArticleResponse if found
     */
    @Override
    public ApiResponse<ArticleResponse> getArticleById(Long articleId) {

        log.info("getArticleById service called with articleId={}", articleId);

        ApiResponse<ArticleResponse> response;

        try {
            // Fetch article from repository
            Optional<Article> articleOptional = articleRepository.findById(articleId);

            log.info("Article fetched from repository for articleId={}", articleId);

            // Check if article exists
            if (articleOptional.isPresent()) {

                Article article = articleOptional.get();
                log.info("Article found for articleId={}", articleId);

                // Fetch sections for this article
                List<ArticleSection> sections =
                        articleSectionRepository.findByArticleIdOrderByPosition(articleId);

                log.info("Fetched {} sections for articleId={}", sections.size(), articleId);

                // Build section map
                Map<String, SectionResponse> sectionMap = new LinkedHashMap<>();

                for (ArticleSection section : sections) {
                    sectionMap.put(
                            section.getSectionKey(),
                            new SectionResponse(
                                    section.getExplanation(),
                                    section.getImageUrl()
                            )
                    );
                }

                ContentResponse content = new ContentResponse(sectionMap);

                // Build response DTO
                ArticleResponse articleResponse = new ArticleResponse(
                        article.getId(),
                        article.getTitle(),
                        article.getDescription(),
                        article.getPdfPath(),
                        article.getArticleStatus(),
                        article.getAuthorId(),
                        article.getReviewMessage(),
                        article.getReviewedBy(),
                        article.getReviewedAt(),
                        content
                );

                log.info("ArticleResponse prepared successfully for articleId={}", articleId);

                response = new ApiResponse<>(
                        SUCCESS,
                        FETCH_ARTICLE_SUCCESS,
                        HttpStatus.OK.value(),
                        articleResponse
                );
            } else {
                // Article not found
                log.info("No article found for articleId={}", articleId);
                response = new ApiResponse<>(
                        FAILED,
                        ARTICLE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value(),
                        null
                );
            }
        } catch (Exception ex) {
            // Handle unexpected exceptions
            log.error("Exception occurred while fetching article by articleId={}", articleId, ex);
            response = new ApiResponse<>(
                    FAILED,
                    SOMETHING_WENT_WRONG,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );
        }

        // Log method exit
        log.info("getArticleById service completed for articleId={}", articleId);

        return response;
    }


    /**
     * Fetch all active articles created by the currently logged-in user.
     *
     * The userId is extracted from the JWT token present in the Authorization header.
     *
     * @param servletRequest HTTP request used to extract the Authorization header
     * @return ApiResponse containing a list of ArticleResponse
     */
    @Override
    public ApiResponse<List<ArticleResponse>> getAllArticlesByUserId(HttpServletRequest servletRequest) {

        log.info("getAllArticlesByUserId service called for user");

        ApiResponse<List<ArticleResponse>> response;

        try {
            String authHeader = servletRequest.getHeader("Authorization");
            log.info("Authorization header received for getAllArticlesByUserId API");

            // Extract JWT token by removing 'Bearer ' prefix
            String token = authHeader.substring(7);
            log.info("JWT token extracted successfully for getAllArticlesByUserId API");

            // Fetch token details
            Optional<UserToken> userTokenOptional = userTokenRepository.findByToken(token);

            Long authorId = null;

            // If token exists, extract userId as authorId
            if (userTokenOptional.isPresent()) {
                authorId = userTokenOptional.get().getUserId();
                log.info("Author identified with userId={} for getAllArticlesByUserId API", authorId);
            }

            if (authorId == null) {
                log.error("UserId could not be extracted from token");
                return new ApiResponse<>(
                        FAILED,
                        USER_NOT_FOUND,
                        HttpStatus.UNAUTHORIZED.value(),
                        null
                );
            }

            String isActive = "Y";
            List<Article> userArticleList = articleRepository.findByAuthorIdAndIsActive(authorId, isActive);

            if (userArticleList.isEmpty()) {
                log.info("No active articles found for userId={}", authorId);
                return new ApiResponse<>(
                        SUCCESS,
                        DATA_NOT_FOUND,
                        HttpStatus.OK.value(),
                        List.of()
                );
            }

            // Map each article entity of the user to CMS-style response DTO
            List<ArticleResponse> articleResponses = userArticleList.stream()
                    .map(article -> {

                        // Fetch sections for this article
                        List<ArticleSection> sections =
                                articleSectionRepository.findByArticleIdOrderByPosition(article.getId());

                        // Build section map
                        Map<String, SectionResponse> sectionMap = new LinkedHashMap<>();

                        for (ArticleSection section : sections) {
                            sectionMap.put(
                                    section.getSectionKey(),
                                    new SectionResponse(
                                            section.getExplanation(),
                                            section.getImageUrl()
                                    )
                            );
                        }

                        ContentResponse content = new ContentResponse(sectionMap);

                        return new ArticleResponse(
                                article.getId(),
                                article.getTitle(),
                                article.getDescription(),
                                article.getPdfPath(),
                                article.getArticleStatus(),
                                article.getAuthorId(),
                                article.getReviewMessage(),
                                article.getReviewedBy(),
                                article.getReviewedAt(),
                                content
                        );
                    })
                    .toList();

            log.info("Successfully mapped {} List of articles of a user", articleResponses.size());

            response = new ApiResponse<>(
                    SUCCESS,
                    FETCH_ARTICLE_SUCCESS,
                    HttpStatus.OK.value(),
                    articleResponses
            );

        } catch (Exception ex) {
            log.error("Error while fetching articles", ex);

            response = new ApiResponse<>(
                    FAILED,
                    FETCH_ARTICLE_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );
        }

        return response;
    }

    /**
     * Update article details.
     * Allows updating title, description, and content of an active article.
     * If content is updated, the old PDF file is deleted and a new one is created.
     */
    @Override
    public ApiResponse<ArticleResponse> updateArticleById(UpdateArticleRequest request,
                                                          HttpServletRequest servletRequest) {

        log.info("updateArticleById called with request: {}", request);

        if (request.getArticleId() == null) {
            return new ApiResponse<>(FAILED, ARTICLE_NOT_FOUND,
                    HttpStatus.BAD_REQUEST.value(), null);
        }

        try {
            Optional<Article> articleOptional =
                    articleRepository.findByIdAndIsActive(request.getArticleId(), "Y");

            if (articleOptional.isEmpty()) {
                return new ApiResponse<>(FAILED, ARTICLE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value(), null);
            }

            Article article = articleOptional.get();

            boolean contentUpdated = false;

            // Update only if not null
            if (request.getTitle() != null) {
                article.setTitle(request.getTitle());
            }

            if (request.getDescription() != null) {
                article.setDescription(request.getDescription());
            }

            // Update sections only if content is provided
            List<ArticleSection> savedSections = null;

            if (request.getContent() != null && request.getContent().getSections() != null) {
                contentUpdated = true;

                // Delete old sections
                articleSectionRepository.deleteByArticleId(article.getId());

                savedSections = new ArrayList<>();
                int position = 1;

                for (Map.Entry<String, SectionRequest> entry :
                        request.getContent().getSections().entrySet()) {

                    ArticleSection section = new ArticleSection();
                    section.setArticle(article);
                    section.setSectionKey(entry.getKey());
                    section.setExplanation(entry.getValue().getExplanation());
                    section.setImageUrl(entry.getValue().getImageUrl());
                    section.setPosition(position++);

                    savedSections.add(articleSectionRepository.save(section));
                }
            }

            // Regenerate PDF only if content changed
            if (contentUpdated) {
                String pdfPath = pdfGeneratorUtil.generateOrUpdatePdf(article, savedSections);
                article.setPdfPath(pdfPath);
            }

            // Update status only if provided
            if (request.getArticleStatus() != null) {
                updateArticleStatus(servletRequest, request, article);
            }

            // Save updated article
            Article updatedArticle = articleRepository.save(article);

            // If content was not updated, load existing sections
            if (!contentUpdated) {
                savedSections = articleSectionRepository
                        .findByArticleIdOrderByPosition(article.getId());
            }

            ArticleResponse response = mapToResponse(updatedArticle, savedSections);

            return new ApiResponse<>(
                    SUCCESS,
                    ARTICLE_UPDATE_SUCCESS,
                    HttpStatus.OK.value(),
                    response
            );

        } catch (Exception ex) {
            log.error("Error while updating article", ex);
            return new ApiResponse<>(
                    FAILED,
                    ARTICLE_UPDATE_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );
        }
    }

    private void updateArticleStatus(HttpServletRequest servletRequest, UpdateArticleRequest request, Article article) {
        String authHeader = servletRequest.getHeader("Authorization");
        log.info("Authorization header received for getAllArticlesByUserId API {}", authHeader);

        // Extract JWT token by removing 'Bearer ' prefix
        String token = authHeader.substring(7);
        log.info("JWT token extracted successfully for getAllArticlesByUserId API {}", token);

        // Fetch token details
        Optional<UserToken> userTokenOptional =
                userTokenRepository.findByToken(token);
        Long userId = 0L;
        if (userTokenOptional.isPresent()) {
            userId = userTokenOptional.get().getUserId();
        }

        String isActive = "Y";
        Optional<User> user = userRepository.findByIdAndIsActive(userId, isActive);

        article.setArticleStatus(ArticleStatus.valueOf(String.valueOf(request.getArticleStatus())));
        user.ifPresent(value -> article.setReviewedBy(value.getId()));
        article.setReviewedAt(LocalDateTime.now());
    }

}
package org.project.serviceImpl;

import static org.project.constants.MessageConstants.*;
import jakarta.servlet.http.HttpServletRequest;
import org.project.dto.requestDto.ArticleRequest;
import org.project.dto.requestDto.RegisterRequest;
import org.project.dto.requestDto.UpdateArticleRequest;
import org.project.dto.responseDto.*;
import org.project.enums.ArticleStatus;
import org.project.model.Address;
import org.project.model.Article;
import org.project.model.User;
import org.project.model.UserToken;
import org.project.repository.ArticleRepository;
import org.project.repository.UserRepository;
import org.project.repository.UserTokenRepository;
import org.project.service.ArticleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.javapoet.ClassName;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class ArticleServiceImpl implements ArticleService {


    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTokenRepository userTokenRepository;

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
    @Override
    public ApiResponse<ArticleResponse> createArticle(ArticleRequest request, HttpServletRequest servletRequest) {

        log.info("Create Article service started");
        try {


            String authHeader = servletRequest.getHeader("Authorization");
            log.info("Authorization header received");

            // Extract JWT token by removing 'Bearer ' prefix
            String token = authHeader.substring(7);
            log.info("JWT token extracted successfully");

            // Fetch token details
            Optional<UserToken> userTokenOptional =
                    userTokenRepository.findByToken(token);

            Long authorId = 0L;

            // If token exists, extract userId as authorId
            if(userTokenOptional.isPresent())
            {
                authorId = userTokenOptional.get().getUserId();
                log.info("Author identified with userId={}", authorId);
            }

            // Save article content to file system and get file path
            log.info("Saving article content to file for authorId={}", authorId);
            String filePath = saveContentToFile(
                    request.getContent(),
                    authorId
            );
            log.info("Article content saved successfully at path={}", filePath);

            Article article = Article.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .pdfPath(filePath)
                    .articleStatus(ArticleStatus.DRAFT)
                    .authorId(authorId)
                    .isActive("Y")
                    .build();

            // Save article to database
            Article savedArticle = articleRepository.save(article);
            log.info("Article persisted successfully with id={}", savedArticle.getId());

            // Prepare response DTO
            ArticleResponse response = new ArticleResponse(
                    savedArticle.getId(),
                    savedArticle.getTitle(),
                    savedArticle.getDescription(),
                    savedArticle.getPdfPath(),
                    savedArticle.getArticleStatus(),
                    savedArticle.getAuthorId(),
                    savedArticle.getReviewMessage(),
                    savedArticle.getReviewedBy(),
                    savedArticle.getReviewedAt()
            );
            log.info("Create Article service completed successfully");
            ;

            return new ApiResponse<>(
                    SUCCESS,
                    ARTICLE_CREATED_SUCCESS,
                    HttpStatus.CREATED.value(),
                    response

            );
        }
        catch (IOException ioEx){
            log.error("Error while creating article", ioEx);
            return new ApiResponse<>(
                    FAILED,
                    ARTICLE_FILE_SAVE_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );
        }
        catch (Exception ex) {
            log.error("Error while creating article", ex);
            return new ApiResponse<>(
                    FAILED,
                    ARTICLE_CREATED_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );
        }
    }

    /**
     * Saves article content to a file on the server and returns the file path.
     * The file name contains authorId and timestamp for uniqueness.
     *
     * @param content  Article content sent by user
     * @param authorId ID of the author creating the article
     * @return Path of the saved file as String
     * @throws IOException if file creation or write fails
     */
    private String saveContentToFile(String content, Long authorId) throws IOException {

        try {
            log.info("Starting file save operation for authorId={}", authorId);

            // Base directory where article files are stored
            Files.createDirectories(Paths.get(articleUploadPath));
            log.info("Verified/created base directory: {}", articleUploadPath);

            // Generate unique file name using authorId and timestamp
            String fileName = "Article_" + authorId + "_" + System.currentTimeMillis() + ".pdf";
            Path filePath = Paths.get(articleUploadPath, fileName);

            // Write content to file
            try (PDDocument document = new PDDocument()) {

                log.info("Starting PDF generation for authorId={}", authorId);

                PDPage page = new PDPage();
                document.addPage(page);

                // Create content stream to write text into the PDF page
                try (PDPageContentStream contentStream =
                             new PDPageContentStream(document, page)) {

                    log.info("PDF content stream opened");

                    //starts adding text
                    contentStream.beginText();

                    // Set font and font size
                    contentStream.setFont(PDType1Font.HELVETICA, 12);

                    //line spacing
                    contentStream.setLeading(14.5f);

                    // Set starting position
                    contentStream.newLineAtOffset(50, 750);

                    log.debug("Writing article content into PDF");

                    // Write content line by line to support multi-line text
                    for (String line : content.split("\n")) {
                        contentStream.showText(line);
                        contentStream.newLine();
                    }
                    contentStream.endText();
                }

                // Save the PDF document to the file system
                document.save(filePath.toFile());
            }
            log.info("File written successfully: {}", filePath);

            return filePath.toString();
        } catch (IOException ex) {
            log.error("Failed to save article content to PDF for authorId={}", authorId, ex);
            throw new IOException(ARTICLE_FILE_SAVE_FAILED, ex);
        }
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

            log.info("Articles fetched from repository: {}", articleList);

            if (articleList.isEmpty()) {
                log.info("No active articles found");
                return new ApiResponse<>(
                        SUCCESS,
                        DATA_NOT_FOUND,
                        HttpStatus.OK.value(),
                        List.of()
                );
            }

            // Map each article entity to response DTO
            List<ArticleResponse> articleResponses = articleList.stream()
                    .map(article -> {

                                return new ArticleResponse(
                                        article.getId(),
                                        article.getTitle(),
                                        article.getDescription(),
                                        article.getPdfPath(),
                                        article.getArticleStatus(),
                                        article.getAuthorId(),
                                        article.getReviewMessage(),
                                        article.getReviewedBy(),
                                        article.getReviewedAt()
                                );
                            }
                    ).toList();

            log.info("Successfully mapped {} List of articles to List of ArticleResponse", articleResponses.size());

            response = new ApiResponse<List<ArticleResponse>>(
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

                // Map Article entity to ArticleResponse DTO
                ArticleResponse articleResponse = new ArticleResponse(
                        article.getId(),
                        article.getTitle(),
                        article.getDescription(),
                        article.getPdfPath(),
                        article.getArticleStatus(),
                        article.getAuthorId(),
                        article.getReviewMessage(),
                        article.getReviewedBy(),
                        article.getReviewedAt()
                );

                log.info("ArticleResponse prepared successfully for articleId={}", articleId);

                response = new ApiResponse<>(
                        SUCCESS, FETCH_ARTICLE_SUCCESS, HttpStatus.OK.value(), articleResponse
                );
            } else {
                // Article not found
                log.info("No article found for articleId={}", articleId);
                response = new ApiResponse<>(FAILED, ARTICLE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), null);
            }
        } catch (Exception ex) {
            // Handle unexpected exceptions
            log.error("Exception occurred while fetching article by articleId={}", articleId, ex);
            response = new ApiResponse<>(FAILED, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
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

        try{
            String authHeader = servletRequest.getHeader("Authorization");
            log.info("Authorization header received for getAllArticlesByUserId API");

            // Extract JWT token by removing 'Bearer ' prefix
            String token = authHeader.substring(7);
            log.info("JWT token extracted successfully for getAllArticlesByUserId API");

            // Fetch token details
            Optional<UserToken> userTokenOptional =
                    userTokenRepository.findByToken(token);

            Long authorId = null;

            // // If token exists, extract userId as authorId
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
            String isActive= "Y";
            List<Article> userArticleList = articleRepository.findByAuthorIdAndIsActive(authorId, isActive);

            if(userArticleList.isEmpty()){

                log.info("No active articles found for userId={}", authorId);
                return new ApiResponse<>(
                        SUCCESS,
                        DATA_NOT_FOUND,
                        HttpStatus.OK.value(),
                        List.of()
                );
            }

            // Map each article entity of the user to response DTO
            List<ArticleResponse> articleResponse = userArticleList.stream()
                    .map( article -> {

                                return new ArticleResponse(
                                article.getId(),
                                article.getTitle(),
                                article.getDescription(),
                                article.getPdfPath(),
                                article.getArticleStatus(),
                                article.getAuthorId(),
                                article.getReviewMessage(),
                                article.getReviewedBy(),
                                article.getReviewedAt()
                                );
                    }
                    ).toList();

            log.info("Successfully mapped {} List of articles of a user", articleResponse.size());

            response= new ApiResponse<>(
                    SUCCESS,
                    FETCH_ARTICLE_SUCCESS,
                    HttpStatus.OK.value(),
                    articleResponse
            );
        }
        catch (Exception ex){
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
    public ApiResponse<ArticleResponse> updateArticleById(UpdateArticleRequest request) {

        log.info("updateArticleById called with request: {}", request);

        ApiResponse<ArticleResponse> response;

        try {
            String isActive = "Y";

            // Fetch active article for update
            Optional<Article> articleOptional =
                    articleRepository.findByIdAndIsActive(
                            request.getArticleId(), isActive
                    );

            log.info("Article fetched for update: {}", articleOptional);

            if (articleOptional.isPresent()) {

                Article article = articleOptional.get();
                log.info("Existing article before update: {}", article);

                // Validate articleId
                if (request.getArticleId() == null) {
                    return new ApiResponse<>(FAILED, "Article ID is required", HttpStatus.BAD_REQUEST.value(), null);
                }

                // Update only provided fields
                if (request.getTitle() != null) {
                    article.setTitle(request.getTitle());
                }

                if (request.getDescription() != null) {
                    article.setDescription(request.getDescription());
                }

                // Replace article content if new content is provided
                if (request.getContent() != null) {

                    if (article.getPdfPath() != null) {
                        Files.deleteIfExists(Paths.get(article.getPdfPath()));
                    }

                    String newPdfPath = saveContentToFile(
                            request.getContent(),
                            article.getAuthorId()
                    );

                    article.setPdfPath(newPdfPath);
                }

                // Save updated article
                Article updatedArticle = articleRepository.save(article);
                log.info("Article updated and saved in database: {}", updatedArticle);

                ArticleResponse articleResponse = new ArticleResponse(
                        updatedArticle.getId(),
                        updatedArticle.getTitle(),
                        updatedArticle.getDescription(),
                        updatedArticle.getPdfPath(),
                        updatedArticle.getArticleStatus(),
                        updatedArticle.getAuthorId(),
                        updatedArticle.getReviewMessage(),
                        updatedArticle.getReviewedBy(),
                        updatedArticle.getReviewedAt()
                );

                response = new ApiResponse<>(SUCCESS, ARTICLE_UPDATE_SUCCESS, HttpStatus.OK.value(), articleResponse);

            } else {
                // Article not found or inactive
                log.info("Article not found for update, articleId: {}", request.getArticleId());
                response = new ApiResponse<>(FAILED, ARTICLE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), null);
            }

        } catch (IOException ex) {
            // File handling error during content update
            log.error("Error while updating article PDF", ex);
            response = new ApiResponse<>(FAILED, ARTICLE_FILE_SAVE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            );

        } catch (Exception ex) {
            // Unexpected error during article update
            log.error("Error while updating article", ex);
            response = new ApiResponse<>(FAILED, ARTICLE_UPDATE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            );
        }

        return response;
    }



}
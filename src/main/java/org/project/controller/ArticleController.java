package org.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.project.dto.requestDto.ArticleRequest;
import org.project.dto.requestDto.RegisterRequest;
import org.project.dto.requestDto.UpdateArticleRequest;
import org.project.dto.requestDto.UpdateUserRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.ArticleResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.dto.responseDto.UserResponse;
import org.project.model.Article;
import org.project.service.ArticleService;
import org.project.service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.javapoet.ClassName;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.project.constants.MessageConstants.*;

@RestController
@RequestMapping("/api/article")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    private static final Logger log =
            LoggerFactory.getLogger(ArticleController.class);

    /**
     * The article is created for the currently logged-in user.
     * Author information is extracted internally using JWT token.
     *
     * @param request        ArticleRequest containing title, description, and content
     * @param servletRequest HttpServletRequest used to access Authorization header
     * @return ResponseEntity containing ApiResponse with ArticleResponse
     */
    @Operation(summary = "create a new article.")
    @PostMapping("/createArticle")
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticle(
            @RequestBody ArticleRequest request, HttpServletRequest servletRequest) {
        log.info("Create Article API called");


        // Delegate article creation to service layer
        ApiResponse<ArticleResponse> response = articleService.createArticle(request, servletRequest);
        log.info("Create Article API completed with statusCode={}",
                response.getStatusCode());

        // Return HTTP response with status and body from service
        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);

    }
    /**
     * @return ResponseEntity containing ApiResponse with List of ArticleResponse
     */
    @Operation(summary = "fetch all articles (article is fetched for the admin).")
    @GetMapping("/getAllArticles")
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> getAllArticles(){
        log.info("getAllArticles API called");

        // Delegate article fetching to service layer
        ApiResponse<List<ArticleResponse>> response = articleService.getAllArticles();
        log.info("getAllArticles API completed with statusCode={}",
                response.getStatusCode());

        // Return HTTP response with status and body from service
        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }

    /**
     * @param articleId ID of the article to be fetched
     * @return ApiResponse containing ArticleResponse if found
     */
    @Operation(summary = "Fetch article details by articleId.")
    @GetMapping("/getArticleById")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticleById(
            @RequestParam("articleId") Long articleId){

        log.info("Received request to fetch article with articleId={}", articleId);

        // Call service layer to fetch article details
        ApiResponse<ArticleResponse> response = articleService.getArticleById(articleId);

        // Check if service returned SUCCESS status
        if (response.getStatus().equalsIgnoreCase(SUCCESS)) {

            log.info("Article fetched successfully for articleId={}", articleId);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        }

        log.error("Failed to fetch article for articleId={}, status={}",
                articleId, response.getStatus());

        // Return error response
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Fetch all active articles created by the currently logged-in user.
     *
     * The userId is extracted from the JWT token present in the Authorization header.
     *
     * @param servletRequest HTTP request used to extract the Authorization header
     * @return ApiResponse containing a list of ArticleResponse
     */
    @GetMapping("/getAllArticlesByUserId")
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> getAllArticlesByUserId(
            HttpServletRequest servletRequest){

        log.info("Received request to fetch all articles of user");

        // Call service layer to fetch articles
        ApiResponse<List<ArticleResponse>> response = articleService.getAllArticlesByUserId(servletRequest);

        // Check if service returned SUCCESS status
        if (response.getStatus().equalsIgnoreCase(SUCCESS)) {

            log.info("Articles fetched successfully for user");

            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        }

        log.error("Failed to fetch all articles for user, status={}",
                 response.getStatus());

        // Return error response
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * API to update an existing article.
     * Accepts article update details in request body and delegates
     * the update operation to the service layer.
     */
    @PostMapping("/updateArticleById")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticleById(
            @RequestBody UpdateArticleRequest updateArticleRequest, HttpServletRequest servletRequest) {

        log.info("Received request to update article: {}", updateArticleRequest);

        // Call service layer to update article
        ApiResponse<ArticleResponse> response =
                articleService.updateArticleById(updateArticleRequest, servletRequest);

        // Log response status for the update operation
        log.info("Update user response for articleId {} : {}",
                updateArticleRequest.getArticleId(), response.getStatus());

        // Return response with appropriate HTTP status
        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }


}

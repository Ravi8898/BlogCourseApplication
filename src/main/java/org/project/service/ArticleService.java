package org.project.service;

import jakarta.servlet.http.HttpServletRequest;
import org.project.dto.requestDto.ArticleRequest;
import org.project.dto.requestDto.RegisterRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.ArticleResponse;
import org.project.dto.responseDto.RegisterResponse;

import java.util.List;

public interface ArticleService {
    ApiResponse<ArticleResponse> createArticle(ArticleRequest request, HttpServletRequest servletRequest);
    ApiResponse<List<ArticleResponse>> getAllArticles();

}

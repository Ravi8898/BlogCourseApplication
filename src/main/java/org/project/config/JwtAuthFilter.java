package org.project.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.project.security.JwtUtil;
import org.project.security.SecurityResponseUtil;
import org.project.service.UserTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static org.project.constants.MessageConstants.*;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final SecurityResponseUtil responseUtil;
    private final UserTokenService userTokenService;

    public JwtAuthFilter(JwtUtil jwtUtil,
                         UserDetailsService userDetailsService,
                         SecurityResponseUtil responseUtil, UserTokenService userTokenService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.responseUtil = responseUtil;
        this.userTokenService = userTokenService;
    }
    private static final List<String> PUBLIC_URLS = List.of(
            "/api/login",
            "/api/register"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return PUBLIC_URLS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null && !authHeader.startsWith("Bearer ")) {
            responseUtil.writeResponse(response, FAILED, AUTH_HEADER_MISSING,
                    HttpStatus.UNAUTHORIZED.value());
            return;
        }
        String token = authHeader.substring(7);

        if (userTokenService.isTokenExpired(token)) {
            responseUtil.writeResponse(response, FAILED,
                    TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED.value());
            return;
        } else if (userTokenService.isTokenValid(token)) {
            responseUtil.writeResponse(response, FAILED,
                    TOKEN_INVALIDATED, HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String username = jwtUtil.extractUsername(token);
        UserDetails userDetails =
                userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails,
                        null, userDetails.getAuthorities());

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}


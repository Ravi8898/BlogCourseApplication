package org.project.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityResponseUtil {

    private final ObjectMapper objectMapper;

    public SecurityResponseUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeResponse(HttpServletResponse response,
                              String status,
                              String message,
                              int statusCode) throws IOException {

        ApiResponse<RegisterResponse> apiResponse = new ApiResponse<>();
        apiResponse.setStatus(status);
        apiResponse.setMessage(message);
        apiResponse.setStatusCode(statusCode);
        apiResponse.setData(null);

        response.setStatus(statusCode);
        response.setContentType("application/json");

        response.getWriter()
                .write(objectMapper.writeValueAsString(apiResponse));
    }
}


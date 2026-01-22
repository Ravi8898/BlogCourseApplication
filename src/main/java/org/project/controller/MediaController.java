package org.project.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.dto.responseDto.ApiResponse;
import org.project.util.ImageStorageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.project.constants.MessageConstants.*;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final ImageStorageUtil imageStorageUtil;

    @Value("${image.upload.path}")
    private String imageUploadPath;

    @PostMapping(value = "/uploadImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestPart("file") MultipartFile file) {

        log.info("Image upload API called");

        try {
            // 1. Store image locally
            String imageUrl = imageStorageUtil.store(file);

            // 2. Build full image path
            String imagePath = imageUploadPath + imageUrl;

            // 3. Read image bytes
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));

            // 4. Convert to Base64
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            // 5. Prepare response
            Map<String, String> data = new HashMap<>();
            data.put("base64", base64);
            data.put("imageUrl", imageUrl);
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            SUCCESS,
                            IMAGE_UPLOAD_SUCCESS,
                            HttpStatus.OK.value(),
                            data
                    )
            );

        } catch (Exception ex) {
            log.error("Image upload failed", ex);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            FAILED,
                            IMAGE_UPLOAD_FAILED,
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            null
                    ));
        }
    }
}


package org.project.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.dto.responseDto.ApiResponse;
import org.project.util.ImageStorageUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.project.constants.MessageConstants.*;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final ImageStorageUtil imageStorageUtil;

    @PostMapping(value = "/uploadImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @RequestPart("file") MultipartFile file) {

        log.info("Image upload API called");

        try {
            String imageUrl = imageStorageUtil.store(file);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            SUCCESS,
                            IMAGE_UPLOAD_SUCCESS,
                            HttpStatus.OK.value(),
                            imageUrl
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


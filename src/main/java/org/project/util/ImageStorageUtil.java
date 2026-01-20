package org.project.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Component
@Slf4j
public class ImageStorageUtil {

    @Value("${article.image.upload.path}")
    private String imageUploadPath;

    public String store(MultipartFile file) throws IOException {

        Files.createDirectories(Paths.get(imageUploadPath));

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(imageUploadPath, fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Image saved locally at: {}", filePath);

        return filePath.toString();   // store this in DB
    }
}

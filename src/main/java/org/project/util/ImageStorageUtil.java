package org.project.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;

@Component
@Slf4j
public class ImageStorageUtil {

    @Value("${article.image.upload.path}")
    private String imageUploadPath;

    @Value("${image.base.dir}")
    private String imageBaseDir;

    public String store(MultipartFile file) throws IOException {

        Files.createDirectories(Paths.get(imageUploadPath));

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(imageUploadPath, fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Image saved locally at: {}", imageBaseDir+imageUploadPath+"/"+fileName);
        log.info("Image saved at filePath: {}", filePath);

        return fileName;   // store this in DB
    }

    public static String getImageBase64(String imagePath) throws Exception {
        byte[] imageBytes = Files.readAllBytes(Path.of(imagePath));
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}

package com.riad.book.file;

import com.riad.book.book.Book;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    @Value("${application.file.upload.photos-output-path}")
    private String fileUploadPath;


    public String saveFile(
            @NotNull MultipartFile file,
            @NotNull Integer id
    ) {

     final String fileUploadSubPath = "users" + File.separator + id ;
     return uploadFile(file , fileUploadSubPath);
    }

    private String uploadFile(
            @NotNull MultipartFile file,
            @NotNull String fileUploadSubPath
    ) {
        final String finalUploadPath = fileUploadPath +File.separator+ fileUploadSubPath;
        File targetFolder = new File(finalUploadPath);

        if (!targetFolder.exists()) {
            boolean created = targetFolder.mkdirs();
            if (!created){
                log.warn("failed to create target folder");
                return null;
            }
        }
        
        final String fileExtension = getFileExtension(file.getOriginalFilename());
        String targetFilePath = finalUploadPath + File.separator + System.currentTimeMillis() + "." +fileExtension;
        Path targetPath = Paths.get(targetFilePath);
        try{
            Files.write(targetPath,file.getBytes());
            log.info("saved file to " + targetFilePath);
            return targetFilePath;
        }catch(IOException e){
            log.error("File was not saved" , e);
        }

        return null;
    }

    private String getFileExtension(String fileName) {
        if(fileName == null || fileName.isEmpty()){
            return "";
        }

        int lastDotIndex= fileName.lastIndexOf(".");
        if(lastDotIndex == -1){
            return "";
        }

        return fileName.substring(lastDotIndex+1).toLowerCase();

    }
}

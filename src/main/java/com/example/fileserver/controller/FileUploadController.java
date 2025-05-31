package com.example.fileserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.fileserver.entity.FileInfo;
import com.example.fileserver.service.FileInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private FileInfoService fileInfoService;

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return "文件不能为空";
        }

        // 创建上传目录
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(newFilename);

        // 保存文件
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath);
        }

        // 保存文件信息到数据库
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName(originalFilename);
        fileInfo.setFileName(newFilename);
        fileInfo.setFilePath(filePath.toString());
        fileInfo.setFileType(file.getContentType());
        fileInfo.setFileSize(file.getSize());
        fileInfoService.save(fileInfo);

        return "文件上传成功: " + originalFilename;
    }

    @GetMapping("/download/{filename}")
    public void downloadFile(@PathVariable String filename, HttpServletResponse response) throws IOException {
        FileInfo fileInfo = fileInfoService.getOne(new QueryWrapper<FileInfo>().eq("file_name", filename));
        if (fileInfo == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        File file = new File(fileInfo.getFilePath());
        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        response.setContentType(fileInfo.getFileType());
        response.setHeader("Content-Disposition", "attachment; filename=" + fileInfo.getOriginalName());
        response.setContentLengthLong(fileInfo.getFileSize());

        try (InputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
}
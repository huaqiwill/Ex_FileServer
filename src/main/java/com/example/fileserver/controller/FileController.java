package com.example.fileserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Controller
public class FileController {

    @RequestMapping("/upload")
    public ModelAndView upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        ModelAndView mv = new ModelAndView();
        if (file.isEmpty()) {
            mv.addObject("message", "请选择文件");
            mv.setViewName("uploadResult");
            return mv;
        }

        String path = request.getServletContext().getRealPath("/upload/");
        File uploadDir = new File(path);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + suffix;

        try {
            file.transferTo(new File(path + newFileName));
            mv.addObject("message", "文件上传成功！");
            mv.addObject("fileName", newFileName);
            mv.addObject("originalFilename", originalFilename);
            mv.setViewName("uploadResult");
        } catch (IOException e) {
            e.printStackTrace();
            mv.addObject("message", "文件上传失败：" + e.getMessage());
            mv.setViewName("uploadResult");
        }
        return mv;
    }
}
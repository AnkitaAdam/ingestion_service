package com.vulnuris.IngestionService.controller;

import com.vulnuris.IngestionService.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(
            @RequestParam("files") List<MultipartFile> files) {

        String bundleId = UUID.randomUUID().toString();

        ingestionService.processFiles(files, bundleId);

        return ResponseEntity.ok("Files uploaded. Bundle ID: " + bundleId);
    }
}

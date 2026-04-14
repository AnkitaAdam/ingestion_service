package com.vulnuris.IngestionService.controller;

import com.vulnuris.IngestionService.context.IngestionContext;
import com.vulnuris.IngestionService.service.IngestionService;
import com.vulnuris.IngestionService.service.LogStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;
    private final LogStreamService logStreamService;


    @GetMapping("/stream/{bundleId}")
    public SseEmitter stream(@PathVariable String bundleId) {
        System.out.println("🌐 Stream API called for: " + bundleId);
        return logStreamService.createEmitter(bundleId);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("files") List<MultipartFile> files) throws InterruptedException {

        String bundleId = UUID.randomUUID().toString();
        IngestionContext context = new IngestionContext(bundleId);

        List<String> savedPaths = new ArrayList<>();

        try {

            String uploadDir = System.getProperty("user.dir")
                    + File.separator + "uploads"
                    + File.separator;

            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            for (MultipartFile file : files) {

                String filePath = uploadDir
                        + file.getOriginalFilename();

                File dest = new File(filePath);

                System.out.println("📁 Saving file to: " + filePath);

                file.transferTo(dest);

                savedPaths.add(filePath);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("status", "ERROR", "message", e.getMessage())
            );
        }


        new Thread(() -> {
            try {
                Thread.sleep(1000);

                logStreamService.send(context.getBundleId(), "\uD83C\uDD94 BundleID is created. "+ context.getBundleId());
                Thread.sleep(700);
                ingestionService.processFilesFromDisk(savedPaths, context);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return ResponseEntity.ok(
                Map.of(
                        "status", "SUCCESS",
                        "bundleId", bundleId
                )
        );
    }

}

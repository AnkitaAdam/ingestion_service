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

//    @PostMapping("/upload")
//    public ResponseEntity<String> upload(
//            @RequestParam("files") List<MultipartFile> files) {
//
//        String bundleId = UUID.randomUUID().toString();
//
//        ingestionService.processFiles(files, bundleId);
//
//        System.out.println("Files uploaded. Bundle ID: +" + bundleId);
//        return ResponseEntity.ok("Files uploaded. Bundle ID: " + bundleId);
//
//    }

//    @PostMapping("/upload")
//    public ResponseEntity<?> upload(
//            @RequestParam("files") List<MultipartFile> files) {
//
//
//        String bundleId = UUID.randomUUID().toString();
//
//        try {
//
//            ingestionService.processFiles(files, bundleId);
//
//            System.out.println("Files uploaded. Bundle ID: " + bundleId);
//
//            return ResponseEntity.ok(
//                    Map.of(
//                            "status", "SUCCESS",
//                            "bundleId", bundleId
//                    )
//            );
//
//        } catch (Exception e) {
//
//            e.printStackTrace(); // keep for debugging
//
//            return ResponseEntity.status(500).body(
//                    Map.of(
//                            "status", "ERROR",
//                            "message", e.getMessage(),
//                            "bundleId", bundleId
//                    )
//            );
//        }
//    }

    @GetMapping("/stream/{bundleId}")
    public SseEmitter stream(@PathVariable String bundleId) {
        System.out.println("🌐 Stream API called for: " + bundleId);
        return logStreamService.createEmitter(bundleId);
    }

//    @PostMapping("/upload")
//    public ResponseEntity<?> upload(
//            @RequestParam("files") List<MultipartFile> files) {
//
//        String bundleId = UUID.randomUUID().toString();
//
//        IngestionContext context = new IngestionContext(bundleId);
//
//        logStreamService.send(context.getBundleId(), "📂 BundleID is created.");
//
//        try {
//
//            ingestionService.processFiles(files, context);
//
//            System.out.println("Files are parsed "+bundleId);
//            return ResponseEntity.ok(
//                    Map.of(
//                            "status", "SUCCESS",
//                            "bundleId", bundleId
//                    )
//            );
//
//        } catch (Exception e) {
//
//            return ResponseEntity.status(500).body(
//                    Map.of(
//                            "status", "ERROR",
//                            "message", e.getMessage(),
//                            "bundleId", bundleId
//                    )
//            );
//        }
//
//    }

//    @PostMapping("/upload")
//    public ResponseEntity<?> upload(
//            @RequestParam("files") List<MultipartFile> files) {
//
//        String bundleId = UUID.randomUUID().toString();
//        IngestionContext context = new IngestionContext(bundleId);
//
//        List<String> savedPaths = new ArrayList<>();
//
//        try {
//
//            // 🔥 ADD THIS BLOCK HERE (FOLDER CREATION)
//            String uploadDir = System.getProperty("user.dir")
//                    + File.separator + "uploads"
//                    + File.separator;
//
//            File dir = new File(uploadDir);
//            if (!dir.exists()) {
//                dir.mkdirs(); // ✅ creates folder automatically
//            }
//
//            // 🔥 SAVE FILES TO DISK
//            for (MultipartFile file : files) {
//
//                String filePath = uploadDir + UUID.randomUUID() + "_" + file.getOriginalFilename();
//
//                File dest = new File(filePath);
//
//                file.transferTo(dest); // ✅ file saved to disk
//                System.out.println("Files saved at: "+dest.getAbsolutePath());
//
//                savedPaths.add(filePath);
//            }
//
//        } catch (Exception e) {
//
//            return ResponseEntity.status(500).body(
//                    Map.of(
//                            "status", "ERROR",
//                            "message", e.getMessage()
//                    )
//            );
//        }
//
//        // 🔥 CALL SERVICE (ASYNC)
//        ingestionService.processFilesFromDisk(savedPaths, context);
//
//        return ResponseEntity.ok(
//                Map.of(
//                        "status", "SUCCESS",
//                        "bundleId", bundleId
//                )
//        );
//    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("files") List<MultipartFile> files) throws InterruptedException {

        String bundleId = UUID.randomUUID().toString();
        IngestionContext context = new IngestionContext(bundleId);

        List<String> savedPaths = new ArrayList<>();

        try {

            // ✅ FIX: use absolute project path
            String uploadDir = System.getProperty("user.dir")
                    + File.separator + "uploads"
                    + File.separator;

            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs(); // ✅ create folder if not exists
            }

            for (MultipartFile file : files) {

                String filePath = uploadDir
                        + file.getOriginalFilename();

                File dest = new File(filePath);

                System.out.println("📁 Saving file to: " + filePath); // 🔥 DEBUG

                file.transferTo(dest); // ✅ SAVE TO DISK

                savedPaths.add(filePath);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("status", "ERROR", "message", e.getMessage())
            );
        }

        // ✅ THREAD (your original approach — correct)
        new Thread(() -> {
            try {
                Thread.sleep(1000); // allow SSE connection

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

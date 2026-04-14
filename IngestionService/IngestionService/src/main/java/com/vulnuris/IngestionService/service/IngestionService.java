package com.vulnuris.IngestionService.service;

import com.vulnuris.IngestionService.context.IngestionContext;
import com.vulnuris.IngestionService.kafka.KafkaProducerService;
import com.vulnuris.IngestionService.parser.LogParser;
import com.vulnuris.IngestionService.parser.ParserFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private final ParserFactory parserFactory;
    private final KafkaProducerService kafkaProducer;
    private final LogStreamService logStreamService;

    @Async
    public void processFilesFromDisk(List<String> filePaths, IngestionContext ingestionContext) throws InterruptedException {

        logStreamService.send(ingestionContext.getBundleId(), "📂 Files received");
        Thread.sleep(700);

        for (String path : filePaths) {

            File file = new File(path);

            logStreamService.send(ingestionContext.getBundleId(), "⚙\uFE0F Processing file: " + file.getName());
            Thread.sleep(700);

            try (InputStream is = new FileInputStream(file)) {

                logStreamService.send(ingestionContext.getBundleId(), "🔍 Detecting parser...");
                Thread.sleep(700);
                LogParser parser = parserFactory.getParser(file.getName(), ingestionContext);

                logStreamService.send(ingestionContext.getBundleId(), "🚀 Parsing started");
                Thread.sleep(700);

                logStreamService.send(ingestionContext.getBundleId(), "📤 Sending to Kafka");
                Thread.sleep(700);

                parser.parseStream(is, file.getName())
                        .peek(event -> event.setBundleId(ingestionContext.getBundleId()))
                        .forEach(event -> kafkaProducer.send(event, ingestionContext));

                logStreamService.send(ingestionContext.getBundleId(), "\uD83D\uDC4D Parsing completed: " + file.getName());

            } catch (Exception e) {

                logStreamService.send(ingestionContext.getBundleId(),
                        "❌ Error processing file: " + file.getName());

                throw new RuntimeException("Error processing file: " + file.getName(), e);

            }
        }

        logStreamService.send(ingestionContext.getBundleId(), "🎉 All files processed");
    }


}

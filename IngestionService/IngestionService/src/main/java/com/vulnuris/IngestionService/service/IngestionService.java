package com.vulnuris.IngestionService.service;

import com.vulnuris.IngestionService.kafka.KafkaProducerService;
import com.vulnuris.IngestionService.parser.LogParser;
import com.vulnuris.IngestionService.parser.ParserFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private final ParserFactory parserFactory;
    private final KafkaProducerService kafkaProducer;

    @Async
    public void processFiles(List<MultipartFile> files, String bundleId) {

        for (MultipartFile file : files) {

//            String sourceType = detectSourceType(file);

            LogParser parser = parserFactory.getParser(file);

            try (InputStream is = file.getInputStream()) {

                parser.parseStream(is, file.getOriginalFilename())
                        .peek(event -> event.setBundleId(bundleId)) //  IMPORTANT
                        .forEach(kafkaProducer::send);

            } catch (Exception e) {
                throw new RuntimeException("Error processing file: " + file.getOriginalFilename(), e);
            }
        }
    }


}

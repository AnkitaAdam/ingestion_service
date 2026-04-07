package com.vulnuris.IngestionService.kafka;

import com.vulnuris.IngestionService.model.CesEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, CesEvent> kafkaTemplate;

    public void send(CesEvent event) {
        kafkaTemplate.send("normalized-events", event.getUser(), event);
    }
}

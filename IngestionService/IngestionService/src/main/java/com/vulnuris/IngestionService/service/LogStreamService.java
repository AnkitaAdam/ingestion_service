package com.vulnuris.IngestionService.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class LogStreamService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String bundleId) {
        System.out.println("🟢 Emitter created for: " + bundleId);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(bundleId, emitter);

        emitter.onCompletion(() -> emitters.remove(bundleId));
        emitter.onTimeout(() -> emitters.remove(bundleId));
        System.out.println("here");
        return emitter;
    }

    public void send(String bundleId, String message) {

        System.out.println("📤 Attempting to send log for: " + bundleId);

        SseEmitter emitter = emitters.get(bundleId);

        if (emitter == null) {
            System.out.println("❌ No emitter found for: " + bundleId);
            return;
        }

        try {
            System.out.println("✅ Sending log: " + message);

            emitter.send(SseEmitter.event().data(message));

        } catch (Exception e) {
            emitter.complete();
            emitters.remove(bundleId);
        }
    }
}

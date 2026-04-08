package com.vulnuris.IngestionService.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulnuris.IngestionService.model.CesEvent;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Component
public class O365Parser implements LogParser {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Stream<CesEvent> parseStream(InputStream input, String filename) {

        try {
            AtomicLong offsetCounter = new AtomicLong(0);

            List<Map<String, Object>> logs =
                    mapper.readValue(input, new TypeReference<>() {});

            if (logs == null || logs.isEmpty()) return Stream.empty();

            return logs.stream()
                    .map(log -> convert(log, filename, offsetCounter.getAndIncrement()))
                    .filter(Objects::nonNull);

        } catch (Exception e) {
            throw new RuntimeException("O365 parsing error", e);
        }
    }

    private CesEvent convert(Map<String, Object> log, String file, long offset) {

        try {
            if (log == null) return null;

            // ---------- Core ----------
            String eventId = getOrGenerateId(log);
            String tsOriginal = safeString(log.get("CreationTime"));
            Instant tsUtc = parseTime(tsOriginal);

            String user = safeString(log.get("UserId"));
            String srcIp = extractIp(log);
            String action = safeString(log.get("Operation"));
            String workload = safeString(log.get("Workload"));
            String object = extractObject(log);


            String result = safeString(log.get("ResultStatus"));

            // ---------- Message ----------
            String message = buildMessage(log, user, action, srcIp);

            // ---------- IOC ----------
            List<String> iocs = extractIocs(log, srcIp);

            // ---------- Correlation ----------
            Map<String, String> correlation = new HashMap<>();
            putIfNotNull(correlation, "user", user);
            putIfNotNull(correlation, "srcIp", srcIp);
            putIfNotNull(correlation, "operation", action);
            putIfNotNull(correlation, "workload", workload);

            // ---------- Extra ----------
            Map<String, Object> extra = new HashMap<>();
            putIfNoNull(extra, "o365WorkLoad", workload);

            if (log.containsKey("OperationCount")) {
                extra.put("operationCount", log.get("OperationCount"));
            }

            if (log.containsKey("ExtendedProperties")) {

                Map<String, String> extProps =
                        parseExtendedProperties(log.get("ExtendedProperties"));

                if (!extProps.isEmpty()) {
                    extra.put("extendedProperties", extProps);
                }
            }

            // ---------- Build ----------
            return CesEvent.builder()
                    .eventId(eventId)


                    .tsUtc(tsUtc)
                    .tsOriginal(tsOriginal)
                    .tsOffset("Z")

                    .sourceType("O365")

                    .host(null)
                    .user(user)
                    .srcIp(srcIp)
                    .dstIp(null)
                    .srcPort(null)
                    .dstPort(null)
                    .protocol(null)

                    .action(action)
                    .object(object)

                    .result(result)
                    .severity(null)

                    .message(message)

                    .iocs(iocs)
                    .correlationKeys(correlation)
                    .extra(extra)

                    .rawRefFile(file)
                    .rawRefOffset(offset)

                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    // ================= HELPERS =================

    private String getOrGenerateId(Map<String, Object> log) {
        String id = safeString(log.get("Id"));
        return id != null ? id : UUID.randomUUID().toString();
    }

    private String safeString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Instant parseTime(String time) {
        try {
            if (time == null) return Instant.now();
            return Instant.parse(time); // already UTC in logs
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private String extractIp(Map<String, Object> log) {
        String ip = safeString(log.get("ClientIP"));
        if (ip == null) return null;

        // Sometimes IP comes like "IP:Port"
        if (ip.contains(":")) {
            return ip.split(":")[0];
        }
        return ip;
    }

    private String extractObject(Map<String, Object> log) {

        // Mailbox events → folders
        if (log.containsKey("Folders")) {
            return log.get("Folders").toString();
        }

        // Default → ObjectId
        return safeString(log.get("ObjectId"));
    }

    private List<String> extractIocs(Map<String, Object> log, String srcIp) {
        List<String> iocs = new ArrayList<>();

        if (srcIp != null) iocs.add(srcIp);

        String user = safeString(log.get("UserId"));
        if (user != null && user.contains("@")) {
            iocs.add(user);
        }

        String object = safeString(log.get("ObjectId"));
        if (object != null && object.contains("@")) {
            iocs.add(object);
        }

        String sourceRelativeUrl = safeString(log.get("SourceRelativeUrl"));
        if(sourceRelativeUrl != null){
            iocs.add(sourceRelativeUrl);
        }

        String siteUrl = safeString(log.get("SiteUrl"));
        if(siteUrl != null){
            iocs.add(siteUrl);
        }

        return iocs;
    }



    private String buildMessage(Map<String, Object> log,
                                String user,
                                String action,
                                String ip) {

        StringBuilder msg = new StringBuilder("O365 ");

        if (action != null) msg.append(action);

        if (user != null) msg.append(" by ").append(user);

        if (ip != null) msg.append(" from ").append(ip);

        if (log.containsKey("LogonError")) {
            msg.append(" | Error: ").append(log.get("LogonError"));
        }

        return msg.toString();
    }

    private void putIfNotNull(Map<String, String> map, String k, String v) {
        if (v != null) map.put(k, v);
    }

    private void putIfNoNull(Map<String, Object> map, String k, Object v) {
        if (v != null) map.put(k, v);
    }

    private Map<String, String> parseExtendedProperties(Object extObj) {

        Map<String, String> map = new HashMap<>();

        if (!(extObj instanceof List<?> list)) {
            return map;
        }

        for (Object item : list) {
            if (item instanceof Map<?, ?> entry) {

                Object nameObj = entry.get("Name");
                Object valueObj = entry.get("Value");

                if (nameObj != null && valueObj != null) {
                    String key = nameObj.toString();
                    String value = valueObj.toString();

                    map.put(key, value);
                }
            }
        }

        return map;
    }

}
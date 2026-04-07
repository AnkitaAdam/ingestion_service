package com.vulnuris.IngestionService.parser;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.startup.WebappServiceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ParserFactory {

    private final CloudTrailParser cloudTrail;
    private final O365Parser o365;
    private final PaloAltoFirewallParser paloAlto;
    private final SyslogParser syslog;
    private final WindowsSecurityParser windowsSecurity;
    private final WebAccessLogParser webAccessLog;
    private final WazuhAlertsParser wazuhAlerts;

    public LogParser getParser(MultipartFile file) {

        String fileName = file.getOriginalFilename().toLowerCase();

        if (fileName.contains("cloudtrail") || fileName.contains("aws")) {
            return cloudTrail;
        }

        if (fileName.contains("o365")) {
            return o365;
        }

        if (fileName.contains("paloalto")) {
            return paloAlto;
        }

        if (fileName.contains("linux") || fileName.contains("syslog")) {
            return syslog;
        }

        if (fileName.contains("windows")) {
            return windowsSecurity;
        }

        if (fileName.contains("web")) {
            return webAccessLog;
        }

        if (fileName.contains("wazuh")) {
            return wazuhAlerts;
        }

        throw new RuntimeException("Unsupported file type: " + fileName);
    }
}

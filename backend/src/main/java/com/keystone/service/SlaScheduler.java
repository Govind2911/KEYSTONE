package com.keystone.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled job: periodically flags work orders at risk of, or in, SLA breach. */
@Component
@RequiredArgsConstructor
public class SlaScheduler {
    private static final Logger log = LoggerFactory.getLogger(SlaScheduler.class);
    private final SlaService slaService;

    @Scheduled(fixedDelayString = "${keystone.sla.scan-interval-ms:60000}", initialDelay = 15000)
    public void run() {
        try {
            int n = slaService.scan();
            if (n > 0) log.info("SLA scan flagged {} work order(s)", n);
        } catch (Exception e) {
            log.error("SLA scan failed", e);
        }
    }
}

package com.zeepseek.backend.domain.logevent.event;

import com.zeepseek.backend.domain.logevent.service.LogService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class LogEventListener {

    private final LogService logService;

    public LogEventListener(LogService logService) {
        this.logService = logService;
    }

    @Async
    @EventListener
    public void handleLogEvent(LogEvent event) {
        // event.getExtraData()를 활용하여 추가 데이터도 저장 가능
        logService.logAction(
                event.getAction(),
                event.getType(),
                (int) event.getExtraData().getOrDefault("userId", -1), // 필요에 따라 데이터 변환
                (int) event.getExtraData().getOrDefault("age", -1),
                (String) event.getExtraData().getOrDefault("gender", "unknown"),
                (int) event.getExtraData().getOrDefault("propertyId", -1),
                (int) event.getExtraData().getOrDefault("dongId", -1)
        );
    }
}

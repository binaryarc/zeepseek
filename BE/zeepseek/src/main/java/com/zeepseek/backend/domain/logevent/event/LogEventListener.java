package com.zeepseek.backend.domain.logevent.event;

import com.zeepseek.backend.domain.logevent.service.LogService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LogEventListener {

    private final LogService logService;

    public LogEventListener(LogService logService) {
        this.logService = logService;
    }

    @Async
    @EventListener
    public void handleLogEvent(LogEvent event) {
        int userId = (int) event.getExtraData().getOrDefault("userId", -1);
        int age = (int) event.getExtraData().getOrDefault("age", -1);
        String gender = (String) event.getExtraData().getOrDefault("gender", "unknown");
        int propertyId = (int) event.getExtraData().getOrDefault("propertyId", -1);
        int dongId = (int) event.getExtraData().getOrDefault("dongId", -1);
        List<Integer> dongIds = (List<Integer>) event.getExtraData().getOrDefault("dongIds", null);
        List<Integer> propertyIds = (List<Integer>) event.getExtraData().getOrDefault("propertyIds", null);

        if(dongIds != null) {
            for(Integer id: dongIds) {
                logService.logAction(
                        event.getAction(),
                        event.getType(),
                        userId,
                        age,
                        gender,
                        propertyId,
                        id
                );
            }
        } else if (propertyIds != null) {
            for(Integer id: propertyIds) {
                logService.logAction(
                        event.getAction(),
                        event.getType(),
                        userId,
                        age,
                        gender,
                        id,
                        dongId
                );
            }
        } else {
            logService.logAction(
                    event.getAction(),
                    event.getType(),
                    userId,
                    age,
                    gender,
                    propertyId,
                    dongId
            );
        }
    }
}

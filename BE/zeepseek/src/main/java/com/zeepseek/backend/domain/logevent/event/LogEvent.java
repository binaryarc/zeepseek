package com.zeepseek.backend.domain.logevent.event;

import lombok.*;
import org.springframework.context.ApplicationEvent;
import java.util.Map;

@Getter
@Setter
public class LogEvent extends ApplicationEvent {
    private final String action;
    private final String type;
    private final Map<String, Object> extraData;

    public LogEvent(Object source, String action, String type, Map<String, Object> extraData) {
        super(source);
        this.action = action;
        this.type = type;
        this.extraData = extraData;
    }

    public String getAction() {
        return action;
    }

    public String getType() {
        return type;
    }


    public Map<String, Object> getExtraData() {
        return extraData;
    }
}
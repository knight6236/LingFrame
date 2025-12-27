package com.lingframe.api.event;

import lombok.Getter;

import java.io.Serializable;

/**
 * 框架事件基类
 */
@Getter
public abstract class AbstractLingEvent implements LingEvent, Serializable {
    private final long timestamp;

    public AbstractLingEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[timestamp=" + timestamp + "]";
    }
}

package com.cmu18842.team3.testwifidirect;

import java.io.Serializable;

/**
 * Created by niro on 4/9/15.
 */
public class Message implements Serializable {
    private String messageContent;
    private Object location;
    private boolean isInit = false;

    public Message() {
    }

    public String getMessageContent() {
        return messageContent;
    }

    public Object getLocation() {
        return location;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public void setLocation(Object location) {
        this.location = location;
    }

    public boolean getIsInit() {
        return isInit;
    }

    public void setIsInit(boolean isInit) {
        this.isInit = isInit;
    }
}

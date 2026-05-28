package com.noah.jauth.util.log.decorator;

public class InfoDecorator implements Decorator {

    @Override
    public String decorate(String message) {
        return "INFO: " + message;
    }
}

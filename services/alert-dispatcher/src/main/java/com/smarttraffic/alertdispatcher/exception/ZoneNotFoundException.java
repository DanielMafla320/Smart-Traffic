package com.smarttraffic.alertdispatcher.exception;

public class ZoneNotFoundException extends RuntimeException {

    private final String zoneName;

    public ZoneNotFoundException(String zoneName) {
        super("Zone not found: " + zoneName);
        this.zoneName = zoneName;
    }

    public String zoneName() {
        return zoneName;
    }
}

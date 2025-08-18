package com.safra.app.model;

@SuppressWarnings("unused")
public class NotificationTokenModel {

    private String token;

    public NotificationTokenModel(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

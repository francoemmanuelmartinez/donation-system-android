package com.francomartinez.donationsystem.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import retrofit2.Response;

public final class ApiErrorHandler {

    private ApiErrorHandler() {
    }

    public static String messageFrom(Response<?> response) {
        if (response != null && response.errorBody() != null) {
            try {
                String raw = response.errorBody().string();
                JsonObject object = new JsonParser().parse(raw).getAsJsonObject();
                if (object.has("message")) {
                    String message = object.get("message").getAsString();
                    if (message != null && !message.trim().isEmpty()) {
                        return message;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
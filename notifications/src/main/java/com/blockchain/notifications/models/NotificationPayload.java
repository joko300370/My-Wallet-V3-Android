package com.blockchain.notifications.models;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import timber.log.Timber;

public class NotificationPayload {

    private String title;
    private String body;
    private NotificationData data;
    private Map<String, String> payload;

    public NotificationPayload(Map<String, String> map) {
        if (map.containsKey("title")) {
            title = map.get("title");
        }

        if (map.containsKey("body")) {
            body = map.get("body");
        }

        if (map.containsKey("data")) {
            data = new NotificationData(map.get("data"));
        }
        payload = map;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getBody() {
        return body;
    }

    @Nullable
    public String getAddress() {
        return data != null ? data.getAddress() : null;
    }

    @Nullable
    public NotificationType getType() {
        return data != null ? data.getType() :
            payload.get("type") != null ? NotificationType.fromString(payload.get("type")) : null;
    }

    @Nullable
    public String getMdid() {
        return data != null ? data.getMdid() : null;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    public static final String PUB_KEY_HASH = "fcm_data_pubkeyhash";
    public static final String DATA_MESSAGE = "fcm_data_message";
    public static final String ORIGIN_IP = "origin_ip";
    public static final String ORIGIN_COUNTRY = "origin_country";
    public static final String ORIGIN_BROWSER = "origin_browser";

    private static class NotificationData {

        private String mdid;
        private NotificationType type;
        private String address;

        NotificationData(String data) {
            try {
                JSONObject jsonObject = new JSONObject(data);
                if (jsonObject.has("id")) {
                    mdid = jsonObject.getString("id");
                }

                if (jsonObject.has("type")) {
                    type = NotificationType.fromString(jsonObject.getString("type"));
                }

                if (jsonObject.has("address")) {
                    address = jsonObject.getString("address");
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Nullable
        public String getMdid() {
            return mdid;
        }

        @Nullable
        public NotificationType getType() {
            return type;
        }

        @Nullable
        public String getAddress() {
            return address;
        }
    }

    public enum NotificationType {
        PAYMENT("payment"),
        SECURE_CHANNEL_MESSAGE("secure_channel");

        private String name;

        NotificationType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Nullable
        public static NotificationType fromString(String string) {
            if (string != null) {
                for (NotificationType type : NotificationType.values()) {
                    if (type.getName().equalsIgnoreCase(string)) {
                        return type;
                    }
                }
            }
            return null;
        }
    }
}
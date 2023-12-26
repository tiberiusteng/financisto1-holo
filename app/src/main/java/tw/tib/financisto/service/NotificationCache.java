package tw.tib.financisto.service;

import java.util.HashMap;

public class NotificationCache {
    private static NotificationCache INSTANCE = null;
    public HashMap<String, NotificationListener.ParsedNotification> cache;

    private NotificationCache() {
        cache = new HashMap<>();
    }

    public static NotificationCache getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NotificationCache();
        }
        return INSTANCE;
    }
}

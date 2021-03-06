package com.moac.android.opensecretsanta.util;

import android.content.SharedPreferences;

import rx.Subscription;

public class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    private static final String DO_ONCE_TAG = "do_once";

    // No instances
    private Utils() {}

    public static boolean doOnce(SharedPreferences prefs, String taskTag, Runnable task) {
        final String prefTag = DO_ONCE_TAG + taskTag;
        boolean isDone = prefs.getBoolean(prefTag, false);
        if(!isDone) {
            task.run();
            prefs.edit().putBoolean(prefTag, true).apply();
            return true;
        }
        return false;
    }

    public static void safeUnsubscribe(Subscription subscription) {
        if(subscription != null) {
            subscription.unsubscribe();
        }
    }
}

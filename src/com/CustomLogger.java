package com;

/**
 * Created by crs on 4/7/17.
 */
public class CustomLogger {

    public enum Category {
        EVENT,
        METRIC,
        TRANSACTION,
        WARNING
    };

    public static void log(Category category, String msg) {
        long threadId = Thread.currentThread().getId();

        String output = String.format("[%12s] | [%d] | %s", new Object[] {category, threadId, msg});
        System.out.println(output);
    }

}

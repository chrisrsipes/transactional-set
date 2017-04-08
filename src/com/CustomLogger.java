package com;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by crs on 4/7/17.
 */
public class CustomLogger {

    public enum Category {
        EVENT,
        METRIC,
        TRANSACTION,
        WARNING,
        EXCEPTION,
        DEBUG_FINE,
        DEBUG_COARSE
    };

    // @TODO: create final excluded logs that can be set once at the beginning
    // of the application (during an initialization step), which will filter out excluded logs
    // (or inverse: provide list of logs to print out)
    public static void log(Category category, String msg) {
        long threadId = Thread.currentThread().getId();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        Object[] logArgs = new Object[] {sdf.format(new Date()), category, threadId, msg};
        String output = String.format("[%s] | [%12s] | [%d] | %s", logArgs);
        System.out.println(output);
    }

}

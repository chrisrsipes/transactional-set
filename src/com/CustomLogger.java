/**
 * name: CustomLogger
 * author: crs
 * description: custom logger implementation to produce readable
 *              logs for events, metrics, transaction's actions,
 *              exceptions, and debugging information.
 *              uses pipes as a delimiter, can easily be parsed
 *              in excel by splitting on column and filtering by category
 *
 * Edit History:
 * - Created by crs on 4/7/17.
 *
 * Updates:
 * - new Categories
 *
 */

package com;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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

    private static ArrayList<Category> logCategories = new ArrayList<>();

    public static void setLogCategories(ArrayList<Category> logCategories) {
        // empty out the list
        CustomLogger.logCategories.removeAll(CustomLogger.logCategories);

        for (Category category : logCategories) {
            CustomLogger.logCategories.add(category);
        }
    }

    public static void log(Category category, String msg) {
        if (CustomLogger.logCategories.contains(category)) {
            long threadId = Thread.currentThread().getId();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
            Object[] logArgs = new Object[] {sdf.format(new Date()), category, threadId, msg};
            String output = String.format("[%s] | [%12s] | [%d] | %s", logArgs);
            System.out.println(output);
        }
    }

}

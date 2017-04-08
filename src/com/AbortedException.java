/**
 * author: crs
 * description: defines custom AbortedException for threads to throw when a transaction is aborted.
 *
 * Edit History:
 * - created by crs on 3/30/17.
 *
 * Updates:
 *
 */


package com;

public class AbortedException extends Exception {

    public AbortedException() {
        super();
    }

    public AbortedException(String message) {
        super(message);
    }
}

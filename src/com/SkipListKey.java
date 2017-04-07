package com;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SkipListKey {
    private static final Logger logger = Logger.getLogger( SkipListKey.class.getName() );
    ConcurrentSkipListSet<Integer> list;
    LockKey lock;

    public SkipListKey() {
        list = new ConcurrentSkipListSet<>();
        lock = new LockKey();
    }


    public boolean add(final int v) {
        boolean result = false;
        Object[] logArgs;

        try {
            logArgs = new Object[] {java.lang.Thread.currentThread().getId(), OperationType.ADD.toString(), v};
            logger.log(Level.FINER, "Thread with id {0} attempting to acquire lock for value {2}.", logArgs);

            lock.lock(v, new ArrayList<Lock>());

            logger.log(Level.FINER, "Thread with id {0} successfully acquired lock for value {2}.", logArgs);

            result = list.add(v);

            logArgs = new Object[] {java.lang.Thread.currentThread().getId(), OperationType.ADD.toString(), v};
            logger.log(Level.FINER, "Thread with id {0} performed operation {1} with value {2}.", logArgs);

            if (result) {
                Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(java.lang.Thread t, Throwable e) {
                        if (t instanceof Thread) {
                            Thread et = (Thread) t;
                            logger.log(
                                    Level.WARNING,
                                    "Unhandled Exception in thread with id {0}, of type Thread. (Operation: {1}, Value: {2}).",
                                    new Object[] {et.getId(), et.getOperationType(), et.getOperationValue()}
                            );

                            list.remove(v);
                        }
                        else {
                            logger.log(Level.WARNING, "Unhandled Exception in thread with id {0}, not of type Thread.", t.getId());
                        }

                    }
                });
            }

        } catch (Exception e) {
            logArgs = new Object[] {java.lang.Thread.currentThread().getId(), v};
            logger.log(Level.WARNING, "Thread with id {0} Failed to acquire lock for element {1}.", logArgs);
        }


        return result;
    }

}

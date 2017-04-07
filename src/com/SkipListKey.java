package com;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SkipListKey {

    public static enum OperationType {
        CONTAINS,
        ADD,
        REMOVE
    }

    private static final Logger logger = Logger.getLogger( SkipListKey.class.getName() );
    ConcurrentSkipListSet<Integer> list;
    LockKey lock;

    public SkipListKey() {
        list = new ConcurrentSkipListSet<>();
        lock = new LockKey();
    }


    public boolean add(final int v) {
        boolean result = false;

        try {
            lock.lock(v);
            result = list.add(v);

            if (result) {
                Transaction.getLocal().setUseInverse();
            }
        } catch (Exception e) {
            // do nothing, operation will be marked as failed
        }

        return result;
    }

    public boolean remove(final int v) {
        boolean result = false;

        try {
            lock.lock(v);
            result = list.remove(v);

            if (result) {
                Transaction.getLocal().setUseInverse();
            }
        } catch (Exception e) {
            // do nothing, operation will be marked as failed
        }

        return result;
    }

    public boolean contains(final int v) {
        boolean result = false;

        try {
            lock.lock(v);
            result = list.contains(v);

            if (result) {
                Transaction.getLocal().setUseInverse();
            }
        } catch (Exception e) {
            // do nothing, operation will be marked as failed
        }

        return result;
    }

}

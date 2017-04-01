package com;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;

public class SkipListKey {
    ConcurrentSkipListSet<Integer> list;
    LockKey lock;

    public boolean add(final int v) {
        try {
            lock.lock(v, new ArrayList<Lock>());
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean result = list.add(v);

        if (result) {
            /*
            Thread.onRollback(new Runnable() {
                public void run () {
                    list.remove(v);
                }
            });
            */
        }

        return result;
    }

}


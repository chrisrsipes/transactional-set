package com;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockKey {

    private final int LOCK_TIMEOUT = 10000;
    private ConcurrentHashMap<Integer, Lock> map;

    public LockKey() {
        map = new ConcurrentHashMap<>();
    }

    public void lock(int key, ArrayList<Lock> lockSet) throws AbortedException, InterruptedException {
        Lock lock = map.get(key);

        if (lock == null) {
            Lock newLock = new ReentrantLock();
            Lock oldLock = map.putIfAbsent(key, newLock);
            lock = (oldLock == null) ? newLock : oldLock;
        }

        if (lockSet.add(lock)) {
            if (!lock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                lockSet.remove(lock);
                throw new AbortedException();
                /*
                if (Thread.getStatus() == Status.STATUS_ACTIVE) {
                    Thread.rollback();
                    throw new AbortedException();
                }
                Thread.getTransaction().abort();
                */
            }
        }
    }
}

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

    public void lock(int key) throws AbortedException, InterruptedException {
        Lock lock = map.get(key);
        HashSet<Lock> lockSet = TThread.getLockSet();

        if (lock == null) {
            Lock newLock = new ReentrantLock();
            Lock oldLock = map.putIfAbsent(key, newLock);
            lock = (oldLock == null) ? newLock : oldLock;
        }

        if (lockSet.add(lock)) {
            if (!lock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {

                lockSet.remove(lock);

                if (Transaction.getLocal() != null) {
                    Transaction.getLocal().abort();
                }

                throw new AbortedException();
            }
        }
    }
}

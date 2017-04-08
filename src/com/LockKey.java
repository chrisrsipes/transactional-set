/**
 * name: LockKey
 * author: aar
 * author: crs
 * description: provides a concurrent pool of locks that all threads attempting operations
 *              on the set must acquire.  provides lock() method that creates a new lock
 *              for the semantic key (element) if one does not exist, and attempts to lock
 *              it.  the lock has a timeout to guarantee progress.  additionally, the LockKey
 *              class manages adding locks to the Transaction's ThreadLocal lockList; when the
 *              Transaction is aborted the Transaction manages iterating through this list and releasing
 *              those locks.
 *
 * acknowledgements: LockKey implementation as described by Herlihy and Koskinen
 *
 * extra info: transactional boosting is the process of taking some blackbox base data structure,
 *             semantically identifying conditions when transactions won't commute, and associating abstract
 *             locks on those conditions.  by acquiring a lock associated with the element of the operation,
 *             for sets we can guarantee that all transactions will commute.
 *
 * Edit History:
 * - Renovated by crs on 4/7/17.
 *
 * Updates:
 * - changes to how callables are being generated
 * - operations and inverses are now generated in advance of running the simulation for the configuration
 * - refactored method of logging
 *
 */

package com;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockKey {

    private final int LOCK_TIMEOUT = 10000;
    private ConcurrentHashMap<Integer, Lock> map;

    // instantiate map for key -> lock on construction
    public LockKey() {
        map = new ConcurrentHashMap<>();
    }

    // attempts to acquire a lock associated with the key
    public void lock(int key) throws AbortedException, InterruptedException {
        Lock lock = map.get(key);
        HashSet<Lock> lockSet = Transaction.getLockSet();

        // creates the lock if it does not exist
        if (lock == null) {
            Lock newLock = new ReentrantLock();

            // its possible a lock was created for this element when we created one;
            // we use the newly created lock only if it replaced null at that key.
            Lock oldLock = map.putIfAbsent(key, newLock);
            lock = (oldLock == null) ? newLock : oldLock;
        }

        // try to add the lock to the Transaction's thread-local lock set
        if (lockSet.add(lock)) {

            // if we can't acquire the lock in the time, remove it from the
            // lockSet and abort
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

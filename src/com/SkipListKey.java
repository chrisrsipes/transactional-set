/**
 * name: SkipListKey
 * author: aar
 * author: crs
 * description: wrapper class for the thread-safe base data structure, in our case
 *              Java's lock-free concurrent skiplist implementation.
 *              we wrap the operations of the concurrent data structure to implement transactional boosting
 *              by the abstract semantic locks and atomicity provided by commit / abort.
 *
 * acknowledgements: SkipListKey largely influenced by implementation described by Herlihy and Koskinen
 *
 * extra info:
 *
 * Edit History:
 * - Renovated by crs on 4/7/17.
 *
 * Updates:
 * - now using flag to determine if inverse operation should be invoked on abort
 *   not sure how abort handler was implemented in paper
 *
 */

package com;

import java.util.concurrent.ConcurrentSkipListSet;

public class SkipListKey {

    // describes type of operation being performed
    public enum OperationType {
        CONTAINS,
        ADD,
        REMOVE
    }

    ConcurrentSkipListSet<Integer> list;
    LockKey lock;

    // initializes underling skiplist and its lockkey
    public SkipListKey() {
        list = new ConcurrentSkipListSet<>();
        lock = new LockKey();
    }

    // transaction-boosted wrapper for add
    public boolean add(final int v) {
        boolean result = false;

        try {
            // acquire semantic lock for key to ensure all other concurrent transactions will commute
            lock.lock(v);

            // attempt to add to skiplist, can succeed or fail based on state of skiplist
            result = list.add(v);

            Object[] logArgs = new Object[] {v, result ? "COMPLETED" : "FAILED"};
            CustomLogger.log(CustomLogger.Category.TRANSACTION, String.format("adding %d to the set (status: %s)", logArgs));

            // if we successfully added it to the list, if we abort, we need to
            // invoke the inverse operation, which is driven by this flag being set here
            if (result) {
                Transaction.getLocal().setUseInverse();
            }
        } catch (Exception e) {
            // do nothing, operation will be marked as failed
        }

        // return whether add was successful
        return result;
    }

    // transaction-boosted wrapper for remove
    public boolean remove(final int v) {
        boolean result = false;

        try {
            // acquire semantic lock for key to ensure all other concurrent transactions will commute
            lock.lock(v);

            // attempt to remove from skiplist, can succeed or fail based on state of skiplist
            result = list.remove(v);

            Object[] logArgs = new Object[] {v, result ? "COMPLETED" : "FAILED"};
            CustomLogger.log(CustomLogger.Category.TRANSACTION, String.format("removing %d from the set (status: %s)", logArgs));

            // if we successfully removed it to the list, if we abort, we need to
            // invoke the inverse operation, which is driven by this flag being set here
            if (result) {
                Transaction.getLocal().setUseInverse();
            }
        } catch (Exception e) {
            // do nothing, operation will be marked as failed
        }

        return result;
    }

    // transaction-boosted wrapper for contains
    public boolean contains(final int v) {
        boolean result = false;

        try {
            // acquire semantic lock for key to ensure all other concurrent transactions will commute
            lock.lock(v);

            // checks if skiplist contains key, can succeed or fail based on state of skiplist
            result = list.contains(v);

            // this is mostly for symmetry, this will just invoke a noop
            if (result) {
                Transaction.getLocal().setUseInverse();
            }
        } catch (Exception e) {
            // do nothing, operation will be marked as failed
        }

        return result;
    }

}

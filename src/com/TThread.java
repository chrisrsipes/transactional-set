/**
 * name: TThread
 * author: crs
 * description: provides implementation for transaction-compatible thread
 *
 * Edit History:
 * - Renovated by crs on 4/8/17.
 *
 * Acknowledgements:
 * Largely derived from Art of Multiprocessor Programming page [pageNumber],
 * see TThread implementation of the TinyTM.
 * The commit and abort implementations are influenced by Herlihy and Koskinen
 *
 * Updates:
 *
 */

package com;

import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

public class TThread extends java.lang.Thread {

    // stores operation and inverse as instance variables for
    // when void run() method is invoked by ThreadPoolExecutor.
    // this allows us to send it into the static doIt implementation
    // that drives the transaction's behaviors
    private final Callable<Boolean> operation;
    private final Callable<Boolean> inverse;

    // clear locks as part of abort step, as per Herlihy and Koskinen
    static Runnable onAbort = new Runnable() {
        @Override
        public void run() {
            HashSet<Lock> lockSet = Transaction.getLockSet();
            for (Lock l : lockSet) {
                l.unlock();
                lockSet.remove(l);
            }
        }
    };

    // clear locks as part of commit step, as per Herlihy and Koskinen
    static Runnable onCommit = new Runnable() {
        @Override
        public void run() {
            HashSet<Lock> lockSet = Transaction.getLockSet();
            for (Lock l : lockSet) {
                l.unlock();
                lockSet.remove(l);
            }
        }
    };

    // validates the transaction running to determine whether
    // we're in a state that we can commit, or if the transaction
    // was aborted
    //
    // note that this implementation is different than standard
    // transaction-based validate's as normally, in active state,
    // before commiting you must check the timestamp of the write location
    // and ensure the local read val is greater.  if our transaction is active,
    // then we have the abstract lock associated with the key, and know that it commutes still.
    static Callable<Boolean> onValidate = new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
            Transaction transaction = Transaction.getLocal();

            switch (transaction.getStatus()) {
                case ABORTED:
                    return false;
                case COMMITTED:
                    return true;
                case ACTIVE:
                    return true;
            }

            return false;
        }
    };

    // initializes TThread with the operation and inverse
    public TThread (Callable<Boolean> operation, Callable<Boolean> inverse) {
        // we set these as instance variables so they can be referenced from the void run method,
        // where the transactions are actually being created (which we'll give the operation and inverse to)
        this.operation = operation;
        this.inverse = inverse;
    }

    // targeted by the ThreadPoolExecutor when running a thread
    public void run() {
        try {
            doIt(this.operation, this.inverse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // @TODO: get page number that this if from
    // implementation from Art of MultiProcessorProgramming on page [pageNumber]
    // retries the transaction if aborted until it succeeds, or if there is an exception
    // other than abort triggered by the thread return out.
    public static Boolean doIt(Callable<Boolean> operation, Callable<Boolean> inverse) throws Exception {
        Boolean result = null;

        while (true) {
            Transaction me = new Transaction(operation, inverse);
            Transaction.setLocal(me);

            try {
                result = operation.call();
            } catch (AbortedException e) {

            } catch (Exception e) {
                throw new Exception(e);
            }

            if (onValidate.call()) {
                if (me.commit()) {
                    onCommit.run();
                    return result;
                }
            }

            me.abort();
            onAbort.run();

            return null;
        }
    }

}

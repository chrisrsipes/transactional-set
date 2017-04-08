package com;

import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

public class TThread extends java.lang.Thread {

    private final Callable<Boolean> operation;
    private final Callable<Boolean> inverse;

    // clear locks
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

    // clear locks
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

    // @TODO: figure out what the hell this is, perhaps the Validate class thing
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

    public TThread (Callable<Boolean> operation, Callable<Boolean> inverse) {
        // we set these as instance variables so they can be referenced from the void run method,
        // where the transactions are actually being created (which we'll give the operation and inverse to)
        this.operation = operation;
        this.inverse = inverse;
    }

    public void run() {
        try {
            doIt(this.operation, this.inverse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

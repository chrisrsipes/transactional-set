package com;


import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

public class TThread extends java.lang.Thread {

    private final Callable<Boolean> xaction;

    //static ThreadLocal<HashSet<Lock>> lockSet = new ThreadLocal<>();
    static HashSet<Lock> lockSet = null;

    public static HashSet<Lock> getLockSet() {
        return lockSet;//.get();
    }

    // clear locks
    static Runnable onAbort = new Runnable() {
        @Override
        public void run() {
            //HashSet<Lock> locks = lockSet.get();
            for (Lock l : lockSet) {
                l.unlock();
            }
        }
    };

    // clear locks
    static Runnable onCommit = new Runnable() {
        @Override
        public void run() {
            //HashSet<Lock> locks = lockSet.get();
            for (Lock l : lockSet) {
                l.unlock();
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
                    return false;
            }

            return false;
        }
    };

    public TThread (Callable<Boolean> xaction) {
        this.xaction = xaction;
        //this.lockSet.set(new HashSet<Lock>());
        this.lockSet = new HashSet<Lock>();
    }

    public void run() {
        try {
            doIt(this.xaction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> T doIt(Callable<T> xaction) throws Exception {
        T result = null;

        while (true) {
            Transaction me = new Transaction();
            Transaction.setLocal(me);

            try {
                result = xaction.call();
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

package com;


import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

public class TThread extends java.lang.Thread {

    static ThreadLocal<HashSet<Lock>> lockSet = new ThreadLocal<>();

    // clear locks
    static Runnable onAbort = new Runnable() {
        @Override
        public void run() {
            HashSet<Lock> locks = lockSet.get();
            for (Lock l : locks) {
                l.unlock();
            }
        }
    };

    // clear locks
    static Runnable onCommit = new Runnable() {
        @Override
        public void run() {
            HashSet<Lock> locks = lockSet.get();
            for (Lock l : locks) {
                l.unlock();
            }
        }
    };

    // @TODO: figure out what the hell this is, perhaps the Validate class thing
    static Callable<Boolean> onValidate = null;

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
        }
    }
}

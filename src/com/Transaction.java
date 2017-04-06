package com;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by aar on 4/5/17.
 * Got this straight from the book page 445
 */
public class Transaction {
    public enum Status {ABORTED, ACTIVE, COMMITTED};

    // This Line makes no sense to me.
    public static Transaction COMMITTED = new Transaction(Status.COMMITTED);
    private final AtomicReference<Status> status;

    static ThreadLocal<Transaction> local = new ThreadLocal<Transaction>(){
        protected Transaction initialValue(){
            return new Transaction(Status.COMMITTED);
        }
    };

    public Transaction(){
        status = new AtomicReference<Status>(Status.ACTIVE);
    }

    private Transaction(Transaction.Status myStatus){
        status = new AtomicReference<Status>(myStatus);
    }

    public Status getStatus() {
        return status.get();
    }

    public boolean commit() {
        return status.compareAndSet(Status.ACTIVE, Status.COMMITTED);
    }

    public boolean abort() {
        return status.compareAndSet(Status.ACTIVE, Status.ABORTED);
    }

    public static Transaction getLocal() {
        return local.get();
    }

    public static void setLocal(Transaction transaction) {
        local.set(transaction);
    }


}

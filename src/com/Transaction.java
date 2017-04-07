package com;

import java.util.concurrent.Callable;
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
    private final AtomicReference<Boolean> useInverseOperation;
    private final Callable<Boolean> operation;
    private final Callable<Boolean> inverse;


    static ThreadLocal<Transaction> local = new ThreadLocal<Transaction>(){
        protected Transaction initialValue(){
            return new Transaction(Status.COMMITTED);
        }
    };

    // make thread local lock set with getters / setters
    // static ThreadLocal<Transaction>

    public Transaction(){
        status = new AtomicReference<>(Status.ACTIVE);
        useInverseOperation = new AtomicReference<>(false);
        this.operation = Transaction.getNoopOperation();
        this.inverse = Transaction.getNoopOperation();
    }

    public Transaction(Callable<Boolean> operation, Callable<Boolean> inverse) {
        status = new AtomicReference<>(Status.ACTIVE);
        useInverseOperation = new AtomicReference<>(false);
        this.operation = operation;
        this.inverse = inverse;
    }

    private Transaction(Transaction.Status myStatus){
        status = new AtomicReference<Status>(myStatus);
        useInverseOperation = new AtomicReference<>(false);
        this.operation = Transaction.getNoopOperation();
        this.inverse = Transaction.getNoopOperation();
    }


    public Status getStatus() {
        return status.get();
    }

    public Boolean getUseInverse() {
        return useInverseOperation.get();
    }

    public boolean setUseInverse() {
        return useInverseOperation.compareAndSet(false, true);
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

    private static Callable<Boolean> getAddOperation(SkipListKey.OperationType operationType, final int operationValue, final SkipListKey transactionalSet) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return transactionalSet.add(operationValue);
            }
        };
    }

    private static Callable<Boolean> getRemoveOperation(SkipListKey.OperationType operationType, final int operationValue, final SkipListKey transactionalSet) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return transactionalSet.remove(operationValue);
            }
        };
    }

    private static Callable<Boolean> getContainsOperation(SkipListKey.OperationType operationType, final int operationValue, final SkipListKey transactionalSet) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return transactionalSet.contains(operationValue);
            }
        };
    }

    private static Callable<Boolean> getNoopOperation() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return true;
            }
        };
    }

    public static Callable<Boolean> getCallableOperation(SkipListKey.OperationType operationType, final int operationValue, final SkipListKey transactionalSet) {
        Callable<Boolean> operation;

        if (operationType.equals(SkipListKey.OperationType.ADD)) {
            operation = getAddOperation(operationType, operationValue, transactionalSet);
        }
        else if (operationType.equals(SkipListKey.OperationType.REMOVE)) {
            operation = getRemoveOperation(operationType, operationValue, transactionalSet);
        }
        else if (operationType.equals(SkipListKey.OperationType.CONTAINS)) {
            operation = getContainsOperation(operationType, operationValue, transactionalSet);
        }
        else {
            operation = getNoopOperation();
        }

        return operation;
    }

    public static Callable<Boolean> getCallableInverse(SkipListKey.OperationType operationType, final int operationValue, final SkipListKey transactionalSet) {
        Callable<Boolean> operation;

        if (operationType.equals(SkipListKey.OperationType.ADD)) {
            operation = getRemoveOperation(operationType, operationValue, transactionalSet);
        }
        else if (operationType.equals(SkipListKey.OperationType.REMOVE)) {
            operation = getAddOperation(operationType, operationValue, transactionalSet);
        }
        else if (operationType.equals(SkipListKey.OperationType.CONTAINS)) {
            operation = getNoopOperation();
        }
        else {
            operation = getNoopOperation();
        }

        return operation;
    }


}

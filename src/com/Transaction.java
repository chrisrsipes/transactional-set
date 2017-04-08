/**
 * author: aar
 * author: crs
 * description: provides implementation for abstract computing transaction.
 *              has status, can go from active to committed or aborted,
 *              with consequences for each.  provides utility classes
 *              to generate required runnables for operations and inverses
 *              on the data structure.
 *
 * Edit History:
 * - Created by aar on 4/5/17.
 * - Updated by crs on 4/6/17.
 * - Updated by crs on 4/7/17.
 *
 * Acknowledgements:
 * Largely derived from Art of Multiprocessor Programming page 445,
 * see Transaction implementation for Transactional Threads.
 *
 * Updates:
 * - added ThreadLocal lockSet, as per transactional boosting algorithm
 * - added AtomicReference to indicate whether or not the inverse operation should be invoked
 *   note that this is a deviation from the transactional boosting algorithm where they were
 *   somehow setting an onAbort handler that would be executed on abort.  the requirement for
 *   calling methods to be static prevented us from setting an instance variable or a handler at runtime
 * - new constructors for these variables
 * - extension on abort to conditionally perform inverse operation to correctly mock onAbort handler
 * - static util methods to generate add, remove, contains, and noop callables for private use
 * - static util methods to generate operations and inverses for calling application
 *
 */

package com;

import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

public class Transaction {
    public enum Status {ABORTED, ACTIVE, COMMITTED};

    // @TODO: identify why having a default COMMITTED transaction is necessary according to the book
    public static Transaction COMMITTED = new Transaction(Status.COMMITTED);

    // these drive aborting / committing and correctly invoking the inverse operation
    private final AtomicReference<Status> status;
    private final AtomicReference<Boolean> useInverseOperation;
    private final Callable<Boolean> operation;
    private final Callable<Boolean> inverse;

    // declare and intialize ThreadLocal variables to be statically available
    // to the currently executing thread.  see the getters / setters for access to internal vars
    static ThreadLocal<Transaction> localTransaction = new ThreadLocal<Transaction>(){
        protected Transaction initialValue(){
            return new Transaction(Status.COMMITTED);
        }
    };

    static ThreadLocal<HashSet<Lock>> localLockSet = new ThreadLocal<HashSet<Lock>>(){
        protected HashSet<Lock> initialValue(){
            return new HashSet<>();
        }
    };


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

    public Boolean useInverse() {
        return useInverseOperation.get();
    }

    public boolean setUseInverse() {
        return useInverseOperation.compareAndSet(false, true);
    }

    public boolean commit() {
        return status.compareAndSet(Status.ACTIVE, Status.COMMITTED);
    }

    // modified abort to correctly perform inverse operations on completed (but not committed) operations.
    // because we're working with a set, our operations are naturally 1 operation long, so only
    // 0 or 1 operations need the inversees invoked (indicated by the useInverse flag we're maintaining)
    public boolean abort() {
        if (status.compareAndSet(Status.ACTIVE, Status.ABORTED)) {
            // if we aborted, may need to call inverse operation
            if (Transaction.getLocal().useInverse()) {
                try {
                    this.inverse.call();
                } catch (Exception e) {
                    // don't need to take action here.  inverse operation will retry while aborted,
                    // if the list was affected (which it must be if useInverse is set), and we still have the
                    // abstract lock for the affected element, we will be able to successfully run the inverse
                    String logMsg = String.format("Exception calling inverse in abort: %s", e.getStackTrace().toString());
                    CustomLogger.log(CustomLogger.Category.EXCEPTION, logMsg);
                }
            }

            // successfully aborted
            return true;
        }
        else {
            // was aborted before, repeated work would leave set in invalid state
            return false;
        }
    }

    public static Transaction getLocal() {
        return localTransaction.get();
    }

    public static void setLocal(Transaction transaction) {
        localTransaction.set(transaction);
    }

    public static HashSet<Lock> getLockSet() {
        return localLockSet.get();
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

    // generates operation based on operationType, for operationValue into transactionalSet.
    // the input parameters must be taken as final, as they are
    // used in the generated Callable (all variables used locally in callable must be final).
    public static Callable<Boolean> getCallableOperation(SkipListKey.OperationType operationType, final int operationValue, final SkipListKey transactionalSet) {
        Callable<Boolean> operation;

        switch (operationType) {
            case ADD:
                operation = getAddOperation(operationType, operationValue, transactionalSet);
                break;
            case REMOVE:
                operation = getRemoveOperation(operationType, operationValue, transactionalSet);
                break;
            case CONTAINS:
                operation = getContainsOperation(operationType, operationValue, transactionalSet);
                break;
            default:
                operation = getNoopOperation();
                break;
        }

        return operation;
    }

    // generates inverse operation based on operationType, for operationValue into transactionalSet.
    // the input parameters must be taken as final, as they are
    // used in the generated Callable (all variables used locally in callable must be final).
    public static Callable<Boolean> getCallableInverse(SkipListKey.OperationType operationType, final int operationValue, final SkipListKey transactionalSet) {
        Callable<Boolean> operation;

        switch (operationType) {
            case ADD:
                operation = getRemoveOperation(operationType, operationValue, transactionalSet);
                break;
            case REMOVE:
                operation = getAddOperation(operationType, operationValue, transactionalSet);
                break;
            case CONTAINS:
                operation = getNoopOperation();
                break;
            default:
                operation = getNoopOperation();
                break;
        }

        return operation;
    }


}

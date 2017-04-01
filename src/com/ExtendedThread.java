package com;


import org.omg.CORBA.SystemException;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExtendedThread extends Thread implements Runnable {

    private static final Logger logger = Logger.getLogger( ExtendedThread.class.getName() );
    private ArrayList<Lock> lockSet;
    private OperationType operationType;
    private Integer operationValue;
    private SkipListKey transactionalSet;

    ExtendedThread(OperationType operationType, Integer value, SkipListKey transactionalSet) {

        // initialize lockSet
        lockSet = new ArrayList<>();
        this.operationType = operationType;
        this.operationValue = value;
        this.transactionalSet = transactionalSet;
    }

    @Override
    public void run() {

        switch (operationType) {
            case ADD:
                transactionalSet.add(operationValue);
                break;
        }

        // free locks
        freeLocks();

    }

    public void abort() {

    }

    public ArrayList<Lock> getLockSet() {
        return lockSet;
    }

    public static Logger getLogger() {
        return logger;
    }

    public void setLockSet(ArrayList<Lock> lockSet) {
        this.lockSet = lockSet;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Integer getOperationValue() {
        return operationValue;
    }

    public void setOperationValue(Integer operationValue) {
        this.operationValue = operationValue;
    }

    private void freeLocks() {
        if (lockSet != null) {
            for (Lock l : lockSet) {
                l.unlock();
            }
        }
    }

}

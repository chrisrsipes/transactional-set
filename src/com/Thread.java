package com;


import org.omg.CORBA.SystemException;

import javax.transaction.*;
import javax.transaction.xa.XAResource;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

public class Thread implements javax.transaction.Transaction, Runnable {

    ArrayList<Lock> lockSet;
    OperationType operationType;
    Integer operationValue;

    Thread(OperationType operationType, Integer value) {
        // initialize lockSet
        lockSet = new ArrayList<>();
        this.operationType = operationType;
        this.operationValue = value;

        run();
    }

    @Override
    public void run() {
        boolean success = false;
        boolean error = false;

        // do some shit

        if (error) {
            try {
                this.rollback();
            } catch (SystemException e) {
                e.printStackTrace();
            }
        }
        if (success) {
            try {
                this.commit();
            } catch (HeuristicMixedException e) {
                e.printStackTrace();
            } catch (SystemException e) {
                e.printStackTrace();
            } catch (HeuristicRollbackException e) {
                e.printStackTrace();
            } catch (RollbackException e) {
                e.printStackTrace();
            }
        }

        // free locks
        freeLocks();

    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {

    }

    @Override
    public boolean delistResource(XAResource xaResource, int i) throws IllegalStateException, SystemException {
        return false;
    }

    @Override
    public boolean enlistResource(XAResource xaResource) throws RollbackException, IllegalStateException, SystemException {
        return false;
    }

    @Override
    public int getStatus() throws SystemException {
        if (this.operationType == null) {
            return Status.STATUS_NO_TRANSACTION;
        }
        else {
            return Status.STATUS_ACTIVE;
        }
    }

    @Override
    public void registerSynchronization(Synchronization synchronization) throws RollbackException, IllegalStateException, SystemException {

    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {

        switch (this.operationType) {
            case ADD:
                break;

        }

    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {

    }

    private void freeLocks() {
        if (lockSet != null) {
            for (Lock l : lockSet) {
                l.unlock();
            }
        }
    }

    public ArrayList<Lock> getLockSet() {
        return lockSet;
    }
}

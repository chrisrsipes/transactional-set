package com;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.Random;
import java.util.concurrent.*;

public class Main {

    // private static final int operationCount = 500000;
    // private static final int [] threadCounts = {1,2,4,8};
    // private static final double []  addProportions = {0.25, 0.50, 0.75};

    private static final int operationCount = 25000;
    private static final int [] threadCounts = {2, 4, 8};
    private static final double []  addProportions = {0.25, 0.50, 0.75};

    private static final long timeoutDuration = 10000;
    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 10;

    public static void main(String[] args) {
        Object[] logArgs;
        long startTime, endTime;

        CustomLogger.log(CustomLogger.Category.EVENT, "Beginning test.");

        for (int threadCount : threadCounts) {
            for (double addProportion : addProportions) {

                final SkipListKey transactionalSet = new SkipListKey();

                logArgs = new Object[] {threadCount, addProportion, operationCount};
                CustomLogger.log(
                        CustomLogger.Category.EVENT,
                        String.format("Preparing for simulation with threadCount: %d, addProportion: %f, opCount: %d", logArgs)
                );

                // prepare the operations / inverses to schedule
                Callable<Boolean>[] operations = new Callable[operationCount];
                Callable<Boolean>[] inverses = new Callable[operationCount];
                for (int i = 0; i < operationCount; i++) {
                    final SkipListKey.OperationType operationType = getOperationType(addProportion);
                    final Integer operationValue = getOperationValue();

                    Callable<Boolean> operation = Transaction.getCallableOperation(operationType, operationValue, transactionalSet);
                    Callable<Boolean> inverse   = Transaction.getCallableInverse(operationType, operationValue, transactionalSet);

                    operations[i] = operation;
                    inverses[i] = inverse;

                    logArgs = new Object[] {operationType, operationValue};
                    CustomLogger.log(
                            CustomLogger.Category.DEBUG_FINE,
                            String.format("New operation created: %s(%d)", logArgs)
                    );
                }

                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);

                logArgs = new Object[] {threadCount, addProportion, operationCount};
                CustomLogger.log(CustomLogger.Category.EVENT, "Beginning simulation.");

                startTime = System.nanoTime();

                for (int i = 0; i < operationCount; i++) {
                    threadPoolExecutor.execute(new TThread(operations[i], inverses[i]));
                }


                // this closes down any more tasks being scheduled for the threads to pick up
                threadPoolExecutor.shutdown();

                try {
                    CustomLogger.log(CustomLogger.Category.EVENT, "All operations placed in pool to be scheduled on threads.  Waiting for completion.");

                    // will pause execution of this thread untill all threads in the ThreadPoolExecutor are finished
                    threadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                    // now finished, so end the timer.  durationMS is in miliseconds
                    endTime = System.nanoTime();
                    long durationMS = (endTime - startTime) / 1000000;

                    logArgs = new Object[] {durationMS};
                    CustomLogger.log(
                            CustomLogger.Category.EVENT,
                            String.format("All operations completed.  Total Time: %d", logArgs)
                    );

                    logArgs = new Object[] {threadCount, addProportion, operationCount, durationMS};
                    CustomLogger.log(
                            CustomLogger.Category.METRIC,
                            String.format("threadCount: %d, addProportion: %f, opCount: %d, time: %d", logArgs)
                    );


                } catch (InterruptedException e) {
                    CustomLogger.log(CustomLogger.Category.EXCEPTION, "Timed out waiting for operations in pool to finish.  You may need to increase the max timeout.");
                }

            }
        }

        CustomLogger.log(CustomLogger.Category.EVENT, "Finished simulation.");
    }

    private static SkipListKey.OperationType getOperationType(double addProportion) {
        double rand = Math.random();

        if (rand < addProportion) {
            return SkipListKey.OperationType.ADD;
        }
        else {
            return SkipListKey.OperationType.REMOVE;
        }
    }

    private static int getOperationValue() {
        return ThreadLocalRandom.current().nextInt(MIN_VALUE, MAX_VALUE + 1);
    }
}

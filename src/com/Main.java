/**
 * name: Main
 * author: crs
 * description: entry target for application.  gives example for how to use transactionally-boosted set,
 *              as some setup is required with generating callables for the operations.
 *              main purpose is to vary over the number of threads operations will be executed on,
 *              and the proportions of operations (add / remove / contains) that will be generated.
 *              log metrics for performance benchmarking.
 *
 * Edit History:
 * - Renovated by crs on 4/7/17.
 *
 * Updates:
 * - changes to how callables are being generated
 * - operations and inverses are now generated in advance of running the simulation for the configuration
 * - refactored method of logging
 *
 */


package com;


import java.util.concurrent.*;

// @TODO: remove references to old logger
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

        // for testing purposes, we're varying the number of threads and the proportion of operations
        // for each of these configurations, we need to run operationCount operations
        for (int threadCount : threadCounts) {
            for (double addProportion : addProportions) {

                // declare a new skiplist to start over for each configuration
                SkipListKey transactionalSet = new SkipListKey();

                logArgs = new Object[] {threadCount, addProportion, operationCount};
                CustomLogger.log(
                        CustomLogger.Category.EVENT,
                        String.format("Preparing for simulation with threadCount: %d, addProportion: %f, opCount: %d", logArgs)
                );

                // prepare the operations / inverses to schedule before kicking off threads
                Callable<Boolean>[] operations = new Callable[operationCount];
                Callable<Boolean>[] inverses = new Callable[operationCount];
                for (int i = 0; i < operationCount; i++) {
                    // randomly generate operations and values
                    SkipListKey.OperationType operationType = getOperationType(addProportion);
                    Integer operationValue = getOperationValue();

                    // generate the operations / inverses using utility classes in Transaction
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

                // kick off fixed thread pool, it will manage scheduling threadCount threads as they finish executing
                // we don't need to interact with its job queue
                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);

                CustomLogger.log(CustomLogger.Category.EVENT, "Beginning simulation.");

                startTime = System.nanoTime();

                // kick off all the operations.  this does not wait for the operation to finish, it simply initializes
                // the thread itll run on and adds it to the thread pool
                for (int i = 0; i < operationCount; i++) {
                    threadPoolExecutor.execute(new TThread(operations[i], inverses[i]));
                }

                // this closes down any more tasks being scheduled for the threads to pick up
                threadPoolExecutor.shutdown();

                try {
                    CustomLogger.log(CustomLogger.Category.EVENT, "All operations placed in pool to be scheduled on threads.  Waiting for completion.");

                    // will pause execution of this thread untill all threads in the ThreadPoolExecutor are finished
                    threadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                    // now finished, so end the timer.  durationMS is in ms
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

    // @TODO: change from addProporition to 3 arrays of proportions for respective operations
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

package JavaConcurrentSkipList;

import com.CustomLogger;
import com.SkipListKey;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by aar on 4/10/17.
 */
public class CoarseGrained {

    // private static final int operationCount = 500000;
    // private static final int [] threadCounts = {1,2,4,8};
    // private static final double []  addProportions = {0.25, 0.50, 0.75};

    private static final int operationCount = 25000;
    private static final int [] threadCounts = {2, 4, 8};
    private static final double []  addProportions = {0.25, 0.50, 0.75};
    private static ReentrantLock lock = new ReentrantLock();
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
                ConcurrentSkipListSet<Integer> skiplist = new ConcurrentSkipListSet<Integer>();

                logArgs = new Object[] {threadCount, addProportion, operationCount};
                CustomLogger.log(
                        CustomLogger.Category.EVENT,
                        String.format("Preparing for simulation with threadCount: %d, addProportion: %f, opCount: %d", logArgs)
                );

                // prepare the operations / inverses to schedule before kicking off threads
                Runnable[] operations = new Runnable[operationCount];
                for (int i = 0; i < operationCount; i++) {
                    // randomly generate operations and values
                    SkipListKey.OperationType operationType = getOperationType(addProportion);
                    Integer operationValue = getOperationValue();

                    // generate the operations using utility classes in Transaction
                    Runnable operation = getMyRunanble(operationType, operationValue, skiplist);

                    operations[i] = operation;

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
                    threadPoolExecutor.execute(operations[i]);
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

    public static Runnable getMyRunanble(SkipListKey.OperationType operationType, final int operationValue, final ConcurrentSkipListSet<Integer>  skiplist) {
        Runnable operation;

        switch (operationType) {
            case ADD:
                operation = AddOperation(operationType, operationValue, skiplist);
                break;
            case REMOVE:
                operation = RemoveOperation(operationType, operationValue, skiplist);
                break;
            //case CONTAINS:
            //    operation = getNoopOperation();
            //    break;
            default:
                operation = getNoopOperation();
                break;
        }

        return operation;
    }

    private static Runnable AddOperation(SkipListKey.OperationType operationType, final int operationValue, final ConcurrentSkipListSet<Integer> skiplist) {
        return new Runnable() {
            public void run() {
                try {
                    lock.lock();
                    skiplist.add(operationValue);
                    lock.unlock();
                }catch (Exception ex){

                }
            }
        };
    }

    private static Runnable RemoveOperation(SkipListKey.OperationType operationType, final int operationValue, final ConcurrentSkipListSet<Integer> skiplist) {
        return new Runnable() {
            public void run() {
                try{
                    lock.lock();
                    skiplist.remove(operationValue);
                    lock.unlock();
                }catch( Exception ex) {

                }
            }
        };
    }

    private static Runnable getNoopOperation(){
        return new Runnable() {
            public void run(){
                //Nothing
            }
        };
    }

    private static int getOperationValue() {
        return ThreadLocalRandom.current().nextInt(MIN_VALUE, MAX_VALUE + 1);
    }
}

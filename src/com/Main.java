package com;

import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger( Main.class.getName() );

    private static final int operationCount = 100;
    private static final int [] threadCounts = {1,2,4,8};
    private static final double []  addProportions = {0.25, 0.50, 0.75};
    private static final long timeoutDuration = 10000;
    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 100000;

    public static void main(String[] args) {
        Random r = new Random();

        long startTime, endTime;

        logger.log(Level.INFO, "Starting simulation.");

        System.out.println("tCount\t\t% add\t\ttime (ms)");


        for (int threadCount : threadCounts) {
            for (double addProportion : addProportions) {

                final SkipListKey transactionalSet = new SkipListKey();

                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);

                startTime = System.nanoTime();

                for (int i = 0; i < operationCount; i++) {
                    final SkipListKey.OperationType operationType = getOperationType(addProportion);
                    final Integer operationValue = getOperationValue();

                    Callable<Boolean> callable = null;

                    if (operationType.equals(SkipListKey.OperationType.ADD)) {
                        callable = new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return transactionalSet.add(operationValue);
                            }
                        };
                    }
                    else if (operationType.equals(SkipListKey.OperationType.REMOVE)) {
                        callable = new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return transactionalSet.remove(operationValue);
                            }
                        };
                    }


                    threadPoolExecutor.execute(new TThread(callable));
                }

                // this closes down any more tasks being scheduled for the threads to pick up
                threadPoolExecutor.shutdown();

                try {
                    // will pause execution of this thread untill all threads in the ThreadPoolExecutor are finished
                    threadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                    // now finished, so end the timer.  durationMS is in miliseconds
                    endTime = System.nanoTime();
                    long durationMS = (endTime - startTime) / 1000000;

                    // write the output
                    System.out.println(threadCount + "\t\t" + addProportion + "\t\t" + durationMS);

                } catch (InterruptedException e) {
                    // it shouldn't interrupt my threads
                }

            }
        }

        logger.log(Level.INFO, "Finished simulation.");
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


package com;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {

    private static final int operationCount = 500000;
    private static final int [] threadCounts = {1,2,4,8};
    private static final double []  addProportions = {0.25, 0.50, 0.75};
    private static final long timeoutDuration = 10000;

    public static void main(String[] args) {
        SkipListKey transactionalSet = new SkipListKey();
        Random r = new Random();

        long startTime, endTime;

        System.out.println("tCount\t\t% add\t\ttime (ms)");

        for (int threadCount : threadCounts) {

            for (double addProportion : addProportions) {

                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);

                startTime = System.nanoTime();

                for (int i = 0; i < operationCount; i++) {
                    OperationType operationType = getOperationType(addProportion);
                    threadPoolExecutor.execute(new Thread(operationType, getOperationValue(r)));
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
    }

    private static OperationType getOperationType(double addProportion) {
        double rand = Math.random();

        if (rand < addProportion) {
            return OperationType.ADD;
        }
        else {
            return OperationType.REMOVE;
        }
    }

    private static int getOperationValue(Random r) {
        return r.nextInt();
    }
}

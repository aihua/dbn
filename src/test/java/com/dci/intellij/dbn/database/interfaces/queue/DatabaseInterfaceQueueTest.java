package com.dci.intellij.dbn.database.interfaces.queue;

import com.dci.intellij.dbn.common.Priority;
import com.dci.intellij.dbn.common.thread.Threads;
import com.dci.intellij.dbn.common.util.Exceptions;
import com.dci.intellij.dbn.common.util.TimeUtil;
import com.dci.intellij.dbn.common.util.Unsafe;
import lombok.AllArgsConstructor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public class DatabaseInterfaceQueueTest {
    private static final Random random = new Random();
    private InterfaceQueue queue;

    @Before
    public void setUp() throws Exception {
        queue = new InterfaceQueue(null, task -> Threads.backgroundExecutor().submit(() -> queue.executeTask(task)));
    }

    @Test
    public void scheduleAndWait() {
        invoke(100, task -> {
            try {
                InterfaceTaskDefinition taskDefinition = InterfaceTaskDefinition.create(task.priority, "test", "test", null);
                queue.scheduleAndWait(taskDefinition, () -> {
                    System.out.println("Executing " + task);
                    Unsafe.silent(() -> Thread.sleep(task.time));
                    System.out.println("Done executing "  + task);
                });

                System.out.println("Finished executing " + task);
            } catch (SQLException e) {
                throw Exceptions.toRuntimeException(e);
            }
        });
    }

    @Test
    public void scheduleAndForget() {
        invoke(100, task -> {
            try {
                InterfaceTaskDefinition taskDefinition = InterfaceTaskDefinition.create(task.priority, "test", "test", null);
                queue.scheduleAndForget(taskDefinition, () -> {
                    System.out.println("Executing " + task);
                    Unsafe.silent(() -> Thread.sleep(task.time));
                    System.out.println("Done executing "  + task);
                });
            } catch (SQLException e) {
                throw Exceptions.toRuntimeException(e);
            }
        });
    }



    private void invoke(int times, Consumer<TestTask> runnable) {
        long start = System.currentTimeMillis();
        AtomicLong totalTime = new AtomicLong();

        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < times; i++) {
            int index = i;
            System.out.println("Queueing " + index);
            executorService.submit(() -> {
                TestTask task = new TestTask(index);
                totalTime.addAndGet(task.time);
                runnable.accept(task);
            });
        }

        Thread invoker = Thread.currentThread();
        queue.counters().running().addListener(value -> {
            if (value == 0 && queue.size() == 0) {
                LockSupport.unpark(invoker);
                System.out.println("UNPARKED");
            }
        });


        long sleepStart = System.currentTimeMillis();
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(120));
        System.out.println("DONE " + TimeUnit.MILLISECONDS.toSeconds(TimeUtil.millisSince(sleepStart)));

        executorService.shutdown();
        Assert.assertEquals(times, queue.counters().finished().get());
        long elapsedTime = TimeUtil.millisSince(start);
        long activeTime = totalTime.get() / queue.maxActiveTasks();
        long difference = Math.abs(activeTime - elapsedTime);

        System.out.println("Execution time " + activeTime);
        System.out.println("Elapsed time " + elapsedTime);
        System.out.println("Difference " + difference);

        Assert.assertTrue(difference < 500);
    }

    @AllArgsConstructor
    private static class TestTask {
        private final int index;
        private final long time = random.nextInt(500);
        private final Priority priority = randomEnum(Priority.class);

        @Override
        public String toString() {
            return "Task " + index + " (" + priority + " - " + time + "ms)";
        }
    }


    public static <T extends Enum<?>> T randomEnum(Class<T> clazz){
        T[] enumConstants = clazz.getEnumConstants();
        int index = random.nextInt(enumConstants.length);
        return enumConstants[index];
    }
}
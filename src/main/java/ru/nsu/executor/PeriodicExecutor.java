package ru.nsu.executor;

import lombok.Setter;
import ru.nsu.executor.task.ExecutionDelayType;
import ru.nsu.executor.task.PeriodicTask;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class PeriodicExecutor {

    private static final String CHECKER_THREAD_NAME = "task-queue-checker-thread";

    private final PriorityQueue<PeriodicTask> queue;
    private final ReentrantLock lock;
    private final Condition cv;

    private final Thread checkerThread;

    private final List<Consumer<PeriodicTask>> preExecuteExtensions;
    private final List<Consumer<PeriodicTask>> postExecuteExtensions;

    @Setter
    private Executor taskExecutor;

    public PeriodicExecutor() {
        queue = new PriorityQueue<>();
        taskExecutor = Executors.newCachedThreadPool();

        lock = new ReentrantLock();
        cv = lock.newCondition();

        preExecuteExtensions = new ArrayList<>();
        postExecuteExtensions = new ArrayList<>();

        addInsertIntoQueueExtension(preExecuteExtensions, ExecutionDelayType.PERIOD);
        addInsertIntoQueueExtension(postExecuteExtensions, ExecutionDelayType.FIXED);

        checkerThread = new Thread(this::checkTaskQueue);
        checkerThread.setName(CHECKER_THREAD_NAME);
        checkerThread.setDaemon(false); //todo remove
        checkerThread.start();
    }

    public void addTask(String taskId, Runnable runnable, Duration delay, ExecutionDelayType delayType, boolean shouldExecuteInstantly) {
        PeriodicTask task;
        if (shouldExecuteInstantly) {
            task = new PeriodicTask(taskId, runnable, delay, delayType, Instant.now());
        } else {
            task = new PeriodicTask(taskId, runnable, delay, delayType, Instant.now().plus(delay));
        }

        addTaskInternal(task);
    }

    public void removeTask(String taskId) {
        try {
            lock.lock();
            queue.removeIf(task -> task.getId().equals(taskId));
        } finally {
            lock.unlock();
        }
    }

    public void addPreExecutedExtension(Consumer<PeriodicTask> extension) {
        preExecuteExtensions.add(extension);
    }

    public void addPostExecutedExtension(Consumer<PeriodicTask> extension) {
        postExecuteExtensions.add(extension);
    }

    private void addTaskInternal(PeriodicTask task) {
        try {
            lock.lock();
            queue.add(task);
            cv.signal();
        } finally {
            lock.unlock();
        }
    }

    private void checkTaskQueue() {
        try {
            while (true) {
                lock.lock();

                if (queue.isEmpty()) {
                    cv.await();
                }

                while (isNextTaskNotReady()) {
                    cv.awaitUntil(Date.from(queue.peek().getExecuteAt()));
                }

                PeriodicTask task = queue.poll();
                lock.unlock();

                if (task == null) {
                    continue;
                }

                preExecute(task);
                execute(task);
                postExecute(task);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isNextTaskNotReady() {
        if (queue.peek() == null) {
            return false;
        }

        PeriodicTask task = queue.peek();
        return Instant.now().isBefore(task.getExecuteAt());
    }

    private void execute(PeriodicTask task) {
        taskExecutor.execute(task.getTask());
    }

    private void preExecute(PeriodicTask task) {
        preExecuteExtensions.forEach(extension -> extension.accept(task));
    }

    private void postExecute(PeriodicTask task) {
        postExecuteExtensions.forEach(extension -> extension.accept(task));
    }

    private void addInsertIntoQueueExtension(List<Consumer<PeriodicTask>> extensions, ExecutionDelayType type) {
        extensions.add(task -> {
            if (task.getDelayType() == type) {
                PeriodicTask newTask = task.clone();
                newTask.setExecuteAt(Instant.now().plus(newTask.getDelay()));

                addTaskInternal(newTask);
            }
        });
    }
}

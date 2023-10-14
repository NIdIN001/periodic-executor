package ru.nsu;

import ru.nsu.executor.PeriodicExecutor;
import ru.nsu.executor.task.ExecutionDelayType;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class TestApplication {

    public static void main(String[] args) {
        PeriodicExecutor executor = new PeriodicExecutor();

        executor.addTask(
                "1",
                () -> System.out.printf("%s: task #1 runs every 5 seconds%n", Instant.now().toString()),
                Duration.of(5, ChronoUnit.SECONDS),
                ExecutionDelayType.PERIOD,
                false);

        executor.addTask(
                "2",
                () -> System.out.printf("%s: task #2 runs every 1 seconds%n", Instant.now().toString()),
                Duration.of(1, ChronoUnit.SECONDS),
                ExecutionDelayType.FIXED,
                true);
    }
}

package ru.nsu.executor.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class PeriodicTask implements Comparable<PeriodicTask> {

    private final String id;
    private final Runnable task;

    private final Duration delay;
    private final ExecutionDelayType delayType;

    @Setter
    private Instant executeAt;

    public PeriodicTask clone() {
        return new PeriodicTask(id, task, delay, delayType, executeAt);
    }

    @Override
    public int compareTo(PeriodicTask o) {
        return executeAt.compareTo(o.executeAt);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        return ((PeriodicTask) other).id.equals(this.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format(
                "PeriodicTask[Id=%s, Instant=%s,delay=%s,delayType=%s]",
                id,
                executeAt,
                delay,
                delayType
        );
    }
}

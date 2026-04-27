package com.toanlv.flashsale.common.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Distributed leader election lock backed by Redisson (Redis).
 *
 * Used by scheduled workers (OutboxDispatchWorker, ReconciliationWorker)
 * to ensure only one instance processes a batch at a time in a
 * multi-instance deployment.
 *
 * Redisson RLock provides:
 *   - Automatic lease renewal (watchdog) while lock is held
 *   - Safe release — only the thread that acquired the lock can release it
 *   - Java 21 virtual thread compatible (Redisson 3.27+)
 */
@Component
public class LeaderLock {

    private static final Logger log =
            LoggerFactory.getLogger(LeaderLock.class);

    private static final String LOCK_PREFIX = "leader:lock:";

    private final RedissonClient redisson;

    public LeaderLock(RedissonClient redisson) {
        this.redisson = redisson;
    }

    /**
     * Try to acquire the leader lock without waiting.
     *
     * @param name         lock name, e.g. "outbox-dispatcher"
     * @param leaseSeconds how long to hold the lock before auto-expiry
     *                     (Redisson watchdog extends this automatically
     *                     while the thread is alive)
     * @return true if lock acquired, false if another instance holds it
     */
    public boolean tryAcquire(String name, long leaseSeconds) {
        var lock = getLock(name);
        try {
            return lock.tryLock(0, leaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while trying to acquire lock [{}]", name);
            return false;
        }
    }

    /**
     * Release the lock.
     * No-op if the current thread does not hold the lock —
     * safe to call unconditionally in a finally block.
     *
     * @param name lock name used in tryAcquire
     */
    public void release(String name) {
        var lock = getLock(name);
        if (lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
            } catch (Exception e) {
                log.warn("Failed to release lock [{}]: {}", name, e.getMessage());
            }
        }
    }

    /**
     * Execute a task under the leader lock.
     * Acquires the lock, runs the task, then releases in finally.
     * Returns true if the task was executed, false if lock not acquired.
     *
     * @param name         lock name
     * @param leaseSeconds lease duration
     * @param task         runnable to execute under lock
     * @return true if task was executed
     */
    public boolean runIfLeader(String name, long leaseSeconds, Runnable task) {
        if (!tryAcquire(name, leaseSeconds)) {
            log.trace("Not the leader for [{}], skipping", name);
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            release(name);
        }
    }

    /**
     * Check whether the current thread holds the lock.
     * Useful for assertions in tests.
     */
    public boolean isHeldByCurrentThread(String name) {
        return getLock(name).isHeldByCurrentThread();
    }

    private RLock getLock(String name) {
        return redisson.getLock(LOCK_PREFIX + name);
    }
}

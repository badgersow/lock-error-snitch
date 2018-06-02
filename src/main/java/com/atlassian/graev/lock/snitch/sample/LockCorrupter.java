package com.atlassian.graev.lock.snitch.sample;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Xss256K should do the job :)
 */
public class LockCorrupter {

    private final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        new LockCorrupter().spawnMonster();
    }

    private void spawnMonster() throws InterruptedException {
        final Thread lockMonster = new Thread(this::corruptLockAndSuppressError, "lock-monster");
        lockMonster.start();
        lockMonster.join();

        if (lock.tryLock()) {
            System.out.println("Lock wasn't corrupted :(");
        } else {
            System.out.println("Lock monster did a good job");
        }
    }

    private void corruptLockAndSuppressError() {
        System.out.println("Start corrupting lock :)");
        try {
            messWithLock();
        } catch (Throwable t) {
            // 😈
        }
    }

    @SuppressWarnings("InfiniteRecursion")
    private void messWithLock() {
        lock.lock();
        lock.unlock();
        messWithLock();
    }

}

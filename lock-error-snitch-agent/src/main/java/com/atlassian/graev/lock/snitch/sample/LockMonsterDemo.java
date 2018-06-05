package com.atlassian.graev.lock.snitch.sample;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demo class that corrupts the victim by StackOverflowing and swallows the error.
 * Running this with agent should create trace file to show the exact site of StackOverflowError.
 */
@SuppressWarnings("WeakerAccess")
public class LockMonsterDemo {

    private final Lock victim = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        new LockMonsterDemo().spawnMonster();
    }

    private void spawnMonster() throws InterruptedException {
        final Thread lockMonster = new Thread(
                null,
                this::corruptLockAndSuppressError,
                "lock-monster",
                256 * 1024); // likely value to get corruption
        lockMonster.start();
        lockMonster.join();

        if (victim.tryLock()) {
            System.out.println("Lock wasn't corrupted");
        } else {
            System.out.println("Lock was corrupted by StackOverflowError");
        }

        // As we write traces files asynchronously, allow some time for this
        final int timeoutSec = 3;
        System.out.println("Waiting " + timeoutSec + " sec to allow other threads to write traces");
        Thread.sleep(TimeUnit.SECONDS.toMillis(timeoutSec));
    }

    private void corruptLockAndSuppressError() {
        System.out.println("Start corrupting the lock...");
        try {
            messWithLock();
        } catch (Throwable t) {
            // 😈
        }
    }

    @SuppressWarnings("InfiniteRecursion")
    private void messWithLock() {
        victim.lock();
        victim.unlock();
        messWithLock();
    }

}

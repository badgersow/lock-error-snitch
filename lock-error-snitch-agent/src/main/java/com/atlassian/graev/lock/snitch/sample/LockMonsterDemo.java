package com.atlassian.graev.lock.snitch.sample;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demo class to fail without agent and to path with one.
 * It is very hard to emulate the same thing with unit tests because it is highly platform specific.
 */
@SuppressWarnings("WeakerAccess")
public class LockMonsterDemo {

    private final Lock lock = new ReentrantLock();

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

        if (lock.tryLock()) {
            System.out.println("Lock wasn't corrupted");
        } else {
            System.out.println("Lock was corrupted by StackOverflowError");
        }
    }

    private void corruptLockAndSuppressError() {
        System.out.println("Start corrupting the lock...");
        try {
            messWithLock();
        } catch (Throwable t) {
            t.printStackTrace();
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

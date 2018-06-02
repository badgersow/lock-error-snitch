package com.atlassian.graev.lock.snitch.sample;

import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("WeakerAccess")
public class LockCorrupter {

    private final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        new LockCorrupter().spawnMonster();
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
            System.out.println("Lock monster did a good job");
        }
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
        lock.lock();
        lock.unlock();
        messWithLock();
    }

}

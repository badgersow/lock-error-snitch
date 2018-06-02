package com.atlassian.graev.lock.snitch.agent;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(Parameterized.class)
public class CorruptionTest {

    private static final boolean expectCorrectness = Boolean.getBoolean("lock.snitch.test.expect.correctness");

    private static final int numberOfTrials = 100;

    private static final ExecutorService executor = new ThreadPoolExecutor(1, 1,
            Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            r -> new Thread(null, r, "lock-corrupter-in-UT", 64 * 1024)); // likely value to cause SOE

    @Parameterized.Parameter
    public Class<? extends Lock> lockClass;

    @Parameterized.Parameter(1)
    public Supplier<Lock> supplier;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> createParameters() {
        return Arrays.asList(
                param(ReentrantLock.class, ReentrantLock::new),
                param(ReentrantReadWriteLock.ReadLock.class, () -> new ReentrantReadWriteLock().readLock()),
                param(ReentrantReadWriteLock.WriteLock.class, () -> new ReentrantReadWriteLock().writeLock())
        );
    }

    @Test
    public void testCorruptionOrCorrectness() {
        final int correctExperiments = runExperiments();

        if (expectCorrectness) {
            assertThat(correctExperiments, equalTo(numberOfTrials));
        }

        assertThat(correctExperiments, lessThan(numberOfTrials));
    }

    private int runExperiments() {
        return (int) IntStream.rangeClosed(1, numberOfTrials)
                .filter(i -> isSingleExperimentCorrect())
                .count();
    }

    private boolean isSingleExperimentCorrect() {
        try {
            final Lock lock = supplier.get();
            executor.submit(() -> messWithLock(lock)).get();
            return lock.tryLock();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void messWithLock(Lock lock) {
        try {
            doMessWithLock(lock);
        } catch (StackOverflowError error) {
            // 😈
        }
    }

    @SuppressWarnings("InfiniteRecursion")
    private void doMessWithLock(Lock lock) {
        lock.lock();
        try {
            doMessWithLock(lock);
        } finally {
            lock.unlock();
        }
    }

    @AfterClass
    public static void tearDown() {
        executor.shutdown();
    }

    private static <T extends Lock> Object[] param(Class<T> clazz, Supplier<T> supplier) {
        return new Object[]{clazz, supplier};
    }

}

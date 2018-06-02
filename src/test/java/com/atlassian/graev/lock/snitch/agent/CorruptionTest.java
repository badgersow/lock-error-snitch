package com.atlassian.graev.lock.snitch.agent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
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

    private static final int numberOfTrials = 10;

    @Parameterized.Parameter
    public Class<? extends Lock> lockClass;

    @Parameterized.Parameter(1)
    public Supplier<Lock> supplier;

    private Lock currentLock;

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
            currentLock = supplier.get();
            final Thread worker = new Thread(null, this::messWithLock,
                    "lock-corrupter-in-UT", 256 * 1024);
            worker.start();
            worker.join();
            return currentLock.tryLock();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void messWithLock() {
        try {
            doMessWithLock();
        } catch (StackOverflowError error) {
            error.printStackTrace();
            // 😈
        }
    }

    @SuppressWarnings("InfiniteRecursion")
    private void doMessWithLock() {
        currentLock.lock();
        currentLock.unlock();
        doMessWithLock();
    }

    private static <T extends Lock> Object[] param(Class<T> clazz, Supplier<T> supplier) {
        return new Object[]{clazz, supplier};
    }

}

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
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(Parameterized.class)
public class CorruptionTest {

    private static final boolean expectCorrectness = Boolean.getBoolean("lock.snitch.test.expect.correctness");

    private static final int numberOfTrials = 10;

    private static final double corruptPercentage = 0.9;

    private static final ExecutorService executor = new ThreadPoolExecutor(1, 1,
            Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            r -> new Thread(null, r, "lock-corrupter-in-UT", 64 * 1024)); // likely value to cause SOE

    @Parameterized.Parameter
    public Class<? extends Lock> lockClass;

    private Lock currentLock;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> createParameters() {
        return Arrays.asList(
                param(ReentrantLock.class),
                param(ReentrantReadWriteLock.ReadLock.class),
                param(ReentrantReadWriteLock.WriteLock.class)
        );
    }

    @Test
    public void testCorruptionOrCorrectness() {
        final int correctExperiments = runExperiments(numberOfTrials);

        if (expectCorrectness) {
            assertThat(correctExperiments, equalTo(numberOfTrials));
        }

        assertThat(correctExperiments, lessThan((int) Math.floor(numberOfTrials * (1 - corruptPercentage))));
    }

    public int runExperiments(int experiments) {
        return (int) IntStream.rangeClosed(1, experiments)
                .filter(i -> isSingleExperimentCorrect())
                .count();
    }

    public boolean isSingleExperimentCorrect() {
        try {
            currentLock = lockClass.newInstance();
            executor.submit(this::messWithLock).get();
            return currentLock.tryLock();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void messWithLock() {
        try {
            doMessWithLock();
        } catch (StackOverflowError error) {
            // 😈
        }
    }

    @SuppressWarnings("InfiniteRecursion")
    private void doMessWithLock() {
        currentLock.lock();
        currentLock.unlock();
        doMessWithLock();
    }

    @AfterClass
    public static void tearDown() {
        executor.shutdown();
    }

    private static Object[] param(Class<? extends Lock> clazz) {
        return new Object[]{clazz};
    }

}

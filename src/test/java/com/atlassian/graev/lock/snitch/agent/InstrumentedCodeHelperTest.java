package com.atlassian.graev.lock.snitch.agent;

import org.junit.Test;

import java.util.stream.IntStream;

public class InstrumentedCodeHelperTest {

    /**
     * This test doesn't prove anything, but it makes me sleep better at night.
     */
    @Test(expected = StackOverflowError.class)
    public void testJitDoesNotRemoveInfiniteRecursion() {
        final int iterations = 10_000_000;
        final int depth = 100;
        final int insaneDeepness = 1_000_000_000;

        IntStream.rangeClosed(1, iterations).forEach(i -> InstrumentedCodeHelper.doDummyRecursion(depth));
        InstrumentedCodeHelper.doDummyRecursion(insaneDeepness);

        // Tell JIT we need this counter. This will be done in production.
        System.out.println(InstrumentedCodeHelper.getSuccessfulRuns());
    }
}
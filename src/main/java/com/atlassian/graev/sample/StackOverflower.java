package com.atlassian.graev.sample;

public class StackOverflower {

    public static void main(String[] args) {
        try {
            new StackOverflower().stackOverflow();
        } catch (Throwable t) {
            System.out.println("Swallowed " + t.getClass().getSimpleName());
        }
    }

    // It is supposed to be this way
    @SuppressWarnings("InfiniteRecursion")
    private void stackOverflow() {
        stackOverflow();
    }

}

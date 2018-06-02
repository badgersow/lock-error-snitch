## Lock error snitch

Simple Java Agent to save you from StackOverflowErrors inside ReentrantLocks

#### TL;DR

StackOverflowException inside lock/unlock methods
* Will NOT corrupt lock instances
* Stack trace will be logged for further investigation

*Downside:* lock/unlock methods become slower

#### Background

Sometimes the code has a bug and it falls to infinite recursion.
If StackOverflowError happens inside `lock()` or `unlock()` methods in standard Java locks, it is possible
that these locks will end up in inconsistent state.

This situation is very hard to detect because when you catch StackOverflowError, you might not have enough stack frames
to log this error. As a result logs might not contain the error inside locks. This one unlucky corrupted lock may eventually
force all threads to hang.

Pattern would be the following:
* you see threads waiting for lock in thread dump
* you don't see the owner of the lock in thread dump
* if you look at the owner in heap dump, this owner would be killed or doing unrelated job

Once this error is detected, only JVM restart can help.

#### How it works

Java agent loads before `main()` method is called and instruments bytecode of the methods in standard Java classes:
* `j.u.c.l.ReentrantLock#lock`
* `j.u.c.l.ReentrantLock#unlock`
* `j.u.c.l.ReentrantReadWriteLock$ReadLock#lock`
* `j.u.c.l.ReentrantReadWriteLock$ReadLock#unlock`
* `j.u.c.l.ReentrantReadWriteLock$WriteLock#lock`
* `j.u.c.l.ReentrantReadWriteLock$WriteLock#unlock`

Instrumented code does
1. Before useful method execution it does dummy recursion of predefined depth to check that we have enough
stack space and to provoke StackOverflowError earlier.
2. Catches any Throwables and stores them to file.

These two actions allow us to
* StackOverflowError inside lock/unlock methods won't corrupt them because they will happen in dummy recursion
before any lock state is changed.
* When any error inside lock/unlock methods happen, they will be logged in file for further investigation.

#### Build
```
git clone git@github.com:badgersow/lock-error-snitch.git
cd lock-error-snitch
mvn clean package
```

The agent jar can be found in `target/lock-snitch-agent.jar`

#### Usage

To use in your JVM, simply pass `-javaagent:lock-snitch-agent.jar`. It is required to have this jar in the folder where
JVM is started for correct classloading hacks.

To check that it works, agent jar file has a [demo class that corrupts the lock](https://github.com/badgersow/lock-error-snitch/blob/master/src/main/java/com/atlassian/graev/lock/snitch/sample/LockMonsterDemo.java).
Please see the example below. If your can't reproduce corrupted locks without the agent, don't worry. It's very platform-specific
and depends on many factors. On the other hand, if you can reproduce corrupted locks with agent, this is a bug. Please write me an email.

```
$ java -cp lock-snitch-agent.jar com.atlassian.graev.lock.snitch.sample.LockMonsterDemo
Start corrupting the lock...
Lock was corrupted by StackOverflowError
$ java -cp lock-snitch-agent.jar -javaagent:lock-snitch-agent.jar com.atlassian.graev.lock.snitch.sample.LockMonsterDemo
Instrumenting 2 methods for class java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock
Inserting logging code to the method lock
Inserting logging code to the method unlock
Done! The length of the new bytecode is 2,587
Instrumenting 2 methods for class java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock
Inserting logging code to the method lock
Inserting logging code to the method unlock
Done! The length of the new bytecode is 2,106
Instrumenting 2 methods for class java/util/concurrent/locks/ReentrantLock
Inserting logging code to the method lock
Inserting logging code to the method unlock
Done! The length of the new bytecode is 4,219
Start corrupting the lock...
Lock wasn't corrupted
$ cat lock-snitch-trace-37144858468344-8178 | grep -v InstrumentedCodeHelper.java | head
java.lang.StackOverflowError
	at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:42)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:44)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:44)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:44)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:44)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:44)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:44)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:44)
```

#### Parameters

`-Dlock.snitch.max.trace.files=10`

Maximum number of files with listing for this particular JVM. Allows not to spam the hard drive.

`lock.snitch.recursion.depth=100`

Depth of the dummy recursion. Trade-off between performance and ability to have enough stack frames for logging.
## Lock error snitch

Simple Java Agent to log any Throwables inside ReentrantLocks

#### TL;DR

Throwables inside lock/unlock methods will be logged even if they are caught and swallowed by user code. 
Mainly created for StackOverflowErrors.

*Warning:* this agent loads javassist. Please make sure its version is compatible with your application. 

#### Background

Sometimes the code has a bug and it falls to infinite recursion.
If StackOverflowError happens inside `lock()` or `unlock()` methods in standard Java locks, it is possible
that these locks will end up in inconsistent state.

This situation is very hard to detect because when you catch StackOverflowError, you might not have enough stack frames
to log this error. As a result logs might not contain the error inside locks. This one unlucky corrupted lock may eventually
force all threads to hang.

Symptoms are usually look this way:
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

Instrumented code catches Throwables and writes them to file. Actual writing happens in another thread, which is vital
because after StackOverflowError the original thread might not have enough stack frames for logging.

#### Build
```
git clone git clone git@bitbucket.org:egraev/lock-error-snitch.git
cd lock-error-snitch
mvn clean package
```

The agent jar can be found in `lock-error-snitch-agent/target/lock-snitch-agent.jar`

#### Usage

To use in your JVM, simply pass `-javaagent:/path/to/lock-snitch-agent.jar`. Please note: it is important NOT to rename
agent JAR file because its filename is hardcoded for classloading hacks.

To check that it works, agent jar file has a [demo class that corrupts the lock](https://bitbucket.org/egraev/lock-error-snitch/src/master/lock-error-snitch-agent/src/main/java/com/atlassian/graev/lock/snitch/sample/LockMonsterDemo.java).
Please see the example below. If the lock is corrupted, you should see trace file with error listing.

```
$ java -cp lock-snitch-agent.jar com.atlassian.graev.lock.snitch.sample.LockMonsterDemo
Start corrupting the lock...
Lock was corrupted by StackOverflowError
$ java -cp lock-snitch-agent.jar -javaagent:lock-snitch-agent.jar com.atlassian.graev.lock.snitch.sample.LockMonsterDemo
Instrumenting 2 methods for class java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock
Inserting logging code to the method lock
Inserting logging code to the method unlock
Done! The length of the new bytecode is 2,527
Instrumenting 2 methods for class java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock
Inserting logging code to the method lock
Inserting logging code to the method unlock
Done! The length of the new bytecode is 2,061
Instrumenting 2 methods for class java/util/concurrent/locks/ReentrantLock
Inserting logging code to the method lock
Inserting logging code to the method unlock
Done! The length of the new bytecode is 4,164
All locks bytecode has been instrumented
Traces writing thread has been spawned
Start corrupting the lock...
Lock was corrupted by StackOverflowError
$ cat snitch-trace-226862108281253 | head
java.lang.StackOverflowError
	at java.util.concurrent.locks.AbstractQueuedSynchronizer.setState(AbstractQueuedSynchronizer.java:550)
	at java.util.concurrent.locks.ReentrantLock$Sync.tryRelease(ReentrantLock.java:157)
	at java.util.concurrent.locks.AbstractQueuedSynchronizer.release(AbstractQueuedSynchronizer.java:1261)
	at java.util.concurrent.locks.ReentrantLock.unlock(ReentrantLock.java:457)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:51)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:52)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:52)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:52)
	at com.atlassian.graev.lock.snitch.sample.LockMonsterDemo.messWithLock(LockMonsterDemo.java:52)
```

#### Bytecode

Bytecode of instrumented ReentrantLock#lock
```
public void lock();
  descriptor: ()V
  flags: ACC_PUBLIC
  Code:
    stack=1, locals=2, args_size=1
       0: aload_0
       1: getfield      #4                  // Field sync:Ljava/util/concurrent/locks/ReentrantLock$Sync;
       4: invokevirtual #7                  // Method java/util/concurrent/locks/ReentrantLock$Sync.lock:()V
       7: return
       8: astore_1
       9: aload_1
      10: putstatic     #175                // Field com/atlassian/graev/lock/snitch/agent/AsyncTracesWriter.pendingThrowable:Ljava/lang/Throwable;
      13: aload_1
      14: athrow
    Exception table:
       from    to  target type
           0     8     8   Class java/lang/Throwable
    LineNumberTable:
      line 285: 0
      line 286: 7
    StackMapTable: number_of_entries = 1
      frame_type = 72 /* same_locals_1_stack_item */
        stack = [ class java/lang/Throwable ]
```

#### Benchmarks

Vanilla java locks
```
Benchmark                         Mode  Cnt   Score   Error   Units
LocksBenchmark.testOrdinaryLock  thrpt   25  58.164 ± 0.499  ops/us
LocksBenchmark.testReadLock      thrpt   25  51.046 ± 0.209  ops/us
LocksBenchmark.testWriteLock     thrpt   25  57.134 ± 0.167  ops/us

```

Instrumented locks
```
Benchmark                         Mode  Cnt   Score   Error   Units
LocksBenchmark.testOrdinaryLock  thrpt   25  57.231 ± 0.779  ops/us
LocksBenchmark.testReadLock      thrpt   25  49.532 ± 0.617  ops/us
LocksBenchmark.testWriteLock     thrpt   25  56.285 ± 0.558  ops/us
```

#### Parameters

`-Dlock.snitch.skip.init.file=false`

Skip creation of init file inside temp directory. The sole purpose of this file is to signal that everything is OK 
and the agent is able to write something to the directory.

`-Dlock.snitch.traces.directory=.`

Directory where trace files are going to be created. Please check that application has write access to it.

`-Dlock.snitch.max.trace.files=100`

Maximum number of files with listing for this particular JVM. Helps not to spam the hard drive.

`-Dlock.snitch.store.poll.period=1000`

This period is used by async writer thread to poll any pending errors before writing them to file
## Thread API的详细介绍

##### Thread类中的静态方法

- currentThread()
- sleep()
- yield()
- interrupted()
- activeCount()

多线程中的wait/notify为什么定义在Object方法中?

因为Java提供的锁是对象级的而不是线程级的，每个对象都有锁，通过线程获得，如果线程需要等待某些锁那么调用对象中的wait()方法就有意义了。如果wait()方法定义在Thread类中，线程正在等待的是哪个锁就不明显了。而这些方法属于锁级别的操作，所以把它们定义在Object类中因为锁属于对象。

##### Thread常用API

**sleep**

sleep方法被调用时，这会继续持有它的资源不会释放，并且Thread.sleep()它只会导致当前线程休眠。

jdk1.5之后提供了TimeUnit来替代Thread.sleep，例如：

休眠1小时20分 

TimeUnit.HOURS.sleep(1);

TimeUnit.MINUTES.sleep(20);

**yield**

yield表示自愿放弃当前CPU资源，如果CPU的资源不紧张，则会忽略这种提醒。

调用yield方法会使当前线程从RUNNING状态转换为RUNNABLE状态。

**设置线程优先级**

setPriority()为线程设定优先级

getPriority()获取线程的优先级

**设置线程上下文类加载器**

getContextClassLoader()：获取线程上下文的类加载器，就是这个线程是由哪个类加载器加载的。

setContextClassLoader()：设置该线程的类加载器，它打破了java类加载器的父委托机制。

**线程interrupt，isinterrupted，interrupted**

线程提供了一个方法stop，而该方法已经废除，当调用interrupt时，会修改线程的interrupt flag的标识。调用interrupt()方法仅仅是在当前线程中打了一个停止的标记，并不是真的停止线程。

isInterrupted它主要判断当前线程是否被中断，方法仅仅是对interrupt标识的一个判断，并不会影响标识发生任何改变。

interrupted是一个静态方法，它与isInterrupted不同的是，当调用interrupt时，再调用interrupted返回true之后，它也会修改interrupt flag状态为false。

例如：

isinterrupted：  

```java
Thread thread = new Thread() {
    @Override
    public void run() {
        while(true){}
    }
};
thread.start();
try{
    TimeUnit.MILLISECONDS.sleep(10);
}catch (Exception e){ }
System.out.println(thread.isInterrupted());     //false
thread.interrupt();
System.out.println(thread.isInterrupted());     //true
System.out.println(thread.isInterrupted());     //true
```

```java
interrupted:  
Thread thread2 = new Thread() {
    @Override
    public void run() {
        while(true){
            System.out.println(Thread.interrupted());
        }
    }
};
thread2.setDaemon(true);
thread2.start();
thread2.interrupt();
try{
    TimeUnit.MILLISECONDS.sleep(15);
}catch (Exception e){
    e.printStackTrace();
}
```

那么总结一下项目中该如何使用：

当需要线程中断的时候，调用interrupt方法，如果在此期间又调用了sleep阻塞方法，会产生InterruptedException，可以try/catch进行处理。

**join**

join某个线程A，会使当前线程B进入等待，直到线程A结束生命周期，或者到达给定的时间。

```java
Thread thread = new Thread() {
    @Override
    public void run() {
        for(int i=0;i<10;i++){
            System.out.println(Thread.currentThread().getName()
                    +System.currentTimeMillis());
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
};
thread.start();
thread.join();
for(int i=0;i<10;i++){
    System.out.println(Thread.currentThread().getName()
            +System.currentTimeMillis());
}
System.out.println("main end!");
```

其运行结果表示，main线程会等待thread-0线程执行结束后，才会接着执行。

join方法是由wait方法实现的，所以join时，阻塞的线程也会释放出资源

```java
public final synchronized void join(long millis)
throws InterruptedException {
    long base = System.currentTimeMillis();
    long now = 0;

    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (millis == 0) {
        while (isAlive()) {
            wait(0);
        }
    } else {
        while (isAlive()) {
            long delay = millis - now;
            if (delay <= 0) {
                break;
            }
            wait(delay);
            now = System.currentTimeMillis() - base;
        }
    }
}
```

从源码中可以看出，join其实调用的是wait方法，而它是如何实现，A线程调用B线程的join方法，让A线程等待的呢？

wait()方法调用后会释放锁，并阻塞当前线程

线程A调用线程B线程的join时，需要先获取到线程B的对象锁，进入到线程B的join方法时，调用了wait，而wait方法会释放当前线程B对象锁，并阻塞当前A线程

**关闭线程**

正常结束：

1. 线程结束生命周期正常结束
2. 捕获中断信号关闭线程，这也就是通过interrupt方法
3. 使用volatile修饰自定义一个变量flag，使用flag状态+isInterrupted方法。

异常退出：

1. 进程假死，原因就是某个线程阻塞了，或者线程出现了死锁的情况。


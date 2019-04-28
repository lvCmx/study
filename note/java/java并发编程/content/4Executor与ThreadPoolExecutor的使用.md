### Executor与ThreadPoolExecutor的使用

**Executor接口介绍**

![](F:\__study__\hulianwang\study\note\java\java并发编程\img\04_executor.png)

接口ExecutorService是Executor的子接口，而AbstractExecutorService是它的实现类，具体的方法实现由ThreadPoolExecutor来实现的。

##### 使用Executors工厂类创建线程池

接口Executor仅仅是一种规范，是一种声明，是一种定义，并没有实现任何的功能，而Executors工厂类提供了创建线程池的方法。

![](F:\__study__\hulianwang\study\note\java\java并发编程\img\04_2_executors.png)

**newCachedThreadPool创建无界线程池**

newCachedThreadPool()方法创建的是无界线程池，可以进行线程自动回收。它最大能够存放Integer.MAX_VALUE个线程。

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>());
```

使用方法也很简单：

```java
public class NewCachedThreadPoolDemo {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName()+":"+System.currentTimeMillis());
            }
        });
        System.out.println("--sleep start--");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("--sleep end--");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName()+":"+System.currentTimeMillis());
            }
        });
    }
}
```

**newFixedThreadPool创建有界线程池**

```java
public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
    return new ThreadPoolExecutor(nThreads, nThreads,
                                  0L, TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>(),
                                  threadFactory);
}
```

可以看出，线程池的大小需要我们传参数指定

**newSingleThreadExecutor()创建单一线程池**

```java
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>()));
```

它的coreSize与maxSize的大小都为1 ,表示它最多创建一个线程。



##### ThreadPoolExecutor的使用

上面了解了关于Executors提供的一些创建线程池的方法，而本节主要研究ThreadPoolExecutor

**构造方法**

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler) {}
```

这是ThreadPoolExecutor的构造方法，主要的参数解释如下：

- corePoolSize：池中所保存的线程数，包括空闲线程，也就是核心池的大小。
- maximumPoolSize：池中所允许的最大线程数。
- keepAliveTime：当线程数量大于corePoolSize值时，在没有超过指定的时候内是不从线程池中将空闲线程删除的，如果超过此时间单位，则删除。
- unit：keepAliveTime参数的时间单位。
- workQueue执行前用于保持任务的队列，此队列仅保持由execute方法提交的Runnable任务。
- handler：是指当任务队列满的时候，所采用的线程丢弃策略。

使用线程池一般会出现以下5种情况：

A：execute(runnable)欲执行的runnable的数量；

B：代码corePoolSize；

C：代表maximumPoolSize；

D：代表A-B(假设A>=B)

E：代表new LinkedBlockingDeque<Runnable>队列

F：代表SynchroiousQueue队列

G：代表keepAliveTime

(1)、如果A<=B，那么马上创建线程运行这个任务，并不放入扩展队列Queue中，其他参数功能忽略；

(2)、如果A>B && A<=C && E，则C和G参数忽略，并把D放入E中等待被执行；

(3)、如果A>B && A<=C && F，则C和G参数有效，并且马上创建线程运行这些任务，而不把D放入F中，D执行完成任务后在指定的时间后发生超时时将D进行清除；

(4)、如果A>B && A>C && E，则C和G参数忽略，并把D放入E中等待被执行；

(5)、如果A>B && A>C &&F，则处理C的任务，其他任务则不再处理抛出异常；

**BokcingQueue**

BockingQueue只是一个接口，常用的实现类有LinkedBlockingQueue和ArrayBlockingQueue，用LinkedBlockingQueue的好处在于没有大小限制，优点是队列容量非常大所以执行execute()不会抛出异常。而线程池中运行的线程数永远不会超过corePoolSize值，因为其他多余的线程被放入LinkedBlockQueue队列中，keepAliveTime参数也没有意义了。

使用SynchroiousQueue队列时，maximumPoolSize与keepAliveTime参数的作用将有效，当线程空闲时间超过keepAliveTime时，将清除空线程

**keepAliveTime为0时**

当线程数量大于corePoolSize值时，在没有超过指定的时间内是不从线程池中将空闲线程删除的，如果超过此时间单位，则删除，如果为0则任务执行完毕后立即从队列中删除。

**shutdown与shutdownNow**

方法shutdown的作用是使当前未执行完的线程继续执行，而不再添加新的任务Task，还有shutdown方法不会阻塞，调用shutdown方法后，主线程main就马上结束了，而线程池会继续运行直到所有 任务执行完才会停止，如果不调用shutdown方法，那么线程会一会保持下去，以便随时执行被添加的新Task任务。

shutdownNow的作用是中断所有的任务Task，并且抛出InterruptedExecption异常

**isShutdown()**

作用是判断线程池是否已经关闭

##### 线程池ThreadPoolExecutor的拒绝策略

- AbortPolicy：当任务添加到线程池中被拒绝时，它将抛出RejectedExceutionException异常。
- CallerRunsPolicy：当任务添加到线程池中被拒绝时，会使用调用线程池的Thread线程对象处理被拒绝的任务。
- DiscardOldestPolicy：当任务添加到线程池中被拒绝时，线程池会放弃等待队列中最旧的未处理任务，然后将被拒绝的任务添加到等待队列中。
- DiscardPolicy：当任务添加到线程池中被拒绝时，线程池将丢弃被拒绝的任务。


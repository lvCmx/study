### Future和Callable的使用 

##### Future和Callable的介绍

接口Callable与线程Runnable的主要区别为：

1）Callable接口的call()方法有返回值，而Runnable接口的run()方法没有返回值

2）Callable接口的call()方法可以声明抛出异常，而Runnable接口的run()方法不可以声明抛出异常。

```java
public class CallableTest {
    public static void main(String[] args) throws Exception {
        MyCallable myCallable = new MyCallable();
        FutureTask futureTask = new FutureTask<>(myCallable);
        Thread thread = new Thread(futureTask);
        thread.start();
        Object o1 = futureTask.get();
        System.out.println(o1);
        System.out.println("-----------------------------");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        Future submit = executor.submit(futureTask);
        Object o = submit.get();
        System.out.println(o);
    }
}
class MyCallable implements Callable{
    @Override
    public Object call() {
        System.out.println("in mycallable");
        return 1;
    }
}
```

##### 自定义拒绝策略RejectedExecutionHandler接口的使用

```java
public class RejectedExecutionHandlerTest {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        ThreadPoolExecutor poolExecutor=(ThreadPoolExecutor)executorService;
        poolExecutor.setRejectedExecutionHandler(new MyRejectedExecutionHandler());
        poolExecutor.submit(new MyRunnable("A"));
        poolExecutor.submit(new MyRunnable("B"));
        poolExecutor.submit(new MyRunnable("C"));
        poolExecutor.shutdown();
        poolExecutor.submit(new MyRunnable("D"));
    }
}
class MyRunnable implements Runnable{
    private String name;
    public MyRunnable(String name){
        this.name=name;
    }
    @Override
    public void run() {
        System.out.println(name);
    }
}
class MyRejectedExecutionHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        System.out.println(((FutureTask)r).toString()+"被拒绝!");
    }
}
```

##### 方法execute()与submit()的区别

方法execute()没有返回值，而submit()方法可以有返回值。

方法execute()在默认的情况下异常直接抛出，不能捕获，但可以通过自定义Thread-Factory的方式进行捕获，而submit() 方法在默认的情况下，可以catch Exception-Exception捕获异常。

```java
public class ExecuteAndSubmit {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ThreadPoolExecutor poolExecutor =
                new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        poolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("execute 没有返回值");
            }
        });
        Future<Integer> result = poolExecutor.submit(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 10;
            }
        });
        Integer num = result.get();
        System.out.println(num);
    }
}
```

##### 总结

接口Future的实现类是FutureTask，Callable接口与Runnable接口中不同主要是返回值的，Future接口调用get()方法取得处理的结果值时是阻塞性的，也就是如果调用Future对象的get()方法时，任务尚未执行完成，则调用get()方法时一直阻塞到此任务完成时为止。

```java
public class FutureTaskDemo {
    public static void main(String[] args) throws Exception {
        MyCallable1 myCallable1 = new MyCallable1();
        FutureTask futureTask = new FutureTask(myCallable1);
        Thread thread = new Thread(futureTask);
        thread.start();
        System.out.println("输出参数：");
        Object o = futureTask.get();
        System.out.println(o);
    }
}
class MyCallable1 implements Callable{
    @Override
    public Object call() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 10;
    }
}
```


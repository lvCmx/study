### Java多线程编程核心技术--学习笔记 

<a href="#1">1.进程与线程</a>

<a href="#2">2.线程创建</a>

<a href="#3">3.线程中基本方法详解</a>

<a href="#4">4.对象及变量的并发访问</a>

<a href="#5">5.线程池</a>

#### <a name="1">进程与线程</a>

**进程**

进程是资源（CPU、内存）分配的基本单位，它是程序执行时的一个实例，程序运行时系统就会创建一个进程，并为它分配资源，然后把该进程放入进程就绪队列，进程调用器选中它的时候就会为它分配CPU时间，程序开始真正的运行。

Linux系统函数fork()可以在父进程中创建一个子进程，这样的话在一个进程接到来自客户端新的请求时就可以复制出一个子进程让其来处理，父进程只需要负责监控请求的到来，然后创建子进程让其去处理，这样就能做到并发处理。

```python
pid = os.fork()
if pid == 0:
    print('子进程:%s,父进程是:%s' % (os.getpid(), os.getppid()))
else:
    print('进程:%s 创建了子进程:%s' % (os.getpid(),pid ))
```

```
进程:27223 创建了子进程:27224
子进程:27224,父进程是:27223
```

fork函数会返回两次结果，因为操作系统会把当前进程的数据复制一遍，然后程序就分两个进程继续运行后面的代码，fork分别在父进程和子进程中返回，在子进程返回的值pid永远是0，在父进程返回的是子进程的进程id。

**线程**

线程是程序执行时的最小单位，一个进程至少包含一个线程，线程间共享进程的所有资源，每个线程有自已的堆栈和局部变量，线程由CPU独立调度执行，在多CPU环境下就允许多个线程同时运行，同样多线程也可以实现并发操作，每个请求分配一个线程来处理。

**线程和进程的区别**

- 进程有自己的独立地址空间，每启动一个进程，系统就会为它分配地址空间，建立数据表来维护代码段、堆栈段和数据段。而线程是共享进程中的数据的，CPU切换一个线程的花费远比进程要小很多。
- 线程间通信更方便，同一进程下的线程共享全局变量、静态变量等数据，而进程之间的通信需要以通信的方式进程。
- 多线程程序只要有一个线程死掉，整个进程也死掉了，而一个进程死掉并不会对另外一个进程造成影响，因为进程有自己独立的地址空间。



#### <a name="2">线程创建方式</a>

线程的创建有两种方式

**通过extends Thread类**

我们由Thread的源代码可见，Thread implements Runnable接口，并且使用继承Thread的方式的局限就是不支持多继承。  

```java
class MyThread extends Thread{
    public void run() {
        System.out.println("当前正在执行的线程是:"+
                Thread.currentThread().getName());
    }
}
```

调用：

```java
MyThread myThread =new MyThread();
myThread.setName("mythread");//设置线程名称
myThread.start();//启动线程
```

**通过implements Runnable接口**

```java
class MyThread implements Runnable{
    public void run() {
        System.out.println("当前正在执行的线程是:"+
                Thread.currentThread().getName());
    }
}
```

调用：

```java
Thread myThread = new Thread(new MyThread());
myThread.setName("myThread_Runnable");
myThread.start();
```

如果多次调用start()方法，则会出现异常IllegalThreadStateException(非法线程状态异常)

**通过Callable**

callable创建线程的方式，它与前面两种的不同在于，它有返回值，并且会抛出异常。

```java
class MyThread implements Callable {
    @Override
    public Object call() throws Exception {
        System.out.println("run....");
        return null;
    }
}
```

调用：

```java
Callable<String> callable  =new MyThread();
FutureTask <String>futureTask=new FutureTask<>(callable);
Thread mThread=new Thread(futureTask);
mThread.start();
System.out.println(futureTask.get());
```

**线程池的方式创建线程**

```java
ExecutorService ex=Executors.newFixedThreadPool(5);
for(int i=0;i<5;i++) {
    ex.submit(new Runnable() {
        @Override
        public void run() {
            for(int j=0;j<10;j++) {
                System.out.println(Thread.currentThread().getName()+j);
            }
        }
    });
}
ex.shutdown();
```

#### <a name="3">线程API详解</a>

**1、currentThread()**

例如：获取当前正在执行的线程的名称Thread.*currentThread*();

Thread 构造器与run()是谁来调用?  

 试验：线程的构造方法是由谁来调用？run()方法又是由谁来调用?  

```java
class MyThread implements Runnable{
    public MyThread() {
        System.out.println("构造器:"
                +Thread.currentThread().getName());
    }
    public void run() {
        System.out.println("run():"
                +Thread.currentThread().getName());
    }
}
调用:
public static void main(String[] args) {
    Thread thread = new Thread(new MyThread());
    thread.setName("myThread");
    thread.start();
}
结果：
```

​    构造器:main

​    run():myThread

​    说明：Thread的构造函数是由main或其它线程调用，而run是由当前线程调用

**2、isAlive()**

方法isAlive()的功能是判断当前的线程是否处于活动状态

```java
public class Demo1 {
    public static void main(String[] args){
        Thread thread = new MyThread();                      
        System.out.println("begin:" +thread.isAlive());
        thread.start();
        thread.sleep(100);
        System.out.println(Thread.currentThread().getName());
        System.out.println("end:"+thread.isAlive());
        System.out.println("main:"+Thread.currentThread().isAlive());
    }
}
//通过实现Runnable接口
class MyThread extends Thread{
    public void run() {
        System.out.println("run():"+this.isAlive());
    }
}
```

结果：

begin:false

run():true

main

end:false

Thread.currentThread():true

**3、sleep()**

该方法是将当前线程处于睡眠状态,或者是让当前线程（暂停执行），它在暂停过程中，不会释放资源。

```java
System.out.println("begin");
//暂停当前线程
ThreadTest threadTest = new ThreadTest();
threadTest.setName("MyLove");
threadTest.start();
//暂停threadTest线程
threadTest.sleep(2000);
System.out.println("end");
Thread.sleep(4000):表示让当前正在执行的线程暂停执行

```

**4、getId()**

getId()方法的作用是取得线程的唯一标识

main它的线程名称为main,线程id为1

**5、interrupt()**

 Interrupt()方法停止线程，使用效果不像stop()那样马上就停止，调用interrupt()方法仅仅是在当前线程中打了一个停止的标记，并不是真的停止线程.

**6、interrupted 与isInterrupted**

这两个方法都是用来判断(interrupt)它的状态的  

interrupted():测试当前（指的是运行this.interrupted()方法的线程）线程是否已经中断  

isInterrupted():测试线程是否已经中断  

演示代码：

```java
public static void main(String[] a) throwsInterruptedException {
    ThreadTest threadTest = new ThreadTest();
    threadTest.setName("MyLove");
    threadTest.start();
    Thread.sleep(1000);
    System.out.println("是否已经中断:"+threadTest.interrupted());
}
public void run() {
    Thread.sleep(2000);
}
```

运行结果: 是否已经中断:false

threadTest.isInterrupted()判断当前线程是否已经中断

**7、线程的停止**

- 异常停止
- stop()方法
- 使用return停止线程
- 通过interrupt

**8、线程的暂停**

suspend()是用来暂停线程的，使用resume()方法恢复线程的执行

```java
Thread:
public class ThreadTest extends Thread{
    private long i=0;
    public long getI(){
        return i;
    }
    public void setI(long i){
        this.i=i;
    }
    public void run() {
        while(true){
            i++;
        }
    }
}
Main:
public static void main(String[] args) throws InterruptedException {
    ThreadTest threadTest = new ThreadTest();
    threadTest.start();
    Thread.sleep(5000);
    //暂停线程的执行
    threadTest.suspend();
    System.out.println("a="+System.currentTimeMillis()+"i="+threadTest.getI());    
    Thread.sleep(5000);
    System.out.println("b="+System.currentTimeMillis()+"i="+threadTest.getI());
    //恢复线程的执行
    threadTest.resume();
    Thread.sleep(5000);
    threadTest.suspend();
    System.out.println("c="+System.currentTimeMillis()+"i="+threadTest.getI());
    Thread.sleep(5000);
    System.out.println("d="+System.currentTimeMillis()+"i="+threadTest.getI());
}
```

运行结果:

a=1498867631233i=1865504934

b=1498867636235i=1865504934

c=1498867641253i=3717704368

d=1498867646253i=3717704368

**9、yield()**

 yield方法的作用是放弃当前的cup资源，将它让给其他的任务去占用cup执行时间,但放弃的时候不确定，有可能刚刚放弃，马上又获得CPU时间片。

Thread.yield();当前正在运行的线程放弃CPU资源

一定要记住，CPU要执行谁是由优选权来决定的。放弃后，它的线程状态由运行转为就绪状态

**10、线程的优先级**

在操作系统中，线程可以划分优先级，优先级较高的线程得到的CPU资源较多，也不是CPU优先执行优先级的线程对象中的任务，在java中，线程的优先级分为1~10这10个等级

在java中优先权具有继承性，比如A线程启动B线程，则B线程的优先级与A是一样的。

 **11、volatile关键字**

内存可见性：是指当某个线程正在使用对象状态而另一个线程在同时修改该状态，需要确保当一个线程修改了对象状态后，其他线程能够看到发生的状态变化。

volatile关键字的两层语义：

一旦一个共享变量（类的成员变量、类的静态成员变量）被volatile修饰之后，那么就具备了两层语义

（1）、保证了不同线程对这个变量进行操作时的可见性，即一个线程修改了某个变量的值，这新值对其他线程来说是立即可见的。

（2）、禁止进行指令重排序。

​      第一 :个变量一旦使用volatile关键字会强制将修改的值写入主存；

​      第二：使用volatile关键字的话，当线程2进行修改时，会导致线程1的工作内存中缓存变量（变量值无效）



#### <a name="4">对象及变量的并发访问</a>

这节主要说的就是synchronized的基本使用与控制资源的访问

##### synchroized修饰方法

**方法内的变量为线程安全**

因为在方法内部的变量，无论有多少个线程调用这个方法，它都会重新创建这个变量，因为方法内的变量是局部变量，它只能在当前线程中访问的这个方法中使用。

例如：

```java
public class Demo {
    public void addI(String name) throws Exception{
        int number=0;
        System.out.println(name+":begin demo");
        if("a".equals(name)){
            number=100;
            Thread.sleep(2000);
            System.out.println("a set  over!");
        }else{
            number=200;
            System.out.println("b set  over!");
        }
        System.out.println(name +" is number =" +number);
    }
}
public class ThreadA extends Thread{
    private Demo demo;
    public ThreadA(Demo demo) {
        super();
        this.demo = demo;
    }
    public void run() {
        try {
            demo.addI("a");
        } catch (Exception e) {}
    }
}
public class ThreadB extends Thread{
    private Demo demo;
    public ThreadB(Demo demo) {
        super();
        this.demo = demo;
    }
    public void run(){
        try {
            demo.addI("b");
        } catch (Exception e) {}
    }
}
public class ThreadTest{
    public static void main(String[] args) {
        Demo demo = new Demo();
        ThreadA threadA = new ThreadA(demo);
        threadA.start();
        ThreadB threadB= new ThreadB(demo);
        threadB.start();
    }
}
```

运行结果:

a:begin demo

b:begin demo

b set  over!

b is number =200

a set  over!

a is number =100

**实例变量非线程安全**

如果并发类存在成员变量，并且在编程中使用到了成员变量，会产生并发问题

如果把上面的Demo中的int number=0改成全局变量,则运行的结果:

a:begin demo

b:begin demo

b set  over!

b is number =200

a set  over!

a is number =200

关于实例变量非线程安全的解决方法，就是在add方法的前面加上一个syncrhoized使当前方法变为同步的，就可以了。

修改后，运行结果如下:

b:begin demo

b set  over!

b is number =200

a:begin demo

a set  over!

a is number =100

 实验结论：在两个线程访问同一个对象中的同步方法时一定是线程安全的。

**多个对象多个锁**

修改如下代码:

将addI()修改为synchroinzed，但是要求add方法不能是静态的方法。

修改运行main：

b:begin demo

b set  over!

b is number =200

a:begin demo

a set  over!

a is number =100

从结果中我们可以看出，两个线程分别访问同一个类的两个不同实例的相同名称的同步方法，效果却是以异步的方式运行的。

**使用synchroized的注意事项**

关键字synchroinzed取得的锁都是对象锁，而不是把一段代码或方法当作锁，哪个线程先执行带有synchroized关键字的方法，哪个线程就持有该方法所属对象的锁Lock，那么其它线程只能呈等待状态，前提是多个线程访问的是同一个对象。

另外，synchroinzed加在方法上，也表示的是this锁（对象锁），如果其它线程想访问这个类中的其它带有synchronized的方法，也是不能进行访问的。

出现异常的时候锁会自动释放

java类锁与对象锁：对象锁用于对象实例方法，或者一个对象实例上的，类锁是用于类的静态方法或者一个类的class对象上的。类的对象实例可以有很多个，但是每个类只有一个class对象，所以不同对象实例的对象锁是互不干扰的，但是每个类只有一个类锁。

**同一个类中的synchroized与非synchroized**

当访问一个类中的synchroized的时候，它锁的是当前对象中需要synchronized的方法，而对于当前类中没有使用synchroized修饰的方法，在访问它们的时候，是不需要加锁处理的，所以，当A线程在访问资源类Resource的synchroized的方法时，这个时候，线程B可以同时访问资源类Resource的非syncrhoized方法。

(1) A线程先持有object对象的Lock锁，B线程可以以异步的方式调用object对象中的非synchronized类型的方法。

(2)  A线程先持有object对象的Lock锁，B线程如果在这时调用object对象中的synchroinzed类型的方法则需等待，也就是同步。

**synchronized锁重入**

当线程请求一个由其它线程持有的对象锁时，该线程会阻塞，而当线程请求由自己持有的对象锁时，如果该锁是重入锁，请求就会成功，否则阻塞。

```java
public class Data2 {
    synchronized public void method1(String value){
        System.out.println(value+":Data2->method1->begin");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {}
        System.out.println(value+":Data2->method1->end");
    }
}
public class Data1 {

    synchronized public void method1(Data2 data2){
        System.out.println("Data1->method1->begin");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
        data2.method1("from data1");
        System.out.println("Data1->method1->end");
    }
}
public class ThreadA extends Thread{

    private Data2 data2;

    public ThreadA(Data2 data2) {
        this.data2=data2;
    }
    public void run() {
        Data1 data1 = new Data1();
        data1.method1(data2);
    }
}

public class ThreadTest {

    public static void main(String[] args) {
        Data2 data2 = new Data2();
        ThreadA threadA = new ThreadA(data2);
        threadA.start();
        data2.method1("from main");
    }
}
执行结果：
```

![1556083904685](https://github.com/lvCmx/study/blob/master/note/java/Java%E5%A4%9A%E7%BA%BF%E7%A8%8B%E7%BC%96%E7%A8%8B%E6%A0%B8%E5%BF%83%E6%8A%80%E6%9C%AF/img/%E9%94%81%E9%87%8D%E5%85%A5%E5%9B%BE.png)

**同步不具有继承性**

同步不具有继承性，也就是当父类中的方法是synchronized，而子类重写了父类中的方法时，而子类并没有在方法中加synchronized，这时，子类的方法是非synchronized的。

**synchronized同步语句块**

由于synchronized声明方法在某些情况下是有缺点的，比如A线程调用同步方法执行一个长时间的任务，那么B线程则必须等待比较长时间，在这种情况下可以使用syncrhoized同步语句块来解决。

当两个并发线程访问同一个对象object中的syncrhoized(this)同步代码块时，一段时间内只能有一个线程被执行，另一个线程必须等待当前线程执行完这个代码块以后才能执行该代码块。

**synchronized(非this对象)**

synchroized(非this对象)的作用：

1、在多个线程持有“对象监视器”为同一个对象的前提下，同一时间只有一个线程可以执行syncrhoized(非this对象)同步代码块中的代码。

2、当持有“对象监视器”为同一个对象的提前下，同一时间只有一个线程可以执行synchronized(非this对象)同步代码块的代码。

3、syncrhoized()方法锁或synchroized(this)锁在synchroinized(非this锁)进行并发访问的时候，synchronized方法锁与synchronized(非this锁)所监视的对象必须为同一个，不然的话运行的结果就是异步调用了。

**死锁**

java线程死锁是一个经典的多线程问题，因为不同的线程都在等待根本不可能释放的锁，从而导致所有的任务都无法继续完成。

```java
public class Deadlock {
    private Object obj=new Object();
    private Object obj2=new Object();
    public void methodA(){
        synchronized (obj) {
            String name=Thread.currentThread().getName();
            System.out.println(name+"   在执行   "+obj);
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (obj2) {
                System.out.println(name+"   在等   "+obj2);
            }
        }
    }
    public void methodB(){
        synchronized (obj2) {
            String name=Thread.currentThread().getName();
            System.out.println(name+"   在执行   "+obj2);
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (obj) {
                System.out.println(name+"   在等待   "+obj);
            }
        }
    }
}
线程A
public class ThreadA extends Thread{
    private Deadlock deadlock;
    public ThreadA(Deadlock deadlock) {
        super();
        this.deadlock = deadlock;
    }

    public void run() {
        deadlock.methodA();
    }
}
线程B
public class ThreadB extends Thread{
    private Deadlock deadlock;
    public ThreadB(Deadlock deadlock) {
        super();
        this.deadlock = deadlock;
    }
    public void run() {
        deadlock.methodB();
    }
}
测试
public class DeadlockTest {
    public static void main(String[] args) {
        Deadlock deadlock = new Deadlock();
        ThreadA threadA = new ThreadA(deadlock);
        threadA.setName("ThreadA");
        threadA.start();
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ThreadB threadB = new ThreadB(deadlock);
        threadB.setName("ThreadB");
        threadB.start();
    }
}
```



#### <a name="5">线程池</a>

线程池就是将多个线程放在一个池子里（池化技术），然后需要线程池的时候不是创建一个线程，而是从线程池里面获取一个可用的线程，然后执行任务，线程池的关键在于它为我们管理了多个线程，当需要线程的时候从线程池中获取线程，任务执行完成之后线程不会被锁毁，而会被重新放到池子里面。

**线程池构造函数**

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue)

public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory)

public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          RejectedExecutionHandler handler)

public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler)
```



**线程池的参数**

- corePoolSize：核心池的大小，创建线程池后，默认情况下，在创建了线程池后，线程池中的线程数会有corePoolSize。不管他们创建以为是不是空闲。

- maximumPoolSize：线程池最大的线程数，它表示在线程池中最多能创建多少个线程。

- keepAliveTime：表示线程没有执行时最多保持多久时间会终止。只有当线程池中的线程数大于corePoolSize时，keepAliveTime才会起作用，直到线程池中的线程数不大于corePoolSize即当线程池中的线程数大于corePoolSize时，如果一个线程空闲的时间达到keepAliveTime，则会终止，直到线程数不超过corePoolSize。

- unit：参数是keepAliveTime的时间单位。例如：TimeUnit.HOURS;

- woreQueue：一个阻塞队列，用来存储等待执行的任务。

  - ArrayBlockingQueue

    有界队列。当使用有限的 maximumPoolSizes 时，有界队列（如 ArrayBlockingQueue）有助于防止资源耗尽，但是可能较难调整和控制。

  - LinkedBlockingQueue

    无界队列。使用无界队列（例如，不具有预定义容量的 LinkedBlockingQueue）将导致在所有 corePoolSize 线程都忙时新任务在队列中等待。这样，创建的线程就不会超过 corePoolSize。(maximumPoolSize 的值也就无效了)

  - SynchronousQueue

    直接提交，它将任务直接提交线程而不保持它们

- threadFactory：线程工厂，主要用来创建线程

- handler：表示当拒绝处理任务时的策略，有以下四种取值：

  - ThreadPoolExecutor.AbortPolicy：丢弃任务并抛出"RejectedExecutionException"异常
  - ThreadPoolExecutor.DiscardPolicy：也是丢弃任务，但是不抛出异常
  - ThreadPoolExecutor.DiscardOldestPolicy：丢弃队列最前面的任务，然后重新尝试执行任务
  - ThreadPoolExecutor.CallerRunsPolicy：由调用线程处理该任务。

**线程池创建线程的四种方法**

多线程创建线程池的方式：

- newCachedThreadPool：创建一个可缓存线程池，如果线程池长度超过处理需要，可灵活回收空闲线程，若无可回收，则新创建线程。
- newFixedThreadPool创建一个定长线程池，可控制线程最大并发数，超出的线程会在队列中等待。
- newScheduledThreadPool：创建一个定长线程池，支持定时及周期性任务执行。
- newSingleTrheadExecutor：创建一个单线程化的线程池，只有一个线程来执行任务，保证所有任务按照指定顺序优先级执行。

**newCachedThreadPool**

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, 
                                  Integer.MAX_VALUE,
                                  60L, 
                                  TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>());
```

从源码来看，工作线程的数量几乎没有限制，这样可灵活的往线程池中添加线程。如果长时间没有往线程池提交任务，即如果工作线程空间了指定的时间，则该工作线程将自动终止，如果你又提交了新的任务，则线程池重新创建一个工作线程。

使用方法：

```java
public class NewCachedThreadPoolDemo {
    public static void main(String[] args) {
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < 10; i++) {
            final int index = i;
            try {
                Thread.sleep(index * 100);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println(index+"当前线程"+Thread.currentThread().getName());
                }
            });
        }
    }
}
```

![](https://github.com/lvCmx/study/blob/master/note/java/Java%E5%A4%9A%E7%BA%BF%E7%A8%8B%E7%BC%96%E7%A8%8B%E6%A0%B8%E5%BF%83%E6%8A%80%E6%9C%AF/img/cache_result.png)

**newFixedThreadPool**

```java
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(nThreads,
                                  nThreads,
                                  0L, 
                                  TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>());
}
```

使用newFixedThreadPool必须指定线程的大小，线程池没有默认值，它使用的是LinkedBlockingQueue，但是，创建的时候指定了大小，所以它是有界的。

使用方法：

```java
public class NewFixedThreadPoolDemo {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        for (int i = 0 ; i < 4 ;i ++) {
            int  j = i;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        System.out.println(Thread.currentThread().getName());
                    } catch (Exception e) {
                    }
                }
            });
        }
    }
}

```

**newScheduledThreadPool**

```java
public ScheduledThreadPoolExecutor(int corePoolSize) {
    super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
          new DelayedWorkQueue());
}
```

可以看出，它使用的是DelayedWorkQueue队列，专用延迟队列。

```java
static class DelayedWorkQueue extends AbstractQueue<Runnable>
    implements BlockingQueue<Runnable> {}
```

使用方法：

```java
public class NewScheduledThreadPoolDemo {
    public static void main(String[] args) {
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);
        scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println("delay 1 seconds, and excute every 3 seconds");
            }
        }, 1, 3, TimeUnit.SECONDS);
    }
}
```

调用scheduleAtFixedRate时的参数：

- command：要执行的命令
- initialDelay：初始化延迟第一次执行的延迟时间
- period：连续执行之间的时间段
- unit：参数单位初始延迟和周期参数的时间单位

**newSingleTrheadExecutor**

```
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>()));
}
```

可以看出，它只创建了大小为1个的线程池。
## java并发编程

<a href="#1">1.Semaphore和Exchanger的使用</a>

<a href="#2">2.CountDownLatch和CyclicBarrier的使用</a>

<a href="#3">3.xxxxxxxxx</a>

<a href="#4">4.Executor和ThreadPoolExecutor的使用</a>

<a href="#5">5.Future和Callable的使用</a>

### <a name="1">Semaphore与Exchanger的使用</a>
Semaphore类是一个计数信号量，必须由获取它的线程释放，通常用于限制可以访问某些资源线程数目。  
Semaphore所提供的功能完全就是synchronized关键字的升级版，但它提供的功能更加的强大与方便，主要的作用就是控制线程并发的数量。  
##### 1.1Semaphore的使用  
此类的主要作用就是限制线程并发的数量，如果不限制线程并发的数量，则CPU的资源很快就被耗尽。

```java
public class SemaphoreTest {
    public static void main(String[] args) throws Exception{
        Service service = new Service();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    service.test();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    service.test();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    service.test();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
class Service {
    Semaphore semaphore = new Semaphore(1);
    public void test() throws InterruptedException {
        semaphore.acquire();
        System.out.println(Thread.currentThread().getName()+":"+System.currentTimeMillis());
        Thread.sleep(1000);
        System.out.println(Thread.currentThread().getName()+":"+System.currentTimeMillis());
        semaphore.release();
    }
}
运行结果：
Thread-0:1556096222802
Thread-0:1556096223804
Thread-1:1556096223804
Thread-1:1556096224805
Thread-2:1556096224805
Thread-2:1556096225807
```

在定义Semaphore的时候指定的大小为1，说明同一时刻只允许一个线程执行acquire()和release()之间的代码。  
**方法acquire(permits)参数作用及动态添加permits许可数量**  
有参方法acquire(int permits)的功能是每调用1次此方法，就使用x个许可。  
如果在使用的时候semaphore.acquire(2)，说明同一时间只有5个线程允许执行acquire()和release()之间的代码。  
**acquireUninterruptibly()的使用**  

方法acquireUninterruptibly()的作用是使等待进入acquire方法的线程，不允许被中断。如果调用线程的interrupt后，是不会中断的。

**availablePermits()和drainPermits()**

avaliablePermits()返回此Semaphore对象中当前可用的许可数，此方法通常用于调试。

drainPermits()可获取并返回立即可用的所有许可个数，并且将可用许可置0

**getQueueLength()和hasQueuedThreads(()**

getQueueLength()的作用是取得等待许可的线程个数

hasQueuedThreads()的作用是判断有没有线程在等待这个许可。

**公平与非公平信号量的测试**

所谓公平信号量是获得锁的顺序与线程启动的顺序有关，但不代表100%地获得信号量，仅仅是在概率上能得到保证，而非公平信号量就是无关的了。

Semaphore semaphore = new Semaphore(1,isFair);其中isFair默认为false，如果为true，则表示是公平的。

**tryAcquire()的使用**

无参方法tryAcquire()的作用是尝试地获得1个许可，如果获取不到则返回false，此方法通常与if语句结合使用，其具有无阻塞的特点。

有参方法tryAcquire(int permits)的作用是尝试地获得x个许可，如果获取不到则返回false。

tryAcquire(long timeout,TimeUnit unit)的作用是反在指定的时间内尝试地获得1个许可，如果获取不到则返回false。

tryAcquire(int permits,long timeout,TimeUnit unit)的作用是在指定的时间内尝试地获取x个许可，如果获取不到则返回false。

**利用Semaphore实现生产者与消费者**

```java
public class RepastService {
    volatile private Semaphore setSemaphore=new Semaphore(10); // 生产者
    volatile private Semaphore getSemaphore=new Semaphore(20); // 消费者
    volatile private ReentrantLock lock=new ReentrantLock();
    volatile private Condition setCondition=lock.newCondition();
    volatile private Condition getCondition=lock.newCondition();
    // 最多只有4个盒子存放物品
    volatile private Object[] producePostition=new Object[4];

    private boolean isEmpty(){
        boolean isEmpty=true;
        for(int i=0;i<producePostition.length;i++){
            if(producePostition[i]!=null){
                isEmpty=false;
                break;
            }
        }
        return isEmpty;
    }

    private boolean isFull(){
        boolean isFull=true;
        for(int i=0;i<producePostition.length;i++){
            if(producePostition[i]==null){
                isFull=false;
                break;
            }
        }
        return isFull;
    }

    public void set(){
        try{
            setSemaphore.acquire(); //允许最多同时10个生产者
            lock.lock();
            while(isFull()){
                setCondition.await();
            }
            for(int i=0;i<producePostition.length;i++){
                if(producePostition[i]==null){
                    producePostition[i]="数据";
                    System.out.println(Thread.currentThread().getName()+"生产了"+producePostition[i]);
                    break;
                }
            }
            getCondition.signalAll();
            lock.unlock();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            setSemaphore.release();
        }
    }

    public void get(){
        try{
            getSemaphore.acquire(); //允许20个消费者
            lock.lock();
            while(isEmpty()){
                getCondition.await();
            }
            for(int i=0;i<producePostition.length;i++){
                if(producePostition[i]!=null){
                    System.out.println(Thread.currentThread().getName()+"消费了"+producePostition[i]);
                    producePostition[i]=null;
                    break;
                }
            }
            setCondition.signalAll();
            lock.unlock();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            getSemaphore.release();
        }
    }
}
```

##### 1.2Exchanger的使用

类Exchanger的功能可以使2个线程之间传输数据，它比生产者/消费者模式使有的wait/notify要更加方便。

类Exchanger中的exchange()方法具有阻塞的特色，也就是此方法被调用后等待其他线程来取得数据，如果没有其他线程取得数据，则一直阻塞等待。

**exchange()传递数据**

```java
public class ExchangeTest {
    public static void main(String[] args) {
        Exchanger<String> exchanger=new Exchanger<>();
        ExchangerThreadA exchangerThreadA = new ExchangerThreadA(exchanger);
        ExchangerThreadB exchangerThreadB = new ExchangerThreadB(exchanger);
        exchangerThreadA.start();
        exchangerThreadB.start();
    }
}
class ExchangerThreadA extends Thread{
    private Exchanger<String> exchanger=null;

    public ExchangerThreadA(Exchanger<String> exchanger){
        this.exchanger=exchanger;
    }
    @Override
    public void run() {
        try{
            System.out.println("A线程在B线程中取出的数据："+exchanger.exchange("中国人A"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

class ExchangerThreadB extends Thread{
    private Exchanger<String> exchanger=null;

    public ExchangerThreadB(Exchanger<String> exchanger){
        this.exchanger=exchanger;
    }
    @Override
    public void run() {
        try{
            System.out.println("B线程在A线程中取出的数据："+exchanger.exchange("中国人B"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
运行结果：
B线程在A线程中取出的数据：中国人A
A线程在B线程中取出的数据：中国人B
```

**方法exchage(V v,long timeout,TimeUnit unit)与超时**

当调用这个方法后在指定的时间内没有其他线程获取数据，则出现超时异常TimeoutException。

### <a name="2">CountDownLatch和CyclicBarrier的使用</a>

##### 2.1 CountDownLatch的使用

类CountDownLatch也是一个同步功能的辅助类，使用效果是给定一个计数，当使用这个CountDownLatch类的线程判断计数不为0时，则呈wait状态，如果为0时则继续运行。

实现等待与继续运行的效果分别需要使用await()和countDown()方法来进行，调用await()方法时判断计数是否为0，如果不为0则呈等待状态，其他线程可以调用countDown()方法将计数减1，当计数减到为0时，呈等待的线程继续运行。而方法getCount()就是获得当前的计数个数。

```java
public class CountDownLatchTest {
    public static void main(String[] args) {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        new Thread() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName() + "开始。。。。");
                countDownLatch.countDown();
            }
        }.start();

        new Thread() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName() + "开始。。。。");
                countDownLatch.countDown();
            }
        }.start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("子线程完成!");
    }
}
运行结果：
Thread-0开始。。。。
Thread-1开始。。。。
子线程完成!
```

**await(long timeout,TimeUnit unit)**

它的作用使线程在指定的最大时间单位内进入WAITING状态，如果超过这个时间则自动唤醒，程序继续向下运行。

**getCount()的使用**

方法getCount()获取当前计数的值

##### 2.2CyclicBarrier的使用

CyclicBarrier不仅有CountDownLatch所具有的功能，还可以实现屏障等待的功能，也就是阶段性同步，它在使用上的意义在于可以循环地实现线程要一起做任务的目标，而不是像类CountDownLatch一样，仅仅支持一次线程与同步点阻塞的特性。

CyclicBarrier的作用：多个线程之间相互等待，任何一个线程完成之前，所有的线程都必须等待，多个线程之间任何一个线程没有完成任务，则所有的线程都必须等待。

```java
public class CyclicBarrierTest {
    public static void main(String[] args) {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(4);
        for(int i=0;i<4;i++)
            new CycThread(cyclicBarrier).start();

        System.out.println("main");
    }
}
class CycThread extends Thread{
    private CyclicBarrier cyclicBarrier;
    public CycThread(CyclicBarrier cyclicBarrier) {
        this.cyclicBarrier = cyclicBarrier;
    }

    @Override
    public void run() {
        System.out.println("线程"+Thread.currentThread().getName()+"正在写入数据...");
        try {
            Thread.sleep(5000);      //以睡眠来模拟写入数据操作
            System.out.println("线程"+Thread.currentThread().getName()+"写入数据完毕，等待其他线程写入完毕");
            cyclicBarrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }catch(BrokenBarrierException e){
            e.printStackTrace();
        }
    }
}
main
线程Thread-0正在写入数据...
线程Thread-1正在写入数据...
线程Thread-2正在写入数据...
线程Thread-3正在写入数据...
线程Thread-1写入数据完毕，等待其他线程写入完毕
线程Thread-0写入数据完毕，等待其他线程写入完毕
线程Thread-3写入数据完毕，等待其他线程写入完毕
线程Thread-2写入数据完毕，等待其他线程写入完毕
```


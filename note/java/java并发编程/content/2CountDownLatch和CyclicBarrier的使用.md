### CountDownLatch和CyclicBarrier的使用

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


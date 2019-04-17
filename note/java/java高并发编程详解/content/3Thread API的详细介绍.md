## Thread API的详细介绍

##### Thread类中的静态方法

- currentThread()
- sleep()
- yield()
- interrupted()
- activeCount()

多线程中的wait/notify为什么定义在Object方法中?

因为Java提供的锁是对象级的而不是线程级的，每个对象都有锁，通过线程获得，如果线程需要等待某些锁那么调用对象中的wait()方法就有意义了。如果wait()方法定义在Thread类中，线程正在等待的是哪个锁就不明显了。而这些方法属于锁级别的操作，所以把它们定义在Object类中因为锁属于对象。

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


**1.final关键字**

- 当用final去修饰一个类的时候，表示这个类不能被继承（final类下的方法也是final的）
- final修饰的方法不能被重写，一个类的private方法会隐式的被指定为final方法。
- final修饰的成员变量要么直接赋值或在构造函数中赋值。

**2.默认方法**

java接口中可以定义默认方法，如果A，B两个接口都有相同名称的默认方法，那么在C接口继承A,B之后，需要重写这个默认方法。

**3.类的加载顺序**

- 父类静态变量
- 父类静态代码块
- 子类静态变量
- 子类静态代码块
- 父类普通变量
- 父类普通代码块
- 父类构造函数
- 子类普通变量
- 子类普通代码块
- 子类构造函数

**4.ArrayList**

初始大小10，扩容1.5倍，线程不安全

**5.HashMap**

默认大小16，扩容2倍，单个链表长度达8转红黑树，少于6转链表。或者是总体链表结点个数达64转红黑树。

**6.Object中的方法**

wait/notify/notifyAll/hashCode/clone/toString/equals

**7.sleep与sleep(0)**

sleep方法被调用时，它不会释放占有的资源。sleep时间过后，不一定立即会得到执行，因为睡眠时间到达后，有可能另外一个线程正在使用CPU，或者是有优先级高的线程正在使用CPU。

sleep(0)的作用是：触发操作系统立刻重新进行一次CPU竞争，竞争的结果也许是当前线程仍然获得CPU控制权，也许会换成别的线程获得CPU控制权。

**8.sleep与wait**

sleep：

- 让当前线程休眠指定时间
- 休眠时间的准确性依赖于系统时钟和CPU调用机制
- 不释放已获取的锁资源
- 可以通过调用interrupt()方法来唤醒休眠线程

wait：

- 让当前线程进入等待状态，当别的其他线程调用notify或者notifyAll方法时，当前线程进入就绪状态
- wait方法必须在同步上下文中调用，例如：同步方法块或者同步方法中，这也就意味着如果你想要调用wait方法，提前是必须获取对象上的锁资源。
- 当wait方法调用时，当前线程将会释放已获取的对象锁资源，并进入等待队列，其他线程就可以尝试获取对象上的锁资源。

**9.ThreadLocal**

Thread类中有一个threadLocals，其实每个线程的本地变量不是存放在ThreadLocal实例里面，而是存放在调用线程的threadLocals变量里面。也就是说，ThreadLocal类型的本地变量存放在具体的线程内存空间中。将当前线程以key存放在threadLocals中。使用完毕后需要调用ThreadLocal变量的remove方法，从当前线程的threadLocals里面删除本地变量。

**10.servlet是单例的并且是线程不安全的**

要解释为什么Servlet为什么不是线程安全的，需要了解Servlet容器（即Tomcat）使如何响应HTTP请求的。

当Tomcat接收到Client的HTTP请求时，Tomcat从线程池中取出一个线程，之后找到该请求对应的Servlet对象并进行初始化，之后调用service()方法。要注意的是每一个Servlet对象再Tomcat容器中只有一个实例对象，即是单例模式。如果多个HTTP请求请求的是同一个Servlet，那么着两个HTTP请求对应的线程将并发调用Servlet的service()方法。


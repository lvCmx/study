## 深入理解Thread构造函数

##### 线程的命名

在创建线程的时候，可以通过构造方法指定线程的名称，如果未指定，会生成默认的Thread-X。thread对象可以通过调用setName，而它只能对threadStatus==0的状态进行修改，也就是一旦线程启动，名字将不再被修改。

##### 线程的父子关系

Thread的所有构造函数，最终都会去调用一个静态方法init，其源码：

```java
private void init(ThreadGroup g, Runnable target, String name,
                  long stackSize, AccessControlContext acc) {
    if (name == null) {
        throw new NullPointerException("name cannot be null");
    }

    this.name = name.toCharArray();

    Thread parent = currentThread();
    SecurityManager security = System.getSecurityManager();
    // 后面代码省略
}
```

得出以下结论：

* 一个线程的创建肯定是由另一个线程完成的。
* 被创建线程的父线程是创建它的线程。

##### Thread与ThreadGroup

接着看Thread的init方法：

```java
SecurityManager security = System.getSecurityManager();
if (g == null) {
    /* Determine if it's an applet or not */

    /* If there is a security manager, ask the security manager
       what to do. */
    if (security != null) {
        g = security.getThreadGroup();
    }

    /* If the security doesn't have a strong opinion of the matter
       use the parent thread group. */
    if (g == null) {
        g = parent.getThreadGroup();
    }
}
```

如果在构造Thread的时候没有显示地指定一个ThreadGroup，那么子线程将会被加入父线程所在的组。

##### Thread与JVM虚拟机栈

在创建线程的时候，可以指定stackSize，它是指当前新线程能够占用栈空间的大小。

**JVM内存结构**

- 程序计数器：它在JVM中起的作用就是用于存放当前线程接下来将要执行的字节码指令、分支、循环、跳转、异常处理等信息。
- Java虚拟机栈：是在JVM运行时所创建的，在线程中，方法在执行的时候都会创建一个名为栈帧的数据结构，主要用于存放局部变量表、操作栈、动态链接、方法出口等信息。
- 本地方法栈：JVM为本地方法所划分的内存区域便是本地方法栈，主要用于调用JNI方法使用的。
- 堆内存：JAVA在运行期间创建的所有对象几乎都存放在该内存区域。
- 方法区：线程共享的，主要用于存储已经被虚拟机加载的类信息、常量、静态变量、即时编译器编译后的代码等数据。称为持久代
- java8元空间：JDK1.8之后，持久代被删除，取而代之的是元空间，它使用的是物理内存，大小也是受物理内存的限制。

**JVM常用参数**

- -Xmx：设置最大堆
- -Xms：设计初始堆内存大小
- -Xss：每个线程的栈大小
- -Xmn：设计年轻代大小，整个堆内存大小确定，增大年轻代将会减小老年代（大小比3:8）
- -XX:NewSize：年轻代初始值大小
- -XX:MaxNewSize：设置年轻代最大值
- -XX:NewRatio：设置年轻代（1个Eden和2个Survivor）与老年代的比值（1:4）
- -XX:SurvivorRatio：设置年轻代中Eden区与Survivor区的比值

##### 守护线程

守护线程经常用作与执行一些后台任务，因此有时它也被称为后台线程，当你希望关闭某些线程的时候，或者退出JVM进程的时候，一些线程能够自动关闭，此时就可以考虑用守护线程为你完成这样的工作。
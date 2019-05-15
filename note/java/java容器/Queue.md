建议参考：java并发编程之美第七章

#### 1.Queue接口

队列是一种先进先出(FIFO)的数据结构，Queue接口继承了Collection接口，LinkedList实现了Queue接口。

队列在java中的一种典型使用场景是线程池，在线程池中，当提交的任务不能被立即处理时，线程池将提交的任务放进队列中。

**阻塞队列**

当队列是空的时候，从队列中获取元素的操作将会阻塞直到有新元素添加时，或者当队列是满时，往队列里添加元素的操作会被阻塞直到队列空闲时。

- ArrayBlockingQueue：由数组结构组成的有界队列
- LinkedBlockingQueue：由链表结构组成的无界队列
- PriorityBlockingQueue：支持优先级排序的无界队列
- SynchronousQueue：不存储元素的阻塞队列
- DelayQueue：延迟阻塞队列
- ArrayDeque：基于数组的双向队列
- LinkedBlockingDeque：基于链表的双向阻塞队列

**并发队列**

ConcurrentLinkedQueue是一个适用于高并发场景下的队列，它是基于链表的无界线程安全的并发队列。

#### 2.ArrayBlockingQueue

![](F:\__study__\hulianwang\study\note\java\java容器\img\arrayBlockingQueue01.png)

ArrayBlockingQueue是基于数组实现的有界阻塞队列。新元素插入到队列的尾部，队列获取操作则是从队列头部开始获得元素。

ArrayBlockingQueue在创建的时候需要指定容量大小，一旦指定就不能再增加其容量，试图向已满队列中放入元素会导致操作受阻塞；试图从空队列中提取元素也将导致队列阻塞。

**主要成员变量**

```java
// 存放队列元素的数组
final Object[] items;

// 用来为下一个take/poll/remove的索引（出队）
int takeIndex;
// 用来为下一个put/offer/add的索引（入队）
int putIndex;

// 队列中元素的个数
int count;

final ReentrantLock lock;

// 等待出队的条件
private final Condition notEmpty;

// 等待入队的条件
private final Condition notFull;

transient Itrs itrs = null;
```

**构造方法**

```java
// 创建一个指定界线的队列
public ArrayBlockingQueue(int capacity) {
    this(capacity, false);
}
// capacity：指定队列有界线
// fair：false表示非公平锁， true：表示公平锁
public ArrayBlockingQueue(int capacity, boolean fair) {
    if (capacity <= 0)
        throw new IllegalArgumentException();
    this.items = new Object[capacity];
    lock = new ReentrantLock(fair); // 默认false非公平锁
    notEmpty = lock.newCondition();
    notFull =  lock.newCondition();
}
// 通过集合创建队列
public ArrayBlockingQueue(int capacity, boolean fair,
                          Collection<? extends E> c) {
    this(capacity, fair);

    final ReentrantLock lock = this.lock;
    lock.lock(); // Lock only for visibility, not mutual exclusion
    try {
        int i = 0;
        try {
            for (E e : c) {
                checkNotNull(e);
                items[i++] = e;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
        count = i;
        putIndex = (i == capacity) ? 0 : i;
    } finally {
        lock.unlock();
    }
}
```

**主要方法**

```java
// add调用的是offer方法添加元素，如果调用offer返回false，则抛出 IllegalStateException("Queue full")
public boolean add(E e) {
    return super.add(e);
}

// 将指定的元素插入到此队列的尾部（如果立即可行且不会超过该队列的容量），在成功时返回 true
public boolean offer(E e) {
    checkNotNull(e);
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        // 如果已经满了
        if (count == items.length)
            return false;
        else {
            enqueue(e); // 将元素添加到数组中
            return true;
        }
    } finally {
        lock.unlock();
    }
}
// 将元素添加到数组中
private void enqueue(E x) {
    // assert lock.getHoldCount() == 1;
    // assert items[putIndex] == null;
    final Object[] items = this.items;
    items[putIndex] = x;
    // 如果已经满了，则将下次putIndex的值为0
    if (++putIndex == items.length)
        putIndex = 0;
    count++;
    notEmpty.signal(); // 唤醒一个在 await()等待队列中的线程。
}

// 将指定的元素插入此队列的尾部，如果该队列已满，则等待可用的空间。
public void put(E e) throws InterruptedException {
    checkNotNull(e);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        // 如果队列满了，则等待
        while (count == items.length)
            notFull.await();
        // 添加元素
        enqueue(e);
    } finally {
        lock.unlock();
    }
}

// 将指定的元素插入此队列的尾部，如果该队列已满，则在到达指定的等待时间之前等待可用的空间。
public boolean offer(E e, long timeout, TimeUnit unit)
    throws InterruptedException {

    checkNotNull(e);
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == items.length) {
            if (nanos <= 0)
                return false;
            nanos = notFull.awaitNanos(nanos);
        }
        enqueue(e);
        return true;
    } finally {
        lock.unlock();
    }
}
// 返回队列首部元素
public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return (count == 0) ? null : dequeue();
    } finally {
        lock.unlock();
    }
}
// 
private E dequeue() {
    // assert lock.getHoldCount() == 1;
    // assert items[takeIndex] != null;
    final Object[] items = this.items;
    @SuppressWarnings("unchecked")
	// 获取未尾元素
    E x = (E) items[takeIndex];
    items[takeIndex] = null;
    // takeIndex+1,为下次返回首部元素做准备
    if (++takeIndex == items.length)
        takeIndex = 0;
    count--;
    if (itrs != null)
        itrs.elementDequeued();
    notFull.signal();
    return x;
}
// 获取首部元素，没有元素则阻塞
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == 0)
            notEmpty.await();
        return dequeue();
    } finally {
        lock.unlock();
    }
}
// 获取并移除此队列的头部，在指定的等待时间前等待可用的元素
public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == 0) {
            if (nanos <= 0)
                return null;
            nanos = notEmpty.awaitNanos(nanos);
        }
        return dequeue();
    } finally {
        lock.unlock();
    }
}
// 获取但不移除此队列的头；如果此队列为空，则返回 null
public E peek() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return itemAt(takeIndex); // null when queue is empty
    } finally {
        lock.unlock();
    }
}
public int size() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return count;
    } finally {
        lock.unlock();
    }
}

public boolean remove(Object o) {
    if (o == null) return false;
    final Object[] items = this.items;
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        if (count > 0) {
            final int putIndex = this.putIndex;
            int i = takeIndex;
            // 寻找要删除的元素，将其删除
            do {
                if (o.equals(items[i])) {
                    removeAt(i);
                    return true;
                }
                if (++i == items.length)
                    i = 0;
            } while (i != putIndex);
        }
        return false;
    } finally {
        lock.unlock();
    }
}
void removeAt(final int removeIndex) {
    final Object[] items = this.items;
    if (removeIndex == takeIndex) {
        // removing front item; just advance
        items[takeIndex] = null;
        if (++takeIndex == items.length)
            takeIndex = 0;
        count--;
        if (itrs != null)
            itrs.elementDequeued();
    } else {
        // an "interior" remove

        // slide over all others up through putIndex.
        final int putIndex = this.putIndex;
        for (int i = removeIndex;;) {
            int next = i + 1;
            if (next == items.length)
                next = 0;
            if (next != putIndex) {
                items[i] = items[next];
                i = next;
            } else {
                items[i] = null;
                this.putIndex = i;
                break;
            }
        }
        count--;
        if (itrs != null)
            itrs.removedAt(removeIndex);
    }
    notFull.signal();
}

public boolean contains(Object o) {
    if (o == null) return false;
    final Object[] items = this.items;
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        if (count > 0) {
            final int putIndex = this.putIndex;
            int i = takeIndex;
            do {
                if (o.equals(items[i]))
                   return true;
                if (++i == items.length)
                    i = 0;
            } while (i != putIndex);
        }
        return false;
    } finally {
        lock.unlock();
    }
}
```

#### 3.LinkedBlockingQueue

![](F:\__study__\hulianwang\study\note\java\java容器\img\linkedBlockingQueue01.png)

LinkedBlockingQueue是基于链表的无界阻塞队列，它最多可以存放Integer.MAX_VALUE个元素

LinkedBlockingQueue内定义一个内部类，Node

```java
static class Node<E> {
    E item;
    Node<E> next;
    Node(E x) { item = x; }
}
```

**源码主要成员**

```java
// 链表的界线，如果没有指定，则Integer.MAX_VALUE 
private final int capacity;
// 当前链表节点个数
private final AtomicInteger count = new AtomicInteger();
// 头节点
transient Node<E> head;
// 尾部结点
private transient Node<E> last;

/** 获取并移除元素时使用的锁，如：take, poll, etc */
private final ReentrantLock takeLock = new ReentrantLock();

/** 当队列没有数据时用于挂起执行删除的线程 */
private final Condition notEmpty = takeLock.newCondition();

/** 添加元素时使用的锁，如: put, offer, etc */
private final ReentrantLock putLock = new ReentrantLock();

/** 当队列数据已满时用于挂起执行添加的线程  */
private final Condition notFull = putLock.newCondition();
```

**构造方法**

```java
public LinkedBlockingQueue() {
    this(Integer.MAX_VALUE);
}

// 指定边界
public LinkedBlockingQueue(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException();
    this.capacity = capacity;
    last = head = new Node<E>(null);
}

public LinkedBlockingQueue(Collection<? extends E> c) {
    this(Integer.MAX_VALUE);
    final ReentrantLock putLock = this.putLock;
    putLock.lock(); // Never contended, but necessary for visibility
    try {
        int n = 0;
        for (E e : c) {
            if (e == null)
                throw new NullPointerException();
            if (n == capacity)
                throw new IllegalStateException("Queue full");
            enqueue(new Node<E>(e));
            ++n;
        }
        count.set(n);
    } finally {
        putLock.unlock();
    }
}
```

**主要方法**

```java
public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
        // 如果已经满了
        while (count.get() == capacity) {
            notFull.await();
        }
        enqueue(node);
        c = count.getAndIncrement();
        // 唤醒那些因队列满而阻塞的队列
        if (c + 1 < capacity)
            notFull.signal();
    } finally {
        putLock.unlock();
    }
    if (c == 0)
        signalNotEmpty();
}

// 
public boolean offer(E e, long timeout, TimeUnit unit)
    throws InterruptedException {

    if (e == null) throw new NullPointerException();
    long nanos = unit.toNanos(timeout);
    int c = -1;
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
        while (count.get() == capacity) {
            if (nanos <= 0)
                return false;
            // 等待指定的时间
            nanos = notFull.awaitNanos(nanos);
        }
        enqueue(new Node<E>(e));
        c = count.getAndIncrement();
        if (c + 1 < capacity)
            notFull.signal();
    } finally {
        putLock.unlock();
    }
    if (c == 0)
        signalNotEmpty();
    return true;
}

// 
public boolean offer(E e) {
    if (e == null) throw new NullPointerException();
    final AtomicInteger count = this.count;
    if (count.get() == capacity)
        return false;
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    putLock.lock();
    try {
        // 只有在满足添加条件的时候添加
        if (count.get() < capacity) {
            enqueue(node);
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        }
    } finally {
        putLock.unlock();
    }
    if (c == 0)
        signalNotEmpty();
    return c >= 0;
}
private void enqueue(Node<E> node) {
    last = last.next = node;
}
-----------------------------------------------
// 取出元素，会阻塞
public E take() throws InterruptedException {
    E x;
    int c = -1;
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
        // 如果没有元素则阻塞
        while (count.get() == 0) {
            notEmpty.await();
        }
        x = dequeue();
        c = count.getAndDecrement();
        if (c > 1)
            notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
    if (c == capacity)
        signalNotFull();
    return x;
}

public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    E x = null;
    int c = -1;
    long nanos = unit.toNanos(timeout);
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
        while (count.get() == 0) {
            if (nanos <= 0)
                return null;
            nanos = notEmpty.awaitNanos(nanos);
        }
        x = dequeue();
        c = count.getAndDecrement();
        if (c > 1)
            notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
    if (c == capacity)
        signalNotFull();
    return x;
}
// 获取并移除此队列的头，如果此队列为空，则返回 null。
public E poll() {
    final AtomicInteger count = this.count;
    if (count.get() == 0)
        return null;
    E x = null;
    int c = -1;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
        if (count.get() > 0) {
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        }
    } finally {
        takeLock.unlock();
    }
    if (c == capacity)
        signalNotFull();
    return x;
}
private E dequeue() {
    // assert takeLock.isHeldByCurrentThread();
    // assert head.item == null;
    Node<E> h = head;
    Node<E> first = h.next;
    h.next = h; // help GC
    head = first;
    E x = first.item;
    first.item = null;
    return x;
}
-----------------------------------------------
// 双重加锁
public boolean remove(Object o) {
    if (o == null) return false;
    fullyLock();
    try {
        for (Node<E> trail = head, p = trail.next;
             p != null;
             trail = p, p = p.next) {
            if (o.equals(p.item)) {
                unlink(p, trail);
                return true;
            }
        }
        return false;
    } finally {
        fullyUnlock();
    }
}
void fullyLock() {
    putLock.lock();
    takeLock.lock();
}
void fullyUnlock() {
    takeLock.unlock();
    putLock.unlock();
}
```

#### 4.PriorityBlockingQueue

![](F:\__study__\hulianwang\study\note\java\java容器\img\priorityBlockingQueue01.png)

PriorityBlockingQueue是一个基于优先级堆的无界的并发安全的优先级队列（FIFO），队列的元素按照其自然顺序进行排序，或者根据构造队列时提供的Comparator进行排序，具体取决于所用的构造方法。

PriorityBlockingQueue通过使用堆这种数据结构实现将队列中的元素按照某种排序规则进行排序，从而改变先进先出的队列顺序，提供开发者改变队列中元素的顺序的能力。队列中的元素必须是可比较的，即实现Comparable接口，或者在构建函数时提供可对队列元素进行比较的Comparator对象。

**主要成员变量**

```java
// 默认队列的大小
private static final int DEFAULT_INITIAL_CAPACITY = 11;
// 最大队列的大小
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
// 存放队列元素的数组
private transient Object[] queue;

// 元素个数
private transient int size;

// 元素比较器
private transient Comparator<? super E> comparator;

private final ReentrantLock lock;

// 因元素为空而阻塞的线程
private final Condition notEmpty;

// 扩容时用到的CAS
private transient volatile int allocationSpinLock;

// 序列化队列
private PriorityQueue<E> q;
```

**构造方法**

```java
// 默认初始大小为11
public PriorityBlockingQueue() {
    this(DEFAULT_INITIAL_CAPACITY, null);
}

// 指定默认初始大小
public PriorityBlockingQueue(int initialCapacity) {
    this(initialCapacity, null);
}

// 指定初始大小和比较器
public PriorityBlockingQueue(int initialCapacity,
                             Comparator<? super E> comparator) {
    if (initialCapacity < 1)
        throw new IllegalArgumentException();
    this.lock = new ReentrantLock();
    this.notEmpty = lock.newCondition();
    this.comparator = comparator;
    this.queue = new Object[initialCapacity];
}
```



#### 5.SynchronousQueue



#### 6.DelayQueue

DelayQueue并发队列是一个无界阻塞延迟队列，队列中的每个元素都有一个过期时间，当从队列获取元素时，只有过期元素才会出队列，队列头元素是最快要过期的元素。

DelayQueue内部使用PriorityQueue存放数据，使用ReentrantLock实现线程同步，另外，队列里面的元素要实现Delayed接口，由于每个元素都有一个过期时间，所以要实现获知当前元素剩下多少时间就过期了的接口，由于内部使用优先级队列来实现，所以要实现元素之间相互比较的接口。

```java
public class DelayQueue<E extends Delayed> extends AbstractQueue<E>
    implements BlockingQueue<E> {

    private final transient ReentrantLock lock = new ReentrantLock();
    private final PriorityQueue<E> q = new PriorityQueue<E>();}
```

**主要成员变量**

```java
// 并发访问锁
private final transient ReentrantLock lock = new ReentrantLock();
// DelayQueue是基于PriorityQueue实现的
private final PriorityQueue<E> q = new PriorityQueue<E>();

/**
 * 使用基于Leader-Follower模式的变体，用于尽量减少不必要的线程等待。当一个线程调用队列的take方法变为leader线程后，它会调用条件变量available。awaitNanos(dealy)等待delay时间，但是其他线程(follwer线程)则会调用available.await()进行无限等待。leader线程延迟时间过期后，会退出take方法，并通过调用available.signal()唤醒一个follwer线程。
 */
private Thread leader = null;
private final Condition available = lock.newCondition();
```

**主要方法**

```java
// 插入元素
public boolean offer(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        q.offer(e); // 调用的PriorityQueue.offer
        if (q.peek() == e) {
            leader = null;
            available.signal();
        }
        return true;
    } finally {
        lock.unlock();
    }
}
```

首先获取独占锁，然后添加元素到优先级队列，由于q是优先级队列，所以添加元素后，调用q.peek()方法返回的并不一定是当前添加的元素。如果判断为true，则说明当前元素e是最先将过期的，那么重置leader线程为null，这时候激活avaliable变量条件队列里面的一个线程，告诉它队列里面有元素了。

```java
// 获取并移除队列里面延迟时间过期的元素，如果队列里面没有过期元素则等待。
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        for (;;) {
            // 获取但不移除队首元素
            E first = q.peek();
            if (first == null)
                // 将当前线程放到阻塞队列
                available.await();
            else {
                long delay = first.getDelay(NANOSECONDS);
                // 说明已经过期
                if (delay <= 0)
                    return q.poll();
                first = null; // don't retain ref while waiting
                // leader不为null则说明其他线程也在执行take，则把该线程放入条件队列
                if (leader != null)
                    available.await();
                else {
                    // leader为null，则选取当前线程A为leader线程
                    Thread thisThread = Thread.currentThread();
                    leader = thisThread;
                    try {
                        // 指定该方法等待信号的的最大时间
                        available.awaitNanos(delay);
                    } finally {
                        if (leader == thisThread)
                            leader = null;
                    }
                }
            }
        }
    } finally {
        if (leader == null && q.peek() != null)
            available.signal();
        lock.unlock();
    }
}
```

```java
//获取并移除队头过期元素，如果没有过期元素则返回null.
public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        E first = q.peek();
        if (first == null || first.getDelay(NANOSECONDS) > 0)
            return null;
        else
            return q.poll();
    } finally {
        lock.unlock();
    }
}
```

#### 7.ArrayDeque



#### 8.LinkedBlockingDeque



#### 9.ConcurrentLinkedQueue

ConcurrentLinkedQueue是线程安全的无界非阻塞队列，其底层数据结构使用单向链表实现，对于入队和出队操作使用CAS来实现线程安全。

新的元素插入到队列的尾部，队列获取操作从队列头部获得元素，此队列不允许使用null元素。

![](F:\__study__\hulianwang\study\note\java\java容器\img\concurrentLinkedQueue01.png)

**Node节点**

```java
在Node节点内部则维护一个使用volatile修饰的变量item，用来存放节点的值；next用来存放链表的下一个节点，从而链接为一个单向无界链表。
private static class Node<E> {
    volatile E item;
    volatile Node<E> next;
｝
```

**主要成员变量**

```java
// 链表的头节点，取元素时从头部取 
private transient volatile Node<E> head;
// 链表尾节点，插入元素时从尾部插入
private transient volatile Node<E> tail;
```

**主要方法的实现**

1、offer：在队列未尾添加一个元素，如果传递的参数是null则抛出NPE异常。

```java
public boolean offer(E e) {
    // e为null则抛出空指针异常
    checkNotNull(e);
    // 将当前E元素封装成Node节点
    final Node<E> newNode = new Node<E>(e);
	// 从尾部节点进行插入元素，这个for循环没有退出条件，直到获取CAS成功
    for (Node<E> t = tail, p = t;;) {
        Node<E> q = p.next;
        // q==null 说明p是最后一个节点
        if (q == null) {
            // 使用CAS设置p节点的next节点
            if (p.casNext(null, newNode)) {
                // CAS成功，则说明新节点已经插入到了尾部，然后设置当前尾部节点。
                // 1,3,5,,,个节点为尾节点
                if (p != t) 
                    casTail(t, newNode);  // Failure is OK.
                return true;
            }
            // Lost CAS race to another thread; re-read next
        }
        else if (p == q)
            // 多线程操作时，由于poll操作移除元素后可能会把head变为自引用，也就是head的next变成了head，所以这里需要重新找新的head.
            p = (t != (t = tail)) ? t : head;
        else
            // 寻找尾节点
            p = (p != t && t != (t = tail)) ? t : q;
    }
}
```

2、add操作是在链表未尾添加一个元素，其实在内部调用的还是offer操作。

```java
public boolean add(E e) {
    return offer(e);
}
```

3、poll操作是在队列头部获取并移除一个元素，如果队列为空则返回null.

```java
public E poll() {
    // 无限循环
    restartFromHead:
    for (;;) {
        for (Node<E> h = head, p = h, q;;) {
            // 保存当前节点值
            E item = p.item;
			// 当前节点有值，则通过CAS变为null
            if (item != null && p.casItem(item, null)) {
                // CAS成功则标记当前节点并从链表中删除
                if (p != h) // hop two nodes at a time
                    updateHead(h, ((q = p.next) != null) ? q : p);
                return item;
            }
            // 当前队列为空则返回null
            else if ((q = p.next) == null) {
                updateHead(h, p);
                return null;
            }
            // 如果当前节点被自引用了，则重新寻找新的队列头节点。
            else if (p == q)
                continue restartFromHead;
            else
                p = q;
        }
    }
}
```

poll方法在移除一个元素时，只是简单地使用CAS操作把当前节点的item值设置为null，然后通过重新设置头节点将该元素从队列里面移除，被移除的节点就成了独立节点，这个节点会在垃圾回收时被收掉。

4、 peek操作：获取队列头部第一个元素（只获取不移除）如果队列为空则返回null。

```java
public E peek() {
    restartFromHead:
    for (;;) {
        for (Node<E> h = head, p = h, q;;) {
            E item = p.item;
            if (item != null || (q = p.next) == null) {
                updateHead(h, p);
                return item;
            }
            else if (p == q)
                continue restartFromHead;
            else
                p = q;
        }
    }
}
```

**总结**

ConcurrentLinkedQueue的底层使用单向链表数据结构来保存队列元素，每个元素被包装成一个Node节点，队列是靠头、尾节点来维护的。
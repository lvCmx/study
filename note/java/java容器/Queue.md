Queue队列

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



#### 7.ArrayDeque



#### 8.LinkedBlockingDeque



#### 9.ConcurrentLinkedQueue










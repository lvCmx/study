### Queue队列

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



#### 4.PriorityBlockingQueue

#### 5.SynchronousQueue

#### 6.DelayQueue

#### 7.ArrayDeque

#### 8.LinkedBlockingDeque

#### 9.ConcurrentLinkedQueue








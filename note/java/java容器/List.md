## List接口及实现类详解

#### 1.List接口

List接口继承自Collection，而Collection提供了一些抽象的基本方法。Collection接口继承自Iterable接口。

Iterable它能够让类支持foreach循环，它包含一个返回Iterator的方法。而Iterator是java中的迭代器对象。foreach的内部其实就是Iterator迭代器

```java
public static void main(String[] args) {
    List<Integer> list = new ArrayList<>();
    list.add(0);
    for (Integer it:list) {
        System.out.println(it);
    }
}
反编译：
public static void main(String args[]) {
    List list = new ArrayList();
    list.add(Integer.valueOf(0));
    Integer it;
    for (Iterator iterator = list.iterator(); iterator.hasNext(); System.out.println(it))
        it = (Integer)iterator.next();

}
```

**List的特点**

1. List存放的元素是有顺序的
2. List内部的元素可以重复。
3. List可以添加多个null元素。

**List中包含的方法**

```java
public interface List<E> extends Collection<E> {
    int size(); // 返回元素个数，最大Integer.MAX_VALUE
    boolean isEmpty(); // 集合不包含元素返回true
    boolean contains(Object o); // 是否包含某个元素
    Iterator<E> iterator(); // 返回Iterator迭代器
    Object[] toArray(); // 将列表转为数组,返回的数组是一个全新的数组
    <T> T[] toArray(T[] a); // 将列表数据转换到指定的数组中
    boolean add(E e); // 向列表中添加元素，可以添加null元素
    boolean remove(Object o); // 从此列表中删除第一个出现的指定元素
    boolean containsAll(Collection<?> c); // 是否包含一个集合
    boolean addAll(Collection<? extends E> c); // 添加集合中所有的元素
    E get(int index); // 获取指定下标的元素
    E set(int index, E element)
    void add(int index, E element); // 如果指定的位置有元素，则将元素后移再插入到当前位置
    E remove(int index);
    int indexOf(Object o);
    int lastIndexOf(Object o);
    ListIterator<E> listIterator();
}
```

#### 2.ArrayList

![](F:\__study__\hulianwang\study\note\java\java容器\img\arraylist_01.png)

**ArrayList特点**

ArrayList是基于数组来实现的，对于get和set方法调用花费常数时间。其缺点是新增和删除元素时（除非变动未端）需要花费O(n)的时间。

ArrayList不是线程安全的，只能用在单线程环境下，多线程环境下可以考虑用Collections.synchronizedList(List)函数返回一个线程安全的ArrayList类。

ArrayLit在创建时默认初始化10个存储空间，每次扩容是以1.5倍的空间递增。递增的公式：int newCapacity = oldCapacity + (oldCapacity >> 1);

**ArrayList成员变量**

```java
private static final int DEFAULT_CAPACITY = 10; // 数组初始化大小
private static final Object[] EMPTY_ELEMENTDATA = {}; //  定义一个空的数组实例以供其他需要用到空数组的地方调用
// 默认空数组，这是jdk1.8之后新加入的，它与上面数组的区别是：该数组是默认返回的，而上面是用户指定容量为0时返回的
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {}; 
// 存放元素的数组，用transient修饰的变量不需要被序列化。
// elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA的List，将在添加第一个元素时扩展为DEFAULT_CAPACITY
transient Object[] elementData; // 存储元素的数组
private int size; // 当前数组长度(包含元素的个数)
```

**ArrayList构造方法**

```java
无参构造器：
public ArrayList() {
    super();
    this.elementData = EMPTY_ELEMENTDATA;
}
指定大小构造器：
public ArrayList(int initialCapacity) {
    super();
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal Capacity: "+ initialCapacity);
    this.elementData = new Object[initialCapacity];
}
通过集合创建：
public ArrayList(Collection<? extends E> c) {
    elementData = c.toArray();
    size = elementData.length;
    // c.toArray might (incorrectly) not return Object[] (see 6260652)
    if (elementData.getClass() != Object[].class)
        elementData = Arrays.copyOf(elementData, size, Object[].class);
}
```

**ArrayList扩容机制**

```java
// 假设，目前ArrayList已经有10个元素，现在需要添加第11个元素。
public boolean add(E e) {
    // 扩容
    ensureCapacityInternal(size + 1);  // Increments modCount!!
    elementData[size++] = e;
    return true;
}

private void ensureCapacityInternal(int minCapacity) {
    // 如果elementData为空数组时，初始化值大小为DEFAULT_CAPACITY(10)
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
         minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    // 扩容minCapacity
    ensureExplicitCapacity(minCapacity);
}

private void ensureExplicitCapacity(int minCapacity) {
    modCount++;
    // overflow-conscious code
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}

private void grow(int minCapacity) {
    // overflow-conscious code
    int oldCapacity = elementData.length;   // 原数组的大小
    int newCapacity = oldCapacity + (oldCapacity >> 1);  // 将原数组扩大1.5倍
    if (newCapacity - minCapacity < 0) // (扩容后的大小 - 原数组元素个数+1)<0
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    // minCapacity is usually close to size, so this is a win:
    elementData = Arrays.copyOf(elementData, newCapacity);
}

Arrays.copyOf采用的是System.arraycopy来实现的。
public static <T,U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        @SuppressWarnings("unchecked")
        T[] copy = ((Object)newType == (Object)Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

src - 源数组。
srcPos - 源数组中的起始位置。
dest - 目标数组。
destPos - 目标数据中的起始位置。
length - 要复制的数组元素的数量。
public static native void arraycopy(Object src,  int  srcPos,Object dest, int destPos,int length);

是直接对内存中的数据块进行复制的，是一整块一起复制的，它是采用本地编码实现的。

```

**ArrayList的size**

通过ArrayList(Collection c)创建时，if ((size = elementData.length) != 0) {  通过一句来完成为size赋值。

add元素时，elementData[size++] = e; 保证添加元素时，size值自增1

remove元素时：elementData[--size] = null; // clear to let GC do its work

clear元素时：size = 0;

**ArrayList的序列化**

ArrayList中所有的元素存放在elementData中，而它采用transient所修饰，证明该数组是不可以被序列化的，而ArrayList类又实现了Serialiable接口，那么它的序列化是如何实现的？

```java
private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException{
    // Write out element count, and any hidden stuff
    int expectedModCount = modCount;
    s.defaultWriteObject();

    // Write out size as capacity for behavioural compatibility with clone()
    s.writeInt(size);

    // Write out all elements in the proper order.
    for (int i=0; i<size; i++) {
        s.writeObject(elementData[i]);
    }

    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
}
```

ArrayList在序列化的时候调用writeObject，直接将elementData输入了ObjectOutputStream；

```java
private void readObject(java.io.ObjectInputStream s)
    throws java.io.IOException, ClassNotFoundException {
    elementData = EMPTY_ELEMENTDATA;

    // Read in size, and any hidden stuff
    s.defaultReadObject();

    // Read in capacity
    s.readInt(); // ignored

    if (size > 0) {
        // be like clone(), allocate array based upon size not capacity
        ensureCapacityInternal(size);

        Object[] a = elementData;
        // Read in all elements in the proper order.
        for (int i=0; i<size; i++) {
            a[i] = s.readObject();
        }
    }
}
```

反充列化的时候调用readObject时，从ObjectInputStream获取到了元素的值和size值，再恢复到elementData中。

至于为什么序列化的时候不直接使用elementData来序列化，是因为elementData通常会预留一些容量，如果直接序列化会造成空间的浪费 。

**ArrayList的Fail-Fast机制**

fail-fast机制是java集合中的一种错误机制。当多个线程对同一个集合的内容进行操作时，就可能会产生fail-fast事件。例如：当某一个线程A通过Iterator去遍历某集合的过程中，若该集合的内容被其他线程改变了；那么线程A访问集合时，就会抛出ConcurrentModificationException异常，产生fail-fast事件。

单线程条件下如果违反了规则也是会产生fail-fast事件的。

```java
public static void main(String[] args) {
    List<Integer> list = new ArrayList<>();
    for (int i=0;i<9;i++) {
        list.add(i);
    }
    Iterator<Integer> iterator = list.iterator();
    while(iterator.hasNext()){
        Integer next = iterator.next();
        if(next==5){
            list.add(7);
        }
    }
}
```

通过查看源码会发现：

```java
public Iterator<E> iterator() {
    return new Itr();
}
private class Itr implements Iterator<E> {
    int expectedModCount = modCount;
    final void checkForComodification() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        }
}
```

在调用iterator的add/remove/next方法，都会先执行checkForComodification方法。

modCount在集合的add/remove/set等方法中都会改变它的值，而返回Iterator时expectedModCount=modCount，所以如果集合操作了一些导致modCount变化的方法，在checkForComodification验证时，发现值不相等，则抛出错误。

#### LinkedList

![](F:\__study__\hulianwang\study\note\java\java容器\img\linkedlist01.png)

LinkedList的底层是Deque双向链表，实现了Deque接口，而Deque接口继承于Queue接口。

LinkedList中Node节点的定义：

```java
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
}
```

**LinkedList特点**

LinkedList是基于双向链表实现的，它允许添加null元素，并且添加元素时能够保持元素的顺序。

LinkedList不是同步的，如果多个线程同时访问一个链表，则可以通过Collections.synchronizedList方法包装列表。

因为LinkedList是双向链表，所以在插入或删除头/尾部元素时，时间复杂度为O(1)

**LinkedList源码**

```java
public class LinkedList<E>
    extends AbstractSequentialList<E>
    implements List<E>, Deque<E>, Cloneable, java.io.Serializable
{
    transient int size = 0;  // 当前链表的个数
    transient Node<E> first; // 链表的头部
    transient Node<E> last;  // 链表的尾部

    public LinkedList() {
    }
    // 将集合元素添加到链表中
    public LinkedList(Collection<? extends E> c) {
        this();
        addAll(c);
    }
	
	// 采用头插法插入元素
    private void linkFirst(E e) {
        final Node<E> f = first;
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;
        if (f == null)
            last = newNode;
        else
            f.prev = newNode;
        size++;
        modCount++;
    }
   
    // 采用尾插法
    void linkLast(E e) {
        final Node<E> l = last;
        final Node<E> newNode = new Node<>(l, e, null);
        last = newNode;
        if (l == null)
            first = newNode;
        else
            l.next = newNode;
        size++;
        modCount++;
    }

    // 向当前元素之前插入一个元素
    void linkBefore(E e, Node<E> succ) {
        // assert succ != null;
        final Node<E> pred = succ.prev;
        final Node<E> newNode = new Node<>(pred, e, succ);
        succ.prev = newNode;
        if (pred == null)
            first = newNode;
        else
            pred.next = newNode;
        size++;
        modCount++;
    }

    // 删除first元素
    private E unlinkFirst(Node<E> f) {
        // assert f == first && f != null;
        final E element = f.item;
        final Node<E> next = f.next;
        f.item = null;
        f.next = null; // help GC
        first = next;
        if (next == null)
            last = null;
        else
            next.prev = null;
        size--;
        modCount++;
        return element;
    }

    // 删除最后一个元素
    private E unlinkLast(Node<E> l) {
        // assert l == last && l != null;
        final E element = l.item;
        final Node<E> prev = l.prev;
        l.item = null;
        l.prev = null; // help GC
        last = prev;
        if (prev == null)
            first = null;
        else
            prev.next = null;
        size--;
        modCount++;
        return element;
    }

    // 删除指定位置的元素
    E unlink(Node<E> x) {
        // assert x != null;
        final E element = x.item;
        final Node<E> next = x.next;
        final Node<E> prev = x.prev;

        if (prev == null) {
            first = next;
        } else {
            prev.next = next;
            x.prev = null;
        }

        if (next == null) {
            last = prev;
        } else {
            next.prev = prev;
            x.next = null;
        }

        x.item = null;
        size--;
        modCount++;
        return element;
    }

    // 获取第一个元素
    public E getFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return f.item;
    }

    // 获取最后一个元素
    public E getLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return l.item;
    }

    // 删除第一个元素
    public E removeFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return unlinkFirst(f);
    }

    // 删除最后一个元素
    public E removeLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return unlinkLast(l);
    }

    // 头插法插入元素
    public void addFirst(E e) {
        linkFirst(e);
    }

    // 向链表最后添加元素
    public void addLast(E e) {
        linkLast(e);
    }

    // 是否包含元素
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    // 元素个数
    public int size() {
        return size;
    }

    // 添加元素，默认添加在链表最后面。
    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    // 删除元素，允许传入null
    public boolean remove(Object o) {
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);
    }

    // 将集合元素添加到链表中
    public boolean addAll(int index, Collection<? extends E> c) {
        checkPositionIndex(index);

        Object[] a = c.toArray();
        int numNew = a.length;
        if (numNew == 0)
            return false;

        Node<E> pred, succ;
        if (index == size) {
            succ = null;
            pred = last;
        } else {
            succ = node(index);
            pred = succ.prev;
        }

        for (Object o : a) {
            @SuppressWarnings("unchecked") E e = (E) o;
            Node<E> newNode = new Node<>(pred, e, null);
            if (pred == null)
                first = newNode;
            else
                pred.next = newNode;
            pred = newNode;
        }

        if (succ == null) {
            last = pred;
        } else {
            pred.next = succ;
            succ.prev = pred;
        }

        size += numNew;
        modCount++;
        return true;
    }

    // 清空链表
    public void clear() {
        for (Node<E> x = first; x != null; ) {
            Node<E> next = x.next;
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }
        first = last = null;
        size = 0;
        modCount++;
    }


    // 获取指定位置的元素
    public E get(int index) {
        checkElementIndex(index);
        return node(index).item;
    }

    // 设置index下的元素，但是，要保证index<size
    public E set(int index, E element) {
        checkElementIndex(index);
        Node<E> x = node(index);
        E oldVal = x.item;
        x.item = element;
        return oldVal;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any
     * subsequent elements to the right (adds one to their indices).
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void add(int index, E element) {
        checkPositionIndex(index);

        if (index == size)
            linkLast(element);
        else
            linkBefore(element, node(index));
    }

    // 删除元素
    public E remove(int index) {
        checkElementIndex(index);
        return unlink(node(index));
    }

    
    // 检查index位置是否合法
    private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }
    private boolean isPositionIndex(int index) {
        return index >= 0 && index <= size;
    }
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }
    private void checkElementIndex(int index) {
        if (!isElementIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
    private void checkPositionIndex(int index) {
        if (!isPositionIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    // 根据index，返回元素
    Node<E> node(int index) {
        // assert isElementIndex(index);

        if (index < (size >> 1)) {  // 如果index位置离first比较近
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }

    // 查找o元素的index，它找的是第一次满足条件的元素
    public int indexOf(Object o) {
        int index = 0;
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null)
                    return index;
                index++;
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item))
                    return index;
                index++;
            }
        }
        return -1;
    }

    // 返回最后一个满足条件的元素的下标
    public int lastIndexOf(Object o) {
        int index = size;
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (x.item == null)
                    return index;
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (o.equals(x.item))
                    return index;
            }
        }
        return -1;
    }

    // 返回第一个元素
    public E peek() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }

    // 获得第一个元素
    public E element() {
        return getFirst();
    }

    // 删除并返回第一个元素
    public E poll() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    // 删除第一个元素
    public E remove() {
        return removeFirst();
    }

    // 默认是添加到链表后面
    public boolean offer(E e) {
        return add(e);
    }

    // 向链表前添加元素
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    // 向链表最后添加元素
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    // 返回第一个元素
    public E peekFirst() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
     }

    // 返回最后一个元素
    public E peekLast() {
        final Node<E> l = last;
        return (l == null) ? null : l.item;
    }

    // 删除并返回第一个元素
    public E pollFirst() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    // 删除并返回最后一个元素
    public E pollLast() {
        final Node<E> l = last;
        return (l == null) ? null : unlinkLast(l);
    }

    // 向链表前面压入一个元素
    public void push(E e) {
        addFirst(e);
    }

    // 删除并返回元素中第一个元素
    public E pop() {
        return removeFirst();
    }
    
    // 将链表转成数组
    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;
        return result;
    }
}
```

**LinkedList的序列化**

```java
将所有的元素，循环写入到ObjectOutputStream流中。序列化的时候只保存了元素的值，而没有保存它们的关系
private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException {
    // Write out any hidden serialization magic
    s.defaultWriteObject();

    // Write out size
    s.writeInt(size);

    // Write out all elements in the proper order.
    for (Node<E> x = first; x != null; x = x.next)
        s.writeObject(x.item);
}

// 由于序列化的时候没有保存前后驱关系，所以恢复的时候靠前后顺序
@SuppressWarnings("unchecked")
private void readObject(java.io.ObjectInputStream s)
    throws java.io.IOException, ClassNotFoundException {
    // Read in any hidden serialization magic
    s.defaultReadObject();

    // Read in size
    int size = s.readInt();

    // Read in all elements in the proper order.
    for (int i = 0; i < size; i++)
        linkLast((E)s.readObject());
}
```
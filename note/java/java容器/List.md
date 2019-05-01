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

**ArrayList原理**

ArrayList是基于数组来实现的，对于get和set方法调用花费常数时间。其缺点是新增和删除元素时（除非变动未端）需要花费O(n)的时间。



ArrayLit在创建时默认初始化10个存储空间，每次扩容是以1.5倍的空间递增。递增的公式：int newCapacity = oldCapacity + (oldCapacity >> 1);

**ArrayList成员变量**

```java
private static final int DEFAULT_CAPACITY = 10; // 数组初始化大小
private static final Object[] EMPTY_ELEMENTDATA = {}; //  定义一个空的数组实例以供其他需要用到空数组的地方调用
// 默认空数组，这是jdk1.8之后新加入的，它与上面数组的区别是：该数组是默认返回的，而上面是用户指定容量为0时返回的
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {}; 
// 存放元素的数组，用transient修饰的变量不需要被序列化。
// elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA的List，将在添加第一个元素时扩展为DEFAULT_CAPACITY
transient Object[] elementData;
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

**ArrayList源码解析**





**ArrayList扩容机制**





**ArrayList的size玄机**





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
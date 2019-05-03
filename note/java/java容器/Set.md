## Set接口及实现类详解

#### 1.Set接口

Set接口继承Collection接口，Set存放元素的特点在于：无序不可重复。常用的Set实现类包括HashSet、LinkedHashSet、TreeSet和SortedSet.

当向 Set 中添加对象时，首先调用此对象所在类的 hashCode() 方法，计算此对象的哈希值，此哈希值决定了此对象在 Set 中的存储位置。

Set采用Map实现，无法通过下标来取元素。只能通过iterator或者将set转换成list取元素。

#### 2.HashSet

![](https://github.com/lvCmx/study/blob/master/note/java/java%E5%AE%B9%E5%99%A8/img/hashset01.png)

**HashSet实现**

HashSet继承了AbstractSet，HashSet存放元素无序并且不可重复，HashSet通过HashMap来实现的。

HashSet不线程不安全的，可以通过Collections.synchronizedSet包装实现支持同步。

```java
private transient HashMap<E,Object> map;
private static final Object PRESENT = new Object();
public HashSet() {
    map = new HashMap<>();
}
```

添加元素时，将元素当作HashMap的key，value为固定的PRESENT。HashSet中不能保存null值。

**HashSet常用方法源码**

```java
// 创建一个HashMap，容量与加载因子使用默认的
public HashSet() {
    map = new HashMap<>();
}
// 通过Collection创建一个HashMap，指定initialCapacity容量参数
public HashSet(Collection<? extends E> c) {
    map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
    addAll(c);
}
// 自定义容量与加载因子
public HashSet(int initialCapacity, float loadFactor) {
    map = new HashMap<>(initialCapacity, loadFactor);
}
// 自定义容量，使用默认的加载因子
public HashSet(int initialCapacity) {
    map = new HashMap<>(initialCapacity);
}
// 前面说过，元素都存放在了HashMap中的key里面了
public Iterator<E> iterator() {
    return map.keySet().iterator();
}
// 返回大小
public int size() {
    return map.size();
}
// 是否为空
public boolean isEmpty() {
    return map.isEmpty();
}
// 是否包含指定元素
public boolean contains(Object o) {
    return map.containsKey(o);
}
// 如果此 set 已包含该元素，则该调用不更改 set 并返回 false
public boolean add(E e) {
    return map.put(e, PRESENT)==null;
}
// 删除元素
public boolean remove(Object o) {
    return map.remove(o)==PRESENT;
}
// 清除
public void clear() {
    map.clear();
}
// HashSet序列化，将容量、加载因子、大小和HashSet中的元素存储。
private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
    s.defaultWriteObject();

    s.writeInt(map.capacity());
    s.writeFloat(map.loadFactor());

    s.writeInt(map.size());

    for (E e : map.keySet())
        s.writeObject(e);
}


private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();

    int capacity = s.readInt();
    if (capacity < 0) {
        throw new InvalidObjectException("Illegal capacity: " +
                capacity);
    }

    float loadFactor = s.readFloat();
    if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
        throw new InvalidObjectException("Illegal load factor: " +
                loadFactor);
    }

    int size = s.readInt();
    if (size < 0) {
        throw new InvalidObjectException("Illegal size: " +
                size);
    }

    capacity = (int) Math.min(size * Math.min(1 / loadFactor, 4.0f),
            HashMap.MAXIMUM_CAPACITY);

    map = (((HashSet<?>)this) instanceof LinkedHashSet ?
            new LinkedHashMap<E,Object>(capacity, loadFactor) :
            new HashMap<E,Object>(capacity, loadFactor));

    for (int i=0; i<size; i++) {
        @SuppressWarnings("unchecked")
        E e = (E) s.readObject();
        map.put(e, PRESENT);
    }
}
```

#### 3.LinkedHashSet

![](https://github.com/lvCmx/study/blob/master/note/java/java%E5%AE%B9%E5%99%A8/img/linkedHashSet01.png)

**LinkedHashSet特点**

- LinkedHashSet是线程不安全的，如果想保证线程安全，则通过Collections.synchronizedSet。
- LinkedHashSet底层采用LinkedHashMap。
- LinkedHashSet可以保证元素的添加顺序。

**LinkedHashSet构造方法**

```java
public LinkedHashSet(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor, true);
}
public LinkedHashSet(int initialCapacity) {
    super(initialCapacity, .75f, true);
}
public LinkedHashSet() {
    super(16, .75f, true);
}
// 通过调用AbstractCollection中的addAll将元素添加到set中
public LinkedHashSet(Collection<? extends E> c) {
    super(Math.max(2*c.size(), 11), .75f, true);
    addAll(c);
}
// 这四个构造方法是通过HashSet中的这个构造方法来实现的初始化的
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new LinkedHashMap<>(initialCapacity, loadFactor);
}
```




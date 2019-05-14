### Map集合

Map是独立的一个接口，它没有继承自Collection，Map存储的是键值对，包含key和value。

比较key的时候，首先计算出key的hashCode值，找到key对应的hash槽，然后比较值是否相等。注意：equals相等的两个值hashCode一定相等，hashCode相等equals不一定相等。

Map它的实现类主要包括：HashMap(基于Hash的)、LinkedHashMap(基于链表的)、Hashtable()和TreeMap

#### HashMap

HashMap在jdk1.7和1.8发生了很大的变化，1.7之前采用的是数组+链表，1.8采用的是数组+链表+红黑树。本文主要针对jdk1.8来分析，必要时会提到jdk1.7

HashMap的实现不是同步的，所以它不是线程安全的，在jdk1.8之前HashMap多线程会出现死循环问题，后面会说到

HashMap空间占用75%时开始扩容，扩容2倍。

HashMap允许key和value存储null，只存在一组key=null的键值对，但它无法保证存放元素的顺序。

**HashMap成员变量**

```java
// 默认初始容量
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

// 最大容量
static final int MAXIMUM_CAPACITY = 1 << 30;

// 默认加载因子，达到此值将会扩容
static final float DEFAULT_LOAD_FACTOR = 0.75f;

// 当某Hash槽后面的链表长度达到8时，则将链表转换成红黑树
static final int TREEIFY_THRESHOLD = 8;

// 当红黑树的元素个数少于6个时，将红黑树转换成链表
static final int UNTREEIFY_THRESHOLD = 6;

// 当所有的链表节点个数达到64时，将转换成红黑树
static final int MIN_TREEIFY_CAPACITY = 64;

// 存储（桶,一个链表节点称为桶）的数组
// 链表的时候存储Node，红黑树的时候存储TreeNode，其中TreeNode继承自Node
transient Node<K,V>[] table;

/**
 * Holds cached entrySet(). Note that AbstractMap fields are used
 * for keySet() and values().
 */
transient Set<Map.Entry<K,V>> entrySet;

// 当前hashMap中元素的个数
transient int size;

// 记录修改的次数，主要用于在迭代的过程中，突然有别的线程对map进行插入或删除操作。用于快速失败机制。
transient int modCount;

// 临界值 当实际大小 (capacity*load factor)超过临界值时，会进行扩容
int threshold;

// 哈希表的加载因子。
final float loadFactor;
```

**链表接点**

```java
// 当数组后面连接的是链表的时候，存放的是Node
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;
}
```

**TreeNode**

```java
// 当将链表转换成红黑树的时候，存储的是TreeNode
// LinkedHashMap.Entry extends HashMap.Node
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
    TreeNode<K, V> parent;  // 父节点
    TreeNode<K, V> left;
    TreeNode<K, V> right;
    TreeNode<K, V> prev;    // 删除时需要取消下一个链接
    boolean red;
}
```

**构造方法**

```java
// 指定initialCapacity与loadFactor
public HashMap(int initialCapacity, float loadFactor) {
    // 初始容量小于0，抛出异常
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal initial capacity: " +
                                           initialCapacity);
    // 初始容量大于最大值时
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal load factor: " +
                                           loadFactor);
    this.loadFactor = loadFactor;
    this.threshold = tableSizeFor(initialCapacity);
}
// 指定initialCapacity，加载因子使用默认的值0.75
public HashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
}
// all other fields defaulted
public HashMap() {
    this.loadFactor = DEFAULT_LOAD_FACTOR; 
}

// 通过Map构造一个新的HashMap
public HashMap(Map<? extends K, ? extends V> m) {
    this.loadFactor = DEFAULT_LOAD_FACTOR;
    putMapEntries(m, false);
}
```

**Hash算法**

hash算法的目的是通过计算key的哈希值来确定value要存放到的hash槽，HashMap规定了数据长度必须是2的次方。

当数组长度length是2的n次方时，length-1的二进制由n个1组成，相当于只取了hash值在n次位以下的位数，因为当hash值>2^n时，hash值必定可以分解成 （k*(2^n) + 余数），因此如果优化后的操作可以保证只取到n位以下的余数，那么该操作将等价于取模。

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
// 将hashCode低16位于高16位进行异或，目的是加大hash随机性，将高位也加到干扰因素中。
```

**HashMap是如何保证长度是2的次方**

```java
static final int tableSizeFor(int cap) { // 以12为例
    // 是为了保证如果cap本身就是2^k 那么结果也将是其本身。
    int n = cap - 1; // 11    
    n |= n >>> 1; // --> 00001011 | 00000101 = 00001111
    n |= n >>> 2; // --> 00001111 | 00000111 = 00001111
    n |= n >>> 4; // --> 00001111 | 00000000 = 00001111
    n |= n >>> 8; // --> 00001111 | 00000000 = 00001111
    n |= n >>> 16; // --> 00001111 | 00000000 = 00001111
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```

**主要方法**

```java
// 存放元素
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
               boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    // 将table赋值给tab，并判断table是否初始化
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    // table长度与hash与运算，计算存放元素的hash槽，并且如果当前位置没有元素，则直接存储。
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        // 说明存在冲突
        Node<K,V> e; K k;
        // 要插入的元素的hash值与table中的第一个hash值相等，并且值也相等
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        // 如果p是treeNode节点
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        else {
            // 否则，将元素插入到链表的后面
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    // 将链表转换成红黑树
                    if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                        treeifyBin(tab, hash);
                    break;
                }
                // 如果当前元素已经在链表中
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        // e!=null时，说明已经在链表中找到了key相同的元素
        // 根据onlyIfAbsent或者oldValue为null判断是否需要覆盖value
        // put元素时，onlyIfAbsent为false，表示覆盖旧值
        if (e != null) { // existing mapping for key
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            // 用于LinkedHashMap的回调方法，HashMap为空实现
            afterNodeAccess(e);
            return oldValue;
        }
    }
    ++modCount;
    // 判断是否需要扩容
    if (++size > threshold)
        resize();
    afterNodeInsertion(evict);
    return null;
}

// 获取元素
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}
// 传入key的hash值和key
final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    // table不为空 && table的长度大于0 && 查找到table存在该下标
    if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
        // 如果链表第一个元素就是所要查找的，则直接返回
        if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        // 链表存在next
        if ((e = first.next) != null) {
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            // 遍布next，查找满足条件的链表节点
            do {
                if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}

// 扩容
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    if (oldCap > 0) {
        // 如果oldCap已经超过限制最大值，则直接将threshold设置为最大值
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        // 如果当前hash桶数组的长度在扩容后仍然小于最大容量 并且oldCap大于默认值16
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                oldCap >= DEFAULT_INITIAL_CAPACITY)
            newThr = oldThr << 1; // double threshold
    }
    else if (oldThr > 0) // initial capacity was placed in threshold
        newCap = oldThr;
    else {               // zero initial threshold signifies using defaults
        // 如果table长度为0，并且threshold长度为0时，则按默认的初始大小。
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }
    if (newThr == 0) {
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                (int)ft : Integer.MAX_VALUE);
    }
    threshold = newThr;
    @SuppressWarnings({"rawtypes","unchecked"})
    // 创建一个新的大小为threshold 2 倍的空间
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    // 源数组存在元素
    if (oldTab != null) {
        // 下面的过程是将旧数组中的元素复制到新数组中，元素的位置会发生变化 
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                // 如果j位置有节点元素，则取出该元素e，并且把数组中的该位置的node节点赋值null
                oldTab[j] = null;
                // 如果下个节点没有数据了（也就是链表的尾部），则把e,放在newTab位置为e.hash & (newCap - 1) 的地方
                if (e.next == null)
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else { // preserve order
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    do {
                        next = e.next;
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        }
                        else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}
```

#### LinkedHashMap

![](https://github.com/lvCmx/study/blob/master/note/java/java%E5%AE%B9%E5%99%A8/img/linkedHashMap01.png)

前面介绍了HashMap是无序的一个key/value对，而LinkedHashMap是基于双向链表并且保持插入顺序的。

LinkedHashMap的特点：

- 保存插入顺序。
- key/value允许为null
- 线程不安全的
- Key重复会覆盖

**Entity对象**

```java
static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before, after;
    Entry(int hash, K key, V value, Node<K,V> next) {
        super(hash, key, value, next);
    }
}
// 可以看出，它包含before与after两个方向的指针，因此链表是一个双向的链表。
```

**成员变量**

```java
// 链表头节点
transient LinkedHashMap.Entry<K,V> head; 

// 链表尾接点
transient LinkedHashMap.Entry<K,V> tail;

// 指定LinkedHashMap的迭代顺序
// true：则表示按照基于访问的顺序来排列，意思就是最近使用的entry，放在链表的最末尾。
// false：则表示按照插入顺序。
final boolean accessOrder;
// accessOrder的final关键字，说明我们要在构造方法里给它初始化
```

**构造方法**

```java
public LinkedHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
    accessOrder = false;
}
public LinkedHashMap(int initialCapacity) {
    super(initialCapacity);
    accessOrder = false;
}
public LinkedHashMap() {
    super();
    accessOrder = false;
}
public LinkedHashMap(Map<? extends K, ? extends V> m) {
    super();
    accessOrder = false;
    putMapEntries(m, false);
}
// 可以指定accessOrder
public LinkedHashMap(int initialCapacity,
                     float loadFactor,
                     boolean accessOrder) {
    super(initialCapacity, loadFactor);
    this.accessOrder = accessOrder;
}
```

**get方法**

```java
public V get(Object key) {
    Node<K,V> e;
    // 如果没有找到，则返回null
    if ((e = getNode(hash(key), key)) == null)
        return null;
    if (accessOrder)
        afterNodeAccess(e);
    return e.value;
}
final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
        // 首先检查第一个节点是不是要查找的节点。
        if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        // first的下个元素是否是需要寻找的元素。
        if ((e = first.next) != null) {
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            // 循环查找
            do {
                if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
//accessOrder 则表示按照基于访问的顺序来排列，意思就是最近使用的entry，放在链表的最末尾。
// afterNodeAccess(e) 就是基于访问的顺序排列的关键
void afterNodeAccess(HashMap.Node<K,V> e) { // move node to last
    LinkedHashMap.Entry<K,V> last;
    /// 将元素移到最后面，如果最后面一个元素是要移动的元素，则不再需要移动。
    if (accessOrder && (last = tail) != e) {
        // //将e赋值临时节点p， b是e的前一个节点， a是e的后一个节点
        LinkedHashMap.Entry<K,V> p =
                (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        p.after = null;
        // b为空说明 e 前面没有元素
        if (b == null)
            head = a;
        else
            b.after = a;
        // 说明e后面还有元素
        if (a != null)
            a.before = b;
        else
            last = b;
        // 将元素放到最后
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
        tail = p;
        ++modCount;
    }
}
```

**put方法**

```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}

// put使用的是LinkedHashMap中的方法
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
               boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                        treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        if (e != null) { // existing mapping for key
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            // 由LinkedHashMap提供具体实现
            afterNodeAccess(e);
            return oldValue;
        }
    }
    ++modCount;
    if (++size > threshold)
        resize();
    // 由LinkedHashMap提供具体实现
    afterNodeInsertion(evict);
    return null;
}
// LinkedHashMap将其中newNode方法以及之前设置下的钩子方法afterNodeAccess和afterNodeInsertion进行了重写
Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
    // new的Entry是LinkedHashMap自己的双向链表，并且将元素添加到最后面。
    LinkedHashMap.Entry<K,V> p =
        new LinkedHashMap.Entry<K,V>(hash, key, value, e);
    linkNodeLast(p);
    return p;
}
// move node to last
void afterNodeAccess(Node<K,V> e) { 
    LinkedHashMap.Entry<K,V> last;
    if (accessOrder && (last = tail) != e) {
        LinkedHashMap.Entry<K,V> p =
                (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a != null)
            a.before = b;
        else
            last = b;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
        tail = p;
        ++modCount;
    }
}
// 插入后把最老的Entry删除，不过removeEldestEntry总是返回false，所以不会删除，估计又是一个钩子方法给子类用的
void afterNodeInsertion(boolean evict) { // possibly remove eldest
    LinkedHashMap.Entry<K,V> first;
    if (evict && (first = head) != null && removeEldestEntry(first)) {
        K key = first.key;
        removeNode(hash(key), key, null, false, true);
    }
}
```

#### SortedMap与TreeMap

![](https://github.com/lvCmx/study/blob/master/note/java/java%E5%AE%B9%E5%99%A8/img/treeMap01.png)

TreeMap是一个支持排序的HashMap，它存放的key元素需要实现Comparable接口，或者是自定义排序Comparator。


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

// 下一个要调整大小的大小值 (capacity * load factor).
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

// 扩容
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    if (oldCap > 0) {
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                oldCap >= DEFAULT_INITIAL_CAPACITY)
            newThr = oldThr << 1; // double threshold
    }
    else if (oldThr > 0) // initial capacity was placed in threshold
        newCap = oldThr;
    else {               // zero initial threshold signifies using defaults
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
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    if (oldTab != null) {
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
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






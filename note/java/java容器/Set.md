## Set接口及实现类详解

#### 1.Set接口

Set接口继承Collection接口，Set存放元素的特点在于：无序不可重复。常用的Set实现类包括HashSet、LinkedHashSet、TreeSet和SortedSet

#### 2.HashSet

![](F:\__study__\hulianwang\study\note\java\java容器\img\hashset01.png)

**HashSet实现**

HashSet继承了AbstractSet，HashSet存放元素无序并且不可重复，HashSet通过HashMap来实现的

```java
private transient HashMap<E,Object> map;
private static final Object PRESENT = new Object();
public HashSet() {
    map = new HashMap<>();
}
```

添加元素时，将元素当作HashMap的key，value为固定的PRESENT

当向 Set 中添加对象时，首先调用此对象所在类的 hashCode() 方法，计算此对象的哈希值，此哈希值决定了此对象在 Set 中的存储位置。

Set采用HashMap实现，无法通过下标来取元素。只能通过iterator或者将set转换成list取元素。


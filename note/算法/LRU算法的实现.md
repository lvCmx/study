## URL算法

**算法简介**

LRU（least recently used）最近最少使用算法根据数据的历史访问记录进行淘汰数据，其核心思想是如果数据最近被访问过，那么将来被访问的几率也更高。

如果一个数据在最近一段时间没有被访问到，那么在将来它被访问的可能性也很小。

**实现思路**

利用链表和HashMap，当需要插入新的数据项的时候，如果新数据项在链表中存在，则把该节点移到链表头部，如果不存在，则新建一个节点，放到链表头部，若缓存满了。则把链表最后一个节点删除即可，在访问数据的时候，如果数据项在链表中存在，则把该节点移到链表头部，否则返回-1。这样一来在链表尾部的节点就是最近最少未访问的数据项。

**算法实现**

经过上面的分析，其实只需要一个双向链表+一个Hash表。而在java中有已经实现的LinkedHashMap可直接来完成。

```java
public class URLLinkedHashMap {
    int capacity;
    HashMap<Integer, Node> map = new HashMap<Integer, Node>();
    Node head=null;
    Node end=null;
    public URLLinkedHashMap(int capacity) {
        this.capacity = capacity;
    }

    public int get(int key) {
        if(map.containsKey(key)){
            Node n = map.get(key);
            remove(n);
            setHead(n);
            return n.value;
        }

        return -1;
    }

    public void remove(Node n){
        if(n.pre!=null){
            n.pre.next = n.next;
        }else{
            head = n.next;
        }

        if(n.next!=null){
            n.next.pre = n.pre;
        }else{
            end = n.pre;
        }

    }

    public void setHead(Node n){
        n.next = head;
        n.pre = null;

        if(head!=null)
            head.pre = n;

        head = n;

        if(end ==null)
            end = head;
    }
	// 设置元素，如果存在，则将它移到前面。
    public void set(int key, int value) {
        if(map.containsKey(key)){
            Node old = map.get(key);
            old.value = value;
            remove(old);
            setHead(old);
        }else{
            Node created = new Node(key, value);
            if(map.size()>=capacity){
                map.remove(end.key);
                remove(end);
                setHead(created);

            }else{
                setHead(created);
            }

            map.put(key, created);
        }
    }
}
class Node{
    int key;
    int value;
    Node pre;
    Node next;

    public Node(int key, int value){
        this.key = key;
        this.value = value;
    }
}
```

**LRU算法在项目中的应用**

例如在业务中，对用户信息的查询频率很高，需要做一定的缓存技术，可以使用LRU算法达到效果。

在Redis中也使用到了LRU算法的思想。
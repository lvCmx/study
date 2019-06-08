## 关于Redis的面试题

​							————持续更新内容

<a href="#1">1.在项目中缓存是如何使用的？缓存如果使用不当会造成什么后果？</a>  
<a href="#2">2.来聊聊redis的线程模型吧？为啥单线程还能有很高的效率？</a>  
<a href="#3">3.redis都有哪些数据类型？分别在哪些场景下使用比较合适呢？</a>  
<a href="#4">4.redis的过期策略能介绍一下？要不你再手写一个LRU？</a>  
<a href="#5">5.怎么保证redis是高并发以及高可用的？</a>    
<a  href="#6">6.redis内部结构实现原理</a>     
<a  href="#7">7.redis实现分布式锁</a>       
<a href="#8">8.你能说说我们一般如何应对缓存雪崩以及穿透问题吗？</a>  
<a href="#9">9.如何保证缓存与数据库双写时的数据一致性？</a>  
<a href="#10">10.你能说说redis的并发竞争问题该如何解决吗？</a>


## <a name="1">1.在项目中缓存是如何使用的？缓存如果使用不当会造成什么后果？</a>

### 为啥在项目里要用缓存呢？

用缓存，主要是两个用途：高性能和高并发

**1）高性能** 

假设这么个场景，你有个操作，一个请求过来，后台需要操作mysql，半天查出来一个结果，耗时600ms。但是这个结果可能接下来几个小时都不会变了，或者变了也可以不用立即反馈给用户，那么此时咋办？

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/缓存实现高性能.png)

缓存啊，折腾600ms查出来的结果，扔缓存里，一个key对应一个value，下次再有人查，别走mysql折腾600ms了。直接从缓存里，通过一个key查出来一个value，2ms搞定。性能提升300倍。

这就是所谓的高性能。就是把你一些复杂操作耗时查出来的结果，如果确定后面不咋变了，然后但是马上还有很多读请求，那么直接结果放缓存，后面直接读缓存就好了。

**2）高并发**

Mysql数据是落到磁盘中的，而Redis数据是暂存在内存中的，所以Redis可以支持高并发。

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/redis高并发.png)

所以要是你有个系统，高峰期一秒钟过来的请求有1万，那一个mysql单机绝对会死掉。你这个时候就只能上缓存，把很多数据放缓存，别放mysql。缓存功能简单，说白了就是key-value式操作，单机支撑的并发量轻松一秒几万十几万，支撑高并发so easy。单机承载并发量是mysql单机的几十倍。

所以你要结合这俩场景考虑一下，你为啥要用缓存？ 

一般很多同学项目里没啥高并发场景，那就别折腾了，直接用高性能那个场景吧，就思考有没有可以缓存结果的复杂查询场景，后续可以大幅度提升性能，优化用户体验，有，就说这个理由，没有？？那你也得编一个出来吧，不然你不是在搞笑么

### 用了缓存之后会有啥不良的后果？

呵呵。。。你要是没考虑过这个问题，那你就尴尬了，面试官会觉得你头脑简单，四肢也不发达。你别光是傻用一个东西，多考虑考虑背后的一些事儿。

常见的缓存问题有四个（当然其实有很多，我这里就说仨，你能说出来也可以了）

1. 缓存与数据库双写不一致
2. 缓存雪崩
3. 缓存穿透
4. 缓存并发竞争

## <a name="2">2.来聊聊redis的线程模型吧？为啥单线程还能有很高的效率？</a>

### redis与mamcached的区别？

1）Redis支持服务器端的数据操作：Redis相比Memcached来说，拥有更多的数据结构和并支持更丰富的数据操作，通常在Memcached里，你需要将数据拿到客户端来进行类似的修改再set回去。这大大增加了网络IO的次数和数据体积。在Redis中，这些复杂的操作通常和一般的GET/SET一样高效。所以，如果需要缓存能够支持更复杂的结构和操作，那么Redis会是不错的选择。 

2）内存使用效率对比：使用简单的key-value存储的话，Memcached的内存利用率更高，而如果Redis采用hash结构来做key-value存储，由于其组合式的压缩，其内存利用率会高于Memcached。

3）性能对比：由于Redis只使用单核，而Memcached可以使用多核，所以平均每一个核上Redis在存储小数据时比Memcached性能更高。而在100k以上的数据中，Memcached性能要高于Redis，虽然Redis最近也在存储大数据的性能上进行优化，但是比起Memcached，还是稍有逊色。

 4）集群模式：memcached没有原生的集群模式，需要依靠客户端来实现往集群中分片写入数据；但是redis目前是原生支持cluster模式的，redis官方就是支持redis cluster集群模式的，比memcached来说要更好

### redis的单线程模型

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/01_redis单线程模型.jpg)

**1).文件事件处理器**

​	redis基于reactor模式开发了网络事件处理器，这个处理器叫做文件事件处理器，file event handler。这个文件事件处理器，是单线程的，redis才叫做单线程的模型，采用IO多路复用机制同时监听多个socket，根据socket上的事件来选择对应的事件处理器来处理这个事件。  
​	如果被监听的socket准备好执行accept、read、write、close等操作的时候，跟操作对应的文件事件就会产生，这个时候文件事件处理器就会调用之前关联好的事件处理器来处理这个事件。  
​	文件事件处理器是单线程模式运行的，但是通过IO多路复用机制监听多个socket，可以实现高性能的网络通信模型，又可以跟内部其他单线程的模块进行对接，保证了redis内部的线程模型的简单性。  
​	文件事件处理器的结构包含4个部分：多个socket，IO多路复用程序，文件事件分派器，事件处理器（命令请求处理器、命令回复处理器、连接应答处理器，等等）。  
​	多个socket可能并发的产生不同的操作，每个操作对应不同的文件事件，但是IO多路复用程序会监听多个socket，但是会将socket放入一个队列中排队，每次从队列中取出一个socket给事件分派器，事件分派器把socket给对应的事件处理器。  
​	然后一个socket的事件处理完之后，IO多路复用程序才会将队列中的下一个socket给事件分派器。文件事件分派器会根据每个socket当前产生的事件，来选择对应的事件处理器来处理。

**2).文件事件**

​	当socket变得可读时（比如客户端对redis执行write操作，或者close操作），或者有新的可以应答的socket出现时（客户端对redis执行connect操作），socket就会产生一个AE_READABLE事件。  
​	当socket变得可写的时候（客户端对redis执行read操作），socket会产生一个AE_WRITABLE事件。
​	IO多路复用程序可以同时监听AE_REABLE和AE_WRITABLE两种事件，要是一个socket同时产生了AE_READABLE和AE_WRITABLE两种事件，那么文件事件分派器优先处理AE_REABLE事件，然后才是AE_WRITABLE事件。

**3).文件事件处理器**  

如果是客户端要连接redis，那么会为socket关联连接应答处理器  
如果是客户端要写数据到redis，那么会为socket关联命令请求处理器  
如果是客户端要从redis读数据，那么会为socket关联命令回复处理器

**4).客户端与redis通信的一次流程**

​	在redis启动初始化的时候，redis会将连接应答处理器跟AE_READABLE事件关联起来，接着如果一个客户端跟redis发起连接，此时会产生一个AE_READABLE事件，然后由连接应答处理器来处理跟客户端建立连接，创建客户端对应的socket，同时将这个socket的AE_READABLE事件跟命令请求处理器关联起来。  
​	当客户端向redis发起请求的时候（不管是读请求还是写请求，都一样），首先就会在socket产生一个AE_READABLE事件，然后由对应的命令请求处理器来处理。这个命令请求处理器就会从socket中读取请求相关数据，然后进行执行和处理。  
​	接着redis这边准备好了给客户端的响应数据之后，就会将socket的AE_WRITABLE事件跟命令回复处理器关联起来，当客户端这边准备好读取响应数据时，就会在socket上产生一个AE_WRITABLE事件，会由对应的命令回复处理器来处理，就是将准备好的响应数据写入socket，供客户端来读取。  
​	命令回复处理器写完之后，就会删除这个socket的AE_WRITABLE事件和命令回复处理器的关联关系。

### 为啥redis单线程模型也能效率这么高？

1. 纯内存操作
2. 核心是基于非阻塞的IO多路复用机制
3. 单线程反而避免了多线程的频繁上下文切换问题，因为是单线程，所以没有线程切换资源消耗。

线程是由CPU进行调度的，CPU的一个时间片内只执行一个线程上下文内的线程，当CPU由执行线程A切换到执行线程B的过程中会发生一些列的操作，这些操作主要有”保存线程A的执行现场“然后”载入线程B的执行现场”，这个过程称之为“上下文切换（context switch）”,这个上下文切换过程并不廉价，如果没有必要，应该尽量减少上下文切换的发生。

## <a name="3">3.redis都有哪些数据类型？分别在哪些场景下使用比较合适呢？</a>

**（1）string**

这是最基本的类型了，没啥可说的，就是普通的set和get，做简单的kv缓存。


**（2）hash**

这个是类似map的一种结构，这个一般就是可以将结构化的数据，比如一个对象（前提是这个对象没嵌套其他的对象）给缓存在redis里，然后每次读写缓存的时候，可以就操作hash里的某个字段。   
key=150  
value={  
  “id”: 150,  
  “name”: “zhangsan”,  
  “age”: 20  
}

hash类的数据结构，主要是用来存放一些对象，把一些简单的对象给缓存起来，后续操作的时候，你可以直接仅仅修改这个对象中的某个字段的值

**（3）list**

微博，某个大v的粉丝，就可以以list的格式放在redis里去缓存    key=某大v     value=[zhangsan, lisi, wangwu]

 比如可以通过list存储一些列表型的数据结构，类似粉丝列表了、文章的评论列表了之类的东西

 比如可以通过lrange命令，就是从某个元素开始读取多少个元素，可以基于list实现分页查询，这个很棒的一个功能，基于redis实现简单的高性能分页，可以做类似微博那种下拉不断分页的东西，性能高，就一页一页走

 比如可以搞个简单的消息队列，从list头怼进去，从list尾巴那里弄出来

**（4）set**

直接基于set将系统里需要去重的数据扔进去，自动就给去重了，如果你需要对一些数据进行快速的全局去重，你当然也可以基于jvm内存里的HashSet进行去重，但是如果你的某个系统部署在多台机器上呢？得基于redis进行全局的set去重

**（5）sorted set**

sorted set内部基于跳表结构

 排序的set，去重但是可以排序，写进去的时候给一个分数，自动根据分数排序，这个可以玩儿很多的花样，最大的特点是有个分数可以自定义排序规则

 比如说你要是想根据时间对数据排序，那么可以写入进去的时候用某个时间作为分数，人家自动给你按照时间排序了

 排行榜：将每个用户以及其对应的什么分数写入进去，zadd board score username，接着zrevrange board 0 99，就可以获取排名前100的用户；zrank board username，可以看到用户在排行榜里的排名

## <a name="4">4.redis的过期策略能介绍一下？要不你再手写一个LRU？</a>

### （1）设置过期时间

我们set key的时候，都可以给一个expire time，就是过期时间，指定这个key比如说只能存活1个小时？10分钟？这个很有用，我们自己可以指定缓存到期就失效

如果假设你设置一个一批key只能存活1个小时，那么接下来1小时后，redis是怎么对这批key进行删除的？  
答案是：定期删除+惰性删除

**定期删除**

​	所谓定期删除，指的是redis默认是每隔100ms就随机抽取一些设置了过期时间的key，检查其是否过期，如果过期就删除。假设redis里放了10万个key，都设置了过期时间，你每隔几百毫秒，就检查10万个key，那redis基本上就死了，cpu负载会很高的，消耗在你的检查过期key上了。注意，这里可不是每隔100ms就遍历所有的设置过期时间的key，那样就是一场性能上的灾难。实际上redis是每隔100ms随机抽取一些key来检查和删除的。

**惰性删除**

但是问题是，定期删除可能会导致很多过期key到了时间并没有被删除掉，那咋整呢？所以就是惰性删除了。这就是说，在你获取某个key的时候，redis会检查一下 ，这个key如果设置了过期时间那么是否过期了？如果过期了此时就会删除，不会给你返回任何东西。并不是key到时间就被删除掉，而是你查询这个key的时候，redis再懒惰的检查一下

通过上述两种手段结合起来，保证过期的key一定会被干掉。

很简单，就是说，你的过期key，靠定期删除没有被删除掉，还停留在内存里，占用着你的内存呢，除非你的系统去查一下那个key，才会被redis给删除掉。

### （2）内存淘汰

但是实际上这还是有问题的，如果定期删除漏掉了很多过期key，然后你也没及时去查，也就没走惰性删除，此时会怎么样？如果大量过期key堆积在内存里，导致redis内存块耗尽了，咋整？

答案是：走内存淘汰机制。

如果redis的内存占用过多的时候，此时会进行内存淘汰，有如下一些策略：redis 10个key，现在已经满了，redis需要删除掉5个key  
1个key，最近1分钟被查询了100次  
1个key，最近10分钟被查询了50次  
1个key，最近1个小时倍查询了1次 

- noeviction：当内存不足以容纳新写入数据时，新写入操作会报错，这个一般没人用吧，实在是太恶心了
- allkeys-lru：当内存不足以容纳新写入数据时，在键空间中，移除最近最少使用的key（这个是最常用的）
- allkeys-random：当内存不足以容纳新写入数据时，在键空间中，随机移除某个key，这个一般没人用吧，为啥要随机，肯定是把最近最少使用的key给干掉啊
- volatile-lru：当内存不足以容纳新写入数据时，在设置了过期时间的键空间中，移除最近最少使用的key（这个一般不太合适）
- volatile-random：当内存不足以容纳新写入数据时，在设置了过期时间的键空间中，随机移除某个key
- volatile-ttl：当内存不足以容纳新写入数据时，在设置了过期时间的键空间中，有更早过期时间的key优先移除

### 手写一个LRU

```java
public class LRUCache<K, V> {

    private int currentCacheSize;
    private int CacheCapcity;
    private HashMap<K,CacheNode> caches;
    private CacheNode first;
    private CacheNode last;

    public LRUCache(int size){
        currentCacheSize = 0;
        this.CacheCapcity = size;
        caches = new HashMap<K,CacheNode>(size);
    }

    public void put(K k,V v){
        CacheNode node = caches.get(k);
        if(node == null){
            if(caches.size() >= CacheCapcity){
                caches.remove(last.key);
                removeLast();
            }
            node = new CacheNode();
            node.key = k;
        }
        node.value = v;
        moveToFirst(node);
        caches.put(k, node);
    }

    public Object  get(K k){
        CacheNode node = caches.get(k);
        if(node == null){
            return null;
        }
        moveToFirst(node);
        return node.value;
    }

    public Object remove(K k){
        CacheNode node = caches.get(k);
        if(node != null){
            if(node.pre != null){
                node.pre.next=node.next;
            }
            if(node.next != null){
                node.next.pre=node.pre;
            }
            if(node == first){
                first = node.next;
            }
            if(node == last){
                last = node.pre;
            }
        }
        return caches.remove(k);
    }

    public void clear(){
        first = null;
        last = null;
        caches.clear();
    }

    private void moveToFirst(CacheNode node){
        if(first == node){
            return;
        }
        if(node.next != null){
            node.next.pre = node.pre;
        }
        if(node.pre != null){
            node.pre.next = node.next;
        }
        if(node == last){
            last= last.pre;
        }
        if(first == null || last == null){
            first = last = node;
            return;
        }
        node.next=first;
        first.pre = node;
        first = node;
        first.pre=null;
    }

    private void removeLast(){
        if(last != null){
            last = last.pre;
            if(last == null){
                first = null;
            }else{
                last.next = null;
            }
        }
    }
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        CacheNode node = first;
        while(node != null){
            sb.append(String.format("%s:%s ", node.key,node.value));
            node = node.next;
        }
        return sb.toString();
    }
    class CacheNode{
        CacheNode pre;
        CacheNode next;
        Object key;
        Object value;
        public CacheNode(){}
    }

    public static void main(String[] args) {
        LRUCache<Integer,String> lru = new LRUCache<Integer,String>(3);
        lru.put(1, "a");    // 1:a
        System.out.println(lru.toString());
        lru.put(2, "b");    // 2:b 1:a
        System.out.println(lru.toString());
        lru.put(3, "c");    // 3:c 2:b 1:a
        System.out.println(lru.toString());
        lru.put(4, "d");    // 4:d 3:c 2:b
        System.out.println(lru.toString());
        lru.put(1, "aa");   // 1:aa 4:d 3:c
        System.out.println(lru.toString());
        lru.put(2, "bb");   // 2:bb 1:aa 4:d
        System.out.println(lru.toString());
        lru.put(5, "e");    // 5:e 2:bb 1:aa
        System.out.println(lru.toString());
        lru.get(1);         // 1:aa 5:e 2:bb
        System.out.println(lru.toString());
        lru.remove(11);     // 1:aa 5:e 2:bb
        System.out.println(lru.toString());
        lru.remove(1);      //5:e 2:bb
        System.out.println(lru.toString());
        lru.put(1, "aaa");  //1:aaa 5:e 2:bb
        System.out.println(lru.toString());
    }
}
```

## <a name="5">5.怎么保证redis是高并发以及高可用的？</a>

### redis如何通过读写分离来承载读请求QPS超过10万+？

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/redis集群结构.png)

redis主从架构 -> 读写分离架构 -> 可支持水平扩展的读高并发架构

### redis的持久化

由于Redis的数据都存放在内存中，如果没有配置持久化，redis重启后数据就全丢失了，于是需要开启redis的持久化功能，将数据保存到磁 盘上，当redis重启后，可以从磁盘中恢复数据。redis提供两种方式进行持久化：  
	一种是RDB持久化（原理是将Reids在内存中的数据库记录定时 dump到磁盘上的RDB持久化），  
	另外一种是AOF（append only file）持久化（原理是将Reids的操作日志以追加的方式写入文件）。

**二者的区别**  
	RDB持久化是指在指定的时间间隔内将内存中的数据集快照写入磁盘，实际操作过程是fork一个子进程，先将数据集写入临时文件，写入成功后，再替换之前的文件，用二进制压缩存储。  
	AOF持久化以日志的形式记录服务器所处理的每一个写、删除操作，查询操作不会记录，以文本的方式记录，可以打开文件看到详细的操作记录。

**RDB的特点**  
	1). 一旦采用该方式，那么你的整个Redis数据库将只包含一个文件，这对于文件备份而言是非常完美的。比如，你可能打算每个小时归档一次最近24小时的数 据，同时还要每天归档一次最近30天的数据。通过这样的备份策略，一旦系统出现灾难性故障，我们可以非常容易的进行恢复。  
	2). 对于灾难恢复而言，RDB是非常不错的选择。因为我们可以非常轻松的将一个单独的文件压缩后再转移到其它存储介质上。   
	3). 性能最大化。对于Redis的服务进程而言，在开始持久化时，它唯一需要做的只是fork出子进程，之后再由子进程完成这些持久化的工作，这样就可以极大的避免服务进程执行IO操作了  
	4). 相比于AOF机制，如果数据集很大，RDB的启动效率会更高。

RDB又存在哪些劣势呢？  
	1). 如果你想保证数据的高可用性，即最大限度的避免数据丢失，那么RDB将不是一个很好的选择。因为系统一旦在定时持久化之前出现宕机现象，此前没有来得及写入磁盘的数据都将丢失。  
	2). 由于RDB是通过fork子进程来协助完成数据持久化工作的，因此，如果当数据集较大时，可能会导致整个服务器停止服务几百毫秒，甚至是1秒钟。

**AOF的优势有哪些呢？**  
	1). 该机制可以带来更高的数据安全性，即数据持久性。Redis中提供了3中同步策略，即每秒同步、每修改同步和不同步。事实上，每秒同步也是异步完成的，其 效率也是非常高的，所差的是一旦系统出现宕机现象，那么这一秒钟之内修改的数据将会丢失。而每修改同步，我们可以将其视为同步持久化，即每次发生的数据变 化都会被立即记录到磁盘中。可以预见，这种方式在效率上是最低的。至于无同步，无需多言，我想大家都能正确的理解它。  
	2). 由于该机制对日志文件的写入操作采用的是append模式，因此在写入过程中即使出现宕机现象，也不会破坏日志文件中已经存在的内容。然而如果我们本次操 作只是写入了一半数据就出现了系统崩溃问题，不用担心，在Redis下一次启动之前，我们可以通过redis-check-aof工具来帮助我们解决数据 一致性的问题。  
	3). 如果日志过大，Redis可以自动启用rewrite机制。即Redis以append模式不断的将修改数据写入到老的磁盘文件中，同时Redis还会创 建一个新的文件用于记录此期间有哪些修改命令被执行。因此在进行rewrite切换时可以更好的保证数据安全性。  
	4). AOF包含一个格式清晰、易于理解的日志文件用于记录所有的修改操作。事实上，我们也可以通过该文件完成数据的重建。

AOF的劣势有哪些呢？  
	1). 对于相同数量的数据集而言，AOF文件通常要大于RDB文件。RDB 在恢复大数据集时的速度比 AOF 的恢复速度要快。  
	2). 根据同步策略的不同，AOF在运行效率上往往会慢于RDB。总之，每秒同步策略的效率是比较高的，同步禁用策略的效率和RDB一样高效。

二者选择的标准，就是看系统是愿意牺牲一些性能，换取更高的缓存一致性（aof），还是愿意写操作频繁的时候，不启用备份来换取更高的性能，待手动运行save的时候，再做备份（rdb）。

**如何配置**

RDB：  
save 900 1              #在900秒(15分钟)之后，如果至少有1个key发生变化，则dump内存快照。  
save 300 10            #在300秒(5分钟)之后，如果至少有10个key发生变化，则dump内存快照。  
save 60 10000        #在60秒(1分钟)之后，如果至少有10000个key发生变化，则dump内存快照。

AOF：  
appendfsync always     #每次有数据修改发生时都会写入AOF文件。   
appendfsync everysec  #每秒钟同步一次，该策略为AOF的缺省策略。  
appendfsync no          #从不同步。高效但是数据不会被持久化。

### redis replication以及master持久化对主从架构的安全意义

**图解redis replication基本原理**

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/redis_replication.png)

**redis replication的核心机制**

1. redis采用异步的方式复制数据到slave节点，不过redis2.8开始，slave node会周期性地确认自己每次复制的数据量。
2. 一个master node是可以配置多个slave node的。
3. slave node也可以连接其他的slave node。
4. slave node做复制的时候，是不会block master node的正常工作的。
5. slave node在做复制的时候，也不会block对自己的查询操作，它会用旧的数据集来提供服务；但是复制完成的时候，需要删除旧数据集，加载新数据集，这个时候就会暂停对外服务了。
6. slave node主要用来进行横向扩容，做读写分离，扩容的slave node可以提高读的吞吐量。

**master持久化对于主从架构安全的保障的意义**

如果采用了主从架构，那么建议必须开启master node的持久化！  
不建议用slave node作为master node的数据热备，因为那样的话，如果你关掉master的持久化，可能在master宕机重启的时候数据是空的，然后可能一经过复制，salve node数据也丢了。master节点，必须要使用持久化机制  

### redis主从复制原理、断点续传、无磁盘化复制、过期key处理

**主从架构的核心原理**

当启动一个slave node的时候，它会发送一个PSYNC命令给master node

如果这是slave node重新连接master node，那么master node仅仅会复制给slave部分缺少的数据; 否则如果是slave node第一次连接master node，那么会触发一次full resynchronization

开始full resynchronization的时候，master会启动一个后台线程，开始生成一份RDB快照文件，同时还会将从客户端收到的所有写命令缓存在内存中。RDB文件生成完毕之后，master会将这个RDB发送给slave，slave会先写入本地磁盘，然后再从本地磁盘加载到内存中。然后master会将内存中缓存的写命令发送给slave，slave也会同步这些数据。

**主从复制的断点续传**

从redis 2.8开始，就支持主从复制的断点续传，如果主从复制过程中，网络连接断掉了，那么可以接着上次复制的地方，继续复制下去，而不是从头开始复制一份

master node会在内存中常见一个backlog，master和slave都会保存一个replica offset还有一个master id，offset就是保存在backlog中的。如果master和slave网络连接断掉了，slave会让master从上次的replica offset开始继续复制

但是如果没有找到对应的offset，那么就会执行一次resynchronization

**无磁盘化复制**

master在内存中直接创建rdb，然后发送给slave，不会在自己本地落地磁盘了

repl-diskless-sync
repl-diskless-sync-delay，等待一定时长再开始复制，因为要等更多slave重新连接过来

**过期key处理**

slave不会过期key，只会等待master过期key。如果master过期了一个key，或者通过LRU淘汰了一个key，那么会模拟一条del命令发送给slave。

### redis replication的完整流运行程和原理的再次深入剖析

**复制的完整流程**

（1）slave node启动，仅仅保存master node的信息，包括master node的host和ip，但是复制流程没开始。master host和ip是从哪儿来的，redis.conf里面的slaveof配置的

（2）slave node内部有个定时任务，每秒检查是否有新的master node要连接和复制，如果发现，就跟master node建立socket网络连接
（3）slave node发送ping命令给master node
（4）口令认证，如果master设置了requirepass，那么salve node必须发送masterauth的口令过去进行认证
（5）master node第一次执行全量复制，将所有数据发给slave node
（6）master node后续持续将写命令，异步复制给slave node

**数据同步相关的核心机制**

指的就是第一次slave连接msater的时候，执行的全量复制，那个过程里面你的一些细节的机制

（1）master和slave都会维护一个offset

master会在自身不断累加offset，slave也会在自身不断累加offset
slave每秒都会上报自己的offset给master，同时master也会保存每个slave的offset

这个倒不是说特定就用在全量复制的，主要是master和slave都要知道各自的数据的offset，才能知道互相之间的数据不一致的情况

（2）backlog

master node有一个backlog，默认是1MB大小
master node给slave node复制数据时，也会将数据在backlog中同步写一份
backlog主要是用来做全量复制中断候的增量复制的

（3）master run id

info server，可以看到master run id
如果根据host+ip定位master node，是不靠谱的，如果master node重启或者数据出现了变化，那么slave node应该根据不同的run id区分，run id不同就做全量复制
如果需要不更改run id重启redis，可以使用redis-cli debug reload命令

（4）psync

从节点使用psync从master node进行复制，psync runid offset
master node会根据自身的情况返回响应信息，可能是FULLRESYNC runid offset触发全量复制，可能是CONTINUE触发增量复制

**全量复制**

（1）master执行bgsave，在本地生成一份rdb快照文件
（2）master node将rdb快照文件发送给salve node，如果rdb复制时间超过60秒（repl-timeout），那么slave node就会认为复制失败，可以适当调节大这个参数
（3）对于千兆网卡的机器，一般每秒传输100MB，6G文件，很可能超过60s
（4）master node在生成rdb时，会将所有新的写命令缓存在内存中，在salve node保存了rdb之后，再将新的写命令复制给salve node
（5）client-output-buffer-limit slave 256MB 64MB 60，如果在复制期间，内存缓冲区持续消耗超过64MB，或者一次性超过256MB，那么停止复制，复制失败
（6）slave node接收到rdb之后，清空自己的旧数据，然后重新加载rdb到自己的内存中，同时基于旧的数据版本对外提供服务
（7）如果slave node开启了AOF，那么会立即执行BGREWRITEAOF，重写AOF

rdb生成、rdb通过网络拷贝、slave旧数据的清理、slave aof rewrite，很耗费时间

如果复制的数据量在4G~6G之间，那么很可能全量复制时间消耗到1分半到2分钟

**增量复制**

（1）如果全量复制过程中，master-slave网络连接断掉，那么salve重新连接master时，会触发增量复制
（2）master直接从自己的backlog中获取部分丢失的数据，发送给slave node，默认backlog就是1MB
（3）msater就是根据slave发送的psync中的offset来从backlog中获取数据的

**heartbeat**

主从节点互相都会发送heartbeat信息

master默认每隔10秒发送一次heartbeat，salve node每隔1秒发送一个heartbeat

**异步复制**

master每次接收到写命令之后，现在内部写入数据，然后异步发送给slave node

### 哨兵架构的相关基础知识的讲解





## <a name="6">6.redis内部结构实现原理</a>

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/redis_jiegou.png)

**string**

如果一个字符串的内容可以转换为long，那么该字符串就会被转换成为long类型，对象的ptr就会指向该long，并且对象类型也用int类型表示。  
普通的字符串有两种，embstr和raw。embstr应该是Redis 3.0新增的数据结构,在2.8中是没有的。如果字符串对象的长度小于39字节，就用embstr对象。否则用传统的raw对象。可以从下面这段代码看出：

```c
#define REDIS_ENCODING_EMBSTR_SIZE_LIMIT 39
robj *createStringObject(char *ptr, size_t len) {
if (len <= REDIS_ENCODING_EMBSTR_SIZE_LIMIT)
    return createEmbeddedStringObject(ptr,len);
else
    return createRawStringObject(ptr,len);
}
```

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/redis_str.png)

embstr的好处有如下几点：

- embstr的创建只需分配一次内存，而raw为两次（一次为sds分配对象，另一次为objet分配对象，embstr省去了第一次）。
- 相对地，释放内存的次数也由两次变为一次。
- embstr的objet和sds放在一起，更好地利用缓存带来的优势。

**list**

***linkedList：***

链表提供了节点重排以及节点顺序访问的能力，redis中的列表对象主要是由压缩列表和双端链表实现的，其定义结构如下：  

```c
type struct list{
    //表头节点
    listNode *head;
    //表尾节点
    listNode *tail;
    //包含的节点总数
    unsigned long len;
    //一些操作函数 dup free match...
};
```

其中每个listNode，都包含一个pre指针和next指针，并且包含有value则是列表对象的具体值。

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/redis_list.png)

***ziplist***

一个普通的双向链表，链表中每一项都占用独立的一块内存，各项之间用地址指针（或引用）连接起来。这种方式会带来大量的内存碎片，而且地址指针也会占用额外的内存。而ziplist却是将表中每一项存放在前后连续的地址空间内，一个ziplist整体占用一大块内存。它是一个表（list），但其实不是一个链表（linked list）。

```c
type struct ziplist{
    //整个压缩列表的字节数
    uint32_t zlbytes;
    //记录压缩列表尾节点到头结点的字节数，直接可以求节点的地址
    uint32_t zltail_offset;
    //记录了节点数，有多种类型，默认如下
    uint16_t zllength;
    //节点列表节点
    entryX;
}
//<zlbytes><zltail><zllen><entry>...<entry><zlend>
```

- `<zlbytes>`: 32bit，表示ziplist占用的字节总数（也包括`<zlbytes>`本身占用的4个字节）。
- `<zltail>`: 32bit，表示ziplist表中最后一项（entry）在ziplist中的偏移字节数。`<zltail>`的存在，使得我们可以很方便地找到最后一项（不用遍历整个ziplist），从而可以在ziplist尾端快速地执行push或pop操作。
- `<zllen>`: 16bit， 表示ziplist中数据项（entry）的个数。zllen字段因为只有16bit，所以可以表达的最大值为2^16-1。这里需要特别注意的是，如果ziplist中数据项个数超过了16bit能表达的最大值，ziplist仍然可以来表示。那怎么表示呢？这里做了这样的规定：如果`<zllen>`小于等于2^16-2（也就是不等于2^16-1），那么`<zllen>`就表示ziplist中数据项的个数；否则，也就是`<zllen>`等于16bit全为1的情况，那么`<zllen>`就不表示数据项个数了，这时候要想知道ziplist中数据项总数，那么必须对ziplist从头到尾遍历各个数据项，才能计数出来。
- `<entry>`: 表示真正存放数据的数据项，长度不定。一个数据项（entry）也有它自己的内部结构，这个稍后再解释。
- `<zlend>`: ziplist最后1个字节，是一个结束标记，值固定等于255。

<entry>的组成：<prevrawlen><len><data>

我们看到在真正的数据（`<data>`）前面，还有两个字段：

- `<prevrawlen>`: 表示前一个数据项占用的总字节数。这个字段的用处是为了让ziplist能够从后向前遍历（从后一项的位置，只需向前偏移prevrawlen个字节，就找到了前一项）。这个字段采用变长编码。
- `<len>`: 表示当前数据项的数据长度（即`<data>`部分的长度）。也采用变长编码。

***总结：***

当列表对象所存储的字符串元素长度小于64字节并且元素数量小于512个时，使用ziplist编码，否则使用linkedlist编码。

**set**

当集合对象所保存的元素都是整数值且元素数量不超过512个时，使用intset编码，否则使用hashtable编码

***intset：***

整数集合结构定义：

```c
typedef struct intset{
    //编码方式
    uint32_t encoding;
    //元素数量
    uint32_t length;
    //存储元素的数组
    int8_t contents[];
}
```

整数集合的每个元素都是contents数组的一个数组项，各个项在数组中按值得大小从小到大有序排列，并且不包含重复的项。contents数组中元素的类型由encoding决定，当新加入元素之时，如果元素的编码大于contents是数组的编码，则会将所有元素的编码升级为新加入元素的编码，然后再插入。编码不会发生降级。 

***hashtable：***

字典定义：

```c
typedef struct dict{
    //类型特定函数
    dictType *type;
    //哈希表 两个，一个用于实时存储，一个用于rehash
    dictht ht[2];
    //rehash索引 数据迁移时使用
    unsigned rehashidx;
}
```

哈希表结构：

```c
typedef struct dictht{
    //哈希表数组
    dictEntry**table;
    //哈希表大小
    unsigned long size;
    //哈希表掩码，总是等于size-1，存储时计算索引值
    unsigned long sizemask;
    //已有元素数量
    unsigned long used;
}
```

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/redis_hash_table.png)

其中键值对都保存在节点dictEntity之中，并且通过拉链法解决哈希冲突，存储时通过MurmurHash算法来计算键的哈希值，能够更好的提供随机分布性且速度也快，扩容时采用渐进式的rehash，采用分而治之的方法，通过改变rehashidx的值，来一个个将元素移动到ht[1]中，完成以后将ht[1]变为ht[0]，原先的ht[0]变为ht[1]，同时将redhashidx置为-1。

**hash**

哈希对象的编码可以是压缩列表（ziplist）或者字典（hashtable），当哈希对象保存的所有键值对的键和值得长度都小于64字节并且元素数量小于512个时使用ziplist，否则使用hashtable。使用ziplist时，是依次将键和值压入链表之中，两者相邻。使用hashtable是是将键值对存于dictEntry之中。

**zset**

有序集合的编码可以是压缩列表(ziplist)或者跳跃表(skiplist)。当元素数量小于128个并且所有元素成员的长度都小于64字节之时使用ziplist编码，否则使用skiplist编码

有关skiplist的数据结构，参考：https://blog.csdn.net/yellowriver007/article/details/79021103

## <a name="7">7.redis如何实现分布式锁?</a>

### 分布式锁的特性

分布式锁有4个重要的考量标准：

- 互斥性：在任意时刻，只有一个客户端能持有锁。
- 不会发生死锁：即使有一个客户端在持有锁的期间崩溃而没有主动解锁，也能保证后续其他客户端能加锁。
- 具有容错性：只要大部分的redis节点正常运行，客户端就可以加锁和解锁。
- 解铃还须系铃人：加锁和解锁必须是同一个客户端，客户端自己不能把别人加的锁给解了。

### redis单节点实现分布式锁

单节点加锁时，不需要考虑太多因素。  

```java
public static boolean tryGetDistributedLock(Jedis jedis, String lockKey, String requestId, int expireTime) {
    String result = jedis.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
    if (LOCK_SUCCESS.equals(result)) {
        return true;
    }
    return false;
}
```

命令行的方式：set mylock 随机值 nx px 30000

- 第一个为key,我们使用key来当锁，因为key是唯一的
- 第二个为value，有key作为锁不就够了吗？为什么还要用到value?原因就是我们在上面讲到可靠性时，分布式锁要满足第4个条件解铃还须系铃人，我们通过value可以确定这把锁是哪个请求加的了，在解锁的时候可以有依据。
- 第三个：nx这个参数我们填的是nx，意思是set if not exist，即当key不存在时，我们进行set操作；若key已经存在，则不做任何操作；
- 第四个expx，这个参数我们传的是px，意思是我们要给这个key加一个过期的设置，具体时间由第五个参数决定。
- 第五个为time，与第四个参数相呼应，代表key的过期时间。

释放锁就是删除key，但是一般可以用lua脚本删除，判断value一样才删除。 

```java
String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
```

解锁只需要两行代码就可以搞定了，第一行代码，我们写了一个简单的 lua脚本代码，第二行代码，我们将lua代码传到jedis.eval()方法。这段lua代码的功能是什么呢？首先获取锁对应的value值，检查是否与requestId相等，如果相等则删除锁(解锁)因为lua脚本具有原子性。

**单节点存在的问题**

因为如果是普通的 redis 单实例，那就是单点故障。或者是 redis 普通主从，那 redis 主从异步复制，如果主节点挂了（key 就没有了），key 还没同步到从节点，此时从节点切换为主节点，别人就可以 set key，从而拿到锁。

### 集群模式的Redlock

这个场景是假设有一个redis cluster，有5个redis master实例。然后执行如下步骤获取一把锁：

1. 获取当前时间（单位是毫秒）
2. 轮流用相同的key和随机值在5个节点上请求锁，在这一步里，客户端在每个master上请求锁时，会有一个和总的锁释放时间相比小的多的超时时间。比如如果锁自动释放时间是19秒钟，那每个节点锁请求的超时时间可能是5-50毫秒的范围，这个可以防止一个客户端在某个宕掉的master节点上阻塞过长时间，如果一个master节点不可用了，我们应该尽快尝试下一个master节点。
3. 客户端计算第二步中获取锁所花的时间，只有当客户端在大多数master节点上成功获取了锁，而且总共消耗的时间不超过锁释放时间，这个锁就认为是获取成功了。
4. 如果锁获取成功了，那现在锁自动释放时间是最初锁释放时间减去之前获取锁所消耗的时间。
5. 如果获取锁失败了，不管是因为获取成功的锁不超过一半还是因为总消耗时间超过了锁释放时间，客户端都会到每个master节点上释放锁，即便是那些他认为没有获取成功的锁。




## <a name="8">8.你能说说我们一般如何应对缓存雪崩以及穿透问题吗？</a>

### 缓存雪崩

缓存雪崩是指缓存中数据大批量到过期时间，而查询数据量巨大，引起数据库压力过大甚至down机。和缓存击穿不同的是，缓存击穿指并发查同一条数据，缓存雪崩是不同数据都过期了，很多数据都查不到从而查数据库。

**解决方案**  
可以给缓存设置过期时间时加上一个随机值时间，使得每个key的过期时间分布开来，不会集中在同一时刻失效。

**缓存挂掉引起的问题及解决方案**

假如现在有这样一个场景，缓存突然挂了，而系统目前正在处于一个高并发期，将所有的请求都落到了数据库中，使得数据库的压力大增导致数据库宕机。

对于这种情况解决方案如下：可以通过限流组件来限制落到数据库中的请求数量。或者是在程序中再使用ehcache再做一层小缓存，也能减轻一点数据库的压力  
同时，也要保持redis对缓存有持久化的机制。

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/redis雪崩.png)

### 缓存穿透

缓存穿透是指缓存和数据库中都没有的数据，而用户不断发起请求，如发起为id为“-1”的数据或id为特别大不存在的数据。这时的用户很可能是攻击者，攻击会导致数据库压力过大。

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/redis穿透.png)

解决方案：在数据库中查询不存在时，将key设置到redis中，其value可以设置一个固定的值，例如UNKONWN

### 缓存击穿

缓存击穿是指缓存中没有但数据库中有的数据（一般是缓存时间到期），这时由于并发用户特别多，同时读缓存没读到数据，又同时去数据库去取数据，引起数据库压力瞬间增大，造成过大压力  
解决方案：

1. 设置热点数据永远不过期。
2. 加互斥锁

## <a name="9">9.如何保证缓存与数据库双写时的数据一致性？</a>

### 缓存+数据库读写模式的分析

**(1) Cache Aside Pattern**

（1）读的时候，先读缓存，缓存没有的话，那么就读数据库，然后取出数据后放入缓存，同时返回响应

（2）更新的时候，先删除缓存，然后再更新数据库

**(2) 为什么是删除缓存，而不是更新缓存呢？**

问题：先修改数据库，再删除缓存，如果删除缓存失败了，那么会导致数据库中是新数据，缓存中是旧数据，数据出现不一致
解决思路：
先删除缓存，再修改数据库，如果删除缓存成功了，如果修改数据库失败了，那么数据库中是旧数据，缓存中是空的，那么数据不会不一致因为读的时候缓存没有，则读数据库中旧数据，然后更新到缓存中

### 高并发场景下的缓存+数据库双写不一致问题分析与解决方案设计

数据发生了变更，先删除了缓存，然后要去修改数据库，此时还没修改一个请求过来，去读缓存，发现缓存空了，去查询数据库，查到了修改前的旧数据，放到了缓存中数据变更的程序完成了数据库的修改完了，数据库和缓存中的数据不一样了。

**(1) 加互斥锁来实现**

也就是如果存在向redis中写数据的时候，就加上锁进行向redis中添加数据，而在读取的时候，则判断是否有线程在对redis添加数据，如果存在，则等待。

**(2) 队列来实现**

更新数据的时候，根据数据的唯一标识，将操作路由之后，发送到一个jvm内部的队列中，读取数据的时候，如果发现数据不在缓存中，那么将重新读取数据+更新缓存的操作，根据唯一标识路由之后，也发送同一个jvm内部的队列中一个队列对应一个工作线程每个工作线程串行拿到对应的操作，然后一条一条的执行这样的话，一个数据变更的操作，先执行，删除缓存，然后再去更新数据库，但是还没完成更新，此时如果一个读请求过来，读到了空的缓存，那么可以先将缓存更新的请求发送到队列中，此时会在队列中积压，然后同步等待缓存更新完成，这里有一个优化点，一个队列中，其实多个更新缓存请求串在一起是没意义的，因此可以做过滤，如果发现队列中已经有一个更新缓存的请求了，那么就不用再放个更新请求操作进去了，直接等待前面的更新操作请求完成即可，待那个队列对应的工作线程完成了上一个操作的数据库的修改之后，才会去执行下一个操作，也就是缓存更新的操作，此时会从数据库中读取最新的值，然后写入缓存中，如果请求还在等待时间范围内，不断轮询发现可以取到值了，那么就直接返回; 如果请求等待的时间超过一定时长，那么这一次直接从数据库中读取当前的旧值。

## <a name="10">10.你能说说redis的并发竞争问题该如何解决吗？</a>

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/redis_并发竞争问题.png)

在redis的并发竞争问题中，可以引入zk的分布式锁。

写入到redis的数据，都是从mysql中查询出来的，在mysql中保存一个时间戳，然后在redis中保存的时候，也保存一份时间戳。然后在并发写的时候，判断当前设置的时间戳是不是比系统里面新，如果比它新则更新。也是旧数据不允许出现覆盖新数据。








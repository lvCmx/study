## Zookeeper常见面试题

### <a name="1">1.zk都有哪些使用场景?</a>

**<a name="11">(1) 分布式协调</a>**

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/zk_分布式协调.png)

这个其实是zk很经典的一个用法，简单来说，就好比，你A系统发送个请求到MQ，然后B消息消费之后处理了，那A系统如何知道B系统的处理结果？用zk就可以实现分布式系统之间的协调工作。A系统发送请求之后可以在zk上对某个节点的值注册监听器，一旦B系统处理完了就修改zk那个节点的值，A立马就可以收到通知，完美解决。

**<a name="12">(2) 分布式锁</a>**

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/zk_分布式锁特征.png)

对某一个数据连续发出两个修改操作，两台机器同时收到了请求，但是只能一台机器先执行另外一个机器再执行。那么此时就可以使用zk分布式锁，一个机器接收到了请求之后先获取zk上的一把分布式锁，就是可以去创建一个znode，接着执行操作；然后另外一个机器也尝试去创建那个znode，结果发现自己创建不了，因为被别人创建了，那它就对这个节点注册监听，如果此znode被删除，监听器会感知到。

**<a name="13">(3) 元数据/配置信息管理</a>**

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/zk_注册中心.png) 

zk可以用作很多系统的配置信息的管理，比如kafka、storm等等很多分布式系统都会选用zk来做一些元数据、配置信息的管理，包括dubbo注册中心也是依赖zk。

**<a name="14">(4) HA高可用性</a>**

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/zk_HA高可用.png)

这个应该是很常见的，比如Haddop、HDFS、YARN等大数据系统中，都选择基于zk来开发HA高可用机制，就是一个重要进程一般会做主备两个，主进程挂了立马通过zk感知到切换到备用进程中。

### <a name="2">2.zk实现分布式锁</a>

​	前面提取了zk在分布锁方面的介绍，它主要是依赖zk可以创建临时节点，此时创建成功了就获取了这个锁；这个时候别的客户端来创建锁会失败，只能注册个监听器监听这个节点。释放锁就是删除这个znode，一旦释放掉就会通知客户端，然后有一个等待着的客户端就可以再次重新加锁。

**<a name="21">(1) zk的节点类型</a>**

- PERSISTENT  持久化节点
- PERSISTENT_SEQUENTIAL  顺序自动编号持久化节点，这种节点会根据当前已存在的节点数自动加1
- EPHEMERAL  临时节点
- EPHEMERAL_SEQUENTIAL  临时自动编号节点

**<a name="22">(2) zk的watch机制</a>**

一个zk的节点可以被监控，包括这个目录中存储的数据的改变，子节点目录的变化，一旦变化可以通知设置监控的客户端。  
watch机制官方说明：一个watch事件是一个一次性的触发器，当被设置了watch的数据发生了变化的时候，则服务器将这个改变发送给设置了watch的客户端，以便通知它们。  
*一次性的触发器：*当数据改变的时候，那么一个watch事件会产生并且被发送到客户端中，但是客户端只会收到一次这样的通知，如果以后这个数据再次发生改变的时候，之前设置watch的客户端将不会再次收到改变的通知，因为watch机制规定了它是一个一次性的触发器。当设置监视的数据发生改变时。

**<a name="23">(3) zk实现分布式锁原理</a>**

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/zk获取锁流程.png)

EPHEMERAL_SEQUENTIAL这种节点，会对创建相同的节点进行自动编号，而其它的进程创建znode之后，需要判断自己是不是第一个节点，如果不是第一个，则需要监听第一个节点，直到第一个节点被删除。

**<a name="24">(4) 互斥锁mutex lock</a>**

顾名思义就是排它锁，同一时间只允许一个客户端执行。实现步骤：

1) 首先，创建一个有序临时节点，例如"guid-lock-”，其中guid可以是你客户端的唯一识别序号，如果发生前面说的创建失败问题，需要使用guid进行手动检查。  
2) 调用getChildren(watch=false)获取子节点列表，注意watch设置为false，以避免羊群效应，即可时收到太多无效节点删除通知。  
3) 从这个列表中，判断自己创建的节点序号是否是最小的，如果是则直接返回true ,否则继续往下走。  
4) 如果当前节点不是最小的节点，则需要判断最小的节点是否存在，如果存在，则对其监听，收到通知后，再调用getChildren进行判断。  
5) 客户端unlock只需要调用delete删除掉节点，其它的监听器会收到通知。  
![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/mutex_zk.png)

*优点：*  避免了轮询和超时控制，每次一个节点的删除动作，只会触发唯一一个客户端的watch动作，从而避免了羊群效应。

*缺点：* 没有解决锁重入的问题，因为采用的是有序临时节点，因此多次调用create并不会触发什么异常，从南昌无法实现锁重入功能，如果需要解决，则在步骤我时，需要先进行判断当前节点是否已经存在，已经创建则直接从步骤3开始，不需要再创建一个节点。

**<a name="25">(5) 共享锁Shared Locks或读写锁Read/Write Locks</a>**

read读锁是共享锁，write写锁是排它锁，当没有写时，允许多个read实例获取读锁，当有一个write实例获得写锁时，则不允许任何其他write实例和read实例获得锁。

***读锁：***

1.创建一个有序临时节点，例如"locknode/read-guid-lock-"。  
2.调用getChildren(watch=false)，从这个列表中，判断是否有序号比自己小，且路径名以"write-"开头的节点，如果没有，则直接获取读锁，否则继续如下步骤。  
3.从步骤2中获取list中选取排在当前节点前一位的，且路径名以"write-"开头的节点，调用exists(watch=true)方法，监听这个节点。如果exists返回false，则回到步骤2 

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/zk_read.png)

***写锁：***

1.创建一个有序临时节点，例如"locknode/write-guid-lock-"。  
2.调用getChildren(watch=false)，从这个列表中，判断是否有序号比自己小，如果是则直接返回，否则继续往下走。  
3.从步骤2中获取list中选取排在第1位的节点，调用exists(watch=true)方法，监听这个节点。如果exists返回false，则回到步骤2 

![](https://github.com/lvCmx/study/blob/master/note/面试题/resource/write_zk.png)

**<a name="25">(6) 参考代码</a>**


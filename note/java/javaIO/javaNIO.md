## Java NIO模型

### 1.IO模型

**1:1形式的同步阻塞IO通信模型：**

![](F:\__study__\hulianwang\study\note\java\javaIO\img\io模型.png)

首先解释一下，在传统IO里面的，阻塞是发生在哪里的：

1. 服务端的accept接收连接是阻塞的
2. 客户端中读取数据是阻塞的

通信过程：

1. 服务端通常由一个独立的acceptor线程负责监听客户端的连接；
2. acceptor监听到客户端的连接请求后，为每个客户端创建一个新的线程进行链路处理；
3. 链路处理线程完成客户端请求的处理后，通过输出流返回应答给客户端，然后线程销毁；

模型缺点：

1. 服务端线程个数与客户端并发访问连接数是1:1的关系；
2. 随着客户端并发访问量增大，服务端线程个数线性膨胀，系统性能急剧下降；

**M:N形式的同步阻塞IO通信模型**

![](F:\__study__\hulianwang\study\note\java\javaIO\img\M.N的io模型.png)

相比上一版本，这里引入了线程池，服务端通过线程池来处理多个客户端的接入请求，通过线程池约束及调配服务端线程资源。形成客户端个数M：服务端线程池最大线程数N的比例关系。

**通信过程：**

1. 当有新的客户端接入时，将客户端Socket封装成一个Task投递到服务端任务队列；
2. 服务端任务线程池中的多个线程对任务队列中的Task进行并行处理；
3. 任务线程处理完当前Task后，继续从任务队列中取新的Task进行处理；

**模型缺点：**

​    1.BIO的读和写操作都是同步阻塞的，阻塞时间取决于对端IO线程的处理速度和网络IO的传输速度，可靠性差；

​    2.当线程池中所有线程都因对端IO线程处理速度慢导致阻塞时，所有后续接入的客户端连接请求都将在任务队列中排队阻塞堆积；

​    3.任务队列堆积满后，新的客户端连接请求将被服务端单线程Acceptor阻塞或拒绝，客户端会发生大量连接失败和连接超时。

### 2.非阻塞式IO模型(NIO)

**基础知识：**

多路复用器Selector是NIO模型的基础，一个多路复用器Selector可以同时轮询多个注册在它上面的Channel，服务端只需要一人线程负责Selector的轮询，就可以拉入成千上万的客户端连接。

模型优点：

​    1）NIO中Channel是全双工(是说可以通过Channel 即可完成读操作，也可以完成写操作)的，Channel比流(InputStream/OutputStream)可以更好地映射底层操作系统的API（UNIX网络[编程](https://m.2cto.com/kf)模型中，底层操作系统的通道都是全双工的，同时支持读写操作）；

​    2）客户端发起的连接操作是异步的，不需要像之前的客户端那样被同步阻塞；(此时，客户端不依赖于服务器端，也就是说客户端发完请求可以做其他其他事情)

​    3）一个Selector线程可以同时处理成千上万个client的请求，而且性能不会随着客户端链接的增加而线性下降；原因：JDK的Selector在[Linux](https://m.2cto.com/os/linux/)等主流操作系统上通过epoll实现，它没有连接句柄数的限制，适合做高性能高负载的网络服务器方案

**Reactor模式的思想：**

1.分而治之

- 一个connection里完整的网络处理过程一般分为6步：accept、read、decode、process、encode、send。

- Reactor模式将每个步骤映射为一个Task，服务端线程执行的最小逻辑单元不再是一次完整的网络请求，而是Task，且采用非阻塞方式执行。

2.事件驱动

-  每个Task对应一个特定事件，当Task准备就绪时，对应的事件通知就会发出。

- Reactor收到事件通知后，分发给绑定了对应事件的Handler执行Task。

**NIO+单线程Reactor模式**

![](F:\__study__\hulianwang\study\note\java\javaIO\img\nio-reacter.png)

说明：

Reactor：负责响应事件，将事件分发给绑定了该事件的Handler处理；

Handler：事件处理器，绑定了某类事件，负责执行对应事件的Task对事件进行处理；

Acceptor：就是处理客户端链接connect事件的；Handler的一种，绑定了connect事件，当客户端发起connect请求时，reactor会将accept事件分发到acceptor处理。

该模型的缺点：

- 单线程版本Reactor模型优点是不需要做并发控制，代码实现简单清晰；

- 缺点是不能利用多核CPU，一个线程需要执行处理所有的accept、read、decode、process、encode、send事件，如果其中decode、process、encode事件的处理很耗时，则服务端无法及时响应其他客户端的请求事件。


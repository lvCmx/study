### AOF持久化技术

扼持久化是将进程数据写入文件，是对内存中的redis数据生成一个快照。而AOF持久化(Append Only File持久化)，则是将Redis执行的每次写命令记录到单独的日志文件中；当Redis重启时再次执行AOF文件中的命令来恢复数据。

**1.开启AOF**

redis服务默认开启RDB，关闭AOF；要开启AOF，需要在配置文件中配置：appendons yes1

**2.执行流程**

命令追加：append将Redis的写命令追加到缓冲区aof_buf;

文件写入(write)和文件同步(sync)：根据不同的同步策略将aof_buf中的内容同步到硬盘；

文件重写(rewrite)：定期重写AOF文件，达到压缩的目的。

**3.命令追加**

redis先将写命令追回到缓冲区，而不是直接写入文件，主要是为了避免每次有写命令都直接写入磁盘，导致磁盘IO成为redis负载的瓶颈。

**4.文件写入write和文件同步sync**

Redis提供了多种AOF缓存区的同步文件策略，策略涉及到操作系统的write函数和fsync函数，说明如下： 
为了提高文件写入效率，在现代操作系统中，当用户调用write函数将数据写入文件时，操作系统通常会将数据暂存到一个内存缓冲区里，当缓冲区被填满或超过了指定时限后，才真正将缓冲区的数据写入到硬盘里。这样的操作虽然提高了效率，但也带来了安全问题：如果计算机停机，内存缓冲区中的数据会丢失；因此系统同时提供了fsync、fdatasync等同步函数，可以强制操作系统立刻将缓冲区中的数据写入到硬盘里，从而确保数据的安全性。

AOF缓存区的同步文件策略由参数appendfsync控制，各个值的含义如下： 
always：命令写入aof_buf后立即调用系统fsync操作同步到AOF文件，fsync完成后线程返回。(立即同步) 这种情况下，每次有写命令都要同步到AOF文件，硬盘IO成为性能瓶颈，Redis只能支持大约几百TPS写入，严重降低了Redis的性能；即便是使用固态硬盘（SSD），每秒大约也只能处理几万个命令，而且会大大降低SSD的寿命。 
no：命令写入aof_buf后调用系统write操作，不对AOF文件做fsync同步（由操作系统进行同步）；同步由操作系统负责，通常同步周期为30秒。这种情况下，文件同步的时间不可控，且缓冲区中堆积的数据会很多，数据安全性无法保证。 
everysec：命令写入aof_buf后调用系统write操作，write完成后线程返回（每秒一次进行同步）；fsync同步文件操作由专门的线程每秒调用一次。everysec是前述两种策略的折中，是性能和数据安全性的平衡，因此是Redis的默认配置，也是我们推荐的配置。

**5.文件重写(rewrite)**

随着时间流逝，Redis服务器执行的写命令越来越多，AOF文件也会越来越大；过大的AOF文件不仅会影响服务器的正常运行，也会导致数据恢复需要的时间过长。

文件重写是指定期重写AOF文件，减小AOF文件的体积。需要注意的是，AOF重写是把Redis进程内的数据转化为写命令，同步到新的AOF文件；不会对旧的AOF文件进行任何读取、写入操作!

关于文件重写需要注意的另一点是：对于AOF持久化来说，文件重写虽然是强烈推荐的，但并不是必须的；即使没有文件重写，数据也可以被持久化并在Redis启动的时候导入；因此在一些实现中，会关闭自动的文件重写，然后通过定时任务在每天的某一时刻定时执行。

文件重写之所以能够压缩AOF文件，原因在于： 
过期的数据不再写入文件 
无效的命令不再写入文件：如有些数据被重复设值(set mykey v1, set mykey v2)、有些数据被删除了(sadd myset v1, del myset)等等 
多条命令可以合并为一个：如sadd myset v1, sadd myset v2, sadd myset v3可以合并为sadd myset v1 v2 v3。不过为了防止单条命令过大造成客户端缓冲区溢出，对于list、set、hash、zset类型的key，并不一定只使用一条命令；而是以某个常量为界将命令拆分为多条。这个常量在redis.h/REDIS_AOF_REWRITE_ITEMS_PER_CMD中定义，不可更改，3.0版本中值是64。

**6.文件重写的触发**
文件重写的触发，分为手动触发和自动触发： 
手动触发：直接调用bgrewriteaof命令，该命令的执行与bgsave有些类似：都是fork子进程进行具体的工作，且都只有在fork时阻塞。

自动触发：根据auto-aof-rewrite-min-size和auto-aof-rewrite-percentage参数，以及aof_current_size和aof_base_size状态确定触发时机。 
auto-aof-rewrite-min-size：执行AOF重写时，文件的最小体积，默认值为64MB。 
auto-aof-rewrite-percentage：执行AOF重写时，当前AOF大小(即aof_current_size)和上一次重写时AOF大小(aof_base_size)的比值。 
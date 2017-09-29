GeeQuery使用手册——Chapter-15  JMX监控

[TOC]

# Chapter-15  JMX监控

EF_ORM支持JMX监控。

目前EF-ORM提供两个JMX监控Bean，分别是DbClientInfo和ORMConfig。路径如下图。

 ![15.](E:\User\ef-orm\manual\Chapter\images\15..png)

## 15.1.  DbClientInfo

 	 ![15.1](E:\User\ef-orm\manual\Chapter\images\15.1.png)

每个DbClient对象对应一个DbClientInfo的监控Bean。在一个进程中，如果有多个DbClient对象，那么也会有多个DbClientInfo的MXBean。

DbClientInfo的五个属性都是只读属性。记录了当前的一些运行情况信息。

| 属性                      | 含义                                       |
| ----------------------- | ---------------------------------------- |
| Connected               | 是否连接中                                    |
| RoutingDbClient         | 是否为多数据源DbClient                          |
| DataSourceNames         | 如果是多数据源的DbClient，会列出所有已知数据源的名称           |
| EmfName                 | EntityManagerFactory的名称，如果有多个EntityManagerFactory可用于区分。 |
| InnerConnectionPoolInfo | 内置连接池信息。    Max：最大连接数  Min：最小连接数      Current:当前连接数  Used：使用中的连接数 Free:空闲连接数  Poll：连接取用累计次数 Offer:连接归还累计数 |

## 15.2.  ORMConfig

 ![15.2](E:\User\ef-orm\manual\Chapter\images\15.2.png)

ORMConfig记录框架的各项配置信息，每个进程中仅有一个ORMConfig的MXBean。

**支持动态参数调整**

​ORMConfig中的属性都是可读写的属性，即可以在运行过程中调整ORM的各项参数。包括调试日志、连接池大小等。这些参数大多数都和jef.properties中的参数对应。因此可以查看《附录一配置参数一览》或者API-DOC。
GeeQuery使用手册

[TOC]


# 1.    EF-ORM概览

## 1.1.  名词解释

本文中出现的一些术语的解释。其中，知识领域一列指出了对该词条进行定义的知识领域。凡是知识领域为EF-ORM的词条，都是在本文中有特殊定义的词汇。

| 中文名       | 英文名           | 知识领域           | 解释                                       |
| --------- | ------------- | -------------- | ---------------------------------------- |
| 关系数据库管理系统 | RDBMS         | 数据库常识          | 即各种数据库软件本身，如Oracle, DB2,Postgresql等。     |
| --        | DML           | 数据库常识          | Data  manipulation language：指SELECT、UPDATE、INSERT、DELETE四种操作数据的语句。 数据库的事务控制只针对DML。 |
| --        | DDL           | 数据库常识          | Data  definition language：DDL。命令有CREATE、ALTER、DROP等，DDL主要是用在定义或改变表（TABLE）的结构，数据类型等初始化工作上。除了四句DML外其他所有语句一般都认为是DDL，如TRUNCATE也是。 |
| --        | JPA           | Java常识         | Java  EE规范之一，由JCP组织定义的Java持久层(适用于关系数据库)操作规范。JPA有1.0和2.0两个版本，2.0是1.0的补充。 |
| --        | JTA           | Java常识         | Java事务API，由JCP组织定义的Java EE标准之一。使用JTA可以实现分布式事务的一致性。 |
| 注解        | Annotation    | Java常识         | JavaSE特性之一，编程时在代码上增加“@xxx”这样的标注来描述代码的行为。 |
| 简单对象      | POJO          | Java常识         | 只有字段和get/set方法，不继承任何类或接口的java bean。      |
| 实体        | Entity        | Jave  EE   JPA | 一个实体就是用@Entity注解标注的一个类。这个类是一张数据库表的java映射。 |
| 元模型       | Meta-model    | Jave  EE   JPA | 元数据模型，描述数据库表以及其对应的Java类的结构、映射关系等信息的对象模型。在Java中有对应的对象和类。 |
| 增强        | Enhancement   | Jave  EE   JPA | 使用字节码技术对java类文件进行代码植入，从而实现Enity类一些高级行为的技术手段。 |
| 查询        | Query         | EF-ORM         | 一项对数据库操作请求。请不要从字面含义理解为是select语句，查询也可以是一项数据update操作。  在EF-ORM中Criteria 和 本地化查询都是查询的主要形式，最终都对应某项数据库操作。 |
| --        | Criteria  API | EF-ORM         | 原指JPA 2.0规范制定的一套用对象来实现复杂数据库查询的API。本文中指EF-ORM自身实现的类似功能的API。使用该API可以构造出查询对象（Criteria Query）来执行数据库查询。 |
| 本地化查询     | --            | EF-ORM         | 使用SQL（或JPQL）进行的数据库操作。本地化查询包含NamedQuery、NativeQuery、NativeCall三种。 |
| --        | NativeQuery   | EF-ORM         | 基于增强型SQL或JPQL语句的查询。                      |
| 命名查询      | NamedQuery    | EF-ORM         | 将NativeQuery的SQL语句和变参等信息预先配置并取名，在使用时按名称来调用查询。 |
| 存储过程调用    | NativeCall    | EF-ORM         | 一个调用存储过程的查询。                             |
| 数据库方言     | Dialect       | EF-ORM         | 由于不同RDBMS语法和功能的差异，EF-ORM将这些行为差异集中到一个接口上，并为每个RDBMS单独提供一个实现类，用这种方法来支持不同的RDBMS。每个实现类就是一种方言。 |
|           | Session       | EF-ORM         | 和某H框架相似，主要的数据库操作方法都集中在这个对象上。Session表示一个在事务状态下,或者非事务状态下的数据库操作句柄。  本文中 最常见的DbClient对象，是Session的子类。Session的另一个子类是Transaction类。 |

 

## 1.2.  主要特点和特性

EF-ORM是一个轻量，便捷的Java ORM框架。在经历了金融和电信等项目的锤炼后，具备了众多的企业级特性。

 

**主要特点 **

| 特点                  | 描述                                       |
| ------------------- | ---------------------------------------- |
| 轻量                  | 该框架对应用环境、连接池、 是否为J2EE应用等没有特殊要求。可以和EJB集成，也可与Spring集成，也可以单独使用。整个框架模块和功能都较为轻量。 |
| 依赖少                 | 整个框架只有两个jar包。间接依赖仅有commons-lang, slf4j等7个通用库，作为一个ORM框架，对第三方依赖极小。 |
| 简单直接的API            | 框架的API设计直接面向数据库操作，不绕弯子，开发者只需要数据库基本知识，不必学习大量新的操作概念即可使用API完成各种DDL/DML操作。 |
| 最大限度利用编译器减少编码错误的可能性 | API设计和元数据模型（meta-model）的使用，使得常规的数据库查询都可以直接通过Criteria API来完成，无需使用任何JPQL/HQL/SQL。可以让避免用户犯一些语法、拼写等错误。 |
| JPA2规范兼容            | 使用JPA 2.0规范的标准注解方式来定义和操作对象。（但整个ORM不是完整的JPA兼容实现） |
| 更高的性能               | 依赖于ASM等静态字节码技术而不是CGlib，使得改善了代理性能；依赖于动态反射框架，内部数据处理上的开销几乎可以忽略。操作性能接近JDBC水平。对比某H开头的框架，在写入操作上大约领先30%，在大量数据读取上领先50%以上。 |
| 更多的性能调优手段           | debug模式下提供了大量性能日志，帮您分析性能瓶颈所在。同时每个查询都可以针对batch、fetchSize、maxResult、缓存、级联操作类型等进行调整和开关，可以将性能调到最优。 |
| 可在主流数据库之间任意切换       | 支持Oracle、MySQL、Postgres、GBase、SQLite、HSQL、Derby等数据库。  除了API方式下的操作能兼容各个数据库之外，就连SQL的本地化查询也能使之兼容。 |
| JMX动态调节             | 可以用JMX查看框架运行统计。框架的debug开关和其他参数都可以使用JMX动态调整。 |
| 企业级特性支持             | SQL分析，性能统计，分库分表，Oracle RAC支持，读写分离支持，动态字段支持 |

 

一谈ORM很多朋友都会自然性的和某H框架进行比较，从而发现这些ORM很多都只是功能简单的“玩具”。但是EF不是仅仅是一个玩具。某H框架支持的功能EF-ORM几乎都能支持，或者有替代的支持手段。

**和某H****框架支持特性对比**

|                       | 某H框架                                     | EF-ORM                                   |
| --------------------- | ---------------------------------------- | ---------------------------------------- |
| 兼容主流数据库               | 几乎所有数据库。一些过于简单的数据库不支持，如SQLite。           | 支持所有主流数据库，包括SQLite                       |
| 数据库移植性                | 通过配置数据库方言和调整一些映射配置，可以实现移植。  HQL 中的部分语法和函数支持跨库操作。SQL不支持跨库 | 自动检测数据库，自动兼容本地数据库方言，无需人工干预。  SQL/JPQL都支持自动跨库改写。  后续还将支持单Session同时支持多个异构数据库并发操作。 |
| 级联                    | 支持JPA等规范要求的One-toOne, One-to-many, many-to-on,  many-to-many下的CRUD级联操作。 | 除了支持JPA等规范要求四种级联关系外，还保留单表操作的方式，可以使用API在级联操作和单表操作中灵活变化。 |
| 延迟加载                  | 支持级联场景下的各种延迟加载。                          | 除了支持级联场景下的延迟加载外，还支持LOB字段延迟加载。            |
| 投影操作                  | 支持                                       | 支持                                       |
| Criteria  API         | 支持JPA的Criteria API和框架自己的Criteria API     | 支持框架自己的Criteria API。                     |
| 自生成主键策略               | 支持Sequence,Native，Identity，GUID,Hilo,table, Froigen等。一旦配置为非Native方式，会降低可移植性 | 支持Identity、Sequence、Table、Auto(Native)、GUID等策略，Hilo作为辅助算法可以和Sequence或Table随意组合。  Sequence名称可定制，支持自动创建Sequence，支持Sequence步长检测。可根据DBMS自动切换自增方案。 |
| 通过Transformer定义查询结果转换 | 支持Map，支持用Object[]返回多个对象，支持用ResultTransformer类自定义转换逻辑。 | 支持Map，支持用Object[]返回多个对象，支持用Transformer自定义转换逻辑。 |
| 一级缓存                  | 支持。作为Session的核心概念和实现手段，无法关闭。当影响结果正确性时，需通过flush 等API来手动操作。  HQL或SQL等操作不会被缓存，也不会刷新一级缓存中的的数据。 | 支持。可关闭。SQL语句和API操作都会刷新缓存中的数据。            |
| 动态表支持                 | 在进行一些配置和定制后可以支持动态表的单表操作。                 | 原生支持，所有动态表和静态表都会被提取为元模型，后台处理完全一致。因静态表支持的功能均能在动态表上使用，甚至包括动态表和静态表之间的级联操作。  还支持直接扫描数据库结构动态生成元模型。 |
| Oracle  ROWID支持       | 支持                                       | 目前支持得还不太好，待改进。                           |
| JTA事务                 | 支持                                       | 支持                                       |
| Spring集成              | Spring原生支持。方便。支持Spring全部的7种事务传播行为、隔离级别。  | 按JPA规范和Spring集成。方便。提供JPADialect来支持7种传播行为、事务隔离级别、事务超时（标准JPA不支持） |
| 约束，所有表必须有主键           | ORM的基本约束之一                               | 无需此约束                                    |
| 支持数据库逆向工程             | 通过第三方插件可从数据库生成对象映射                       | EF  Eclipse插件可从数据库生成对象映射。还可以从PDM文件中生成对象映射。 |
| 自增值也可以手工指定            | 不支持                                      | 支持                                       |
| 存储过程                  | 不支持                                      | 支持存储过程，支持存储过程返回值的转换和封装  含匿名存储过程，即动态执行一段存储过程。 |
| 命名查询                  | 配置在注解上                                   | 集中配置。可配在XML文件中和数据库中                      |
| 动态SQL                 | 不支持                                      | 可根据条件自动省略SQL语句中的条件、函数等SQL片段。  SQL中可指定部分片段为变量，(如表名、条件等)，在运行时动态形成SQL。 |
| DDL                   | 支持hbm2ddl的配置方式刷新数据库结构。                   | 使用实体扫描器可以验证和刷新数据库结构。同时所有的DDL操作也以API的形式直接提供，用户可随意操作。 |
| 分库分表                  | 不支持                                      | 支持                                       |
| 针对Oracle RAC的优化       | 不支持                                      | 可根据分库路由结果或读写分离策略，单独操作RAC的节点。针对RAC的读写争用问题进行优化。 |
| Oracle  Hint支持        | 不支持                                      | 原生支持Oracle的 /*+ APPEND */等性能优化的手段。       |
| SQL分页改写               | 不支持                                      | 通过内置分析器，直接改写原SQL查询形成count语句。原生支持分页操作。    |
| 绑定变量                  | HQL中支持。API操作均使用绑定变量。                     | SQL/JPQL中支持。API操作均使用绑定变量。(部分方言中禁止绑定变量的情况除外) |
| 数据库关键字判断              | 部分数据库支持                                  | 建立了完整的常用数据库关键字列表，对用到的关键字自动添加引号或`符号，确保带有数据库关键字的操作也都能正常执行。 |

 

    上面是EF-ORM和H框架的一个简单对比。总结下来，EF-ORM的支持功能基本覆盖了大家耳熟目详的H框架，甚至在很多项上要更进一步。但EF-ORM和传统的如H框架,T框架，OpenJPA框架的设计思想是不同的，这些思想上的差异体现在——

l 更多考虑逆向工程数据库的各种场景。

l 要求操作者面向数据库来思维，面向对象的操作手段更多只是带来开发效率的提升和开发难度的降低。

l 追求更高的性能。

l 同时保留多表级联的关系型操作和单表的操作。能保留在关系型数据库的ER模型的基础上完整的操作数据库，而不是要求用户必须使用对象模型来操作数据库。

l 兼容和保留以SQL为主的传统数据库操作方式，并使之能兼容多种不同的DBMS。在金融和电信领域，还有大量传统的以SQL（甚至是存储过程）为主的数据库使用方式。我们不可能让这些领域的应用完全改弦易帜到纯粹的对象型数据操作上。

l 不但封装了DML操作，也封装了建表、建索引、ALTERTABLE、TRUNCATE等DDL操作。 

 

除此之外EF-ORM还有很多同类框架中很少具备的企业级应用特性

l 分库分表 

l 分布式事务 JTA 

l 多数据源与路由 

l 多数据源支持

l Oracle RAC FCF支持，Oracle RAC读写分离

 

综上所述，EF-ORM可以是一个小玩具，但也是支撑大型项目中的企业级框架。

 

## 1.3.  选择EF的理由

### 1.3.1.   API方式和xxQL(查询语言)之间的平衡

EF-ORM是以轻量、易用为目的设计的开源关系型数据库ORM框架。其特点之一是：

l  尽可能通过编译器检查数据库操作的正确性

EF-ORM通过原生的元模型和内建的Criteria API来完成类型**安全**的动态查询。 这种查询优于传统的基于字符串的 Java Persistence Query Language(JPQL/HQL) 查询。本文假设您具备基础的 Java 语言编程知识，并了解常见的 JPA 使用，比如 EntityManagerFactory 或 EntityManager。首先我们来看JPQL / HQL 查询有什么缺陷？

某H框架引入了HQL；JPA 1.0也引进了 JPQL，它们都是强大的查询语言，它在很大程度上导致了各自框架的流行。不过，基于字符串并使用有限语法的JPQL 存在一些限制。要理解 JPQL 的主要限制之一，请查看清单 1 中的简单代码片段，它通过执行 JPQL 查询选择年龄大于 20 岁的 Person 列表：

清单 1. 一个简单（并且错误）的 JPQL 查询

但是这个简单的例子有一个验证的错误。该代码能够顺利通过编译，但将在运行时失败，因为该 JPQL 查询字符串的语法有误。清单 1 的第 2 行的正确语法为：

不幸的是，Java 编译器不能发现此类错误。在运行时，该错误才会出现并提示。

这个例子几乎描述出了一切xxQL（包括SQL）带来的困境。因为我们总是无法轻易发现查询语句中的错误。无论是在语句初次编写后，还是在数据库结构变化后的维护过程中。

而使用Criteria API来执行动态查询，将会安全得多，几乎任何错误都将会被编译器发现并无情的指出，码农可以放心的重构他们的代码乃至数据库，因为编译器会将一切笔误和疏忽指出。不要小看这样一个小小的进步，因为当您考量一个软件的成本会发现，绝大部分的软件成本都是在后期的维护和测试当中。

l  内建的元模型

JPA引入了meta-model这个概念。OpenJPA可以使用编译的处理器生成名为元模型的类。简单说来，元模型就是将数据表结构用Java类、字段、枚举等形式表现出来的静态结构。我们在代码使用元模型的常量来表示表和列，也就相当于将我们的查询让编译器来校验一遍。

OpenJPA的元模型是依赖于编译时生成的一个奇特的类，比如Entity类A.java，那么生成一个名为A_.java的原模型类。EF-ORM的元模型则是利用了java的内部类机制。它需要在您的Entity中生成一个 枚举类型

这个枚举就成为了该Entity的元模型描述。可以在构建查询的时候使用。

l  Say ‘No’ to XML

EF-ORM希望尽可能少的配置，以及由此带来的开发效率的提升。因此EF-ORM使用标准JPA注解方式来描述一个Entity。整个过程实现了零配置（如果您不把注解当做配置的话）。

### 1.3.2.   比某H框架更灵活

围绕H框架一直以来都有用性能和效率和灵活性的争论。人们在徘徊于这些问题的同时，往往忽视了H框架这些年来的巨大改进和性能优化。H框架其实提供了让人眼花缭乱的特性，以及优秀的性能。大多数围绕H框架的性能诟病，其实多是使用不当造成的。个人感觉，很多人在H框架的使用中存在以下的问题：

1 对于一级缓存等特性认识并不深刻，甚至有人为了避免麻烦，动辄flush，evict，性能不佳。

2 对于HQL过度依赖，最终走回到拼凑HQL语句的路子上去，就像当初拼凑SQL一样。这十多年前那些拼凑SQL的代码有什么不同？ 除了开发者需要学习一种形似SQL的新查询语言以外，代码的可维护性和开发效率的提升又在哪里？

EF-ORM的出发点之一是更简单和直白的使用数据库。并不希望使用者认为自己是在操作一个“对象库”。而是将JDBC用更简练，更方便的手段来封装给用户使用。 

 

​                                   

 

EF-ORM干的事情很简单，将SQL语句，或者和SQL语句直观对应的Query对象传给数据库执行操作；同时将查询结果的JDBC封装对象转换成用户需要的各种Object，至于不顾关系型数据库的特色把它封装成一个按主键操作的对象库更是想都没想过。EF-ORM提供的大部分API都可以直观的映射到传统JDBC的操作方式上，并没有虚拟出一个“Session”的对象库。也不存在什么“游离对象”“临时对象””持久对象”之类的概念；无需人工flush、evict。将数据库的一切特性和用法明明白白的展现在开发者面前。对于熟悉SQL的开发者和DBA来说，既保留了某H框架操作的高效，又没有“隔靴搔痒”的困惑。

正是这个特点，使得EF-ORM对于熟悉SQL语句的开发者来说，学习成本非常低。

 

EF-ORM原汁原味的保留了JDBC的高效特性，这会不会造成开发效率上的降低呢？众所周知，JDBC的开发效率和维护效率要低于各种ORM框架。事实上EF-ORM也汲取了JPA等持久化框架的优点，“拟态”出了某H框架能支持的几乎全部行为，提供了高效而实用的API。在表面上，EF-ORM像是一个完全的面向对象进行操作的ORM框架，实际上这些都是假象。EF-ORM本质上不支持任何对象操作，操作数据库只有两种方式——Query API、SQL。前者可以利用编译器发现大多数操作错误，减少开发者犯错误的可能。后者可以灵活的操作数据库，但保留了对象映射和多数据库兼容等特色。

 

### 1.3.3.   比○Batis框架更高效

很多朋友在发现某H框架难以适应自己的项目后，最终都选择了○Batis框架作为项目的首选。○Batis也是一个非常优秀的框架。

使用动态SQL语句是这个框架的大亮点，这给了开人人员灵活而强有力的武器来操作数据库。但是这也使在这个框架上开发的应用存在数据库移植性上的问题。此外，对象和表的映射关系维护过于繁琐等问题，也困扰着○Batis的使用者。○Batis使用者还需要记忆特定的一系列XML标签来帮助他们实现动态SQL的特性。而且这些也需要纯手工完成。

综上所述○Batis的开发效率，和H框架还是有明显的差距的，相对于某H框架这个全自动框架，大多数人认为○Batis更像是一个“半自动”的框架。

但是我认为最大的问题不在于开发效率，而在维护效率上。其过于原生的数据库操作方式，难以避免项目维护过程中的巨大成本。当数据库字段变化带来的修改工作虽然可以集中到少数几个XML文件中，但是依然会分散在文件的各处，并且你无法依靠Java编译器帮助你发现这些修改是否有错漏。在一个复杂的使用○Batis的项目中，变更数据库结构往往带来大量的CodeReview和测试工作，否则难以保证项目的稳定性。

EF-ORM也有动态SQL的特性，但在一般性的项目中，使用CriteriaAPI和SQL的比例大约是8:2。能够不写SQL就能解决的问题，为什么还要写SQL语句呢。

### 1.3.4.   性能

    得益于EF内部的动态反射框架和字节码技术，EF-ORM拥有优秀的性能，几乎接近于直接操作JDBC API。

    在操作大量数据读写的的场合下，这一优势发挥得更为明显。

    用EF-ORM和某H框架进行的读写性能测试表明

\* *

 

EF的性能优势来源于

1、 使用ASM而不是反射来构造对象

2、 类的增强在编译期就已经完成。

3、 大量后期的优化和策略模式的应用。

 

# 2.    入门 Getting started

我们在这一章，将帮助您搭建一个简单的测试工程，完成您EF-ORM使用的第一步。

## 1.1. 第一个案例 

### 1.1.1.   Install plug-in for Eclipse

请先安装EF-ORM foreclipse插件。

1、在Eclipse的Update Site中增加站点[http://geequery.github.io/plugins/1.3.x/](http://geequery.github.io/plugins/1.3.x/)，

2、选择help \ Install new Software来安装JEF插件。

### 1.1.2.   示例工程搭建

**直接下载**

您可以直接下载本文的示例工程，也可以按后面的说明一步一步搭建此工程。

该示例工程中，包含了本文中的所有代码清单

**自行创建**

新建测试工程，然后将EF-ORM的包加入到工程。

l  本文中的测试都采用Apache derby来进行，为了运行本文的案例，你需要下载一个derby.jar放到你的工程目录下。

l 未使用Maven场合下

可以从Git直接构建

 

### 1.1.3.   Hello World！

代码示例

 

src/main/java/org/easyframe/tutorial/lesson1/entity/Foo.java

src/main/resources/jef.properties

src/main/java/org/easyframe/tutorial/lesson1/Case1.java

上面这个案例，运行以后，注意观察控制台，将会打印出所有执行的SQL语句和耗时。您将会注意到，EF-ORM在这个案例中，顺序进行了——

建表、插入记录、查出记录、按主键更新记录、按主键删除记录、查所有记录、删除表等7次数据库操作。这就是EF-ORM的“Hello,World”。希望您一切顺利。

## 1.2. 正式开始EF-ORM之旅

### 1.2.1.   EF-ORM的原生Entity

在上面的Case1.java中，我们将会发现操作数据库是如此轻松的一件事。如果您有兴趣可以尝试在jef.properties中配置别的数据源，目前EF-ORM能完全支持的RDBMS参见附录二。

在上面例子其实并不是EF-ORM的基本功能，其中使用到了EF-ORM的一个特殊功能，POJO-Support。事实上，EF-ORM中，会希望您用一个更复杂的对象来描述数据库实体而不是上例中的POJO，这样才能使用EF-ORM的全部特性和API。

我们再来一次。将刚才的Foo.java拷贝一份，名叫Foo2.java。除了类名变化外内容无需修改。然后我们在资源树上，文件上点右键，选择“Convert POJO to a JEF Entity ”。

 

您将发现，类的代码变成了下面所示的内容

插件会自动在类中插入一些代码，包括JPA实体定义的Annonation，由enum构成的EF-ORM数据元模型(Meta Model)。然后我们可以用更简洁的代码，来重复上面Case1.java的案例。

在Case2.java中，我们可以发现变化——所有的数据库操作都可以直接在DbClient对象上进行了。

细心的同学可能会发现，update语句从原来的

updateFOO set CREATED = ?, NAME = ? where ID=?

 变成了

updateFOO2 set NAME = ? where ID=?

这才是原生的EF-ORM的实现。事实上EF-ORM并不支持直接操作POJO类型的实体。所有EF-ORM中操作的Entity都必须实现jef.database.IQueryableEntity接口。这个接口提供了更多的方法，将各种复杂查询，更新操作等功能集成起来，比如上例中的update语句之所以被优化，就是因为EF-ORM检测到执行过setName方法，而其他字段并未执行过set方法。

上面的例子中，将POJO转换为原生的实体是使用JEF插件进行的，如果您熟练了，也可以自己编码。当然，更多的场合，您的实体是从数据库或PDM文件里导入的，这些场合下都会生成原生的实体。

### 1.2.2.   POJO or Non POJO

同学们可能会有很多吐槽，我们这里用问答的形式来讲述。

Q: 为什么要实现IQueryableEntity接口或继承DataObject，有什么用处？

A: EF的设计的一个主要目的是提高开发效率，减少编码工作。为此，可以让开发者“零配置”“少编码”的操作数据库大部分功能。而数据库查询条件问题是所有框架都不能回避的一个问题，所以我经常在想——既然我们可以用向DAO传入一个Entity来实现插入操作，为什么就不能用同样的方法来描述一个不以主键为条件的update/select/delete操作？为什么DAO的接口参数老是变来变去？为什么很多应用中，自行设计开发类来描述各种业务查询条件才能传入DAO？为什么我们不能在数据访问层上花费更少的时间和精力?

JPA1.0和早期的H框架，其思想是将关系型数据库抽象为对象池，这极大的限制了本来非常灵活的SQL语句的发挥空间。而本质上，当我们调用某H框架的session.get、session.load、session.delete时，我们是想传递一个以对象形式表达的数据库操作请求。只不过某H框架要求（并且限制）我们将其视作纯粹的“单个”对象而已。JPA 2.0为了弥补JPA1.0的不足，才将这种Query的思想引入为框架中的另一套查询体系——CriteriaAPI。事实上针对单个对象的get/load/persist/save/update/merge/saveOrUpdateAPI和Criteria API本来就为一体，只不过是历史的原因被人为割裂成为两套数据库操作API罢了。

因此，我认为对于关系型数据库而言——Entity和Query是一体两面的事物，所谓Query，可以包含各种复杂的查询条件，甚至可以作为一个完整的SQL操作请求的描述。为此，EF彻底将Entity和Query绑在了一起。这种思想，使得——

1.    开发人员需要编写的类更少。开发人员无需编写其他类来描述复杂的SQL查询条件。也无需编写代码将这些查询条件转换为SQL/HQL/JPQL。DAO层也不会有老要改来改去的接口和API，几乎可以做到零编码。

2.    对单个对象进行CRUD的操作API现在和Criteria API合并在一起。Session对象可以直接提供原本要Criteria API才能提供实现的功能。API大大简化。

3.    
            IQueryableEntity允许你将一个实体直接变化为一个查询（Query），在很多时候可以用来完成复杂条件下的数据查询。比如 ‘in (?,?,?)’， ‘Between 1 and 10’之类的条件。
            xxQL有着拼装语句可读性差、编译器无法检查、变更维护困难等问题，但是却广受开发人员欢迎。这多少有历史原因，也有Criteria API设计上过于复杂的因素。两者一方是极端灵活但维护困难，一方是严谨强大而学习和编写繁琐，两边都是极端。事实上JPA的几种数据查询方式存在青黄不接的问题。选择查询语言xxQL，项目面临后续维护困难，跨数据库移植性差；选择Criteria API，代码臃肿，操作繁琐，很多人望而却步。EF的设计思想是使人早日摆脱拼装SQL/HQL/JPQL的困扰，而是用（更精简易用的）Criteria API来操作数据库。

不再执着于POJO、还带来了另外一些方面的好处，比如继承DataObject类后可以实现对set方法调用的记录，判断一个字段有没有被赋值，用来优化查询、更新等操作。还有延迟加载特性需要每个entity中记录延迟加载的相关信息。

Q：哎~~我以前用过H框架和My框架都是用POJO映射记录的啊。

A：基于API的功能和易用性之间平衡，EF-ORM最终还是决定让所有的实体实现一个接口，好在这个接口倒无需编写任何代码，直接继承jef.database.DataObject就可以了。从使用者编写的代码来看，倒也能算是”伪POJO”，并不会增加开发的复杂性。

Q:继承DataObject对象后，实体变重了，会不会影响性能？

A:DataObject中的所有字段都是默认为null的，构造方法中不会对其进行初始化，因此对性能几乎没有影响。事实上和H框架的对比测试结果表明了这一点。

Q:Java的单继承结构决定了类只能继承一个父类，这会不会影响开发设计的灵活性？

A:多个Entity依然可以互相继承，只不过是最顶层的Entity需要继承DataObject类。对灵活性的影响多少有一点，但并不明显。

Q:如果我要将Entity使用反射拷贝到别的对象，或者转换为JSON/XML。那么这些父类中的属性不是会干扰我功能的正确性吗?

A:这个问题也考虑过了。父类DataObject中设计的方法，都是和POJO的getter/setter的格式有区别的，反射中一般不会认错。其次这些属性都是标记为transient的，大部分序列化框架（包含JDK内部序列化、Google Gson、FastJSON、JAXB等在序列化时都会忽略这些属性）。

Q：但是前面也演示了直接使用POJO操作数据库，这样用不行吗？

A:在小型项目中可以这样用，但这样做并没有发挥出EF-ORM的特色，事实上这样做性能较差，而且EF-ORM特色Criteria API也无法使用了。POJO的支持更多是为了兼容性的考虑，以及降低一些特定条件下的使用门槛设计的。支持POJO的API都集中在CommonDao上，这是在Spring集成下的一个通用DAO实现。实际上EF-ORM的核心API中都是不支持POJO的。

总之，POJO的问题就这样了，如果您有耐心继续向后阅读，就会明白为什么在这个地方做出如此的取舍，根本原因在于EF-ORM框架和JPA体系框架的设计思想的不同。

### 1.2.3.   实体的增强

细心的同学还会问，从上面的例子看，Foo2.java中setName()方法的代码中只有一个简单的赋值语句，没有任何监听器之类的设计，EF-ORM怎么可能监测到这个赋值语句被执行呢？

这就必须解释，在OpenJPA和H框架中都有的增强（Enhancement）概念了。实际上在上面的例子中，我们执行了

这个调用实际上对我们编译后的Foo2.class文件进行了代码植入，这种植入称为增强。EF-ORM遵循Java EE 中的JPA规范，允许为实体类进行增强。增强的意思就是说在编译或者运行时修改实体类的一些行为，使得实体能支持更好的ORM特性。

关于实体增强，有兴趣的朋友可以阅读一下OpenJPA中的介绍：[http://openjpa.apache.org/entity-enhancement.html](http://openjpa.apache.org/entity-enhancement.html)

H框架中也有增强，以前H框架总是要依赖一个CGlib，现在则是离不开javassist，就是这个道理。

和H框架在运行时动态执行增强不同，EF-ORM为了更好的性能，直接使用ASM(以前也用过Javassist)直接对磁盘上的静态类做增强。我们分三种情况描述EF-ORM是如何让增强操作不再困扰您的。

#### 1.2.3.1. 使用Maven构建时

可以配置Maven-Plugin，使其在编译完后自动扫描编译路径并执行增强操作。jef-maven-plugin-1.9.0.RELEASE.jar在之前的下载包中可以下载到。

 

#### 1.2.3.2. 在Eclipse中运行单元测试时

Eclipse会经常自动构建，因此即使我们执行了手工增强，也不能保证当运行单元测试时，类已经做过增强，比较直接的办法就是和上面的例子一样，在单元测试的开始(@BeforeClass)，执行一次 

#### 1.2.3.3. 在开发调试时

JEF插件可以支持在开发时动态增强实体，其原理和前面的三种方式不同，是动态的。通过使用自定义的ClassLoader，在类加载时自动增强类。

操作方式如下： 

JEFApplication是运行指定类的Main方法。JEF Web Application则是启动一个内置的Jetty，然后自动查找WEB-INF目录，并按Java的Web开发规范，将工程发布出来。

这两种方式都十分简单。使用这种方式运行的程序中的Entity都不需要手工增强，过程对开发人员透明，有利于编码时提高效率。

其中，Run As / JEF WebApplication中，您可以在Run Configuration界面中调整Context path和Web发布端口。

 

如果要用调试模式运行，启动时选择 ‘Debug As’即可。

三种场景下，EF-ORM都提供了相应的增强操作支持。除了Eclipse中运行单元测试外，你都无需去关注增强操作的存在。

#### 1.2.3.4. 手工增强

最后，还有一种手动方式，可以修改编译目录下的class文件进行增强。

我们在**工程**上点右键，上下文菜单中就有“Enhance JEF Entites“

 

使用这里的Enhance EF-ORMEntities功能，默认会选中编译输出class文件夹，不用作任何修改直接点确定。

然后可以看到控制台输出：

这就说明class文件已经被修改。

 

 

## 1.3. 创建实体

### 1.3.1.   从数据库创建实体

先选中我们要生成映射类的包。右键上下文菜单中有“Generate JEF Entity from Database“.

 

 

 

点确定后，出现检测到的数据库表名称，选择其中要映射的表。最后点击‘确定‘。

正常情况下EF-ORM生成的实体会自动判断数据库主键和主键生成策略。但不会对外键和多表关联操作进行生成，要使用级联功能，您需要按第四章进行操作。 

### 1.3.2.   从PDM文件导入实体

先选中我们要生成映射类的包。右键上下文菜单中有“Generate JEF Entity from PDM“.

 

然后按提示步骤操作即可。

 

一个自动生成的实体可能如下所示：正常情况下所有的主键、表名、字段长度定义都已经以Annotation的形式标注出来。这意味着你可以用这个实体直接进行表的操作了。

 

自动生成的实体中，关于自增主键的生成规则‘GeneratedValue’还有各种多表关系一般需要手工调整，手工修改JPA注解等介绍参见后文。

### 1.3.3.   手工编辑和常用JPA Annotation

#### 1.3.3.1. 注解的使用

除了上述两种导入方式外，您也可以直接编码来创建实体。请看下面这个代码例子中的注释信息

总结一下，

1、上面提到了常见数据类型的定义方法。

而常见的数据类型不外乎varchar,blob,clob number, integer, nclob, nvarchar, double, float, bigint,Boolean等。

值得一提的是，您可以用java当中的Boolean类型。EF-ORM会根据方言的不同，支持原生boolean类型的数据库中，映射为Boolean，不支持原生Boolean的则映射为char(1)。

2、索引的定义方法

3、复合主键的定义方法

4、自增键的定义方法。需要指出的是，如果数据类型为String，而GenerationType=AUTO或IDENTITY时，会使用GUID生成主键，此时要求字段长度36位。

 

    另外，上例中@Table(schema = "ad", name = "ca_asset") 中指定了表所在的Schema为ad，这项配置是可以在运行环境中被重新映射的，相关特性参见7.3.1节Schema重定向。

#### 1.3.3.2. 映射关系和注解的省略

许多时候注解可以省略，省略时，EF-ORM会根据默认的数据类型计算相应的数据库存储类型。事实上，columnDefinition也可以写成int, double, integer,varchar2等各种标准的SQL类型。因此，如果不指定Column类型，EF-ORM会默认的生成Column的类型如下

| **Java****类型**     | **数据库类型**              |
| ------------------ | ---------------------- |
| String + @Lob      | CLOB                   |
| String             | Varchar(255)           |
| Int / Integer      | Integer /  Number(8)   |
| double/Double      | Double /  Number(16,6) |
| float/Float        | Float /  Number(16,6)  |
| boolean /Boolean   | Boolean / char(1)      |
| Long / Long        | Bigint /  Number(16)   |
| java.util.Date     | timestamp /  datetime  |
| java.sql.Date      | Date                   |
| java.sql.Timestamp | timestamp /  datetime  |
| byte[]             | BLOB                   |
| Enum               | Varchar(32)            |

                               表 1-1 映射关系

上表列举了您未定义Column注解时，java字段到数据库的映射关系。这也是建议您在实际使用注解时进行的映射方式。在实体加载时，EF-ORM会适配实体与数据库的映射关系，如果发现属于无法支持的映射（EF-ORM能兼容大部分不同数据类型的映射，包括从String到number等），那么会抛出异常，此时您需要修改java的映射字段类型。

    值得注意的是EF-ORM支持您使用enum对象和varchar列发生映射，使用枚举类型有助于提高您应用系统的可理解性。

 

# 3.    基本操作与对象映射

## 3.1. 基本单表操作

本节介绍几种常用的单表查询方式，EF-ORM还支持更多复杂的查询，后面几节中还会继续描述。

在Hello,World!案例中，我们已经了解了实现单表基于主键进行增删改查操作的方法。我们来回顾一下，在此处和后面的代码清单中，我们都在session对象上操作数据库，这里的session对象可以理解为就是DbClient对象。，

在上面的Foo对象中，其Id属性被标记为主键。当id属性有值时，load方法,update方法、delete方法都是按主键操作的。上面的操作，归纳起来就是插入记录、按主键加载、按主键更新、按主键删除。

### 3.1.1.   复合主键

当有复合主键的场景下，情况是完全一样的：

在所有作为主键的列上增加@Id注解。

代码src/main/java/org/easyframe/tutorial/lesson2/entity/StudentToLesson.java

** **

**调整复合主键顺序**

在上例中，我们执行

建表时的定义是

在复合主键中studentId在前，lessonId在后，怎么样其调整为lessonId在前，studentId在后呢？ 

对于1.8.0以下版本，方法是将lessonId的定义移动到studentId之前。

1.8.0以后的版本，是在枚举的enumField中将lesseionId移动到studentId之前。

 

有同学可能会问，你搞这个功能有什么意义呢？复合主键字段顺序的无论怎么变化其产生的约束效果是一样的。

实际上这不一样。数据库会为复合主键创建一个同名的复合索引。在上例中，我们如果要查询某个学生(studentId)的课程选修情况，那么studentId列在前可以让我们无需再为studentId创建单独的索引。事实上Oracle DBA们往往会称其为“复合索引前导列”。精确控制复合主键的顺序可以改善你的数据库设计中的索引规划，而无故增加索引则会造成每次CRUD时无谓的开销。

 

**其他**

使用过JPA的同学可能会说，EF-ORM这里的处理不规范啊，JPA规范中碰到复合主键都是必须定义一个@IdClass，要么作为一个独立类要么用@EmbeddedI嵌入对象中，你这种支持方法不是标准JPA的支持方式。

我的回答是：JPA的规范在实际使用时问题很多。由于凭空增加了一个对象的关系，在实际编码时需要增加大量的非null判断、主键对象必须实现hashCode和equals方法、主键值设置时是替换主键对象还是更新主键对象方法不统一，操作起来问题很多。这种坏味道甚至影响了代码的健壮性。实际情况下，大部分JPA使用者和H框架使用者会主动的给表增加一个物理键，避免使用业务键（复合主键），但操作中又基本上使用业务键来增删改查，物理键几乎无用。这样的因素也将影响DBA对数据库的设计发挥。

因此，在EF-ORM中，不支持标准JPA的复合主键方式，而是采用上文所述的方式。就像传统关系型RDBMS表结构定义那样，为平铺的属性增加两个主键的标识即可，类似于在PowerDesigner中定义主键那样。我还是那么想，既然用了关系数据库，就要让关系数据库的优势体现出来。

 

### [3.1.2.   主键自增

#### [3.1.2.1. 自增值自动回写

    正常情况下，执行插入操作后，数据库生成的自增值主键值都会被设回对象中，我们看下面的示例。

src/main/java/org/easyframe/tutorial/lesson2/entity/Student.java

 

src/main/java/org/easyframe/tutorial/lesson2/Case1.java

    从上例可以发现，当insert方法执行后，自增值即被回写到对象中。每次调用Session中的操作方法，都是即时操作数据库的。再说一次，EF-ORM是JDBC的封装，目的是帮助用户更精确和更简便的操作关系数据库。所以不会有一个“对象池”的概念，将您所有操作记录下来，到最后再执行。

    此外，无论在何种数据库下、无论在单条还是batch模式下（Batch模式以后会讲到），插入操作生成的值都会被回写到对象中。

    对于自增值的生成有很多用法，后文详细讲述。

#### [3.1.2.2. 四种JPA主键生成方式

EF-ORM支持JPA的主键生成方式。以下是JPA注解的实现方式

 

JPA注解中的GenerationType有四种，其用法如下

| 注解类型     | 含义                  | 在EF-ORM中的用法                              |
| -------- | ------------------- | ---------------------------------------- |
| AUTO     | 自动，根据当前数据库等自动选择自增实现 | 1.为int或long类型时，根据数据库自动选择 Identity  > Sequence > Table 。(由于数据库多少都支持前两个特性，所以实际上Table默认不会被使用 )  2.如果字段是String类型，使用GUID策略(即32位guid)。 |
| IDENTITY | 使用列自增特性             | 1. 为int或long类型时，使用表的列自增特性  2.如果字段是String类型，使用GUID策略(即32位guid)。 |
| SEQUENCE | 使用Sequence。         | 使用Sequence生成自增值                          |
| TABLE    | 使用Table模拟Sequence。  | 使用Table生成自增值                             |

 

如果不配置，那么会隐式使用Annotation的默认值AUTO。

下面列举几种规则的用法和效果，嫌麻烦的可以直接跳转到3.1.2.6章看结论。

#### [3.1.2.3. 配置的简化

上述四种配置的基础上，EF-ORM进行了一些配置的简化。形成了一套独有的规则。

**1 . ****识别为GUID**

当字段类型为String时，如果自增方式为Identity或Auto那么使用guid策略。 但如果为Sequence和Table，EF-ORM还是认为该字段属于自增值，数据库中会使用Int等数值类型，只不过在java类中自动转换为String。

** **

**2.     ****识别为AUTO**

当EF-ORM获得一个自增值的注解配置后，默认情况下会将Sequence和Identity两种方式忽略，理解为AUTO方式。因为Identity和Sequence方式都有跨数据库移植性差的问题，为了更好的支持数据库Native的自增实现方式， EF-ORM默认Identity和Sequence方式都等同于Auto。除非用户禁用Native支持功能，即在jef.properties中配置

 

**3****、按本地数据库能支持的方式实现自增**

对于AOTU类型，按 数据库列自增 > Sequence > Table这样的优先级，使用数据库能支持的方式实现自增。

对于Sequence类型，按 Sequence > Table这样的优先级，使用数据库能支持的方式实现自增。

对于Table类型，使用Table模拟Sequence方式实现自增。由于默认情况下数据库多少都会支持优先度更高的两种特性之一。所以Table方式会很少用到。

 

4、**不使用数据库列自增方式的场合******

以下两种情况下，EF-ORM会认为列自增特性不可使用

l  数据库方言中指定不支持的，如Oracle，这是大家容易想到的。

l  对象开启了分库分表功能的，由于分库分表后列自增值不能保证唯一性，因此会认为不支持。

#### [3.1.2.4. Sequence或Table下的其他配置

可以用JPA注解@SequenceGenerator或@TableGenerator两者，为自增值设置Sequence名或表名。

例如：

    通过这种方式，您也可以让多张表共享一个Sequence或Table。

 

需要注意的是，@SequenceGenerator仅在GenerateType为SEQUENCE时生效，@TableGenerator仅在GenerateType为TABLE时生效。这里的GenerateType不是指你注解里写的是什么，而是按照3.1.2.2的简化规则计算后生效。 也就是说，这两个注解不会影响EF-ORM用什么方式来实现自增，只会在确定自增方式之后，调整自增的实现参数。

默认情况下，您都不需要配置这两项内容，EF-ORM全局配置参数中可以配置命名模板，会根据模板来计算Sequence的名称。如果您单独配置了Sequence的名称，那么会覆盖全局配置。但从项目实践看，我们更建议您使用模板的方式，全局性的命名Sequence。

默认情况下，全局Sequence的名称为“%_SEQ”,其中%是表的名称。也就是说，名为”TABLE1”的表，其使用Sequence为”TABLE_SEQ”。

 

如果表的名称大于26个字符，会截取前26个字符。如果你有两张表名称达到或超过26个字符，并且前26个字符都是一样的，那么这两张表会公用同一个SEQUENCE。这是为了防止出现SEQUENCE名称大于30个字符，在Oracle下你无法创建大于30个字符的数据库对象。

前面既然说了Sequence名称模板默认为”%_SEQ”，那也意味着您可以修改名称模板，比如您可以把它改为”S_%” 。修改方法是jef.properties中。

 

对于Table模拟Sequence，也可以全局配置， 可以是全局一张表实现所有Table Sequence值。例如创建一张名为GLOBAL_SEQ_TABLE的表，要启用此功能，可以在jef.properties中

 

综上所述，一般情况下，我们不需要使用@SequenceGenerator 和 @TableGenerator注解。

#### [3.1.2.5. Sequence或Table的性能优化

**Hilo****算法**

熟悉数据库的人都知道，大量数据并发插入时，频繁到数据库中去获取Seqence性能是很差的。

某H框架支持hilo算法来优化Sequence的取值性能，原理是对获得到的值乘以系数，将值的范围放大后自动填充范围内的值，从而一次访问数据库客户端能得到多个不重复的序列值。

在EF-ORM中，将Hilo算法作为一种修饰器。当使用Table和Sequence两种方式，都可以添加Hilo算法的修饰。

比如

或者

其中maxLo的含义和H框架相同。每次都是生成value *(maxLo + 1) 到 value * (maxLo +2)范围内的值，算法此处不解释，请自行百度。

   @HiloGeneration仅对SEQUENCE和TABLE两种算法有效，如果按上一节的规则，最终使用数据库列自增方式，那么@HiloGenerator注解会失效。比如我们可以配置下面的自增主键：

上面的注解配置，在不同数据库下会产生不同的表现——

在MySQL下，@HiloGeneration、@SequenceGenerator、@TableGenerator都不会生效。（使用自增列，故这些生成规则都无效）

在Oracle下，@HiloGenerator和@SequenceGenerator会生效。（Oracle下默认使用SEQUENCE）

如果这张表启用了分库分表功能，那么在Oracle下情况不变。而在MySQL下，因为MySQL不支持Sequence，最终实际上变为TABLE模式，此时@HiloGenerator和@TableGenerator即会生效。

 

**步长跳跃**

这是另一种优化方式，步长跳跃的意思就是让Sequence或Table中的值不是每次自增1，而是一次性跳好几个数值，然后由EF-ORM自动填补这中间的空白整数。也可以起到一次访问数据库获得多个序列值。

使用Hilo的情况，如果随意修改Hilo的参数，很有可能造成主键冲突。此外数据库中的序列值和表中的ID之间也很难对应起来。从数据库维护角度来看很不直观，因此EF-ORM更为推荐步长跳跃这种方式来优化序列值的获取。

事实上，在使用TABLE方式时，EF-ORM已经用了这个方法一次性将表中的Value值加上较大的一个值。这个值就是 sequence.batch.size，默认20，也就是说一次访问数据库就获得20个序列值。

而如果使用数据库原生的Sequence，那么如何优化呢？

对于Oracle数据库，你可以直接将Sequence的步长调节为更大的值，例如40.

 

EF-ORM会在首次使用Sequence前，检查Sequence的步长，这是自动的。非Oracle下，EF-ORM不会主动检查步长。

如果在非Oracle数据下使用Sequence跳跃，例如Postgres，可以使用jef.properties参数

 

当配置为>0的数值时，Sequence步长固定为指定值（不推荐这样做），因为这样配置时如果和数据库中的Sequence实际步长不匹配，可能出现错误。

正常情况下，如果步长为5，selectX_SEQ.nextVal from dual如果返回结果=10，那么实际上EF-ORM会获得10,11,12,13,14这样五个值。再次调用后，数据库端的Sequence值=15，实际使用序列号到19为止。

由于非Oracle数据库没有有效的Sequence步长检测机制，在非oracle上检测步长将消耗掉一个Sequence值。

在Oracle上，如果数据库用户权限被严格限制，可能会无权限访问user_sequences视图，此时您需要将db.sequence.step配置为一个正数来避免自动检查步长。

#### [3.1.2.6. 配置方法和总结

对前面的种种规则比较含糊的同学，直接看这一节就行了。

| ** **                | **H****框架中的用法******                      | **EF-ORM****对应******                     | **EF-ORM****下特点******                    |
| -------------------- | ---------------------------------------- | ---------------------------------------- | ---------------------------------------- |
| identity             | 数据库列自增特性，不支持Oracle。                      | @GeneratedValue(strategy = GenerationType.IDENTITY) | 在Oracle下自动转换为Sequence                    |
| sequence             | 仅在支持SEQUENCE的数据库上可用，必须指定Name。如果没有指定则默认为全局唯一的’hibernate_sequence’。 | @GeneratedValue(strategy = GenerationType.SEQUENCE) | Oracle等支持Sequence的数据库下Sequence会自动命名、自动创建。  MySQL等不支持的数据库下自动转换为Identity或Table。 |
| hilo(table hilo)     | 使用高/低位算法生成数值型，给定一个表和字段作为高位值的来源，默认的表是hibernate_unique_key，默认的字段是next_hi。它将id的产生源分成两部分，DB+内存，然后按照算法结合在一起产生id值。目的是减少访问次数提高效率 | @HiloGeneration  @GeneratedValue(strategy = GenerationType.TABLE) | 可支持所有数据库。通过序列步长调整也可保持高性能。                |
| native               | 对于 oracle 采用 Sequence 方式，对于MySQL 和 SQL Server 采用identity（自增主键生成机制） | @GeneratedValue(strategy = GenerationType.AUTO) | Sequence可以做到一表一Sequence。 自动命名和创建         |
| seqhilo              | 由于局限于支持Seq的数据库，所以少用。                     | @HiloGeneration  @GeneratedValue(strategy = GenerationType.SEQUENCE) | 默认情况下还可以使用数据库列自增。仅在Oracle下生效。            |
| uuid.hex uuid.string | 从H框架3.0开始已经不再支持uuid.string               | @GeneratedValue(strategy = GenerationType.IDENTITY) | 数据类型为String时自动适配。  效果和H框架一样              |
| assigned             | 人工指定                                     | 不配置                                      | EF-ORM可支持assigned，还支持手动和自增方式混用。          |
| foreign              | 使用外部表的字段作为主键                             | 不支持                                      | --                                       |

结论，对于大部分情况，大家只要对自增主键配置

就行了，别的啥都不用操心了。在常见数据库中，如果是在Oracle下发现获取Sequence不够快，那么可以按照3.1.2.5的方法进行优化。

### [3.1.3.   查询操作

在了解了单表插入和主键自增后，我们来看其他操作。在类jef.databse.Session当中，有着

load(T entity)和select(T entity)方法。之前的例子中我们一直用这个方法来查询插入的记录。EF-ORM会按照对象中存在的主键进行查询。这是不是这个方法的唯一用法呢？除了这个方法之外，有没有其他方法按主键查询对象呢？

#### [3.1.3.1. 按主键或模板查询

其实、前面用到的load方法和select方法并不仅仅是按照主键进行数据查询，它们其实是将传入对象当做一个模板来使用的，参见下面一组例子。

从上面的例子，我们可以发现

1.     load和select区别在于一个返回单条记录，一个返回多条记录的List。

2.     load和select对于传入的条件，判断其主键是否就绪，有则按主键查询，没有则将传入对象当做一个模板查询。

3.     另外有一个load(Class, Serializable…)的方法，可以直接传入对象类和主键值来加载单条记录。

4.     复合主键必须全部都赋值才认为主键就绪，否则也是将传入对象当做模板查询。

#### [3.1.3.2. 更复杂的条件

按主键或模板查询能适应不少查询场景，但还远远不够。比如我们要查询所有姓名中带有”Jhon”字样的学生。用SQL语句表达的话，就是

对象Student看似无法描述这种带有运算符的条件。但不要忘了我们的Student已经实现了jef.database.IqueryableEntity接口，这使Student对象携带复杂的条件成为可能。

代码清单： src/main/java/org/easyframe/tutorial/lesson2/Case1.java

在上例中，我们从Student对象中通过getQuery()得到一个Query对象，我们可以在这个Query对象上添加各种复杂条件。我们在后面的章节可以了解到，这里可以放入几乎SQL语句所能写出的所有查询条件和修饰，例如Order By等等。Query对象的出现，让我们意识到不仅仅是复杂条件可以被对象所描述。它会带来更多的灵活用法。

在Query对象中，我们可以添加条件，每个条件中可以包含运算符（Operator），在jef.database.Condition.Operator类中，定义了以下几种运算符

| **运算符**          | **对应SQL**                   | **含义**    | **要求参数类型**                               |
| ---------------- | --------------------------- | --------- | ---------------------------------------- |
| **EQUALS**       | =                           | 等于        | 文本、数值、日期等                                |
| **GREAT**        | >                           | 大于        | 文本、数值、日期等                                |
| **LESS**         | <                           | 小于        | 文本、数值、日期等                                |
| **GREAT_EQUALS** | >=                          | 大于等于      | 文本、数值、日期等                                |
| **LESS_EQUALS**  | <=                          | 小于等于      | 文本、数值、日期等                                |
| **MATCH_ANY**    | like ‘%*param*%’            | 匹配字符串任意位置 | 文本                                       |
| **MATCH_START**  | like ‘*param*%’             | 匹配字符串头部   | 文本                                       |
| **MATCH_END**    | like ‘%*param*’             | 匹配字符串尾部   | 文本                                       |
| **IN**           | in (*param,param*…)         | 属于（列表）    | 数组、List、Set、Collection、逗号分隔多值的文本         |
| **NOT_IN**       | not in(*param,param*…)      | 不属于（列表）   | 数组、List、Set、Collection、逗号分隔多值的文本         |
| **NOT_EQUALS**   | != 或者 <>                    | 不等于       | 文本、数值、日期等                                |
| **BETWEEN_L_L**  | between *param* and *param* | 介于xx和xx之间 | 数组、List、Set、Collection、逗号分隔多值的文本  (长度为2) |
| **IS_NULL**      | is null                     | 为NULL     | 无                                        |
| **IS_NOT_NULL**  | is not null                 | 不为NULL    | 无                                        |

这些运算符都可以通过addCondition方法加入到条件中。在传入参数时，注意部分运算符是只针对集合操作进行的，必须传入集合对象。

部分操作仅针对文本，如果传入非String对象，将被强制转换为String对象处理。

## [3.2. 你不是在操作‘对象池’

前面的例子中EF-ORM表现得和H框架非常相似，但是前面已经讲过，EF-ORM是基于数据库和SQL的，它的API是对JDBC的封装。而H框架则是视图让你面对一个“对象池”（一级缓存），要求你**按主键**来操作每一个对象。而EF-ORM完全没有这个限制，getQuery()带来的是概念上的变化。

下面是一个例子

代码清单：  src/main/java/org/easyframe/tutorial/lesson2/Case1.java

上例中，两次数据库操作对应的SQL语句已经写在注释代码里了。

我们可以发现，同样是我们前几章介绍的update方法和delete方法，在有了Query对象后产生了不同于之前的效果。它们并不仅仅只能按主键更新或删除某条记录。事实上在上例中，Student这个对象并不代表任何一个“学生”，它就是一条完整SQL语句，一次完整的数据库操作请求。

现在明白了IQueryableEntity的含义吗？每个Entity不仅仅是一条记录的载体，通过getQuery()方法，Entity同时也可以表现为一个SQL操作，这个操作并不仅仅限于单条记录。

默认情况下，Entity并不对应任何Query，但如果您将其用于查询、更新、删除操作，或者调用其 getQuery()方法，就可以让一个Query对象和Entity绑定。通过getQuery()方法和Query的getInstance()方法，你可以在互相绑定的两个对象之间转来转去。如下图：

 

图3-1 Entity和Query的关系

这对对象，在不同的操作场景下，用法是这样的——

| **场合****** | **处理规则******                             |
| ---------- | ---------------------------------------- |
| **insert** | Query对象无效。总是将Entity当做一条待插入的记录。           |
| **update** | Query部分是where条件，Entity中被设值的数据是update语句的set部分。  如果没有Query部分，那么Entity主键作为where条件，其他字段作为set部分。  如果Query部分和主键都没有，那么不构成一个update请求(抛出异常) |
| **select** | Query部分是where条件  如果Query部分不存在，那么Entity主键作为where条件。  如果主键没有设值，那么Entity作为模板，按其他设了值的字段查询。  如果Entity和Query都没有设置任何值，那么不构成一个select请求（抛出异常） |
| **delete** | 和select规则相同                              |

也就是说，前面的例子中，我们传入对象，并且按主键实现update、delete操作时，其实并不是像H框架那样在操作对象池，而是传入了一个SQL请求，是在操作一个关系型数据库。

看到这里大家应该已经明白为什么说EF-ORM和其他ORM不同的根本原因。EF-ORM的API，封装的其实是SQL操作。前面的基于对象主键的CRUD操作，其实是用SQL规则在模拟对象操作的一种“拟态”。

这是EF-ORM和H框架的根本性不同。也正是因为这些不同，我们可以解决很多其他ORM中不够灵活的问题，可以高效的操作关系型数据库。比如

等效于update set gender=’f’ where name=’Mary’。而在某H框架的API中，先不谈非主键是否可用作update条件的问题，即便是用主键做update条件，H框架也会提示你这是一个游离对象，不能直接做update。也就是说它规定了你只能先按主键查出对象(放入一级缓存)，再按主键写入到数据库里。试想如果我们要批量更新多条记录，对于这种场景循环逐条处理，效率会怎么样。

当然了某H框架还有法宝，用一种Query Language(HQL)来封装另一种Query Language(SQL)，这种想法也是挺有“创意”的。不过也不能老抓着人家的黑历史不放。事实上业界对于使用API和xQL来封装数据库操作的优劣早有定论。JSR 317 JPA 2.0规范中增加了Criteria API，其实是比xQL更方便并能提高软件可维护性的手段。H框架除了实现JPA 2.0规范以外，本身也有非常强大的Criteria API。个人认为H框架仍然是业界最好的ORM，没有之一。不过是觉得EF-ORM更适应在性能和数据规模有较高要求的行业而已。

回归正题，在理解了Query的作用之后，我们可以看一下Query的简单用法。

 

### [3.2.1.   查询条件与字段排序

查询免不了Order by, groupby, having等等。这里先说order by。

代码清单：src/main/java/org/easyframe/tutorial/lesson2/Case1.java

对应的SQL语句如下

 

那么，在这个案例中，我们如果增加一个条件，要求 student的grade=’3’，如何写呢？

代码清单：src/main/java/org/easyframe/tutorial/lesson2/Case1.java

上面的代码实际执行后，会发现无法达成预期目的。grade=’3’这个不会成为条件。

从前面的“拟态“规则中我们可以推出

 

* *

*当Query对象中有条件时，对象中的所有字段值将不再作为条件。*

* *



因此，在这个例子中，要增加一个条件，您必须这样写

### [3.2.2.   更新主键列

在其他的基于主键管理Entity的ORM中，使用API去更新对象的主键是不可能的，但是在基于SQL的EF-ORM中，这成为可能

代码清单：src/main/java/org/easyframe/tutorial/lesson2/Case1.java

上例中展示了旧的主键值作为条件，去替换新的主键值的用法。这是基于SQL封装框架的思想体现。

### [3.2.3.   特殊条件: AllRecordsCondition

最简单的，我们可以这样查询出所有的学生

 

那么如果我们需要学生按照学号正序排列，这个selectAll的API就无法提供了。实际上，selectAll这个方法是可有可无的，它可以用普通的select()方法来代替。按照以前掌握的技巧，您可能会这样写

不过您会收到一个错误消息

您可能会问，我就是要查全部啊，怎么还要放条件到Query里去呢？

在EF-ORM中，有一个特殊的条件，名叫 AllRecordsCondition，它是这样使用的。

 

代码清单：src/main/java/org/easyframe/tutorial/lesson2/Case2.java

这里，通过setAllRecordsCondition()。指定了查询条件。

为什么这么设计？不传入任何条件，就查出全部记录，这不是很方便吗？

这个设计的本意是为了防止用户出现误用，我们假定这样一种场景：
用户在查询前，通过若干if分支对条件进行赋值。如果用户没有考虑到的一条分支下，没有产生任何有效条件，而此时数据库中有大约一千万条记录。

大家可以想一想，是用户该次操作失败好一些呢，还是整个服务器因为内存溢出而退出要好一些？正是因为这种场景，我们希望开发人员在使用框架时想清楚，能不能承受将全部数据加载到内存的开销。

这个API的设计有人喜欢有人不喜欢。不过您可以在jef.properties中加上一行，让无条件查询等效于查询全部记录。

### [3.2.4.   QueryBuilder的使用

在使用EF-ORM的查询API进行数据操作时，您必然会接触到一个工具类QueryBuilder。

QueryBuilder提供了很多基础方法，用来生成Condition、Query等。

比如，前面我们都是用 一个Student对象来构造一个SQL操作的描述的。在了解了这个Student对象的本质，其实是一个SQL查询操作之后，我们可以换一个角度来看问题。

这是前面出现过的代码（使用QueryBuilder之前）

代码清单： src/main/java/org/easyframe/tutorial/lesson2/Case1.java

当改为使用QueryBuilder之后——

代码清单： src/main/java/org/easyframe/tutorial/lesson2/Case1.java

这样整个查询操作都以Query作为主体了。

之前的所有条件运算符，都有在QueryBuilder中生成条件的方法

| **条件方法****** | **对应****Operator****中的运算符****** | **备注******  |
| ------------ | ------------------------------- | ----------- |
| eq()         | **EQUALS******                  | 等于          |
| gt()         | **GREAT******                   | 大于          |
| lt()         | **LESS******                    | 小于          |
| ge()         | **GREAT_EQUALS******            | 等于          |
| le()         | **LESS_EQUALS**                 | 小于等于        |
| matchAny()   | **MATCH_ANY**                   | 匹配任意位置      |
| matchStart() | **MATCH_START**                 | 匹配尾部        |
| matchEnd()   | **MATCH_END**                   | 匹配尾部        |
| in()         | **IN**                          | 属于          |
| notin()      | **NOT_IN**                      | 不属于         |
| ne()         | **NOT_EQUALS**                  | 不等于         |
| between()    | **BETWEEN_L_L**                 | 介于..之间      |
| isNull()     | **IS_NULL**                     | 为NULL       |
| notNull()    | **IS_NOT_NULL**                 | 不为NULL      |
| like()       | **无，可用SqlExpression()组合成条件**    | LIKE        |
| or()         | **无**                           | 将多个条件以OR相连  |
| and()        | **无**                           | 将多个条件以AND相连 |
| not()        | **无**                           | 一个条件修饰为NOT  |

 

QueryBuilder有13个字符，所以做了一个别名，我们可以将QueryBuilder简写为QB。QB和QueryBuilder使用上没有任何不同。例如，上面的示例还可以写成——

   

### [3.2.5.   Criteria API和查询语言

    前面讲过，EF-ORM的API封装的其实是SQL操作。有朋友问：ORM框架顾名思义是将E-R的关系型结果转换为对象模型来操作，最好是映射成JDO一般，把大家从写SQL语句中解放出来。你怎么能把框架设计成这样？

我的回答是：关系型数据库的设计有其经典的理论支持，也有众多的约束和性能局限，这些都是必须使用者去面对去解决的。如果完全像一个对象数据库操作，那么我们项目中根本不需要使用MySQL或是Oracle。NoSQL的数据存储满大街都是，对内存的使用率更高性能也更好。只满足主键操作的情况下，ORM根本连存在的意义都没有，redis,mongodb等大堆流行技术爱谁用谁。

正是因为SQL和E-R思想的必要性，在实际项目中，使用H框架的人最后不过是从SQL中跳出，又掉进HQL里去。有什么被解放了？使用RDBMS的人，谁能不学习SQL，谁能不了解SQL?

SQL的思想（而不是语法）才是关系型数据存储的精髓，要在RDBMS上做出性能优秀的应用，不可能不掌握这种思想。正是因为这样，EF-ORM试图在保留SQL的思想、淡化SQL的语法的方向上封装JDBC。

这包括了用JavaAPI代替查询语言解决SQL编译检查和开发效率的问题；用Java反射等特性解数据库行、列到数据对象的自动转换；使使用者更简单（而符合E-R关系）的方式操作数据库。

因此，EF-ORM封装后提供给开发者的API，不是面向对象操作的API，依然是一套基于SQL思想的API，只不过其中面向转换等操作被自动化了。用通俗一点的说法，EF是披着某H框架的外皮，但骨子里是IBatis的思想。

上面我们讲这套用于操作数据库的API，其实就是一直在说的Criteria API（非JPA标准的）。其实严格来说EF-ORM中只有两种数据库操作方式，即Criteria API和NativeQuery。之前所有模仿H框架的单记录操作都是用CriteriaAPI实现的，正如前文说的——“拟态”。

 

图3-2 EF-ORM的操作方式

 

另外的一个操作体系是NativeQuery，这将在第7章讲到。NativeQuery其实就是对SQL的直接封装和改良。用户直接提供SQL语句，EF-ORM负责对其进行管理、解析、动态改写、参数化等工作。

原生SQL则是可以让用户直接将SQL语句提交给数据库，EF-ORM不作任何处理，只不过允许其和其他操作处于同一个事务当中。这算不上是一种操作方式。

 

以JPA为代表的ORM应用， 最后都增加了Criteria API和xxQL的功能，来支持直接操作数据库表，这些都是与时俱进下不得不做出的改变，只是Criteria API来的有点晚，并且给人太多选择，实际上很多人已经走了弯路。而且个人觉得，JPA Criteria API设计得有点不接地气，虽然类型校验能力是很强，但是对开发者过于繁琐。比如——查询一张表里所有记录的条数：

 

**SQL**

评价：简单直接。

 

**JPA(Criteria API)**

评价：充满了专家组的风格，极其严谨的API，每个对象每个概念都一清二楚，显式声明数据类型和可校验。但是世界上大部分人都是懒人，不是专家。这样的API是不足以诱使人们放弃xxQL的。

 

某H框架的写法(Criteria API)就好了很多

过渡到H框架的Criteria API也不错，不过对某H框架不熟的的同学请先学习什么叫“投影统计“。

 

EF-ORM写法则更为简单：

没有完全按照JPA来设计API的重要原因之一就是，大部分开发人员对J2EE规范都是选择性遵循的，EJB的普及比起Spring来在国内已经落后得太多。更接地气的东西才更有存在的意义。

# [4.    Query API单表操作

前面的许多查询和更新示例，其实就是在使用Criteria API。这套API涵盖了实体操作。囊括了单表的各种SQL操作和多表下的操作。我们先了解还有哪些单表下的操作用法。

注意：下面的例子都是在某些场景下，需要达到某个目的的API用法。

但这些用法不是一成不变是，是可以随意组合的。此外，列出的也不是实现目的的唯一方法。

请在理解其含义后灵活使用。

## [4.1. 查询

### [4.1.1.   查询条件的用法

我们先从一组示例来理解和使用CreteriaAPI的单表查询操作。

#### [4.1.1.1. And Or Not

在SQL条件中，我们经常会碰到这三种组合的场景。下面这个案例中组合使用了And Or Not三种条件连接。可以发现，Condition作为一个条件描述，可以互相嵌套，从而描述出And Or Not之间的顺序关系。

 

代码示例:src/main/java/org/easyframe/tutorial/lesson3/Case1.java

 

对应SQL语句为：

   这里要指出的是，当使用Like语句时，为了支持传入的参数中存在 % _等SQL字符的场景，EF-ORM会自动进行转义。同时也能防止注入攻击。

   上面的写法还是稍显累赘，前面已经说过，QueryBuilder这个类可以省略写作QB。

   此外上例中的最外层的And嵌套可以省略。因为Query对象中可以填入多个Condition，这些Condition互相之间就是And关系。省略最外层的嵌套后，写法为——

 

#### 4.1.1.2. 使用函数或表达式作为条件

使用函数运算作为查询条件，对数据库字段计算函数

下面例子中，

 

用FBIField可以对数据库中的字段进行函数运算。上例的条件实际执行SQL如下：

FBIField这个词可能会引起吐槽。其实这个词和美国的FBI没有什么关系。由来是Oracle的Function based index。即函数列索引。许多数据库都不支持将用函数值来创建索引，而Oracle支持并且将这类索引命名为FBI。

由于在where条件中使用函数会造成数据库索引无效，是无法在数据量较大的表中使用的。即便是Oracle上也需要专门为此创建一个函数索引。这里用了FBIField的名称即来源于此。上例的这种用法仅限于数据量小的表使用。

仔细的同学还会发现，concat函数被转化为了lower(gender)||grade这样的表达式。这其实是EF-ORM后文要提到的一个特点——方言转换功能，EF-ORM会将用户填入的JPQL或SQL表达式用当前数据库方言重写，以适应不同数据库的语法差异。这一功能主要针对NativeQuery查询，不过在这里也能生效。

在上面的例子中还要注意，FBIField中的表达式是用java模型的字段名来描述的。这里的gender、grade都是java字段名，不是数据库列名。 EF-ORM在实际查询前会使用真正的数据库列名替换这些Java属性名。这个替换的规则需要FBIFIeld对象绑定到一个Entity上。在单表查询中这不成问题，框架能自动完成绑定，但是如果在多表查询中，可能就无法准确的绑定到特定的对象上，这时候就需要用到FBIField的另两个构造函数。

这两个种构造函数可以让FBIField显式的绑定到一个查询Entity上。确保各个java属性能被正确的转换为SQL语句中的列名。

#### [4.1.1.3. 使用JPQLExpression

在Condition的Value部分，也允许自行编写表达式。EF-ORM提供了JpqlExpression和SqlExpression的类。

JpqlExpression和SqlExpression都是指表达式。但两者用法上还是有所不同的。这两个表达式的作用都是将制定的文本包装为数据库的原生SQL片段，将这部分SQL嵌入到我们的Criteria API查询中去，可以更为灵活和方便。

   下面是JPQLExpression的两个例子

src/main/java/org/easyframe/tutorial/lesson3/Case1.java

 

上面的代码演示了两个例子，第一个例子很简单，使用数据库的upper和nvl函数。

Nvl函数是Oracle专用的，Derby不支持，这里EF-ORM将其自动改写为coalesce函数。

第二个例子复杂一些了

这严格意义上是一个多表查询。不过我们也能看出其特点

l  整个表达式被嵌入到where条件中的值的位置。

l  表达式中的java属性名’dateOfBirth’，被替换为了数据库列名 ‘DATE_OF_BIRTH’。

其实就这是JPQLExpression的特点，和前面介绍的一样，EF-ORM会对JPQLExpression表达式进行解析和改写处理。这包括数据库方言适配和字段名称别名匹配。

从上面例子可以看出，表达式是一个强力的工具，灵活使用表达式能让EF-ORM简单的API发挥出预想意外的强大功能。

#### [4.1.1.4. 使用SqlExpression

上一节的案例二，也可以用SqlExpression来编写，写法上稍有不同

生成的SQL语句和上例是一样的。

SqlExpression相比JpqlExpression，是更接近SQL底层的表达式。EF-ORM不会对SqlExpression进行改写。也就是说，SqlExpression对象中的字符被直接输出到SQL语句当中，为此你需要保证其中的内容符合数据库的语法，并且用作参数的是数据库列名，而不是对象的属性名称。这里的列名date_of_birth 必须直接写成数据库里的字段名。如果写成 dataOfBirth那么肯定是要出错的。

SqlExpression可以提供更为灵活的功能。比如SqlExpression可以直接作为Condition使用。

当然，使用SqlExpression要兼容各种数据库，对开发者的SQL功底有一定的要求。

### [4.1.2.   使用Selects对象

#### [4.1.2.1. 定义查询的列

经常有DBA告诫开发人员说，在Select语句中，不要总是查t.*，而是需要那几个列就查哪几个列。

因此，我们在使用查询时，也可以指定要查的列。

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

当查询指定了若干列时，返回的对象中未指定选出的列都是null。只有选定的几个列的属性是有值的，因此查出的是一组“不完整”对象。

#### [4.1.2.2. 使用Distinct

刚才说到，QueryBuilder可以从一个查询请求中提取出 Selects对象，Selects对象是一个操作SQL语句select部分的工具。它对应到SQL语句的select部分，是select部分的封装。

使用selects工具，我们可以完成各种诸如列选择、distinct、group by、having、为列指定别名等各种操作。例如

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

上例中，通过在selects中设置distinct=true，实现distinct的查询。

#### [4.1.2.3. 使用Group和max/min

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

上例是Selects对象的进一步用法。通过Selects对象，可以指定SQL语句的group部分，同时可以执行count、sum、avg、max、min等通用的统计函数。

 

上例中还要注意一个问题，即查询对象的返回问题。在这例中，查询对象无法转换为Student对象了，因此我们需要设置一个能存放这些字段的数据类型作为返回记录的容器。最基本的容器当然就是Map和数组了。此处我们用一个String[]作为返回值的容器。

关于如何传入合适的返回结果容器，以及结果转换过程的干预等等，可以参见第8章，高级查询特性。

 

有朋友问，Oracle分析函数 partition by能不能用Query对象写出来。这个 是不支持的，因为这样的SQL语句只能在Oracle上使用，且在别的数据库上难以写出替代语句，因此此类语句建议您使用NativeQuery 来写。同时NativeQuery也无法自动将其改写为其他数据库兼容的语句，因此多数据库SQL方言也需要人工编写。

#### [4.1.2.4. 使用Having

上面的例子稍微改写一下，可以产生Having的语句

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

上面的查询产生的SQL语句如下

在count()后面，还可以用havingOnly。其效果是 count(id)将不作为select的一项出现，仅作为having子句的内容。

#### [4.1.2.5. count的用法

在Session类当中，还有一个常用的方法 count();

count方法和select方法经常被拿在一起比较，当传入相同的查询请求时，select方法会查出全部数据，而count方法会将请求改写，用数据库的count函数去计算这个查询将返回多少条结果。

比如下面这个例子——

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

这个案例使用了同一个Query请求去执行count方法和select方法，前者使用了count distinct函数，查询不重复的名称数量，返回15。后者去查询，但只返回了一条结果，因为在请求中要求最多返回一条记录。

从上面的例子来看，count方法总是通过改写一个“查询数据内容”的请求来得到数量。

 

再来看下面这个有点相似的例子

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

就count而言，两种方法产生的SQL语句是完全一样的，但两种查询的含义是不同的。第二个例子中，count语句是由使用者自行指定，作为一个返回的列上的函数操作出现的。这种方式是可以和group混用的。相当于使用者自定义了一条查count的SQL语句，同时这个Query对象也只能返回数值类型的结果。

因此上面两端代码，虽然都实现了count distinct功能，但是使用的机制是不一样的，请仔细体会。

 

API说明：将传入的普通查询请求转换为count语句的查询方法

| 方法                                       | 作用                                       |
| ---------------------------------------- | ---------------------------------------- |
| jef.database.Session.count(ConditionQuery) | 将传入的查询请求（单表/多表）改写为count语句，然后求满足查询条件的记录总数。 |
| jef.database.Session.count(IQueryableEntity,  String) | 根据传入的查询请求（单表），求符合条件记录总数。第二个参数可以强制指定表名。一般传入null即可。 |

#### [4.1.2.6. 使用数据库函数

刚才我们已经在group中了解了常见统计函数的用法。我们在另一个案例中稍稍回顾一下

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

上例中对统计列添加了别名，同时返回结果改用Map封装。

 

那么是不是就只能用几个基本的统计函数了呢？再看下面的例子

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

在上面这个例子中，使用了三个不同的函数，对结果进行处理。

此外，如果不是EF-ORM内部的标注函数，您可以可以直接输入函数名，比如在mySQL上，您可以这样用

 

上面的方法都是使用API创建的函数，还有一种用法，直接传入SQL表达式，这也是可以的。

看下面的例子。

#### [4.1.2.7. 在查询项中使用SQL表达式

整个查询部分使用SQL表达式。

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

上面是最接近SQL的写法。相当于整个select部分都直接用SQL片段表达了。

 

还可以这样写——单个字段的函数使用表达式。

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

单个字段使用表达式，为每个学生的出生日期增加24个月，SQL表达式后面也可以添加列别名等修饰。

上面两个例子中传入的都是SQL表达式。因此注意列按数据库的名称。

上面两例都对传入的SQL表达式进行了解析和改写。某些时候如不希望EF-ORM进行改写，可以使用rawExpression()方法

 

### [4.1.3.   分页

一般来说，分页查询包含两方面的内容，一是获取总数，二是查询限定范围的结果数据。为了准确的限定结果范围，排序条件必不可少。

EF-ORM对于分页的两步操作，总体上来将遵循以下建议。

关于总数的获取，我们可使用前文介绍的几种count方法（参见 4.1.2.5）。

关于结果范围的限定，我们可以使用EF-ORM提供的几个API （参见4.1.3.1）。

EF-ORM还提供了将上述分页行为封装在一起的操作对象，可以——

l  直接获取总数、总页数等分页信息。

l  可从头到尾进行顺序翻页或跳转，获取当页数据。

该对象名为PagingIterator的类。(参见4.1.3.2)。

#### [4.1.3.1. 限定结果范围

Session类中有以下几个方法，可以传入类型为IntRange的参数，这里的IntRange就可以用来限定结果范围。

 

| **方法**                                   | **用途说明**                                 |
| ---------------------------------------- | ---------------------------------------- |
| **Session.select(T,  IntRange)**         | 传入Entity形态的查询(单表/级联)，限定返回条数在IntRange区间范围内。 |
| **Session.select(ConditionQuery,  IntRange)**  ** ** | 传入Query形态的查询(单表、Union、Join均可)，限定返回条数在IntRange区间范围内。 |
| **Session.select(ConditionQuery,  Class, IntRange)** | 传入Query形态的查询(单表、Union、Join均可)，结果转换为指定类型，限定返回条数在IntRange区间范围内。 |
| **Session.selectForUpdate(Query,  IntRange)** | 传入Query形态的单表查询，可在结果集上直接更新记录。             |
| **Session.iteratedSelect(T,  IntRange)** | 传入Entity形态的查询(单表/级联)，限定返回条数在IntRange区间范围内。将游标封装为返回结果遍历器。 |
| **Session.iteratedSelect(TypedQuery,  IntRange)** | 传入Query形态的查询(单表/级联/Union)，限定返回条数在IntRange区间范围内。将游标封装为返回结果遍历器。 |
| **Session.iteratedSelect(ConditionQuery,  IntRange)** | 传入Query形态的查询(单表、Union、Join均可)，限定返回条数在IntRange区间范围内。将游标封装为返回结果遍历器。 |
| **Session.iteratedSelect(ConditionQuery,  Class, IntRange)** | 传入Query形态的查询(单表、Union、Join均可)，限定返回条数在IntRange区间范围内。将游标封装为返回结果遍历器。 |

   可以发现，EF-ORM查询接口高度集中。主要分为几个系列： select系列是查询出List结果。iteratedSelect是查询出游标待遍历。还有一个load系列的方法是查出单条记录的。

 

IntRange表示的是一个含头含尾的区间(闭区间)，和Java Collection中常见的前闭后开区间有所不同。比如表示第 1到10条记录。不是用new IntRange(0, 10)，而是用new IntRange(1, 10)。来表示，更为接近我们日常的口头语法。其实Java用习惯的人会更偏好前闭后开区间，以后可能会再考虑向下兼容的前提下支持。

示例，查询返回11~20条记录

src/main/java/org/easyframe/tutorial/lesson3/Case3.java

使用上述方法后，在不同的数据库下，框架会生成不同的分页语句。实现数据库分页。

#### [4.1.3.2. 使用PagingIterator

Session的API方法中，除了select系列、iteratedSelect系列、load系列外，还有一套pageSelect系列的方法。

| **方法**                                   | **作用**                                   |
| ---------------------------------------- | ---------------------------------------- |
| **Session.pageSelect(T,  int)**  ** **   | 传入Entity形态的查询(单表/级联) 和 分页大小              |
| **Session.pageSelect(ConditionQuery,  int)**  ** ** | 传入Query形态的查询(单表、Union、Join均可)            |
| **Session.pageSelect(ConditionQuery,  Class, int)**  ** ** | 传入Query形态的查询(单表、Union、Join均可)， 并指定返回结果类型 |
| **Session.pageSelect(String,  Class, int)**  ** ** | 传入NativeQuery形态的查询，并指定返回结果类型。            |
| **Session.pageSelect(String,  ITableMetadata, int)** | 传入NativeQuery形态的查询，并指定返回结果类型元数据（一般用来描述动态表的模型）。 |
| **Session.pageSelect(NativeQuery,  int)** | 传入NativeQuery形态的查询，查询结果类型已经在NativeQuery中指定。一般为传入命名查询（NamedQuery）. |

上面的后三个方法涉及了NativeQuery和NamedQuery，可参见第7章。

另外，还有pageSelectBySQL的方法，但该方法支持的是原生SQL，即EF-ORM不作任何解析和改写处理的SQL。此处先不介绍。

 

pageSelect系列API的特点是——

l  都返回PagingIterator对象，其中封装了分页逻辑

l  最后一个int参数表示分页的大小

l  都只需要传入查询数据的请求，框架会自动改写出count语句来查总数，当然传入 SQL或Criteria Query的对象的改写实现是不同的，不过这被封装在框架内部。

 

当得到PagingIterator后，我们可以通过以下的API来实现分页操作

 

| **方法**                                   | **作用**                                   |
| ---------------------------------------- | ---------------------------------------- |
| **jef.database.PagingIterator.getTotalAsInt()** | 获得总记录数（int）                              |
| **jef.database.PagingIterator.getTotal()** | 获得总记录数 (long)                            |
| **jef.database.PagingIterator.getTotalPage()** | 获得总页数。无记录时为0。例如每页10条，总记录数11时算2页。         |
| **jef.database.PagingIterator.setOffset(int)** | 设置跳过的记录数。例如如果从第1条记录开始返回，传入0；要从第11条记录开始返回，传入10即可。 |
| **jef.database.PagingIterator.getRowsPerPage()** | 得到每页的记录数                                 |
| **jef.database.PagingIterator.getPageData()** | 得到Page对象，其中存放了当前页中的数据和总记录数。              |
| **jef.database.PagingIterator.recalculatePages()** | 强制重新计算页数。正常情况下，只会查询一次总数。基于数据库的不可重复读特性（在处理过程中有新的数据插入或被删除），如需刷新总数需要调用此方法。 |
| **jef.database.PagingIterator.hasNext()** | 当前页后面是否还有数据。（实现Iterator接口）               |
| **jef.database.PagingIterator.next()**   | 返回当前页数据，同时页码向后翻一页。（实现Iterator接口）         |

 

 

我们用举例来看分页查询的用法

src/main/java/org/easyframe/tutorial/lesson3/Case3.java

注意PagingIterator对象是一个重量级的懒加载对象，其数据只有在被请求时才会去数据库查询。因此不适合作为DTO传递数据。要获得其实际信息，可用jef.database.PagingIterator.getPageData()方法。得到的Page对象较为适合作为DTO传输。

    上面的例子中，我们直接从pageSelect返回的PagingIterator对象中得到了Page这个对象。

   Page是一个适合于传输的POJO对象，其中只有两个属性

l  总数

l  当页的记录内容

事实上，分页经常用在Web界面显示上，需要从数据库获得的也就是这两个信息。

使用PagingIterator一个特点是，其对于范围的限定和目前大多数Web前端框架一致，都是从0开始的。

 

PagingIterator对象要重量得多，其中封装了很多分页逻辑等。但最后发现Web服务一般是无状态服务，不可能持有PagingIterator很久 ，所以从实际业务看PagingIterator对象使用频率不高。需要了解PagingIterator还提供了哪些功能的，可以自行阅读API-DOC。

 

 

 

### [4.1.4.   小结

总而言之，Criteria API各种组合下，用法十分灵活，在上面这些案例中，请体会每个方法的用法。更多的API，请参阅API-DOC

单表的Criteria API，能将一般项目中90%以上的常见SQL语句都表达出来。在多表情况下大约75%左右的SQL语句也都能表达出来。更复杂的SQL语句可能就要使用EF-ORM中的另一操作体系，NativeQuery了。

要设计一套易于使用，并且含义明确的查询API是相当困难的。JPA 标准Criteria API集合了众多专家的智慧才能这样严谨。EF-ORM中的这套API比较随意，很大程度上来自于众多用户的意见和想法而设计。所以——

如果有什么用法，你不确定是否可以支持，可联系作者或尝试阅读源码。

如果有什么用法，你决定应该支持但却没能很支持，请联系作者。

## [4.2. 更新

### [4.2.1.   基本操作

前面我们已经讲过，update请求用到了Entity-Query这对对象。Entity中的值描述更新后的值，Query描述更新的Where条件。

在3.2.2中，我们甚至更新了对象的主键列。那么我们先回顾一下基本的更新操作用法。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

上面列出了三种更新的场景。第一种，先从数据库读出再更新。第二种，指定主键后直接更新指定的记录。第三种，按条件更新记录。

这里比较有用的是每次update方法都返回一个数值，表示update操作影响到的记录数。如果为0，那么就没有记录被更新。

### [4.2.2.   更新操作Query的构成

EF的更新操作有点“神奇”的地方是，它总是知道哪些字段被设过值、哪些字段没有设值。EF是如何做到这一点的呢，我们来看这段代码。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

运行代码后，输出

这就是实现的机制。

每个实体都隐式扩充一个UpdateValueMap,用来存放需要更新的字段。每当我们调用方法startUpdate后，我们对实体的每个set操作都会被记录下来，放入到updateMap中去，当我们调用DbClient.update(T)方法时，只有这些修改过的字段才会被更新到数据库中去。

当我们调用set方法， updateMap中记录下当前设置的值。由此也可以判断出哪些字段被赋过值。

EF在处理时，如果某个字段（主键）被挪作Where条件使用，那么它就被从updateMap中去除。因此你就不会看到这种SQL语句了——

我们来试一下，如果将这个Map清除会怎么样，我们将上面代码中注释掉的 updateMap.clear()语句重新恢复。然后运行，发现日志中输出下面的文字，然后什么也没发生。

这就说明，由于updateMap中没有值，因此这就成了一个无效的update请求，框架自动忽略了这个请求。所以事实上，框架所做的——

不是在将数据库中的值更新为Entity中的值，而是将其更新为updateMap中的记录的值。

利用这一点，我们可以写出更多灵活的更新语句来。

### [4.2.3.   更多的更新用法

#### [4.2.3.1. 并发环境下原子操作的更新

这是一种基于数据库频繁操作的业务更新用法。

业务是这样的，有一张用户账户余额表，用户可以消费从余额中扣款，也可以向其中充值。这些操作可能同时发生。

我们在充值时，可以这样做。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

这看上去没什么问题。但我们想一想，如果在load出数据，到update开始之间，用户同时充值了100元并提交了。这会发生什么事？很显然，用户会杯具地发现，他充值的100元消失了。因为我们在load记录时没有锁表，update的时候数据被更新为充值前再扣款50元。基于性能考虑，即便在事务中，一般也不会在一个普通的select语句中锁表。

何况锁表并不是解决问题的最好方法，在业务繁忙的系统中，我们应当尽可能提高系统的并行度，让充值和扣款能同时发生显然并不是一个坏主意。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

基于较小的调整，我们可以将更新请求改成这样。显然，只有当扣款前的数值和预期的一样的时候，扣款才会发生，否则会再到数据库中查询出最新的值，重新扣款。

这种算法实际上是基于Compare And Swap(CAS)的一种乐观锁实现。但是很少人会在实际项目中这么写。何况我们码农处理多变的业务规则已经够烦心的了，还要考虑在开发时考虑设计乐观锁来?

他们有更简单的理由。他们说，用一个很简单的SQL语句就能准确的扣款50元。

没有比这更简单高效的用法了，只要操作一次数据库就可以完成任务。但是这给ORM提出了挑战，怎么样，能够用对象模型完成么？

我们可以这样完成一个扣款的原子操作。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

上例中，prepareUpdate()方法的作用，就是向updateValueMap中标记一个更新的表达式。而updateValueMap的存在，使得我们的Update请求可不仅仅将数据更新为一些字面常量，还可以更新为更多的SQL函数和表达式。

这种写法产生的SQL语句中的参数“50”没有使用绑定变量。对SQL语句绑定变量有严格要求的同学肯定会有意见。对于这些同学，可以这样写——

 

这里我们可以看到，表达式中可以使用绑定变量占位符，这个也是EF-ORM查询语言的特点（参见7 本地化查询）。绑定变量的具体值可以通过Query对象中的Attribute属性传入。

#### [4.2.3.2. 使用prepareUpdate方法

prepareUpdate还可以产生更多的灵活用法。比如，将一个字段的值更新为另外一个字段，或者一个已知的数据库函数。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

上例中，我们将todayAmount更新为表的另一个字段值相加，并将updateTime更新为数据库当前的时间。这体现了prepareUpdate的灵活用法。

当然，update中也可以使用JpqlExpression和SqlExpression。这两个对象在前面的Select中已经演示过了。

update的Query部分则可以用前面select中演示过的大部分语法，包括And or等复杂条件的组合。

最终，使用上述办法Entity能够表达出大部分的Update SQL语句的，体现了这个框架的目标——更简单的操作SQL，而不是用对象关系去代替数据库的E-R关系。

### 4.2.4.   UpdateValueMap的一些特性

#### 4.2.4.1. 回写

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

上例代码运行后，出现信息

这说明，在执行update操作的时候，更新的值会回写到对象中，覆盖旧值。这也使的对象具备同时描述更新前与更新后的值的能力。

 

同时上述案例也提供了另一种更新主键的操作方式。即使用对象中的主键值作为where条件，使用updateValueMap作为更新的语句。

#### 4.2.4.2. 自动清空

在更新操作完成后，updateValueMap将被清空，对象又重新开始记录赋值操作。

#### 4.2.4.3. stopUpdate和startUpdate

在每个实体的基类DataObject当中，提供了两个方法

| **方法**            | **用途说明**                                 |
| ----------------- | ---------------------------------------- |
| **startUpdate()** | 让对象开始记录每次的赋值操作。调用此方法后，向对象赋值会被记录到updateValueMap中。 |
| **stopUpdate()**  | 让对象停止记录每次的赋值操作。调用此方法后，向对象赋值不会更新到updateValueMap中。 |

要提到的是，对于从数据库中查询出来出来的实体，默认都已经执行了startUpdate’方法，此时对其做任何set操作都会被记录。因此我们可以直接在查出的数据上修改字段，然后直接更新到数据库。新构建的对象，也是出于startUpdate阶段的。因此大部分时候我们无需调用上述两个方法。

#### 4.2.4.4. 通过对比形成updateValueMap

 

ER-ORM提供了一些其他的方法来生成更新请求。首先、update之前可以和数据库中的旧对象进行比较来生成updateValueMap。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

通过DbUtils.*compareToUpdateMap()*方法可以比较两个对象的修改内容。另外有一个DbUtils.*compareToNewUpdateMap**（）*方法。其区别是，前者是将对比后的updateValueMap生成在old对象中，后者生成在new对象中。生成在new对象中的update操作更为适合在级联场景下，将多个表的数据都update成新的状态。

 

此外，当得到一个对象时，还可以将除了主键之外的全部数据都主动放置到updateValueMap中去。例如

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

某些时候，对象中的updateValueMap是空的。原因可能有——

l  对象刚刚被执行过update操作，updateValueMap被清空

l  直接从数据库中查出的对象，updateValueMap是空的。

l  类不增强，失去记录赋值变更功能。

l  调用过了stopUpdate()方法。

如同上例演示一般，第一次调用update方法是无效的。
对于updateValueMap信息缺失的情况，可以用一个DbUtils.*fillUpdateMap()*方法，将对象中除主键外所有属性都标记到updateValueMap中去。因此，第二个update语句相当与更新除主键外的所有字段，就成功操作了。

## 4.3. 删除记录

### 4.3.1.   概述

删除记录和查询记录是基本一样的。都是将Entity-Query当中的数据作为条件。因此几乎所有在单表select中的Query用法，都可以直接在delete查询中使用。反过来，delete中的用法也几乎都可以在select中使用。

区别是select查询返回数据本身，delete操作则返回被删除的记录条数。

### 4.3.2.   用法示例

#### 4.3.2.1. 基本操作

在学习了Select的用法以后，我们可以轻易的写出这样的delete用法。

代码： src/main/java/org/easyframe/tutorial/lesson3/CaseDelete.java

 

#### 4.3.2.2. 使用Query对象

在查询时，对Query对象的赋值操作对应于select语句中的where条件。以此类推，在删除时同样可以通过Query对象传递where条件。

上面例子都是将where条件写入到Query对象中，可对比等效的SQL语句。

# 5.    级联操作

级联操作，是指在多个对象建立固定的关联关系，EF-ORM在维护对象的记录时，会将这些关联表中的记录一起维护起来。 在查询数据的时候，也会将这些关联表的中相关记录一起查询出来。

 

当然上面说的是一般情况下，为了防止使用中通过关联查询获得过多的数据影响性能，现代的ORM框架大多都具备延迟加载特性（又名懒加载）。即只有当使用者请求到这部分数据时，才去数据库中查询。

级联操作除了广泛用于查询以外，还可用于插入、修改、删除等场景。

关联关系根据JPA的定义，一般分为以下四种

| 类型   | 注解           | java对象定义                              |
| ---- | ------------ | ------------------------------------- |
| 一对一  | @ OneToOne   | T                                     |
| 一对多  | @ OneToMany  | List<T> / Set<T>  /Collection<T>/ T[] |
| 多对一  | @ ManyToOne  | T                                     |
| 多对多  | @ ManyToMany | List<T> / Set<T>  /Collection<T>/ T[] |

    在JPA中，这些关系都使用Annotation来标注。除此之外，EF-ORM还扩展了几个标注，用来支持一些常用的数据库操作的场景。

## 5.1. 基本操作

### 5.1.1.   使用注解描述级联关系

我们通过案例来看，首先创建如下几个实体

EF-ORM支持JPA所定义的几个多表关联和级联操作（部分支持），这些定义包括：@OneToOne,@ManyToOne, @OneToMany, @ManyToMany。使用注解，我们可以在类上描绘出级联关系。例如

src/main/java/org/easyframe/tutorial/lesson4/entity/School.java

 

src/main/java/org/easyframe/tutorial/lesson4/entity/Person.java

接下来我们在后面的案例中演示这种配置的效果。

### 5.1.2.   单表操作方式的保留

    在使用这个模型进行级联操作之前，我们先看一下，这个模型依然可以进行单表的操作。这意味着前面所看到的基于单表的Criteria API等各种单表用法在这个场景上依然可用。

src/main/java/org/easyframe/tutorial/lesson4/Case1.java

执行上面的代码，可以看到执行的操作和前面的单表几乎一样。这个案例顺序执行了以下的SQL语句

    可以发现，除了在查询对象之外，Insert,update,delete操作都没有去维护级联关系。 而查询对象时默认会尝试用左外连接的方式查出用户所属学校，相当于开启了级联功能。

当然，增删改操作下的级联关系是可以维护的，这在后面会讲到。

 

这里我们阐述EF-ORM对级联关系的理解和处理。在EF-ORM中，级联操作途径不会破坏原有单表操作途径。

级联关系是一种可以灵活的修改和变更的**附加关系模型**。之所以说它是“附加的”，是因为EF-ORM中，级联关系是在保证了单表模型完整可用的基础上，补充的一种关系描述。

上例可以看出，补充上去的模型表现为这一段定义。无论类上有没有定义这个关系，都不影响这个这个类的单表操作。

 

这种设计的补充描述如下：

### 5.1.3.   级联操作的效果

刚才说了单表操作方式得以保留的特点，现在来看级联模型下的用法。

src/main/java/org/easyframe/tutorial/lesson4/Case1.java

这一次发现被执行的SQL语句多了不少，由于最后记录被删除了，所以下面解说一下每个步骤对应的SQL操作。

上面的例子，演示了级联操作的用法。还包括了混合了一次非级联更新操作。

    由于在Person中，同时存在 currentSchoolId和currentSchool两个字段，这两个字段将分别在单表操作和级联操作中使用。（即单表字段和级联描述字段同时存在）。

某些场合下，人为操作可能使Person对象的currentSchoolId和currentSchool可能指向不同的关联记录。为了明确的区分当前用户是使用级联模型操作，还是单表模型操作，EF-ORM将级联插入、更新、删除的API和非级联下的API显式的分离。

|      | 非级联操作    | 级联操作            |
| ---- | -------- | --------------- |
| 插入记录 | insert() | insertCascade() |
| 更新记录 | update() | updateCascade() |
| 删除记录 | delete() | deleteCascade() |

                    表5-1 级联和非级联操作API的对比

 

我们再看例子中的这几句。

显然，在设置新的School对象到Person上之后，person.getCurrentSchoolId() 属性依然指向旧的School记录。但是更新操作完成后。除了刚刚被插入数据库的School对象中的id字段更新为数据库主键之外， Person对象中的外键值也被正确的更新了。

也就是说，虽然过程中Person对象短暂的出现指向School对象不一致的问题，但是在级联操作完成后，ID指向将会被正确的维护。

 

通过分离的级联操作API，我们应该可以清楚的知道，插入数据库的值来自单表模型还是级联模型。

 

级联操作API分离的另外一个好处是，由于级联下不支持分库分表。因此API的分离更容易在分库分表时被掌握和控制。

 

最后，可能有人会问，select和load操作默认都是级联的。这能关闭吗？看下面这个例子

因此，级联查询开关是可以关闭的。

## 5.2. 使用注解定义级联行为

 

这里说的注解即(Annotation)。

### 5.2.1.   仅引用级联对象的单个字段

我们考虑这样一个场景——

Person类中有一个gender的字段，描述用户性别，M表示男性，F表示女性。

另外有一张DATA_DICT表，其中记录了M=男性 F=女性的对应关系。

在查询时，我们希望查出”男”,”女”的属性。而不关心DATA_DICT表中的其他字段。

查询时，很多时候我们希望**只引用目标对象中的个别字段**，并不希望引用整个对象。实际上这种情况在业务中很常见。

@FieldOfTargetEntity注解就是为这种场景设计的。

我们定义的DATA_DICT对象如下：

然后我们在Person对象中。定义一个“单字段”的引用字段。

 

上例中，我们指定了“只引用目标对象中的 text字段”。

每次查询时，就像是访问Person表本身的属性一样。可以查出存储在另一张表中的用户性别的“男”，“女”这样的字样。

   这种方式具有以下特点

1、 由于引用的字段较少，性能会比引用整个对象高很多。

2、 因为引用描述的不是一个完整对象。因此这种引用方式只对查询生效，对插入、更新、删除不会产生影响。

 

本节总结如下

 

@FieldOfTargetEntity

用于指定单字段引用。

| 属性    | 作用                                |
| ----- | --------------------------------- |
| value | String类型。字段名，表示仅引用目标entity中的指定字段。 |

 

我们建议在EF-ORM中使用单字段引用，不再引用整个对象，意味着查询效率的提高。此外，如果相同的引用关系（比如引用目标对象的两个字段），EF-ORM会合并处理（在SQL语句中指定引用的两个字段），因此不会造成多余的数据库查询。

### 5.2.2.   @JoinDescription、@OrderBy

#### 5.2.2.1. 定义与作用

   但是事实情况要比理想中的更为复杂，我们不会就为了存储一个 {M=男, F=女}这样的对应关系去设计一张表，现实中，往往会是这种情况——

DATA_DICT表中另外有一个type字段，当type=’USER.GENDER’时，才表示性别的对应关系。当 type等于别的值时，这些记录表示别的对应关系。（比如 0=’在线’ 1=’离线’，这样的关系）。

所以在orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4\Case1.java中，示例要更复杂一些。对genderName的定义是这样的——

 

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4/ntity/Person.java

通过增加@JoinDescription这样的注解，为SQL中的Join关系增加了一个过滤条件。

实际上查询的SQL语句变为(示意)

这样就起到了过滤其他类型的对应关系的效果。

 

实际示例代码如下

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4\Case1.java

 

上面介绍了@JoinDescription组合的用法。@JoinDescription用来描述多表关联关系时一些额外的特征与特性。

虽然上面的例子中同时使用了@FieldOfTargetEntity和@JoinDescription，但两者各有各的作用，并不一定要组合使用。@JoinDescription和@FieldOfTargetEntity使用上没有必然联系。

 

@ JoinDescription

| 属性              | 作用                                       |
| --------------- | ---------------------------------------- |
| type            | 枚举常量jef.database.annotation.JoinType。  LEFT   左外连接  RIGHT 右外连接  INNER 内连接  FULL  全外连接 |
| filterCondition | JPQL表达式，表示Join时额外的条件。表达式中可以包含参数变量。       |
| maxRows         | 当对多连接时，限制结果记录数最大值。                       |

 

#### 5.2.2.2. 控制级联对象的排序和数量

上表中提到了JoinDescription中的另外属性，我们举例说明

上例表示一个指向ExecutionLog的一对多引用。但是对应的ExecutionLog有很多条，我们只取**执行时间最近的****10****条**。之前的记录不会查出来。

JPA注解@OrderBy可以用于控制级联关系的排序。@OrderBy("execute_time desc")表示对于executeTime字段进行倒序排序。

如果maxRow=1，那么这个映射将只对应最后一条ExecutionLog。那么数据类型可以进一步简化——

 

最后一条ExecutionLog是单值的，可以不使用集合类型。

#### 5.2.2.3. 在FilterCondition中使用变量

某些时候，我们希望FilterCondition中的表达式中出现的不是固定的常量，而是运行时得到的变量。

比如前面那个 “M=男 F=女”的转换例子，如果数据字段中的映射名称并不总是” USER.GENDER”，而是一个变化的值。那该怎么办呢？

 

在执行查询时——

在运行时就会将其作为绑定变量处理。

这种用法适用于以下两种场景——

l  Join过滤条件中存在不确定的值时

l  SQL语句中必须使用绑定变量时

### 5.2.3.   其他JPA注解的支持

这一节，我们不介绍用法，而是针对级联时的操作行为进行一些分析

#### 5.2.3.1. 延迟加载

我们修改一下Person.java，将Person.java中的currentSchool定义改成下面这样。

 

再次运行下面的测试案例

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4\Case1.java

通过输出可以观察到，启用了Lazy的Fetch方法之后，Person对象从数据库中查出时，School对象是未赋值的。只有当调用了getCurrentSchool()之后，才会去数据库中加载School对象。

 

在JPA定义中，fetch属性用于指定级联加载的行为。

| fetch属性取值       | 效果                                      |
| --------------- | --------------------------------------- |
| FetchType.EAGER | 饥渴的。查询时立刻加载级联对象                         |
| FetchType.LAZY  | 懒惰的。只有当属性被使用时（调用get方法），才会去加载级联对象。（延迟加载） |

JPA的四种级联关系中，默认的加载行为是不同的。

 

| 类型   | 注解           | 缺省的fetch行为 |
| ---- | ------------ | ---------- |
| 一对一  | @ OneToOne   | EAGER      |
| 一对多  | @ OneToMany  | LAZY       |
| 多对一  | @ ManyToOne  | EAGER      |
| 多对多  | @ ManyToMany | LAZY       |

如果不使用延迟加载，EF-ORM也会使用“外连接获取”的方式来减少数据库操作次数。见下一节。

#### 5.2.3.2. 外连接获取

从5.1.3的例子中，我们可以从日志观察到：当Person对象中有两个多对一的引用时，在查询Person时，实际SQL语句如下

 

也就是说，在查询Person表中的记录时，将对应的School对象和性别“男/女”的显示问题都一次性的查了出来。最后形成的是并不是一个简单的单表查询语句，而是一个多表联合的Join语句。为了防止Join无法连接的记录（比如未对应School的Person记录）被过滤，因此使用了左外连接(Left Join)。

一般来说，关联查询可以通过多次数据库查询完成，也可以用上面那样较为复杂的SQL语句一次性从数据库中查询得到，这种操作方式被称为外连接查询。

这样想，如果查询出10个Person。如果不使用外连接，那么我们还需要执行10次SQL语句去获得每个Person对象的School。但使用外连接后，一次数据库操作就能代替原先11次操作。

外连接查询只能应用与ManyToOne和OneToOne的级联关系中，并能有效的减少数据库访问次数，在某些场合下具有较好的性能。由于JPA注解中OneToOne和ManyToOne的缺省FetchType都是EAGER，即不使用延迟加载。此时单次SQL操作性能更好，因此这也是EF-ORM的默认实现方式。

 

如果你希望沿用原先执行多次单表查询的方式，可以使用setCascadeViaOuterJoin(false)方法。如下——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4\Case1.java

 

除此之外，从5.2.3.1的内容也可以知道，使用JPA注解显式声明为LAZY，也可以使得级联关系从外连接转变为多次加载。

因此我们总结如下

1、当FetchType标记为LAZY时，所有级联 关系都通过多次的单表查询来实现。

2、当FetchType为EAGER时，OneToOne和ManyToOne可以优化为使用外连接单次查询。OneToMany和ManyToMany无法优化。

3、如果调用setCascadeViaOuterJoin(**false**)方法，或者配置全局参数 db.use.outer.join=false（见附录一），那么EF-ORM将放弃使用外连接优化。此时性能问题依赖于一级缓存和延迟加载来解决。

#### 5.2.3.3. 级联方向

EF-ORM中，级联关系是针对单个对象的辅助描述，因此所有的级联关系维护都是单向的。上面的例子中，如果我们删除 School表中的数据，不会引起Person数据的任何变化。

 如果我们想要在删除School时，级联删除该School的所有学生，那么需要修改School.java这个类。

修改后的School类如下

当配置了从School到Person的关系后，如果级联删除School对象，则也会删除对应的Person对象。

上面的配置中，    @JoinColumn注解可以省略。如果未配置Join的路径，EF-ORM会到目标对象中去查找反向关系。

因此最简的配法是

 

### 5.2.4.   定制级联行为

#### 5.2.4.1. 四种级联关系的行为

 

我们来了解EF-ORM在使用级联方法 (insertCascade()、 updateCascade() deleteCascade()等方法 )操作时，实际执行了什么动作。

 

在EF-ORM中，所有的四种级联关系，都是两表间的关系，不会出现第三张表，包括多对多（@ManyToMany）。事实上这也使ManyToMany的作用和OneToMany其实差不多，因此实用性不大。

这四种级联都对应一些简单的操作步骤，而且并没有提供太多的定制配置。对熟悉H框架的人来说，EF-ORM的级联行为和某H框架并不完全一致。EF-ORM希望设计出一些一刀切的简单规则来处理级联问题，从而降低ORM框架的使用难度。

一个典型的示例就是mappedby属性在EF-ORM中是无效的，EF-ORM也不会为OneToMany生成一张所谓中间表这样的设计。此外，下面的示例在某H框架中是不能删除子表对象的，但在EF-ORM中可以做到。

父表的定义

 orm-tutorial/src/main/java/org/easyframe/tutorial/lesson5/entity/Catalogy.java

这里顺便提一下——当使用主键和其他实体关联时，可以将@JoinColumn(name="id",referencedColumnName="catalogyId")简化为mappedBy="catalogyId"。两者是等效的。

 子表的定义

orm-tutorial/src/main/java/org/easyframe/tutorial/lesson5/entity/Item.java

运行下面的代码——

orm-tutorial/src/main/java/org/easyframe/tutorial/lesson5/Case1.java

 

在EF-ORM中，四种级联关系在增、删、改的时候执行的动作是固定的，下表列出了四种不同级联情况下框架执行的操作。

    其中——当前对象指配置了级联关系的对象（如本例的Catalogy对象），关联对象指被引用的对象（本例的Item对象）。

 

| 关系类型       | insertCascade                            | updateCascade                            | deleteCascade        |
| ---------- | ---------------------------------------- | ---------------------------------------- | -------------------- |
| OneToOne   | 1 插入当前对象。  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。 | 1 更新当前对象  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。  如为不再使用则删除 | 1 删除当前对象  2 再删除关联对象。 |
| OneToMany  | 1 插入当前对象。  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。 | 1 更新当前对象  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。  如为不再使用则删除 | 1 删除当前对象  2 再删除关联对象。 |
| ManyToOne  | 1 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新;  2 插入当前对象。 | 1 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新  不做级联删除  2 再更新当前对象 | 1 删除当前对象  不做级联删除。    |
| ManyToMany | 1 插入当前对象。  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。 | 1 更新当前对象  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。  不做级联删除 | 1 删除当前对象  不做级联删除。    |

  上表列出了四种不同的映射关系下的行为，可以发现基本上各个行为都只分为维护对象本身和维护级联关系两种。
    上表中不同的行为特点是根据大量业务实际情况推导出的。例如：多对一这种关系不做级联删除，这也是考虑到大多数多对一的场景下，被引用的对象一般是公用的。

 

  另外要注意，级联操作是递归的。比如——

A对象@OneToMany到B对象， B对象OneToOne到C对象，在对A对象级联操作的同时，会按上述规则一直维护到C对象为止。如果是更多的对象参与级联，那么以此类推。

 

上述行为中ManyToMany和某H框架差别较大，正常情况下，我们维护多对多关系总是需要一张中间表。因此某H框架提供了隐含的中间表实现。即两个实体、一个关系，总共三张表。EF-ORM早期几个版本也是这样设计的，但是后续版本中去除了这种设计。

那么，如果我们碰到有类似于E-R-E这样的多对多关系时，该怎么做呢？更倾向于设计三个实体，（例如 学生、课程、考试）分别维持以下关系——

也就是说，多对多关系被拆分为两个一对多关系。

 

这是不是说EF-ORM中的@ManyToMany就没有用了呢？不是这样的，再举一个例子。

某学校有10个班，每个班可以有多个学生和多个任课老师。

**教师任课表**

| 教师ID | 班级ID | 课程ID |
| ---- | ---- | ---- |
|      |      |      |

**学生表**

| 学生ID | 班级ID | 姓名   | 性别   | 年龄   |
| ---- | ---- | ---- | ---- | ---- |
|      |      |      |      |      |

在这两个实体中，我们就可以建立双向@ManyToMany的关系。从学生记录中，获得该学生所在班级的所有课程和任课教师。从教师担任的课程的记录中，获得该班级所有的学生记录。

我们怎么区分什么时候该使用@OneToMany什么时候使用@ManyToMany呢？可以遵循这样的原则：

l  当目标实体依赖于当前实体（强关联）时使用OneToMany；

l  当目标实体相对独立     （弱关联）时使用ManyToMany。

弱关联情况下，级联操作将会使用一种较为保守的策略，来避免出现删除关联对象之类的误操作。

关于这个ManyToMany的模型，可以参阅orm-tutorial/src/main/java/org/easyframe/tutorial/lesson5/Case1.java中的testManyToMany方法。本文不再赘述。

#### 5.2.4.2. 使用注解限制级联关系的使用场合

上面提到了，级联关系是递归维护的。而且要注意，一个对象中可能存在多种级联关系，而其级联对象中又可能有多种级联关系。在递归情况下，情形可能会变得相当复杂。如果没有很好的设计你的E-R关系，你可能会发现，一个简单的数据库操作可能会衍生出大量的级联操作语句。

因此，JPA中可以限制级联关系的使用范围。使用JPA注解，使级联关系只在插入/更新/删除等一种或几种行为中生效。简而言之，我们可以让配置的级联关系只用于查询操作、或者只用于插入操作。

 

在@OneToOne @OneToMany @ManyToOne @ManyToMany中，可以设置cascade属性如下

cascade是一个多值属性，配置了以后，则表示此类操作要进行级联，否则就不作级联操作。例如上例配置该字段仅用于级联查询和级联插入更新，不用于级联删除。

| **CascadeType**     | **对应**    |
| ------------------- | --------- |
| CascadeType.PERSIST | 允许级联插入    |
| CascadeType.MERGE   | 允许级联插入或更新 |
| CascadeType.REMOVE  | 允许级联删除    |
| CascadeType.REFERSH | 允许级联查询    |
| CascadeType.ALL     | 允许所有级联操作  |

 

### 5.2.5.   级联条件CascadeCondition

在级联查询中，我们还能在级联查询的对象上添加条件。请看下面这个例子：

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4\Case1.java

在这个例子中，使用addCascadeCondition()方法，在查询中增加了一个专门针对级联对象的过滤条件。在查询级联时中，只有带有”清华“字样的学校才会被查出。

另一个例子是在多对多时

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson5\Case1.java

可以发现，即使是在延迟加载的场景下，级联条件依旧会在查询时生效。

 

由于级联是递归的，即级联对象中我们还可以获得其他级联对象，因此过滤条件也不仅仅针对当前对象的，而是可以灵活的指定。我们再看复杂一点的例子

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson5\Case1.java

 

Cascade条件一旦设置到一个查询中，无论级联操作是单次操作还是多次操作，都会生效。

# 6.    Criteria API多表操作

级联操作本质上是一种单表操作。所有的级联操作都可以以多次单表查询的方式来完成。

但前面的大多数级联操作都使用了自动外连接的功能，因此在查询时，实际上看到的是一个多表连接后的Join查询语句。这种SQL语句在EF-ORM内实现也是使用Criteria API来做到的。

前面的单表Criteria API中大家可以理解，一个单表操作SQL语句可以被抽象后用一个Query对象表示。多表操作Criteria API的概念也差不多。一个多表查询的SQL语句被java抽象后，可以用一个Join对象或者一个UnionQuery对象表示。

Join对象其实就是一个典型的Join的SQL语句在Java中的映射，一般来说标准的Join语句是

可以发现，一个Join语句是针对多个表（或者视图或查询结果）。每个表都可以有自己的where条件（或者没有条件）。表和表之间用 ON条件进行连接。

很多时候，我们在不太复杂的Join语句中，将On条件也放在where条件后面来写，这样写更方便，不过某些时候的计算顺序会和标准写法出现误差，造成一些计算结果不正确的问题。

在Java中，我们也将SQL语句映射为一个Join对象，其包含了多个Query对象，Query之间用 on 条件进行连接。on条件数量不定。

 

 

图6-1 Join的构成

 

上图可以看出，一个Join由多个单表的Query对象构成。我们在实际使用时，可以自由的组合各种Query，形成一个Join查询对象。这个模型实际上和我们编写的SQL是一样的。

 

 

## 6.1. Join查询

### 6.1.1.   基本用法

多表查询的主要场景都是Join查询。Join查询由多个单表的子查询构成，这点在前面已经叙述了。来看看实际用法。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson6\Case1.java

从上例中，我们可以发现

1、 可以用QueryBuilder.innerJoin()将两个Query对象拼接在一起形成内连接。形成Join对象后，join对象自带innerJoin方法，用于添加新的查询表。

2、 使用QueryBuilder.on()可以指定Join时的连接键。

3、 如果不指定Join查询的返回类型，那么会返回所有字段形成的Map，如果指定Join查询返回Object[]类型。那么会将所有参与查询的表的映射对象以数组的形式返回。
Join场景下，查询结果返回的最常用形式是这两种。但其实EF-ORM也支持其他很多种结果转换形式。相关内容请参见8.1节 查询结果的返回。

 

在QueryBuilder中，有以下几个方法用于创建Join连接

 

| 方法                          | 效果                                       |
| --------------------------- | ---------------------------------------- |
| **innerJoin(q1,q2)**        | 创建内连接                                    |
| **leftJoin(q1,q2)**         | 创建左外连接                                   |
| **rightJoin(q1,q2)**        | 创建右外连接                                   |
| **outerJoin(q1,q2)**        | 创建全外连接                                   |
| **innerJoinWithRef(q1,q2)** | 先根据左侧Query对象的级联关系创建级联Join，然后再将其和右侧的对象内连接。 |
| **leftJoinWithRef(q1,q2)**  | 先根据左侧Query对象的级联关系创建级联Join，然后再将其和右侧的对象左外连接。 |
| **rightJoinWithRef(q1,q2)** | 先根据左侧Query对象的级联关系创建级联Join，然后再将其和右侧的对象右外连接。 |
| **outerJoinWithRef(q1,q2)** | 先根据左侧Query对象的级联关系创建级联Join，然后再将其和右侧的对象全外连接。 |

    在Join对象中，也有类似的方法。在连接查询中增加新的表。

在上述方法中，入参包括可选择的On条件，该条件指出了新参与查询的表通过哪些键和旧的表连接。使用 QueryBuilder.on()方法，可以为两个Query指定连接条件。

### 6.1.2.   使用Selects对象

其实就EF-ORM的API设计来说，并没有明显的区分单表操作和多表操作，前面单表查询的各种用法，也都可以直接在多表查询中使用。本例中的Selects使用来控制查出的列就是一例。

此外，单表查询中的 SQLExpression、JpqlExpression等特性都可以在多表查询中使用。

 

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson6\Case1.java

 

在上例中，我们可以发现，Query对象和Join对象都是JoinElement的子类，因此selectFrom方法对这两类对象都有效。

同样的，我们在之前单表查询中学习过的这些特性也都能够直接在Join查询中使用——

l  Distinct

l  Group by

l  Having

l  Max() min() avg() count（）等

l  函数的使用

这些都和在单表查询中没有什么区别。

### 6.1.3.   分页和总数

 

同样的，分页和总数的实现方法和单表查询的API也是一样的，下面是和分页与总数相关的几个API的用法。虽然在单表查询中已经介绍过了。

 

**计算总数**

| 方法                                       | 作用                                       |
| ---------------------------------------- | ---------------------------------------- |
| **jef.database.Session.count(ConditionQuery)** | 将传入的查询请求（单表/多表）改写为count语句，然后求满足查询条件的记录总数。 |

 

 

**传入IntRange****限制结果范围**

| **方法**                                   | **用途说明**                                 |
| ---------------------------------------- | ---------------------------------------- |
| **Session.select(ConditionQuery,  IntRange)**  ** ** | 传入Query形态的查询(单表、Union、Join均可)，限定返回条数在IntRange区间范围内。 |
| **Session.select(ConditionQuery,  Class, IntRange)** | 传入Query形态的查询(单表、Union、Join均可)，结果转换为指定类型，限定返回条数在IntRange区间范围内。 |
| **Session.iteratedSelect(TypedQuery,  IntRange)** | 传入Query形态的查询(单表/级联/Union)，限定返回条数在IntRange区间范围内。将游标封装为返回结果遍历器。 |
| **Session.iteratedSelect(ConditionQuery,  IntRange)** | 传入Query形态的查询(单表、Union、Join均可)，限定返回条数在IntRange区间范围内。将游标封装为返回结果遍历器。 |
| **Session.iteratedSelect(ConditionQuery,  Class, IntRange)** | 传入Query形态的查询(单表、Union、Join均可)，限定返回条数在IntRange区间范围内。将游标封装为返回结果遍历器。 |

 

**使用pageSelect****接口**，得到PageIterator对象。（参见4.1.3.2）

| **方法**                                   | **作用**                                   |
| ---------------------------------------- | ---------------------------------------- |
| **Session.pageSelect(ConditionQuery,  int)**  ** ** | 传入Query形态的查询(单表、Union、Join均可)            |
| **Session.pageSelect(ConditionQuery,  Class, int)**  ** ** | 传入Query形态的查询(单表、Union、Join均可)， 并指定返回结果类型 |

 

用例子来描述： 下面的例子里使用了两种方法来实现

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson6\Case1.java

### 6.1.4.   条件容器的问题

Join是由多个Query构成，而每个Query和Join对象本身都提供了addOrderBy等设置排序字段的方法。除此之外，每个Query对象都提供了addCondition方法，这产生了一个问题——即Join查询中的条件和排序应当设置到哪个对象容器中。如果我们在某个Query中设置了条件或者排序字段，然后再将其添加到一个Join对象中，还能正常生效吗？

从设计目的讲，目前无论是直接加入到Join对象上，还是任意Query对象上的条件，都会在查询中生效。

一种做法是，在构成Join的每个Query对象上添加条件和排序字段

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson6\Case1.java

 

另一种做法，在第一个Query上添加条件，在join中添加排序字段

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson6\Case1.java

上面两种代码实现，运行后可以发现最终效果是一样的。

    也就是说：一般情况下，排序和Where条件可以放在Join的任意Query对象上。排序可以直接放在Join对象上。

 

为什么要限制“一般情况下”？我们需要了解一下在查询时实际发生了什么。我们先来看上面的例子所产生的实际SQL语句。

 

首先，每个查询列（排序列）在查询语句中，都会添加该列所在表的别名（如T1.ID,中的T1为表T_PEERSON的别名）。这是因为不同的表可能有相同名称的列，所以必须用表别名来限定。因此我们可以理解，每个条件中的Field对象最终是必须要绑定到一个Query上去的。如果我们在每个Query上设置条件，那么这个绑定是由开发人员显式完成的。如果将条件和排序设置到其他Query中或者Join对象中，那么绑定的过程是由框架内部完成的。

我们将由用户完成的绑定称为显式绑定，由框架内部自动完成的称为隐式绑定。隐式绑定是根据每个Field所在的Entity类型来判定的，很显然某种情况下隐式绑定是不准确的（也是危险的），那就是在同一张表多次参与Join的时候。

因此，如果在Join中同一张表参与多次，我们更建议显式的指定每个条件和排序对应的Query。否则的话两种方法都可以，可以按个人喜好使用。

 

 

 

## 6.2. Union查询

    多表查询中，除了Join以外，还有一种特殊的请求类型，即Union Query。

和SQL一样，多组查询（无论是Join的还是单表的， 只要选出的列的类型一样，就可以使用Union关系拼合成一个更大的结果集）。

UnionQuery具有特定的使用意义。我们之所以用数据库来union两个结果集，而不是在java代码中拼合两个结果集，最大的原因是——

l  Union语句可以过滤重复行
使用union关键字，让数据库在拼合两个结果集时，去除完全相同的行。这会造成数据库排序等一系列复杂的操作，因此DBA们经常告诫开发人员，在能使用union all的场合就不要使用union。

l  Union语句可以设置独立的排序条件
除了构成Union查询的子查询可以对结果排序外，Union/Union All语句还可以对汇总后的结果集进行排序，确保汇总后的结果集按特定的顺序列出。

 

因此，综上所述，我们在日常业务处理中，很难不使用union。

 

一个union查询的例子是这样的——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson6\Case1.java

上例中，在两个对象的查询语句中，补齐了各自缺少的字段以后，两个查询具有相同的返回结果格式。此时就可以用union语句将两个查询结果合并为一个。(Derby的Union实现较为特殊，结果集之间是按列序号而不是列名进行匹配的，因此要注意列的输出顺序)

 

QueryBuilder可以用于生成union 查询。

在SQL语句中，有 union 和 union all 两种合并方式。对应到QueryBuilder中的以下几个方法上。

| 方法                                       | 作用                                       |
| ---------------------------------------- | ---------------------------------------- |
| QueryBuilder.union(TypedQuery<T>...)     | 将多个Query用 union结合起来。                     |
| QueryBuilder.union(Class<T>,  ConditionQuery...) | 将多个Query用 union结合起来，查询的返回结果为指定的class     |
| QueryBuilder.union(ITableMetadata,  ConditionQuery...) | 将多个Query用 union结合起来，查询的返回结果为指定元模型对应的实体   |
| QueryBuilder.unionAll(TypedQuery<T>...)  | 将多个Query用 union all结合起来。                 |
| QueryBuilder.unionAll(Class<T>,  ConditionQuery...) | 将多个Query用 union all结合起来，查询的返回结果为指定的class |
| QueryBuilder.unionAll(ITableMetadata,  ConditionQuery...) | 将多个Query用 union all结合起来，查询的返回结果为指定元模型对应的实体 |

在使用union或unionAll方法时，需要传入一个类型，该类型为union查询最终要返回的结果容器。可以使用Map，也可以是任意对象。

 

关于排序

UnionQuery可以有单独的排序列，这个排序列属于union query独有，和各个Query对象的排序之间没有关系。这也是union语句的固有特点。再看下面的示例——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson6\Case1.java

执行上述代码，观察输出的SQL语句可以看到

从SQL语句观察，可以发现各个子查询的排序列（Order By）和整个union语句的排序列其作用和定义是不同的。

 

 

## [6.3. Exists条件和NotExists条件

我们还有可能会需要写带有exists条件的SQL语句。一个例子是——

Q: 查出所有姓名在Student表中出现过的Person。

   A: 

 

在这个例子中，exists带来了一个子查询，并且和父表发生了内连接。

EF-ORM中，也能生成这样的查询——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson6\Case1.java

上面这个查询，将Exists作为一个特殊的条件进行处理。因此严格意义上面的查询语句是一个“单表查询”，因此级联功能依然会生效。为了使得生成的SQL更为简单，我们通过p.setCascade(**false**);语句关闭了级联功能。如果去掉这句语句，您将可以看到级联功能和exists共同生效的场景。

 

那么现在我们把问题变一下——

Q: 查出所有姓名未出现在Student表中的Person。

问题很简单，把exists改为not exists就可以了。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson6\Case1.java

 

您可能已经发现，多表查询下是不支持级联功能的。而上面的Exists和Not Exists中都出现了级联功能，所以实际上这两个例子都是单表查询。放在这一章仅仅是因为人们的习惯而已。

 

多表下的API查询就介绍到这里。关于多表查询下返回结果有哪些格式、用什么容器返回结果，本章没有介绍。因为EF-ORM无论在单表/多表/NativeQuery下，都有统一的查询结果返回规则。所以这部分内容将在学习完NativeQuery以后的第八章再介绍。

# 7.    本地化查询

## 7.1. 本地化查询是什么

这里本地化查询即’NativeQuery’，是指利用SQL（或者JPQL）进行的数据库操作——不仅限于select，也可以执行insert、update、甚至create table as、truncate等DDL。

本地化查询让用户能根据临时拼凑的或者预先写好的SQL语句进行数据库查询，查询结果将被转换为用户需要的类型。

在EF-ORM中，NativeQuery也有SQL和JPQL两种语法。其中JPQL是JPA规范定义的查询语言。但JPQL因为模型差距较大，一直没有完全支持，目前提供的名称为JPQL的若干方法仅为向下兼容而保留，不推荐大家使用。

因此**本地化查询就是用****SQL****语句操作数据库的方法**。

 

您可能会问，如果是用SQL，那么我们直接用JDBC就好了，还用ORM框架做什么？

事实上，NativeQuery中用的SQL，是被EF-ORM增强过的SQL，在语法特性上作了很多的补充。下面的表格中列出了所有在SQL上发生的增强，我们可以在下面的表格中查看到这些激动人心的功能。某种意义上讲，增强过的SQL是一种新的查询语言。我们也可以将其称为E-SQL(Enhanced SQL)。

 

E-SQL语法作了哪些改进呢？ 

| 特性                | 说明                                       |
| ----------------- | ---------------------------------------- |
| Schema重定向         | 在Oracle,PG等数据库下，我们可以跨Schema操作。Oracle数据库会为每个用户启用独立Schema，在SQL语句或对象建模中，我们可以指定Entity所属的Schema。 但是实际部署时schema名称如果发生变化，事先写好的程序就不能正常工作。  schema重定向不仅仅应用于CriteriaAPI中，在SQL语句中出现的Schema也会在改写过程中被替换为当前实际的Schema。 |
| 数据库方言：  语法格式整理    | 对于SQL语句进行重写，将其表现为适应本地数据库的写法，比如 \|\| 运算符的使用。  又比如 as 关键字的使用。 |
| 数据库方言：  函数转换      | 对于translate、delete、sysdate、decode、nullif等数据库函数，能自动转换为当前数据库能够支持的表达形式。 |
| 增强的绑定变量           | 1、允许在语句中用占位符来描述变量。绑定变量占位符可以用 :名称 也可以用 ?序号 的形式。即JPQL语法的占位符。  2、绑定变量占位符中可以指定变量数据类型。这样，当传入String类型的参数时，能自动将其转换为绑定变量应当使用的类型，如java.sql.Date,、java.lang.Number等。 |
| 动态SQL功能：  自动省略    | 在SQL中，会自动扫描每个表达式的入参（占位符）是否被用到。如果某个参数未使用，那么该参数所在的表达式会被省略。例如  *select \* from t where id=:id and  name=:name*  中，如果name参数未传入，则and后面的整个条件表达式被略去。 |
| 动态SQL功能：  SQL片段参数 | 在SQL占位符中可以声明一个占位符是SQL片段。这个片段可以在运行时根据传入的SQL参数自动应用。 |

表 7-1 E-SQL的语法特性

EF-ORM会对用户输入的SQL进行解析，改写，从而使得SQL语句的使用更加方便，EF-ORM将不同数据库DBMS下的SQL语句写法进行了兼容处理。 并且提供给上层统一的SQL写法。

 

除了在SQL语法上的增强以外，通过EF-ORM  NativeQuery操作和直接操作JDBC相比，还有以下优势：

| **特点**              | **NativeQuery**                          | **JDBC**                 |
| ------------------- | ---------------------------------------- | ------------------------ |
| **对象返回******        | 转换为需要的对象。转换规则和Criteria API一致。            | ResultSet对象              |
| **自定义返回对象转换映射****** | 可以自定义ResultSet中字段返回的Mapping关系。           | --                       |
| **性能调优******        | 可以指定fetch-size , max-result 等参数，进行性能调优   | JDBC提供各种性能调优的参数          |
| **复用******          | 一个NativeQuery可携带不同的绑定变量参数值，反复多次使用。       | PreparedStatment对象可以反复使用 |
| **一级缓存******        | 会刷新和维护一级缓存中的数据。  比如用API插入一个对象，一级缓存中即缓存了这个对象。  虽然用SQL语句去update这条记录。一级缓存中的该对象会被自动刷新。 | 无此功能                     |
| **SQL****自动选择****** | SQL改写功能不能解决一切跨库移植问题。用户可以对不兼容跨库的SQL写成多个版本，运行时自动选择。 | 无此功能                     |
| **性能******          | SQL解析和改写需要花费0.3~0.6毫秒。其他操作基本和JDBC直接操作保持一致。  对象结果转换会额外花费一点时间，但采用了策略模式和ASM无反射框架，性能优于大多数同类框架。 | 原生方式，性能最佳                |

表 7-2 使用NativeQuery和直接使用JDBC的区别

 

 

 

 

 

## 7.2. 使用本地化查询

### 7.2.1.   NamedQuery和NativeQuery

NativeQuery的用法可以分为两类。一类是在java代码中直接传入E-SQL语句的；另外一类是事先将E-SQL编写在配置文件或者数据库中，运行时加载并解析，使用时按名称进行调用。这类SQL查询被称为NamedQuery。对应JPA规范当中的“命名查询”。

命名查询也就是Named-Query，在某H框架和JPA中都有相关的功能定义。简单来说，命名查询就是将查询语句(SQL,HQL,JPQL等)事先编写好， 然后为其指定一个名称。在使用ORM框架时，取出事先解析好的查询，向其中填入绑定变量的参数，形成完整的查询。

 

EF-ORM的命名查询和OpenJPA以及某H框架中的命名查询用法稍有些不同。

l  命名查询默认定义在配置文件 named-queries.xml中。不支持使用Annotation等方法定义

l  命名查询也可以定义在数据库表中，数据库表的名称可由用户配置 

l  命名查询可以支持 E-SQL和JPQL两种语法（后者特性未全部实现） 

l  由于支持E-SQL，命名查询可以实现动态SQL语句的功能，可以模拟出与IBatis相似的用法。 

 

为什么不使用JPA规范中的基于Annotation的方式来注册命名查询呢？因为考虑到ORM中一般只有跨表的复杂查询才会使用命名查询。而将一个多表的复杂查询注解在任何一个DAO上都是不合适的。分别注解在DAO上的SQL语句除了语法受限之外，还有以下缺点：

l  归属不明确，很难正确评判某个SQL语句应当属于某个DAO。而且不能被其他DAO使用？

l  Java代码中写SQL涉及转义问题

l  DAO太分散，不利于SQL语句的统一维护。

 

EF-ORM默认设计了两种方式来配置命名查询

l  classpath下创建一个名为named-queries.xml的配置文件

l  存放在数据库中，表名可自定义，默认JEF_NAMED_QUERIES

 

NativeQuery是在EF-ORM 1.05开始增加的功能。在1.6开始支持数据库配置，在1.6.7开始支持动态改写和SQL片段。

### 7.2.2.   API和用法

我们分别来看Named-Query和Native Query的使用

 

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

上面的例子中，两次查询使用的SQL语句是一样的，区别在于前者配置在named-queries.xml中，后者直接写在代码中。

 

  

在上面的例子中，无论是createNamedQuery方法还是createNativeQuery方法，返回的对象都是名为NativeQuyery的对象。其行为和功能也完全一样。

 

 

下面是用于获得NativeQuery的方法API

| 方法                                       | 用途                             |
| ---------------------------------------- | ------------------------------ |
| Session.createNamedQuery(String)         | 构造一个命名查询对象。不指定返回类型。            |
| Session.createNamedQuery(String,  Class<T>) | 构造一个命名查询对象。指定返回类型为某class。      |
| Session.createNamedQuery(String,  ITableMetadata) | 构造一个命名查询对象。指定返回类型为某表元模型所对应的类。  |
| Session.createNativeQuery(String)        | 构造一个SQL查询对象。不指定返回类型。           |
| Session.createNativeQuery(String,  Class<T>) | 构造一个SQL查询对象。指定返回类型为某class。     |
| Session.createNativeQuery(String,  ITableMetadata) | 构造一个SQL查询对象。指定返回类型为某表元模型所对应的类。 |
| Session.createQuery(String)              | 构造一个JPQL查询对象。不指定返回类型。          |
| Session.createQuery(String,Class<T>)     | 构造一个JPQL查询对象。指定返回类型为某class。    |

上述方法都可以返回NativeQuery对象。

   NativeQuery并不一定就是select语句。在NativeQuery中完全可以使用update delete insert等语句，甚至是create table等DDL语句。（当执行DDL时会造成事务被提交，需谨慎）。显然，在指定非Select操作时，传入一个返回结果类型是多此一举，可以指定NativeQuery返回的结果类型，也可以不指定。

 

在得到了NativeQuery以后，我们有很多种用法去使用这个Query对象。这里先列举一下主要的方法。

 

| 方法                                       | 用途                                       |
| ---------------------------------------- | ---------------------------------------- |
| **执行查询动作**                               |                                          |
| NativeQuery.getResultList()              | 查询出由多行记录转换的对象列表                          |
| NativeQuery.getSingleResult()            | 查询出单行记录转换的对象，如果有多行返回第一行                  |
| NativeQuery.getResultCount()             | 将SQL改为count语句后查出结果总数                     |
| NativeQuery.getResultIterator()          | 查询出多行记录，并用遍历器方式返回                        |
| NativeQuery.executeUpdate()              | 执行SQL语句，返回影响的记录行数                        |
| NativeQuery.getSingleOnlyResult()        | 和getSingleResult的区别是如果有多行则抛出异常           |
| **性能参数**                                 |                                          |
| NativeQuery.setMaxResults(int)           | 设置查询最大返回结果数                              |
| NativeQuery.getMaxResults()              | 获取查询最大返回结果数                              |
| NativeQuery.getFetchSize()               | 获取结果集加载批次大小                              |
| NativeQuery.setFetchSize(int)            | 设置结果集加载批次大小                              |
| **结果范围限制（分页相关）**                         |                                          |
| NativeQuery.setFirstResult(int)          | 设置查询结果的偏移，0表示不跳过记录。                      |
| NativeQuery.getFirstResult()             | 返回查询结果的偏移，0表示不跳过记录                       |
| NativeQuery.setRange(IntRange)           | 设置查询区间（含头含尾）                             |
| **绑定变量参数**                               |                                          |
| NativeQuery.setParameter(String,  Object) | 设置绑定变量参数                                 |
| NativeQuery.setParameter(int, Object)    | 设置绑定变量参数                                 |
| NativeQuery.setParameterByString(String,  String) | 设置绑定变量参数，传入String后根据变量类型自动转换             |
| NativeQuery.setParameterByString(String,  String[]) | 设置绑定变量参数，传入String[]后根据变量类型自动转换           |
| NativeQuery.setParameterByString(int,  String) | 设置绑定变量参数，传入String后根据变量类型自动转换             |
| NativeQuery.setParameterByString(int,  String[]) | 设置绑定变量参数，传入String[]后根据变量类型自动转换           |
| NativeQuery.setParameterMap(Map<String,  Object>) | 设置多组绑定变量参数                               |
| NativeQuery.getParameterValue(String)    | 获得绑定变量参数                                 |
| NativeQuery.getParameterValue(int)       | 获得绑定变量参数                                 |
| NativeQuery.containsParam(Object)        | 检查某个绑定变量是否已经设置了参数                        |
| NativeQuery.clearParameters()            | 清除目前已经设入的绑定变量参数。当需要重复使用一个NativeQuery对象进行多次查询时，建议每次清空旧参数。 |
| NativeQuery.getParameterNames()          | 获得查询中所有的绑定变量参数名                          |
| **返回结果定义**                               |                                          |
| NativeQuery.getResultTransformer()       | 得到ResultTransformer对象，可定义返回结果转换动作。       |

 

   根据上述API，我们简单的使用如下——

注意观察输出的SQL语句，上面的案例中，演示了

1、绑定变量参数用法

2、Count语句在一些复杂情况下的转换逻辑

3、通过重置参数，可以复用NativeQuery对象。

4、仅返回单条结果的场景

### 7.2.3.   命名查询的配置

    上一节中，我们基本了解了NativeQuery对象的构造和使用。本节来介绍命名查询如何配置。

前面说过，命名查询的配置方法有两种。我们先来看配置在文件中的场景。

#### 7.2.3.1. 配置在named-queries.xml中

在classpath下创建一个名为named-queries.xml的文件。

named-queries.xml

    上面就配置了两个命名查询，名称分别为“getUserById”以及”testIn”。

其中每个查询的SQL中，都有一个参数，参数在SQL中用绑定变量占位符表示。后面的参数使用in条件，使用时可以传入int数组。

     在query元素中，可以设置以下属性

| 属性名        | 作用                                   | 备注               |
| ---------- | ------------------------------------ | ---------------- |
| name       | 指定查询的名称                              | 必填               |
| type       | sql或jpql，表示语句的类型                     | 可选，默认为SQL        |
| fetch-size | 指定结果集每次获取批次大小                        | 可选，默认0即JDBC驱动默认值 |
| tag        | 当DbClient连接到多数据源时，可以指定该查询默认使用哪个数据源连接 | 可选               |
| remark     | 备注信息，可不写                             | 可选               |

 

    最后，当classpath下有多个named-queries.xml时，所有配置均会生效。如果多个文件中配置的同名的查询，那么后面加载的会覆盖前面的。当覆盖现象发生时，日志中会输出警告。

#### 7.2.3.2. 配置在数据库中

你可以将命名查询配置在指定的数据库表中。要启用此功能，需要在jef.properties配置 

当EF-ORM初始化时，会自动检测这张表并加载数据，如果没有则会自动创建。表的结构是固定的(数据类型在不同的数据库上会自动转换为可支持的类型)

命名查询数据表的结构是固定的，结构如下——

| **Column** | **Type**                            | **备注**                               |
| ---------- | ----------------------------------- | ------------------------------------ |
| NAME       | varchar2(256)  not null primary key | 指定查询的名称                              |
| SQL_TEXT   | varchar2(4000)                      | SQL/JPQL语句                           |
| TAG        | varchar2(256)                       | 当DbClient连接到多数据源时，可以指定该查询默认使用哪个数据源连接 |
| TYPE       | number(1)  default 0                | 语句类型。0:SQL 1:JPQL                    |
| FETCH_SIZE | number(6)  default 0                | 指定结果集每次获取批次大小                        |
| REMARK     | varchar2(256)                       | 备注                                   |

表7-3 命名查询的数据库表结构

您可以同时使用文件配置和数据库配置命名查询。但如果出现同名的查询，数据库的配置会覆盖文件的配置。

#### 7.2.3.3. 数据源绑定

    由于EF-ORM是一种支持多数据源自动路由的ORM框架。因此在命名查询中，还可以在tag属性中指定偏好的数据源ID。这样，如果你在query://getUserByName这样的例子中请求数据时，即使不通过_dsname参数来指定数据源id，也可以到正确的数据库中查询数据。 

   我们先配置两个命名查询，区别是一个指定了数据源，另一个未指定数据源。

orm-tutorial\src\main\resources\named-queries.xml

 

然后编写代码如下——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case2.java

   从日志上可以观察到，第一次操作时，操作是在datasource2数据库上执行的，因此能正确查出。第二次操作时，操作在datasource1上执行，由于表不存在，因此抛出异常。

#### 7.2.3.4. 动态更新命名查询配置

   考虑到性能因素，NamedQuery在首次使用时从配置中加载SQL语句并解析，之后解析结果就缓存在内存中了。作为服务器运行来说，这一点没什么问题。但在Web应用开发中，用户如果修改了SQL语句，不得不重启服务才能调试。这就会给开发者造成一定困扰。为了解决配置刷新的问题，框架中增加了配置变更检测的功能。

    在jef.properties中配置

开启更新检测功能后，每次获取命名查询都会检查配置文件是否修改，如果配置来自于数据库，也会加载数据库中的配置。在开发环境可以开启这个参数；考虑到性能，在生产环境建议关闭该参数。

当大量并发请求使用命名查询时，为了避免短时间重复检查更新，一旦检测过一次，10秒内不会再次检测。

一般情况下，我们无需配置该参数。因为该参数的默认值会保持和 db.debug 一致，一般来说开发时我们肯定会设置db.debug=true。而在对性能要求较高的生产环境，需要指定该参数为false。

命名查询自动更新的配置项可以在JMX的ORMConfigMBean中开启或关闭。JMX相关介绍请参阅13章。

 

除了自动检测更新机制外，还有手动刷新命名查询配置的功能。比如在生产环境，关闭自动检测配置更新功能后，可以手工进行更新检测。在DbClient中可以强制立刻检查命名查询的更新。

 

强制检测命名查询功能也可以在JMX的Bean中调用。DbClientInfoMBean中有checkNamedQueryUpdate()方法。

 

 

 

  

## 7.3. NativeQuery特性使用

 

### 7.3.1.   Schema重定向

Schema重定向多使用在Oracle数据库上。在Oracle上，数据库操作可以跨用户(Schema)访问。当跨Schema访问时，SQL语句中会带有用户名的前缀。（这样的应用虽然不多，但是在电信和金融系统中还是经常看到）。

  例如USERA用户下和USERB用户下都有一张名为TT的表。 我们可以在一个SQL语句中访问两个用户下的表

  当使用ORM进行此类映射时，一般用如下方式指定

这样就带来一个问题，在某些场合，实际部署的数据库用户是未定的，在编程时开发人员无法确定今后系统将会以什么用户部署。如果将“USERA”硬编码到程序中，实际部署时数据库就只能建在USERA用户下，部署时缺乏灵活性。

EF-ORM的Schema重定向功能对Query模型和SQL语句都有效。在开发时，用户根据设计中的虚拟Schema名编写代码，而在实际部署时，可以配置文件jef.properties指定虚拟schema对应到真实环境中的schema上。

例如，在jef.properties中配置

 

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

上例中的SQL语句本来是无法执行的，但是因为Schema重定向功能SQL语句在实际执行时，就变为 

这样就能正常执行了。

 

使用schema重定向功能，可以解决开发和部署的 schema耦合问题，为测试、部署等带来更大的灵活性。

一个特殊的配置方式是

即不配置重定向后的schema，这个操作实际上会将原先制定的schema信息去除，相当于使用数据库连接的当前Schema。

 

最后，正如前文所述，重定向功能并不仅仅作用于本地化查询中，如果是在类的注解上配置了Schema，那么其映射会在所有Criteria API查询中也都会生效。

### 7.3.2.   数据库方言——语法格式整理

    根据不同的数据库语法，EF-ORM会在执行SQL语句前根据本地方言对SQL进行修改，以适应当前数据库的需要。

**例1**

在本例中||表示字符串相连，这在大部分数据库上执行都没有问题，但是如果在MySQL上执行就不行了，MySQL中||表示或关系，不表示字符串相加。因此，EF-ORM在MySQL上执行上述E-SQL语句时，实际在数据库上执行的语句变为

这保证了SQL语句按大多数人的习惯在MYSQL上正常使用。

 

**例2**

这句SQL语句在Oracle上是能正常运行的，但是在postgresql上就不行了。因为postgresql要求每个列的别名前都有as关键字。对于这种情况EF-ORM会自动为这样的SQL语句加上缺少的as关键字，从而保证SQL语句在Postgres上也能正常执行。 

 

上述修改过程是全自动的，无需人工干涉。EF-ORM会为所有传入本地化查询进行语法修正，以适应当前操作的数据库。

这些功能提高了SQL语句的兼容性，能对用户屏蔽数据库方言的差异，避免操作者因为使用了SQL而遇到数据库难以迁移的情况。 

我们看一下orm-tutorial中的例子——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

众所周知，Derby数据库中无法使用concat(a,b)的函数。所以经过方言转换会将其表示为

** **

**注意**

并不是所有情况都能实现自动改写SQL，比如有些Oracle的使用者喜欢用+号来表示外连接，写成仅有Oracle能识别的外连接格式。

目前EF-ORM还**不支持**将这种SQL语句改写为其他数据库支持的语法(今后可能会支持)。 因此如果要编写能跨数据库的SQL语句，还是要使用‘OUTER JOIN’这样标准的SQL语法。 

### 7.3.3.   数据库方言——函数转换

#### 7.3.3.1. 示例

在EF-ORM对SQL的解析和改写过程中，还能处理SQL语句当中的数据库函数问题。EF-ORM在为每个数据库建立的方言当中，都指定了常用函数的支持方式。在解析时，EF-ORM能够自动识别SQL语句中的函数，并将其转换为在当前数据库上能够使用的函数。

 

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

我们观察一下在Derby数据库时输出的实际SQL语句

解说一下，上面作了处理的函数包括——

nvl函数被转换为coalesce函数

Decode函数被转换为 case ... When ... Then ... Else...语句。

Replace函数也是很特殊的——Derby本没有Replace函数，这里的replace函数其实是一个用户自定义的java函数。也是由EF-ORM自动注入的自定义函数。

 

再看一个例子

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

上例涉及日期时间的计算，最终结果是一个日期天数。执行上面的代码，可以看到SQL语句变为

其中——

datediff函数被转换为JDBC函数 timestampdiff

add_months和adddate函数被转换为JDBC函数timestampadd

Sysdate函数被转换为current_timestamp。

通过上面的转换过程，EF-ORM尽最大努力的保证查询的跨数据库兼容性。

#### 7.3.3.2. 函数支持

EF-ORM对函数支持的原则是，尽可能将常用函数提取出来，并保证其能在任何数据库上使用。

目前整理定义的常用函数被定义在两个枚举类中，其用法含义如下表所示

jef.database.query.Func

| 函数                | 作用                                       | 参数格式                              |
| ----------------- | ---------------------------------------- | --------------------------------- |
| abs               | 单参数。获取数值的绝对值。                            | (number)                          |
| add_months        | 双参数。在日期上增加月数。（来自于Oracle）。                | (date,  number)                   |
| adddate           | 在日期上增加天数，adddate(current_date, 1)即返回明天  支持Interval语法。Adddate(current_date, interval 1 month)即返回下月当天   (来自于MYSQL,和DATE_ADD含义相同） | (date,  numer)  (date,  interval) |
| avg               | 返回分组中数据的平均值                              |                                   |
| cast              | 类型转换函数，格式为 cast(arg as varchar) cast(arg  as timestamp)等等 |                                   |
| ceil              | 单参数。按大数取整                                | (float)                           |
| coalesce          | 多参数。可以在括号中写多个参数，返回参数中第一个非null值           |                                   |
| concat            | 多参数。连接多个字符串，虽然大部分数据库都用 \|\| 表示两个字符串拼接，但是MYSQL是必须用concat函数的 |                                   |
| count             | 计算行数                                     |                                   |
| current_date      | 无参数。返回当前日期                               |                                   |
| current_time      | 无参数。返回当前不含日期的时间                          |                                   |
| current_timestamp | 无参数。返回当前日期和时间                            |                                   |
| date              | 单参数。截取年月日                                | (timestamp)                       |
| datediff          | 返回第一个日期减去第二个日期相差的天数。                     | (date,date)                       |
| day               | 获得日，如 day(current_date)返回当天是几号。          | (date)                            |
| decode            | 类似于多个if分支。 (来自于Oracle)                   |                                   |
| floor             | 按较小的整数取整                                 |                                   |
| hour              | 获得小时数                                    |                                   |
| length            | 文字长度计算。基于字符个数（英文和汉字都算1）                  | (varchar)                         |
| lengthb           | 文字长度按实际存储大小计算，因此在UTF8编码的数据库中，汉字实际占3个字符。  | (varchar)                         |
| locate            | 双参数,在参数2中查找参数1，返回匹配的序号（序号从1开始）。          | (varchar,varchar)                 |
| lower             | 字符串转小写                                   | (varchar)                         |
| lpad              | 在字符串或数字左侧添加字符，拼到指定的长度。                   | (expr,  varchar, int)             |
| ltrim             | 截去字符串左边的空格                               | (varchar)                         |
| max               | 返回分组中的最大值                                |                                   |
| min               | 返回分组中的最小值                                |                                   |
| minute            | 获得分钟数                                    |                                   |
| mod               | 取模运算。前一个参数除以后一个时的余数。                     | (int,  int)                       |
| month             | 获得月份数                                    |                                   |
| now               | 无参数，和CURRENT_TIMESTAMP含义相同。              |                                   |
| nullif            | 双参数，nullif ( L, R )。两值相等返回null，否则返回第一个参数值。 | (expr,expr)                       |
| nvl               | 双参数，返回第一个非null值 (来自于Oracle)              |                                   |
| replace           | 三参数，查找并替换字符 replace(sourceStr, searchStr,  replacement) | (varchar,varchar,varchar)         |
| round             | 四舍五入取整                                   | (float)                           |
| rpad              | 在字符串或数字右侧添加字符，拼到指定的长度。                   | (expr,  varchar, int)             |
| rtrim             | 单参数，截去字符串右边的空格 (来自于Oracle)               | (varchar)                         |
| second            | 单参数，获得秒数                                 | (timestamp)                       |
| sign              | 单参数，判断数值符号。正数返回1，负数返回-1，零返回0             | (number)                          |
| str               | 但参数，将各种类型变量转为字符串类型 (来自HQL)               |                                   |
| subdate           | 双参数，在日期上减去天数，支持Interval语法。 (来自于MYSQL,和DATE_SUB含义相同) |                                   |
| substring         | 取字符的子串 (第一个字符号为1) ，例如 substring('abcde',2,3)='bcd' | (source,  startIndex, length)     |
| sum               | 返回分组中的数据的总和                              |                                   |
| time              | 单参数，截取时分秒                                |                                   |
| timestampadd      | 时间调整 第一个参数是时间调整单位，第二个参数是调整数值，第三个参数是日期时间表达式。例如 timestampadd( MINUTE, 10,  current_timestamp)   （来自MySQL，同时是JDBC函数） | (SQL_TSI,  number, timestamp)     |
| timestampdiff     | 返回两个时间差值多少，第一个参数为返回差值的单位取值范围是SQL_TSI的枚举，后两个参数是日期1和日期2，返回日期2减去日期1（注意和datediff刚好相反）  例如Timestampdiff(MINUTE, date1,  date2)返回两个时间相差的分钟数。 （来自MySQL，同时是JDBC函数） | (SQL_TSI,  time1, time2)          |
| translate         | 三参数，针对单个字符的批量查找替换。Translate(‘Hello,World’,’eo’,’oe’).  将字符中的e替换为o，o替换为e。 (来自于Oracle)。  注意：在部分数据库上使用多个replace语句来模拟效果，但由于模拟相当于执行多次函数，因此如果先替换字符列表中出现重复替换现象，结果可能和Oracle不一致。 | (varchar,varchar,  varchar)       |
| trim              | 截去字符串两头的空格。                              | (varchar)                         |
| trunc             | 适用于数字，保留其小数点后的指定位数。在Oracle上trunc还可用于截断日期。此处不支持。 | (float)                           |
| upper             | 字符串转大写                                   | (varchar)                         |
| year              | 获得年份数                                    | (timestamp)                       |

 

jef.database.query.Scientific

| 函数      | 作用                                |
| ------- | --------------------------------- |
| acos    | 反余弦函数                             |
| asin    | 反正弦函数                             |
| atan    | 反正切函数                             |
| cos     | 余弦函数                              |
| cosh    | 双曲余弦函数                            |
| cot     | 余切函数                              |
| degrees | 弧度转角度 即value/3.1415926*180        |
| exp     | 返回自然对数的底(e)的n次方。                  |
| ln      | 自然对数等同于log                        |
| log10   | 以10为底的对数                          |
| power   | 乘方                                |
| radians | 角度转弧度 即value/180*3.1415926        |
| rand    | 随机数，返回0..1之间的浮点随机数.               |
| sin     | 正弦函数                              |
| sinh    | 双曲正弦函数                            |
| soundex | 字符串发音特征。不是科学计算，但只对英语有用，对汉语几乎没有作用。 |
| sqrt    | 平方根                               |
| tan     | 正切函数                              |
| tanh    | 双曲正切函数                            |

 

常用函数中，某些函数并非来自于任何数据库的SQL函数中，而是EF-ORM定义的，比如str函数。

 

    除了上述被提取的常用函数外，数据库原生的各种函数和用户自定义的函数仍然能够被使用。但是EF-ORM无法保证包含这些函数的SQL语句被移植到别的RDBMS后还能继续使用。

#### 7.3.3.3. 方言扩展

函数的支持和改写规则定义是通过各个数据库方言来定义的。因此，要支持更多的函数，以及现有的一些不兼容的场景，可以通过扩展方言来实现。

方言扩展的方法是配置自定义的方言类。在jef.properties中，我们可以指定自定义方言配置文件，来覆盖EF-ORM内置的方言。

jef.properties

上面定义了自定义方言映射文件的名称是my_dialacts.properties，然后在my_dialacts.properties中配置自定义的方言映射。

在见示例工程目录：orm-tutorial\src\main\resources\my_dialects.properties

文件中，前面是要定义的数据库类型，后面是方言类。

我们编写的自定义方言类如下。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\dialect\DerbyExtendDialect.java

 

自定义方言可以控制数据库各种本地化行为，包括分页实现方式、数据类型等。这些实现可以参考EF-ORM内置的方言代码。

 

**示例1****：让Derby****支持反三角函数TAN2**

DERBY数据库支持反三角函数TAN2函数。但是因为方言中没有注册这个函数，因此我们在E-SQL中是无法使用的。

 

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\CaseDialectExtend.java

上面的代码会抛出异常信息

这个信息其实是不对的，Derby数据库支持该函数，而方言中遗漏了这个函数。现在我们可以在方言中补上注册

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonc\DerbyExtendDialect.java

然后再运行上面的案例，可以发现案例能够正确运行。

 

**示例2****：让Derby****支持ifnull****函数**

MySQL中有一个ifnull函数。返回参数中第一个非空值。

如果我们要在Derby上支持带有这个函数的SQL语句，要怎么写呢？我们可以在自定义方言的构造函数中，注册这样一个函数。

这个函数的作用是在实际运行时，用一组CASE WHEN... THEN... ELSE... 语句来代替原先的ifnull函数。

我们试一下

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\ DerbyExtendDialect.java

可以发现，在注册ifnull函数之前，上面的SQL语句是无法运行的，而注册了函数之后，SQL语句就可以运行了。

EF-ORM中注册的函数有三种方式

| 方法                   | 作用                                       |
| -------------------- | ---------------------------------------- |
| registerNative()     | 注册一个函数的本地实现。本地实现就是数据库原生支持的函数。比如 count() sum() avg()等函数。 |
| registerAlias()      | 注册一个函数的替换实现。替换实现就是数据库虽然不支持指定的函数，但是有其他函数能兼容或者包含需要的函数功能。在实际执行时，函数名将被替换为本地函数名，但对参数等不作处理。 |
| registerCompatible() | 注册一个函数的模拟实现。模拟实现就是完全改写整个函数的用法。从函数名称到参数，都会被重写。 |

### 7.3.4.   绑定变量占位符

E-SQL中表示参数变量有两种方式 : 

l  :param-name　(如:id :name，用名称表示参数) 

l  ?param-index　 (如 ?1 ?2，用序号表示参数)。 

 

上述绑定变量占位符是和JPA规范完全一致的。

 

E-SQL中，绑定变量的参数类型可以声明也可以不声明。比如上例的

也可以写作

但是如果不声明类型，那么如果传入的参数为List<String>，那么数据库是否能正常执行这个SQL语句取决于JDBC驱动能否支持。（因为数据库里的id字段是number类型而传入了string）。

指定参数类型是一个好习惯，尤其是当参数来自于Web页面时，这个特性尤其实用。

很多时候我们从Web页面或者配置中得到的参数都是string类型的，而数据库操作的类型可能是int,date,boolean等类型。此时我们可以在Native Query语句中指定参数类型，使其可以自动转换。

参数的类型有：date,timestamp,int,string,long,short,float,double,boolean。

参数可以为数组，如上例，可以用数组表示in条件参数中的列表。

 

目前我们支持的参数类型包括(类型不区分大小写)：

| 类型名       | 效果                                       |
| --------- | ---------------------------------------- |
| DATE      | 参数将被转换为java.sql.Date                     |
| TIMESTAMP | 参数将被转换为java.sql.Timestamp                |
| INT       | 参数将被转换为int                               |
| STRING    | 参数将被转换为string                            |
| LONG      | 参数将被转换为long                              |
| SHORT     | 参数将被转换为short                             |
| FLOAT     | 参数将被转换为float                             |
| DOUBLE    | 参数将被转换为double                            |
| BOOLEAN   | 参数将被转换为boolean                           |
| STRING$   | 参数将被转换为string,并且后面加上%，一般用于like xxx% 的场合  |
| $STRING$  | 参数将被转换为string,并且两端加上%，一般用于like %xxx% 的场合 |
| $STRING   | 参数将被转换为string,并且前面加上%，一般用于like %xxx 的场合  |
| SQL       | SQL片段。参数将直接作为SQL语句的一部分，而不是作为SQL语句的绑定变量处理（见后文例子） |

 

上面的STRING$、$STRING$、$STRING三种参数转换，其效果是将$符号替换为%，主要用于从WEB页面传输模糊匹配的查询条件到后台。使用该数据类型后，%号的添加交由框架自动处理，业务代码可以更为清晰简洁。看下面的例子

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

 

SQL类型是将参数作为SQL片段处理，该功能使用参见7.3.6节。

### 7.3.5.   动态SQL——表达式省略

  EF-ORM可以根据未传入的参数，动态的省略某些SQL片段。这个特性往往用于某些参数不传场合下的动态条件，避免写大量的SQL。有点类似于IBatis的动态SQL功能。

我们先来看一个例子

上面列举了五种场合，每种场合都没有完整的传递四个WHERE条件。

上述实现是基于SQL抽象词法树（AST）的。表达式省略功能的定义是，如果一个绑定变量参数条件（如 = > < in like等）一端无效，那么整个条件都无效。如果一个二元表达式（如and or等）的一端无效，那么就退化成剩余一端的表达式。基于这种规则，NativeQuery能够将未设值的条件从查询语句中去除。来满足动态SQL的常见需求。

 

 

 

    最后，还要澄清一点——什么叫“不传入参数”。实时上，不传入参数表示自从NativeQuery构造或上次清空参数之后，都没有调用过setParameter()方法来设置参数的值。将参数设置为””或者null并不表示不设置参数的值。下面的例子说明了这一点。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

 

目前动态表达式省略可以用于两种场景，一是where条件，二是update语句中的set部分（见下面例子）。其他场合，如Insertinto语句中的列等不支持这种用法。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

 

### 7.3.6.   动态SQL片段

    有一种特别的NativeQuery参数类型，<SQL>表示一个SQL片段。严格来说，这其实不是一种绑定变量的实现。凡是标记为<SQL>类型的变量，都是被直接加入到SQL语句中的。

比如：

   orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

    上面的例子中 :columns :orderby中被设置的值，都是SQL语句的一部分，通过这些动态的片段来重写SQL。第一次的查询，在Where条件没有任何输入的情况下where子句被省略。最后实际执行的SQL语句是这样——

第二次查询，select表达式发生了一些变化，最后执行的SQL语句是这样——

 

    前面说了，在查询语句中也可以省略参数类型，比如上例，我们写作

   此时，我们还能将orderBy当做是SQL片段，而不是运行时的绑定变量参数处理吗？

答案是可能的，只要我们传入的参数是SqlExpression对象，那么就会被当做是SQL片段，直接添加到SQL语句中。

### 7.3.7.   分页查询

NativeQuery对象支持分页查询。除了之前7.2.2中我们了解到的getResultCount()方法和setRange()方法外，还有已经封装好的方法。在Session对象中，我们可以从NativeQuery对象得到PagingIterator对象。

为了简化操作Session中还提供两个方法，直接将传入的SQL语句包装为NativeQuery，再从NativeQuery得到PagingIterator对象。

| 方法                                       | 返回值               | 用途                                       |
| ---------------------------------------- | ----------------- | ---------------------------------------- |
| Session.pageSelect(NativeQuery<T>,  int) | PagingIterator<T> | 从NativeQuery得到PagingIterator对象。          |
| Session.pageSelect(String,  Class<T>, int) | PagingIterator<T> | 将传入的SQL语句包装为NativeQuery，再从NativeQuery得到PagingIterator对象。 |
| Session.pageSelect(String,  ITableMetadata, int) | PagingIterator<T> | 将传入的SQL语句包装为NativeQuery，再从NativeQuery得到PagingIterator对象。 |

 

上述API的实际用法如下

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

 

PagingIteraor对象的用法在前面已经介绍过了。这个对象为所有不同的查询方式提供了统一的分页查询接口。

NativeQuery分页应用的要点和前文一样——开发者无需关心COUNT语句和分页的写法，将这部分工作交给框架自行完成。

### 7.3.8.   为不同RDBMS分别编写SQL

   为了让SQL语句能被各种数据库识别，EF-ORM会在SQL语句和数据库函数层面进行解析和重写，但这不可能解决一切问题。在命名查询中，还允许开发者为不同的RDBMS编写专门的SQL语句。在运行时根据当前数据库动态选择合适的SQL语句。

一个实际的例子是这样——

在上例中，名为”*test-tree*”的命名查询有了两句SQL，一句是Oracle专用的，一句是其他数据库通用的。

一般来说，我们配置命名查询的名称是这样的，

              在所有RDBMS上使用的语句

    但我们可以为查询名后面加上一个修饰——

 

                   优先在Oracle上使用的语句

这个查询的名称依然是叫“test-tree”，但这个配置只会在特定的数据库(Oracle)上生效。

在运行时，如果当前数据库是oracle，那么会使用专用的SQL语句，如果当前数据库是其他的，那么会使用通用的SQL语句。由于编码时不确定会使用哪个SQL语句，所以在设置参数前可以用containsParam检查一下是否需要该参数。

一般情况下，我们的查询名称中不用带RDBMS类型。这意味着该SQL语句可以在所有数据库上生效。

    当RDBMS下SQL写法差别较大时，开发者可以使用这种用法，针对性的为不同的数据库编写SQL语句。

### 7.3.9.   对Oracle Hint的支持

正常情况下，解析并重写后的SQL语句中的注释都会被除去。但是在Oracle数据库上，我们可能会用Oracle Hint的方式来提示优化器使用特定的执行计划，或者并行查询等。一个Oracle Hint的语法可能如下所述

 

基于抽象词法树的解析器对注释默认是忽略的，但为了支持Oracle Hint的用法，EF-ORM作了特别的处理，在特殊处理后，只有紧跟着SELECT  UPDATE  DELETE INSERT四个关键字的SQL注释可以被保留下来。一般情况下，Oracle Hint也就在这个位置上。

 

### 7.3.10.   对Limit m,n / Limit n Offset n的支持

    在PostgresSQL和MySQL中，都支持这种限定结果集范围的写法。这也是最简单的数据库分页实现方式。

    我们首先来回顾一下SQL中Limit Offset的写法和几种变体：

1、LIMIT nOFFSET m    跳过前m条记录，取n条记录。

2、LIMITm,n             跳过前m条记录，取n条记录。(注意这种写法下 n,m的顺序是相反的)

3、LIMIT ALLOFFSET m  跳过前m条记录，取所有条记录。

4、OFFSETm            跳过前m条记录，取所有条记录。(即省略LIMIT ALL部分)

5、LIMIT nOFFSET 0     取n条记录。

6、LIMIT n               取n条记录。(即省略 OFFSET 0部分)

    以上就是LIMIT的SQL语法。

 

在E-SQL中，如果用户传入的SQL语句是按照上述语法进行分页的，那么EF-ORM会将其改写成适合当前RDBMS的SQL语句。即——

在非Postgresql或MySQL上，也能正常进行结果集分页。

在支持LIMIT语句的RDBMS上（如MySQL/Postgresql）上，LIMIT关键字将出现在SQL语句中。

### 7.3.11.  对Start with ... Connect by的有限支持

Oracle支持的递归查询是一个让其他数据库用户很“怨念”的功能。这种语法几乎无法在任何其他数据库上使用，然而其用途却无可替代，并且难以用其他函数模拟。除了Postgres也有类似的递归查询用法外，在其他数据库上只有通过复杂的存储过程了……这使得开发要支持多数据库的产品变得更为困难。

EF-ORM却可以在所有数据库上，在一定程度上支持这种操作。

 

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

上面的语句看似是不可能在Derby数据库上执行的，然而运行这个案例，你可以看见正确的结果。为什么呢？

这是因为EF-ORM 1.9.2以后，开始支持查询结果”后处理“。所谓“后处理”就是指对查询结果在内存中再进行过滤、排序等处理。这一功能本来是为了满足多数据库路由操作下的排序、group by、distinct等复杂操作而设计的，不过递归查询也得以从这个功能中获益。对于一些简单使用递归查询的场合，EF-ORM可以在内存中高效的模拟出递归效果。当然，在递归计算过程中需要占用一定的内存。

为什么说是”一定程度上支持这种操作“呢？因为目前对此类操作的支持限制还非常多，当前版本下，要使用内存计算模拟递归查询功能，有以下条件。

1、start with... Connect by条件必须在顶层的select中，不能作为一个嵌套查询的内层。

2、Connect by的键值只允许一对。

3、Start with条件和connect by的键值这些列都必须在查询的结果中。

4、Start with目前还只支持一个条件，不支持AND OR。

 

 

 

 

 

 

## 7.4. 存储过程调用

   使用EF-ORM封装后的存储过程调用，可以——

l  指定存储过程的入参出参，并帮助用户传递。

l  将存储过程传出的游标，映射为合适的java对象。

l  支持匿名存储过程

l  和其他操作处于一个事务中

只要是数据库能支持的存储过程，EF-ORM都可以调用。存储过程调用过程会封装为一个NativeCall对象。使用该对象即可传入/取出存储过程的相关参数。对于游标类的传出参数，还可以直接转换为java bean。

### 7.4.1.   使用存储过程

    我们还是先从一个例子开始。由于Derby在存储过程上功能较弱，我们这个例子需要在MySQL下运行。存储过程为——

数据准备和测试代码：

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case3.java

在运行上面的案例前请先修改

         *db* = **new** DbClient("jdbc:mysql://localhost:3307/test","root","admin",5);

行中的URL，将其配置为一个可以连接的MySQL地址。在testProducre（）方法中对100001和100002号雇员进行了不同的加薪处理。通过存储过程调用前后输出的雇员信息变化，可以看到存储过程的效果。

### 7.4.2.   存储过程传出参数

假设有存储过程如下

该存储过程传出一个数值。

在上例中，存储过程的出参设置，需要用一个名为OutParam的工具类。使用这个类，可以生成若干出参的类型表示。OutParam.*typeOf*(Integer.**class**)表示一个Integer类型的传出参数。

 

在存储过程执行完后，使用getOutParameter(2);方法可以得到存储过程的传出参数，这里的2表示出参是第二个参数，这个序号需要和之前定义的参数需要一致，一个存储过程可以传出多个参数，因此要用序号加以区别。得到的传出参数类型和之前定义的一致，是Integer类型。

### 7.4.3.   存储过程传出游标

Oracle数据库上的存储过程可以传出游标。存储过程如下：

这里的游标类型就是t_person表。因此该存储过程相当于返回了一个在t_person表上的查询结果集。

EF-ORM可以将游标类型的结果集重新映射为java对象。

 

在上例中，存储过程的出参设置，需要用一个名为OutParam的工具类。使用这个类，可以生成若干出参的类型表示。OutParam.*listOf*(Person.**class**)表示传出的游标将被包装为Person类型的List。

 

在存储过程执行完后，使用getOutParameterAsList(1,Person.class);方法可以得到存储过程的传出参数。其中，1是传出参数在存储过程定义中的序号。

大部分数据库都不支持传出游标。这个案例仅对支持的数据库（如Oracle）有效。

最后，要注意的是由于游标的存在，需要显式的去关闭NativeCall对象，否则会发生游标泄露问题。

### 7.4.4.   使用匿名过程（匿名块）

在Oracle中，还可以执行临时编写的未命名的匿名块。匿名块的语法和存储过程基本一致。

对应到EF-ORM可以这样操作——

    在上例中，用户临时定义了一个匿名块。EF-ORM中调用的方法为createAnonymousNativeCall()。该方法允许用户得到一个匿名块构成的NativeCall对象。匿名块的入参设置和出参获取和存储过程完全一样。

 

 

## 7.5. 原生SQL使用

### 7.5.1.   使用原生SQL查询

前面我们已经了解了EF-ORM对SQL的封装和改进。基于这种改进，我们使用E-SQL来享受改进所带来的优点——让SQL在各种RDBMS上运行；和业务代码更好的集成等等。

 

但是，麻烦必然伴随而来。SQL解析和改写器并不是总能完美的工作——

l  SQL解析和改写是一个复杂的过程，尽管经过很多努力优化，但是每个SQL的解析依然要花费0.05到1毫秒不等的时间。可能不满足追求性能极限的场合。

l  一些过于复杂的，或者我们开发时没有测试到的SQL写法可能会解析错误。（请将解析错误的SQL语句发给我们，谢谢。）

EF-ORM内置的SQL分析器能处理绝大多数数据库DDL和DML语句。包括各种建表、删表、truncate、Create Table as、Select嵌套、Oracle分析函数、Oracle树型关系选择语句等。但是RDBMS的多样性和SQL语句的复杂性使得完全解析多少有些难度，因此EF-ORM依然保留原生的，不经过任何改写的SQL查询方式，作为NativeQuery在碰到以下麻烦时的”逃生手段“。

原生SQL和NativeQuery不同，不进行解析和改写。直接用于数据库操作。

 

明显的影响，原生SQL中，绑定变量占位符和E-SQL不同，用一个问号表示，和我们直接操作JDBC时一样——

 

首先我们可以用Session对象(DbClient)中的selectBySql方法进行查询。看这个例子

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

 

上面使用了Session对象中的selectBySql方法，在Session中，这个系列的方法是可以使用原生SQL的——

| 方法                                       | 说明                                       |
| ---------------------------------------- | ---------------------------------------- |
| Session.executeSql(String,  Object...)   | 执行指定的SQL语句                               |
| Session.getResultSet(String,  int, Object...) | 根据SQL语句获得ResultSet对象                     |
| Session.selectBySql(String,  Class<T>, Object...) | 根据SQL查询，返回指定的对象                          |
| Session.loadBySql(String,  Class<T>, Object...) | 根据SQL查询，返回指定的对象（单行）                      |
| Session.selectBySql(String,  Transformer, IntRange, Object...) | 根据SQL查询，传入自定义的结果转换器和分页信息                 |
| Session.getSqlTemplate(String)           | 获得指定数据源下的SqlTemplate对象。SQLTempate是一个可以执行各种SQL和本地化查询的操作句柄。 |

Session对象中，凡是xxxxBySql()这样的方法，都是传入原生SQL语句的。同时这些方法都提供了可变参数，其中的Obejct... 对象就是绑定变量参数。使用时按顺序传入绑定变量就可以了。

最后一个方法，可以得到SqlTemplate对象，SqlTemplate对象是一个可以执行各种SQL和本地化查询的操作句柄。

 

**在拼装SQL****时处理Schema****映射和函数本地化问题**

使用原生SQL，意味着开发者要自行解决schema重定向和数据库函数本地化的问题。可以使用下面两个方法来帮助获得相关的信息。

| 方法                                       | 说明                                       |
| ---------------------------------------- | ---------------------------------------- |
| MetaHolder.getMappingSchema(String)      | 传入开发时的虚拟schema，返回实际运行环境对应的schema。用于拼装到原生SQL中。 |
| Session.func(DbFunction,  Object...)     | 传入函数名和参数。返回该函数在当前数据库下的方言实现。              |
| SqlTemplate.func(DbFunction,  Object...) | 效果同上，区别是使用了特定数据源的方言。                     |

上述三个方法都可以返回String，供开发人员自行拼装SQL语句使用。

 

### 7.5.2.   SqlTemplate

由于EF-ORM支持多数据源，因此要在特定数据源上执行SQL操作时，都要先获得对应的SqlTemplate对象。前面的各种示例中，都是在Session上直接操作本地化查询和SQL的，这种操作方式只会在默认数据源上操作。因此SQLTemplate除了提供更多原生SQL的操作方法以外，还是操作多数据源时必须使用的一个对象。

要获得一个数据源的SqlTemplate对象，可以使用——

 

得到了SqlTemplate

SqlTemplate对象的使用。SqlTemplate中有很多方法是和本地化查询有关的，也有使用原生SQL语句的查询。(   详情参阅API-DOC)

| 方法                                       | 作用                         |
| ---------------------------------------- | -------------------------- |
| getMetaData()                            | 获得数据源元数据操作句柄               |
| createNativeQuery(String,  Class<T>)     | 创建本地化查询                    |
| createNativeQuery(String,  ITableMetadata) | 创建本地化查询                    |
| createNativeCall(String,  Type...)       | 创建存储过程调用。                  |
| createAnonymousNativeCall(String,  Type...) | 创建匿名过程调用。                  |
| pageSelectBySql(String,  Class<T>, int)  | 按原生SQL分页查询                 |
| pageSelectBySql(String,  ITableMetadata, int) | 按原生SQL分页查询                 |
| countBySql(String,  Object...)           | 按SQL语句查出Long型的结果           |
| executeSql(String,  Object...)           | 执行SQL语句                    |
| loadBySql(String,  Class<T>, Object...)  | 按SQL语句查出指定类型结果（单条记录）       |
| selectBySql(String,  Class<T>, Object...) | 按SQL语句查出指定类型结果             |
| selectBySql(String,  Transformer, IntRange, Object...) | 按SQL语句查出指定类型结果(带分页范围)      |
| iteratorBySql(String,  Transformer, int, int, Object...) | 按SQL语句查出指定类型结果，以遍历器形式返回。   |
| executeSqlBatch(String,  List<?>...)     | 执行SQL语句，可以传入多组参数并在一个批次内执行。 |

 

使用SqlTemplate的示例——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case2.java

SqlTemplate中的其他方法以此类推，详见java-doc。

## 7.6. 无表查询

无表查询是一类特殊的SQL查询。比如，查询当前数据库时间，在Oracle中，是通过一张虚拟表DUAL完成的——

在MySQL中，无表查询是这样的——

在Derby中，无表查询是这样的——

 

显然，不同的数据库的无表查询语法是不一样的。为了兼容不同数据库的无表查询场景，框架提供了相应的API。和无表查询相关的API主要有getExpressionValue()系列的方法，该方法可以执行一次无表查询。

API列表

| 方法                                       | 用途                                |
| ---------------------------------------- | --------------------------------- |
| **Session****中的方法**                      |                                   |
| getExpressionValue(String,  Class<T>)    | 传入SqlExpression对象，计算表达式的值。        |
| **SqlTempate****中的方法**                   |                                   |
| getExpressionValue(String,  Class<T>, Object...) | 传入String对象。得到指定的SQL表达式的查询结果(无表查询) |
| getExpressionValue(DbFunction,  Class<T>, String...) | 得到指定的SQL函数的查询结果(无表查询)             |

 

使用举例

Session中的无表查询方法，将在默认数据源上计算指定的数据库表达式；SqlTemplate中的同名方法可以在指定数据源上计算表达式。表达式中出现的函数会被解析和改写。此外SqlTemplate还提供了一个直接传入Func对象计算的函数。

 

# 8.    高级查询特性

## 8.1. 查询结果的转换

ORM框架最被人直观感受的功能就是： JDBC的查询结果ResultSet会被转换为用户需要的Java对象。没有之一。

在前面的例子中，我们已经了解到了一部分查询和其对应返回结果的规律。本章中，我们将全面的罗列这些规律，并介绍一些新的返回结果指定方式。EF-ORM提供了一套统一的查询返回结果定义方式，因此本章中提到的结果返回方式规则，适用于前面讲到的所有查询方式——Criteria API的或者是NativeQuery的。

简单说来，查询结果的返回场景分为以下几种——

\1. 返回简单对象

\2. 返回和查询表匹配的对象

\3. 返回任意对象容器

\4. 返回Var /Map

\5. 多个对象以数组形式返回

\6. 多个列以数组形式返回

 

除了上述几个情况以外，还有两种方式转换结果。

\7. 动态表的返回：在查询动态表时返回结果的场景。所谓动态表，就是开发时不为数据库表编写映射类。而是在运行时直接按表结构生成元数据模型（ITableMetadata），用该模型当做映射类来操作数据表的方法。动态表查询将在后面章节描述。

\8. 自定义ResultSet到Java对象的映射关系： 用户实现ResultSet到Java对象之间的映射器(Mapper)接口，用自行实现的代码来描述从ResultSet到java类之间的转换逻辑。

 

    场景1~6将在这一章介绍；场景8的用法在8.2节介绍。现在我们分别看一下上述场景的例子——

### 8.1.1.   返回简单对象

   查询结果可以返回为一个简单对象，如String、Integer、Long、Boolean、Date这样的基本类型。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

   返回简单对象时，查询出的列只取第一列。该列的值被转换为需要的简单对象。

### 8.1.2.   返回和查询表匹配的对象

这个是使用最广泛的，简单说来查询什么表，返回什么表的对象。这也是大多数人对框架的理解方式。也只有在这种模式下，级联、延迟加载等特性才能体现出来。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

   基于Query对象的查询，因为Query在构造时就已经知道其对应的表和对象。因此默认情况下，Query就能返回其对应表的对象。多表查询如Join、还有SQL查询下是不会出现这种场景的。****

### 8.1.3.   返回任意容器对象

用户可以指定任意一个Bean作为返回结果的容器，只要SQL语句中查出的字段和该容器对象中的字段名称一致（忽略大小写），就会将查询结果列值注入。对于不能匹配的字段，查询结果将丢弃。

比如，我们用临时定义的一个类PersonResult来作为查询结果的容器。

 orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

上例中，定义了一个返回结果容器类PersonResult。该类的字段和Person表并不完全一致。其中Person表中有的current_school_id字段该类没有，因此该列的值被丢弃。同时PersonResult类中的birthday字段Person表中没有，故该字段的值为null。

我们注意到，t_person表中有一个名为created的日期型字段，能不能把birthday的值设置为这个列的值呢？是可以的，一个办法是在birthday上加上@Column注解——

也就是说，无论用于结果容器的类是不是为Entity，其字段上的@Column注解都有效。在ResultSet和结果类的字段进行匹配时，如果没有@Column注解，那么使用java field name和数据库列匹配；反之则用@Column中的name属性和数据库列匹配。均忽略大小写。

 

我们再看下面这个例子。这一次我们利用另一个数据库表的对象Student，来作为Person表的查询容器。

 orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java****

    上例中，采用三种写法，使Person表的数据能被注入到Student对象中去。其中值得注意的是Student对象中有一个名为dateOfBirth，数据库列为"DATE_OF_BIRTH"的字段。

然而在上面的示例中，前两处用的是java名，在SQL语句中用的是数据库列名。这是因为CriteriaAPI中的column(xxx)实现上有一个特殊规则。当使用as()方法指定列在SQL中的别名是dateOfBirth时，同时还指定了这个列要注入到对象的dateOfBirth字段内。因此书写时可以按java field名称。

 

 

使用自定义对象来容纳结果，对于NativeSQL和原生SQL来说是特别方便的。我们可以用任何一个已经存在的Java Bean作为结果返回容器——只要在SQL语句中，将列的别名(alias)写的和java类的中字段名称一样就可以了。

比如，我们找了一个其他库中的类，这个类有 key, value两个属性。我们在编写SQL时候让返回的两个字段名称分别为key,value，查询结果就乖乖的进入到指定的对象里去了

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

### 8.1.4.   返回Var /Map

   在没有合适的容器容纳返回结果时，一种通常的手段是使用Map。在前面的例子中，我们已经很多次的使用了Map容器。我们回过去看一下很久以前的例子——

 

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

一个值得注意是问题是，不同的数据库对列的大小写处理是不同的。比如Oracle中，返回的数据库列名称都是大写的，MySQL正相反。而很不幸的，Map的Key是大小写敏感的，因此当我们从Map中获取数据时，我们要使用大写的列名，还是小写的呢？

为了解决这个问题，EF-ORM返回Map的时候，用的不是JDK中的HashMap或是其他Map。而是自行实现的一个类jef.script.javascript.Var，这个类实现了Map接口，是一个忽略Key大小写的Map。所有的key在被放入时都转为小写。开发者可以任意用大写或小写的方式获取Map中的数值。这种定义也和大部分SQL环境保持一致。

一些时候，不指定返回的结果类型，也会用Map作为默认的数据返回类型。例如NativeQuery的单参数构造、不指定查询返回结果类型的Join查询等。

当需要用Map作为返回类型时，只需指定Var.class/Map.class作为返回类型，无需指定到具体的HashMap等类型。

### 8.1.5.   多个对象以数组形式返回

   在Join查询中我们经常需要返回多个对象。我们看一下前面的示例****

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson6\Case1.java

在使用CriteriaAPI的查询中，我们可以使用Object[].class作为返回结果类型。因为参与查询的每张表，都是以java对象的形式传入的。

那么，在本地化查询中，我们直接编写的SQL语句也能以Object[]分别传回吗？框架默认不提供这种行为。但是可以通过ResultTransformer来定义从ResultSet到Object[]的转换逻辑。这将在下一节介绍。

### 8.1.6.   多个列以数组形式返回

   另一种方式下，我们依然可以返回数组。但这种方式返回的数组和返回多个对象不同。这是将查询出的多个列，直接以数组的方式返回。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

复杂一些的查询，如多表查询，也是可以用这种方式返回结果的.

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

上例中三次查询，指定的返回类型都是数组。查询条结果就是按照select语句中各列的出现顺序形成数组。

l  如果指定为Object[]，那么每列的值将保留为默认的数据类型，如String、Integer等。

l  如果指定为String[]，那么每列的值都会被强转为Striung。

l  如果指定为Integer[]也会执行转换，不过如果有列不能转换为数字就会抛出异常。

l  其他类型同

从上面可以看到，无论用NativeQuery，或是单表查询的CriteriaAPI查询，都可以将结果列拼成数组返回。

    有同学可能会有疑问，目前对于8.1.5和本节中，数组返回的指定方式是一样的，但结果集转换逻辑是有差异的。这是不是会造成混淆呢？请参见8.2章。

 

## 8.2. Transformer的使用

在上面的例子中，我们都使用slectAs() loadAs()等方法，在传入Query对象的同时指定了要返回的class类型。实际上，即便不使用selectAs和loadAs方法，也一样能指定返回的结果类型。

我们看一下Session类中的loadAs的源代码：

代码清单：EF-ORM中Session类的源代码, loadAs方法

我们可以发现，其实数据返回类型就是直接设置在Query对象内的。

我们前面看到过的三种查询对象——Join / Query<T>/UnionQuery，其实都提供了一个方法。

使用这个方法就可以得到Transformer对象，这个对象就是描述结果转换的各种行为的对象。除了控制结果转换的类型以外，还提供了更多的相关参数。我们举几个典型的例子来观察其作用。

### 8.2.1.   直接指定返回类型

首先，前面例子中的一部分使用selectAsloadAs的方法，我们可以直接用select、load等方法等效的来实现。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

   因此loadAs和selectAs方法不是必需的。不过这两类方法存在一是可以简化API，让人更容易理解和使用。二是让传入的Class泛型等同于传出的泛型，使用这两个方法可以利用java语法的校验，减少开发者搞错返回类型的机会。因此，一般在条件允许的情况下，您无需使用Transformer来指定返回结果。

### 8.2.2.   区分两种返回数据的规则

在上一节，介绍了查询时返回数组的两种转换规则。

l  多表查询时每张表对应一个java bean，多个bean构成数组

l  查询的每个列的值对应到一个java值，多列的java值构成数组

大部分情况下，EF-ORM都能判断出用户传入数组的实际意图，并根据这个意图使用合适的转换规则。

但是也有一些特殊情况，用户传入的查询会具有二义性。比如前面的8.1.5 中Join查询场景，Join查询的Object[]返回格式被认为是“多个对象形成数组”返回，而不是“多列形成数组”返回。

 

   这种情况下，也有办法，我们可以显式的提示EF-ORM，要采用后一种方式返回结果。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

上例中join.getResultTransformer().setStrategy(PopulateStrategy.*COLUMN_TO_ARRAY*);就是给查询设置了一个结果转换策略。显式的指定了采用COLUMN_TO_ARRAY的方式转换结果。

### 8.2.3.   忽略@Column注解

8.1.3节中介绍过，可以用任意java bean作为返回数据的容器。同时我们也提到对于自行编写的sql语句，我们只要将SQL语句中列的别名和java bean的属性名对应上，就可以简单的将数据库查询结果注入到这个Bean中。

那么我们可能写出这样一段代码

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

上面的代码看起来没什么问题，Person对象中也有id、name、gender三个属性。数据库中查出的三个列正好对应到这三个属性上。

但实际运行后，我们发现，Person对象中的name属性并没有被赋值。原因很简单，因为Person类中的name属性上有@Column注解，指定这个属性是和数据库列”person_name”映射，而不是和”name”映射。

因此，当开发者希望查询结果直接和对象中的属性名发生映射，不受@Column注解影响时，可以这样

代码清单：修改后的查询写法:orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

   上例中setStrategy(PopulateStrategy.*SKIP_COLUMN_ANNOTATION*); 给了结果转换器一个提示，使其忽略@Column注解。

### 8.2.4.   自定义返回结果转换

这个可能是Transformer最为复杂也最为强大用法了。在本节中，您可以用自行实现的逻辑去转换ResultSet中返回值的处理。

我们还是分几种情况来介绍相关API和用法。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

上例中，为Student表中的数据转换到PersonResult对象提供了自定义的规则实现。

 

上面的例子中，自定义ResultSet到java Bean的转换。如果一个Java Bean的属性非常多，那么代码也会很繁琐。因此，考虑到很多类都是已经通过Entity类定义了数据库字段和对象关系的类，我们是不是可以利用这些Entity中固有的和数据库的映射关系呢？

 

正是基于这种考虑，EF-ORM中还提供了一个Mappers工具类,可以用Mappers工具直接生成Mapper映射器。

比如这个例子，用自行编写的SQL语句返回Person和school对象。其中school对象将被赋值到Person中的currentSchool字段里。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

 

在上例中，使用Mappers.*aliasToProperty*方法，框架提供了一个默认的映射行为。该行为将属于School对象的所有字段组成一个Schoold对象，然后赋值到目标的currentSchool这个属性中去。

正常操作下，只有框架的级联查询才能产生Bean嵌套的结构作为结果返回。当然用上面的例子手工编码也可以做出同样的效果。但是，既然Person和School都是我们已经建模的Entity对象，为什么不能少些一点代码呢。所以Mappers工具里提供了若干方法，作用是生成一个和已知Enrtity进行映射的Mapper对象。

并且Mappers工具里也包含这样的动作，即生成的映射可以将已知的Entity注入到返回结果的属性中去，产生类似级联操作的嵌套结构。

这里的*toResultProperty*的含义就是，由数据库列转换而成的对象，注入到目标的”currentSchool”属性中去。

 

非常不幸的，上面的案例在Derby虽然可以运行，但是隐藏了一个重大的问题。因为t_person表和school表中都有同名列id，因此查询结果中只能得到一个名为id的列，最终返回的数据是错误的。（在某些数据库上这样的SQL语句会直接报错）。

为此，如果我们要想将id的问题解决，我们可以把方法改成下面这样——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

    Mappers工具返回的BeanMapper对象中，提供了adjust方法，可以指定个别属性的列映射。第一个参数是java属性名，第二个参数是数据库列名。这样就能解决实际使用时，一个Entity大部分字段都和数据库列对应、而个别不对应的问题了。

 

Mappers工具类提供的方法如下

| 方法                                       | 效果                                       |
| ---------------------------------------- | ---------------------------------------- |
| Mappers.toResultProperty(String,  Class<T>) | 将查询结果中的列转换为Class所指定的对象后，注入到结果类的属性中。      |
| Mappers.toResultProperty(String,  Class<T>, String) | 将查询结果中的列转换为Class所指定的对象后，注入到结果类的属性中。  可以指定列的命名空间。（参见后文注解） |
| Mappers.toResultProperty(String,  ITableMetadata, String) | 将查询结果中的列转换为ITableMetadata所指定的对象后，注入到结果类的属性中。  可以指定列的命名空间。（参见后文注解） |
| Mappers.toResultBean(Class<T>)           | 将查询结果中的列按指定类型Entity映射关系，直接注入到结果类中。（比如DO对象中配置映射关系，VO对象字段几乎一致但未配置映射关系，此时可以按DO对象的规则将属性注入到VO对象中） |
| Mappers.toResultBean(Class<T>,  String)  | 将查询结果中的列按指定类型Entity映射关系，直接注入到结果类中。  可以指定列的命名空间。（参见后文注解） |
| Mappers.toResultBean(ITableMetadata,  String) | 将查询结果中的列按指定类型ITableMetadata映射关系，直接注入到结果类中。  可以指定列的命名空间。（参见后文注解） |
| Mappers.toArrayElement(int,  Class<T>)   | 将查询结果中的列转换为Class所指定的对象后，注入到结果数组的指定位置上。（要求查询返回结果为数组） |
| Mappers.toArrayElement(int,  Class<T>, String) | 将查询结果中的列转换为Class所指定的对象后，注入到结果数组的指定位置上。（要求查询返回结果为数组）  可以指定列的命名空间。（参见后文注解） |
| Mappers.toArrayElement(int,  ITableMetadata, String) | 将查询结果中的列转换为Class所指定的对象后，注入到结果数组的指定位置上。（要求查询返回结果为数组）  可以指定列的命名空间。（参见后文注解） |

上面列举了9个生成BeanMapper对象的方法。

首先，需要解释“指定列别名的前缀。”究竟是什么意思。

    注意观察的同学可能已经发现，EF-ORM在使用Criteria API进行多表查询时，对列名的默认处理方式是这样的——

也就是说，每个列的别名是通过增加了表别名的前缀来完成的，整个前缀共4个字符，其中两位是分隔符。实际上，就相当于将原来的ID、NAME等列放到了不同的命名空间下。上面这句SQL中，PERSON表的字段都在T1的命名空间下，SCHOOL表的字段都在T2命名空间下。

因此，当我们使用框架的Criteria API查询数据时，使用“命名空间”可以快速的区分出属于一张表的所有列。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

上面的映射中，将Join查询默认返回两个对象的行为，修改成了返回3个对象，同时还调整了School和Person对象在结果中的位置。这看似和默认的返回行为差不多，但整个过程是充分定制的。并且这种定制是可以随心所欲的——包括T1和T2的映射对象可以变为别的Java Bean。

 

最后，ResultTransformer中提供了若干ignore系列的方法。这是由addMapper的动作衍生而来的。在一些查询中，addMapper并不会清除掉框架默认的结果转换规则，而是会并存。而某些时候我们并不希望发生框架默认的转换行为，因此可以用ignoreXXX方法，要求框架不处理返回结果中的指定列。

我们直接在上面的方法结束前增加一些代码——

   orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

可以看到，在这个部分做了两个至关重要的操作——  

l  t.clearMapper();
清除Join中之前设置的映射器，之前的自定义映射器无法针对本次查询的返回类型操作，如果不清除而任由这些映射器生效，必然造成程序异常。

l  t.ignoreAll(); 
使框架默认的映射规则忽略所有列。即禁用默认的映射规则。因为默认的映射规则完全不能识别这次的返回类型，因此如不清除，一样会抛出异常。

 

## 8.3. 流式操作接口

当我们从数据库需要读取数百万记录时（比如报表和导出功能），常常会面临内存不够用的问题。因为如果一个用户就在内存中缓存百万的数据，那么整个系统都没有内存进行其他正常的工作了。

许多人会采用分页的方法，例如每次到数据库读取5000条。但是会受数据库的幻读问题困扰，因为多次分页期间，系统可能并发的删除或添加某些记录。因此在“不可重复读”的环境下，用分页实现高可靠的数据导出是困难的，某些记录可能被丢失，而某些记录可能会被查出两遍。

一个可行的办法，是使用JDBC的流式处理接口。这点尤其在Oracle上特别有用，因为Oracle的驱动实现得较好——可以返回一批数据到JDBC驱动中，随着一边处理一边将结果向后滚动，批量的将后续的数据传入到客户端来。

这种ResultSet实现使得数据库的查询响应变快，同时用户可以合理的调整每批加载的记录数（fetch-size），使得整体性能达到最优。MySQL 5.0以上Server和5.0以上JDBC驱动，多少也实现了这种流式加载的方法。这能有效的避免客户端出现OutOfMemoryError的信息。

到目前为止，并非所有的数据库JDBC驱动都支持流式操作模型。（也有些数据库是一次性拉取全部结果的），但不管怎么说支持流式操作确实是RDBMS在大数据时代面临的和必须解决的一个问题。

 

但是我们再来看之前使用数据库查询接口

很显然，流式处理模型在这个方法上是走不通的，List<T>中会缓存整个结果集的全部数据。内存占用不是一般的大。

为此，EF-ORM中提供了支持流式操作模型的接口。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case2.java

 

在Session对象中，有iteratedSelect的几个方法，这个系列的方法都可以返回一个ResultIterator<T>对象，这个对象提供了流式处理的行为。按照上例的逻辑，每处理一个对象，就可以在内存中释放掉该对象（在取消了结果集缓存的场合下）。结果集向后滚动到底，内存中也只有最近处理的对象。

对于NativeQuery。前面已经提到过，NativeQuery提供了相同功能的查询方法——

那么如果原生SQL，想要查询大量数据并用流式模型操作呢？在SqlTemplate类中也提供了类似方法——

这个方法的参数有些复杂，详情请参阅API-DOC。

 

**注意事项**

1 请注意ResultIterator的关闭问题。正常情况下，当遍历完成后ResultIterator会自动关闭。但是我们希望在编程时，必须在finally块中显式关闭ResultIterator对象。因为这个对象不关闭，意味着JDBC ResultSet不关闭，数据库资源会很快耗尽的。

2 另外。在Session中有一个方法Session.getResultSet(String, int, Object...)。这个方法返回ResultSet对象，可能有人会误以为这就是JDBC驱动的原生结果集。可以实现流式操作。但实际上这个方法是对原生的Result的完整缓存。因此并不能用在超大结果集的返回上。


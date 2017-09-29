GeeQuery使用手册——Chapter-16  并发和锁

[TOC]

# Chapter-16  并发和锁

## 16.1.  并发造成的问题

一个事务是由一系列数据库操作构成。就并发性和恢复控制的角度来看，其具有“原子性”，在一个事务中如果一个操作失败，就必须撤销所有操作。当多个事务同时访问数据库资源时，可能会造成并发问题，这需要通过事务的隔离级别来控制。 数据库事务必须具备ACID特征，即原子性、一致性、隔离性和持久性。              

 例子，当用户存储100元和水电自动扣费同时发生时，会使得账户中的余额出现问题。 

 ![16.1-1](images\16.1-1.png)

当出现这种情况时，用户的余额只剩下80元，这种并发操作会使得用户受损。

还有一种情况：

 ![16.2-2](images\16.2-2.png)

这时候，用户的账户余额显示为200，这样会造成银行的损失。

## 16.2.  并发问题的分析

### 16.2.1.  脏读（Dirty Reads）

一个事务已经更新记录但还没提交。另一个事务读到了还没提交的数据。

>数据库事务隔离级别（ Read Committed）已经帮我们解决了，一般开发者无需关注

### 16.2.2.  幻读（Phantom Reads）

事务进行先后两次查询，第二次查询比第一次查询中多了，或者少了数据（两次SQL可以不同）。因为在两次查询过程中有另外一个事务插入数据造成的。

>一般除了极个别统计需求，大部分业务能够接受这种数据变化

### 16.2.3.  不可重复读（Non-repeatable Reads）

事务1读取某一数据后，事务2对其做了修改，当事务T1再次读该数据时得到与前一次不同的值。

>大部分业务能够接受这种数据变化
>
>Oracle默认级别提供了可重复读

### 16.2.4.  更新丢失

两个事务都同时更新一行数据，导致对数据的两个修改都失效了。系统没有执行任何的锁操作，因此并发事务并没有被隔离开来。

>几乎所有的数据库几乎都能防止这种情况，除非您自己写一个
>
>由于使用了非原子的 loadand update操作，还是有可能丢失更新

### 16.2.5.  Update重灾区

当对数据进行更新时，存在几种情况

| 操作                | 结果                              |
| ----------------- | ------------------------------- |
| A.update B.delete | 最后记录不存在（被删除），结果是一样              |
| A.insert B.insert | 如果数据库有约束，后来者会收到错误。如无约束，两条记录互不影响 |
| A.update B.insert | 如果数据库有约束，后来者会收到错误。如无约束，两条记录互不影响 |
| A.insert B.delete | 要删除的记录如果还不存在，delete方法返回0，可被检测到  |
| A.update B.update | 最危险的情况。双方都收到了更新成功，无法观测到问题       |

## 16.3.  解决方法

### 16.3.1.  悲观锁（Pessimistic Locking）                                               

悲观锁, 顾名思义，就是很悲观，每次去拿数据的时候都认为别人会修改，所以每次在拿数据的时候都会上锁，这样别人想拿这个数据就会block直到它拿到锁。传统的关系型数据库里边就用到了很多这种锁机制，比如行锁，表锁等，读锁，写锁等，都是在做操作之前先上锁。它指的是对数据被外界（包括本系统当前的其他事务，以及来自外部系统的事务处理）修改持保守态度，因此，在整个数据处理过程中，将数据处于锁定状态。悲观锁的实现，往往依靠数据库提供的锁机制（也只有数据库层提供的锁机制才能真正保证数据访问的排他性，否则，即使在本系统中实现了加锁机制，也无法保证外部系统不会修改数据）。

悲观锁的特点：

1. 读取记录并锁定：锁位于结果集上，其他人可以查看，但修改请求会被挂起。
2. 写入记录，释放锁：位于结果集上的锁被去除，但是事务没有提交时，其他人看不见此时的修改情况，修改过的记录上依然有锁，没有修改的记录会完全被释放。
3. 提交事务：当提交事务后，事务对记录的修改被其他人看见，同时其他人也可以修改该记录。

悲观锁的实例如下：

~~~java
/**
* 锁案例：悲观锁的使用
*/
@Test
public void testPessimisticLock() {
   int id;
  {
     VersionLog v = new VersionLog();
     v.setName("我");
     commonDao.insert(v);
     id = v.getId();
  }
  {
     Query<VersionLog> query = QB.create(VersionLog.class).addCondition(QB.between(VersionLog.Field.id, id - 4, id));
     RecordsHolder<VersionLog> records = commonDao.selectForUpdate(query);
     try {
        for (VersionLog version : records) {
           version.setName("此时:" + version.getName());
          // version.setModified(System.currentTimeMillis());
        }
        records.commit();
     } finally {
        records.close();
     }
  }
  {
     Query<VersionLog> query = QB.create(VersionLog.class).addCondition(QB.between(VersionLog.Field.id, id - 4, id));
     List<VersionLog> records = commonDao.find(query);
     for (VersionLog version : records) {
        System.out.println(version.getName());
     }
  }
}
~~~

注意：这里使用的是RecordsHolder

悲观锁的相关结论：

1. 可以锁多条记录，加锁独占时间长，并发度低。
2. 加锁机制由数据库保证。
3. 只能防止部分并发的数据覆盖场景。

### 16.3.2.  乐观锁 （Optimistic Lock）                                                                                                              
乐观锁, 顾名思义，就是很乐观，每次去拿数据的时候都认为别人不会修改，所以不会上锁，但是在更新的时候会判断一下，在此期间别人有没有去更新这个数据，可以使用版本号等机制。乐观锁适用于多读的应用类型，这样可以提高吞吐量，像数据库如果提供类似于write_condition机制的其实都是提供的乐观锁。

原理：

1. 每条记录都有一个版本字段
2. 当两个事务同时读取的时候，都会读取到记录，当A修改记录后，往数据库中提交时，版本号会增加，当B对记录修改后，也向数据库提交时，因为版本号小于等于现有的版本记录号时，则提交不成功，需要重新读取记录并进行修改再往数据库中提交才行。

乐观锁的实例如下：

首先需要构造一个有版本号的类，并在版本号的字段上加入注解，如下：

~~~java
package com.github.geequery.springdata.test.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity()
public class VersionLog extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue
    private int id;

    private String name;
    
    @GeneratedValue(generator = "modified-sys")
    private Date modified;
    
    @Version
    private int version;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}


	public enum Field implements jef.database.Field {
        id, name, modified, version
    }
}
~~~

~~~java
  /**
     * 锁案例1-1： 乐观锁
     */
    @Test(expected = OptimisticLockException.class)
    public void testVersionUpdateAndOptLock() {
        int id;
        {
            VersionLog v = new VersionLog();
            v.setName("叶问");
            commonDao.insert(v);
            id = v.getId();
        }
        {
            VersionLog v = commonDao.load(VersionLog.class, id);
            v.setName("叶问天");
            commonDao.update(v);
        }
        {
            VersionLog v = commonDao.load(VersionLog.class, id);

            VersionLog v2 = commonDao.load(VersionLog.class, id);
            v2.setName("抢先更新");
            commonDao.update(v2);
            try {
                v.setName("啥，已经被人更新了，那我不是写不进去了！");
                commonDao.update(v);
            } catch (OptimisticLockException e) {
                e.printStackTrace();
                throw e;
            }
        }

    }
~~~

第一部分和第二部分对应生成的SQL语句如下：

~~~sql
insert into VERSIONLOG(ID,NAME,MODIFIED,VERSION) values(DEFAULT,?,?,?) | [derby:db@1]
(1)name:         	[叶问]
(2)modified:     	[2017-09-28 20:08:09.886]
(3)version:      	[1]
Insert:1	Time cost([ParseSQL]:0ms, [DbAccess]:2ms) | [derby:db@1]
select t.* from VERSIONLOG t where ID= ? | [derby:db@1]
(1)id:           	[5]
Result Count:1	 Time cost([ParseSQL]:4ms, [DbAccess]:16ms, [Populate]:1ms) max:2/fetch:0/timeout:60 |[derby:db@1]
update VERSIONLOG set MODIFIED = ?, VERSION = VERSION+1, NAME = ? where ID=? and VERSION=? | [derby:db@1]
(1):             	[2017-09-28 20:08:09.927]
(2)null:         	[叶问天]
(3)idEQUALS:     	[5]
(4)versionEQUALS:	[1]
Updated:1	 Time cost([ParseSQL]:24ms, [DbAccess]:14ms) |[derby:db@1]
~~~

第三部分代码块中对应生成的SQL和异常如下：

~~~sql
select t.* from VERSIONLOG t where ID= ? | [derby:db@1]
(1)id:           	[5]
Result Count:1	 Time cost([ParseSQL]:0ms, [DbAccess]:1ms, [Populate]:0ms) max:2/fetch:0/timeout:60 |[derby:db@1]
select t.* from VERSIONLOG t where ID= ? | [derby:db@1]
(1)id:           	[5]
Result Count:1	 Time cost([ParseSQL]:0ms, [DbAccess]:0ms, [Populate]:0ms) max:2/fetch:0/timeout:60 |[derby:db@1]
update VERSIONLOG set MODIFIED = ?, VERSION = VERSION+1, NAME = ? where ID=? and VERSION=? | [derby:db@1]
(1):             	[2017-09-28 20:08:09.951]
(2)null:         	[抢先更新]
(3)idEQUALS:     	[5]
(4)versionEQUALS:	[2]
Updated:1	 Time cost([ParseSQL]:1ms, [DbAccess]:0ms) |[derby:db@1]
javax.persistence.OptimisticLockException: The row in database has been modified by others after the entity was loaded.
//异常堆栈信息....
	
update VERSIONLOG set MODIFIED = ?, VERSION = VERSION+1, NAME = ? where ID=? and VERSION=? | [derby:db@1]
(1):             	[2017-09-28 20:08:09.951]
(2)null:         	[啥，已经被人更新了，那我不是写不进去了！]
(3)idEQUALS:     	[5]
(4)versionEQUALS:	[2]
Updated:0	 Time cost([ParseSQL]:0ms, [DbAccess]:24ms) |[derby:db@1]
~~~

这里更新则更新失败。

实例2：

~~~java
/**
     * 锁案例1-2：批量更新模式下的乐观锁
     */
    @Test
    public void testVersionAndOptLockInBatch() {
        int id;
        {
            VersionLog v = new VersionLog();
            v.setName("我的");
            commonDao.insert(v);
            id = v.getId();
        }
        {
            VersionLog v1 = commonDao.load(VersionLog.class, id);
            VersionLog v2 = commonDao.load(VersionLog.class, id - 3);
            VersionLog v3 = commonDao.load(VersionLog.class, id - 2);
            VersionLog v4 = commonDao.load(VersionLog.class, id - 1);

            VersionLog v_ = commonDao.load(VersionLog.class, id - 2);
            v_.setName("再次抢先更新");
            commonDao.update(v_);

            v1.setName("更新1");
            v2.setName("更新2");
            v3.setName("又被抢了，还能好好做朋友吗？");
            v4.setName("更新4");
            int count = commonDao.batchUpdate(Arrays.asList(v1, v2, v3, v4));
            Assert.assertEquals(3, count); // 该条记录没有更新

            v3 = commonDao.load(v3);
            Assert.assertEquals("再次抢先更新", v3.getName()); // 该条记录没有更新

        }
    }
~~~

对应生成的结果如下：

~~~sql
insert into VERSIONLOG(ID,NAME,MODIFIED,VERSION) values(DEFAULT,?,?,?) | [derby:db@1]
(1)name:         	[我的]
(2)modified:     	[2017-09-28 20:18:13.645]
(3)version:      	[1]
Insert:1	Time cost([ParseSQL]:1ms, [DbAccess]:9ms) | [derby:db@1]
select t.* from VERSIONLOG t where ID= ? | [derby:db@1]
(1)id:           	[5]
Result Count:1	 Time cost([ParseSQL]:4ms, [DbAccess]:17ms, [Populate]:2ms) max:2/fetch:0/timeout:60 |[derby:db@1]
select t.* from VERSIONLOG t where ID= ? | [derby:db@1]
(1)id:           	[2]
Result Count:1	 Time cost([ParseSQL]:0ms, [DbAccess]:1ms, [Populate]:0ms) max:2/fetch:0/timeout:60 |[derby:db@1]
select t.* from VERSIONLOG t where ID= ? | [derby:db@1]
(1)id:           	[3]
Result Count:1	 Time cost([ParseSQL]:0ms, [DbAccess]:0ms, [Populate]:0ms) max:2/fetch:0/timeout:60 |[derby:db@1]
select t.* from VERSIONLOG t where ID= ? | [derby:db@1]
(1)id:           	[4]
Result Count:1	 Time cost([ParseSQL]:0ms, [DbAccess]:1ms, [Populate]:0ms) max:2/fetch:0/timeout:60 |[derby:db@1]
select t.* from VERSIONLOG t where ID= ? | [derby:db@1]
(1)id:           	[3]
Result Count:1	 Time cost([ParseSQL]:0ms, [DbAccess]:0ms, [Populate]:0ms) max:2/fetch:0/timeout:60 |[derby:db@1]
update VERSIONLOG set MODIFIED = ?, VERSION = VERSION+1, NAME = ? where ID=? and VERSION=? | [derby:db@1]
(1):             	[2017-09-28 20:18:13.685]
(2)null:         	[再次抢先更新]
(3)idEQUALS:     	[3]
(4)versionEQUALS:	[1]
Updated:1	 Time cost([ParseSQL]:5ms, [DbAccess]:14ms) |[derby:db@1]
update VERSIONLOG set MODIFIED = ?, VERSION = VERSION+1, NAME = ? where ID=? and VERSION=? | [derby:db@1]
Batch Parameters: 1/4
(1):             	[2017-09-28 20:18:13.699]
(2)null:         	[更新1]
(3)id =:         	[5]
(4)version =:    	[1]
Batch Parameters: 2/4
(1):             	[2017-09-28 20:18:13.699]
(2)null:         	[更新2]
(3)id =:         	[2]
(4)version =:    	[1]
Batch Parameters: 3/4
(1):             	[2017-09-28 20:18:13.699]
(2)null:         	[又被抢了，还能好好做朋友吗？]
(3)id =:         	[3]
(4)version =:    	[1]
Batch Parameters: 4/4
(1):             	[2017-09-28 20:18:13.699]
(2)null:         	[更新4]
(3)id =:         	[4]
(4)version =:    	[1]
Update Batch executed total:4. affect 3 record(s)	 Time cost([ParseSQL]:458us, [DbAccess]:4ms) |[derby:db@1]
select t.* from VERSIONLOG t where t.ID=? | [derby:db@1]
(1)idEQUALS:     	[3]
Result Count:1	 Time cost([ParseSQL]:0ms, [DbAccess]:3ms, [Populate]:1ms) max:2/fetch:0/timeout:60 |[derby:db@1]
~~~

这里采用的是批量模式的乐观锁，注意：在Batch模式下，乐观锁可以阻止覆盖他人的记录（无法写入），但是无法检测出是哪一组记录因为冲突造成无法写入。因此只能确认不覆盖其他人的记录。此外，非按主键更新的场合下，乐观锁也不能生效。因为非按主键更新时的更新请求并不能对应要数据库的的特定记录，无法检查记录是否非修改。无法要求乐观锁进行干预。

> 大家可以思考一下，如果没有版本(VERSION) 字段，就不能实现乐观锁了吗？

乐观锁的相关结论：

1. 由ORM框架可提供乐观锁功能，需要@Version注解。
2. 接受到异常后，是重新读取记录并再次操作，还是失败返回取决于业务。
3. 有严格的限制：仅针对按主键的Update操作有效。

### 16.3.3.  不加锁的安全更新

从上面可以看出，乐观锁的出现，主要是防止Read-Modify-Save这样的非原子操作，所以，只要让我们的操作是原子操作就好了！

如：存钱时：

~~~sql
update Account set value = value+100 where user = ‘张三’
~~~

扣费时：

~~~sql
update Account set value = value-20 where user = ‘张三’
~~~

具体不加锁的安全更新实例如下：

~~~java
    /**
     * 锁案例3：特定场景下不需要加锁更新
     */
    @Test
    public void testUpdateWithoutLock() {
        List<Foo> foos = commonDao.find(QB.create(Foo.class));
        Foo foo = foos.get(0);
        QB.fieldAdd(foo, Foo.Field.age, 100);
        foo.setName("姓名也更新了");
        commonDao.update(foo);
    }
~~~

对应生成的SQL语句如下：

~~~sql
select t.* from FOO t | [derby:db@1]
Result Count:15	 Time cost([ParseSQL]:6ms, [DbAccess]:11ms, [Populate]:2ms) max:0/fetch:0/timeout:60 |[derby:db@1]
update FOO set LASTMODIFIED = current_timestamp, AGE = AGE + ?, NAME_A = ? where ID=? | [derby:db@1]
(1):             	[100]
(2)null:         	[姓名也更新了]
(3)idEQUALS:     	[1]
Updated:1	 Time cost([ParseSQL]:43ms, [DbAccess]:16ms) |[derby:db@1]
~~~

不加锁的安全更新相关结论：

1. 高度并发。
2. 免去了乐观锁的反复重试等复杂逻辑。
3. 充分利用数据库自身的锁和性能优化机制。
4. 有条件限制，更新数据必须来自于SQL表达式

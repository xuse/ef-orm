GeeQuery使用手册——Chapter-13  DB元数据与DDL操作

[TOC]

# Chapter-13  DB元数据与DDL操作

DB元数据，即数据库中的schema、table、view、function、procudure等数据库内建的各种数据结构和代码。

在EF-ORM中，提供了名为DbMetadata的API类来操作数据库的结构信息，初步了解可参见下文的例子。详情请参阅API文档。

使用API来操作数据库结构的主要优点是，一般无需考虑不同的RDBMS的兼容性，因此可以使语句在各种数据库上通用。（但依然要受数据库本身支持的数据类型等限制）

JDBC中，提供了名为DatabaseMetadata的元数据访问接口类。通过这个接口类可以获得大部分的数据库元数据信息。元数据访问接口类中的很多方法，都是在JDBC3或JDBC4中添加的，要想访问这些方法，请尽量使用满足JDBC4接口的驱动。

## 13.1.  访问数据库结构

### 13.1.1.  获取RDBMS版本号等基本信息

下面的例子演示了如何获取数据库的名称、版本号、JDBC驱动名和版本号；还有数据库支持的函数、数据类型等信息。

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

~~~java
	@Test
	public void testDbInfo() throws SQLException {
		// 当有多个数据源时，需要指定数据源的名称，如果只有一个数据源，那么传入null就行了。
		DbMetaData meta = db.getMetaData(null);
		Map<String, String> version = meta.getDbVersion();
		for (String key : version.keySet()) {
			System.out.println(key + ":" + version.get(key));
		}
		System.out.println("schema:" + meta.getCurrentSchema());
		System.out.println("=== Functions ===");
		System.out.println(meta.getAllBuildInFunctions());
		System.out.println("=== DATA TYPES ===");
		System.out.println(meta.getSupportDataType());
	}
~~~

前面已经讲过EF-ORM中，一个session对象中可以封装多个数据源，因此在获取DbMetaData时，首先要指定数据源。如果传入数据源名称为null，那么就会获得第一个（默认的）数据源。

### 13.1.2.  获得表和字段结构信息

下面的例子演示了如何获得数据库中有哪些表和视图，然后可以打印出一张表的各个列和每个列的类型信息。还有相关的索引、主键等信息。

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

~~~java
@Test
public void testTableInfo() throws SQLException {
	DbMetaData meta = db.getMetaData(null);
	//查看数据库中的所有表
	List<TableInfo> tables=meta.getTables();
	System.out.println("=== There are "+ tables.size()+" tables in database. ===");
	for(TableInfo info:tables){
		System.out.println(info);
	}
		
	//查看数据库中的所有视图
	List<TableInfo> views=meta.getViews();
	System.out.println("=== There are "+ views.size()+" views in database. ===");
	for(TableInfo info:views){
		System.out.println(info);
	}
		
	//查看一张表的信息
	if(tables.isEmpty())return;
	String tableName=tables.get(0).getName();
	List<Column> cs = meta.getColumns(tableName,true);
	System.out.println("==== Table " + tableName + " has " + cs.size() + " columns. ====");
	for (Column c : cs) {
		System.out.println(c.getColumnName()+"\t"+c.getDataType() + "\t" + c.getColumnSize() + "\t" + c.getRemarks());
	}
 	//表的主键
	System.out.println("======= Table " + tableName + " Primary key ========");
	System.out.println(meta.getPrimaryKey(tableName));
	//一张表的索引
	Collection<Index> is = meta.getIndexes(tableName);
	System.out.println("==== Table " + tableName + " has " + is.size() + " indexes. ===");
	for (Index i : is) {
		System.out.println(i);
	}
}
~~~

运行上述案例，可以查看到数据库中第一张表的列、数据类型、索引、主键等详细信息。

### 13.1.3.  删表和建表

前面的代码中，您可以已经看到了关于删除表和创建表的例子。在DbMetaData上也有相应的删表和建表方法。

删除表时会主动删除相关的所有主外键、索引、约束。

建表时会同时创建表上定义的索引(使用@Index 或者 @Indexed定义)，但不会创建外键。

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

~~~java
@Test
public void testTableDropCreate() throws SQLException{
	DbMetaData meta = db.getMetaData(null);
	//删除Student表
	meta.dropTable(Student.class);
	//创建Student表
	meta.createTable(Student.class);
}
~~~

注意——

​	在删表时，如果表存在并成功删除则返回true；如果表不存在不会抛出异常，而是返回false。

​	在建表时，如果表不存在并成功创建返回true；如果表已存在不会抛出异常，而是返回false。

### 13.1.4.  操作索引

下面的例子演示了查询索引、创建索引、删除索引。

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

~~~java
@Test
public void testIndexes() throws SQLException{
	DbMetaData meta = db.getMetaData(null);
	System.out.println("=== Indexes on table Student ===");
	for(Index index: meta.getIndexes(Student.class)){
		System.out.println(index);
	}
		
	System.out.println("=== Now we try to create two indexes ===");
	//创建单字段的unique索引
	Index index1=meta.toIndexDescrption(Student.class, "name");
	index1.setUnique(true);
	meta.createIndex(index1);
	//创建复合索引，其中Grade列倒序
	Index index2=meta.toIndexDescrption(Student.class, "grade desc", "gender");
	meta.createIndex(index2);
	//打印出表上的所有索引
	System.out.println("=== Indexes on table Student (After create)===");
	for(Index index: meta.getIndexes(Student.class)){
		System.out.println(index);
	}
	
	System.out.println("=== Now we try to drop all indexes ===");
	//删除所有索引
	for(Index index: meta.getIndexes(Student.class)){
		if(index.getIndexName().startsWith("IDX")){
			meta.dropIndex(index);
		}
	}
	//打印出表上的所有索引
	System.out.println("=== Indexes on table Student (After drop)===");
	for(Index index: meta.getIndexes(Student.class)){
		System.out.println(index);
	}
}
~~~

### 13.1.5.   操作外键

我们可以访问数据库中表和表之间的外键，并且删除它们或者创建它们。下面的例子演示了创建外键、查询外键，删除外键等操作。

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

~~~java
@Test
public void testForeignKeys() throws SQLException{
	String tableName="STUDENT_TO_LESSON";
	DbMetaData meta = db.getMetaData(null);
		
	//在表上面创建两个外键，分别引用Student表和LessonInfo表。
	meta.createForeignKey(StudentToLesson.Field.studentId, Student.Field.id);
	meta.createForeignKey(StudentToLesson.Field.lessionId, LessonInfo.Field.id);
		
	//打印目前表上的外键(应该看到两个外键)
	System.out.println("=== Foreign keys on table ["+tableName+"] ===");
	LogUtil.show(meta.getForeignKey(tableName));
		
	//打印引用到表Student的外键（应该看到一个外键）
	System.out.println("=== Foreign keys on table [STUDENT] ===");
	LogUtil.show(meta.getForeignKeyReferenceTo("STUDENT"));
		
	//打印引用到表LessonInfo的外键（应该看到一个外键）
	System.out.println("=== Foreign keys on table [LESSON_INFO] ===");
	LogUtil.show(meta.getForeignKeyReferenceTo("LESSON_INFO"));
		
	//删除表上的所有外键
	System.out.println("=== Drop Foreign keys ===");
	meta.dropAllForeignKey(tableName);
		
	//打印目前表上的外键(应该没有外键)
	System.out.println("=== Foreign keys on table "+tableName+" ===");
	LogUtil.show(meta.getForeignKey(tableName));
}
~~~

### 13.1.6.  访问自定义函数和存储过程

我们可以访问数据库中的自定义函数和存储过程。不过DbMetaData没有提供方法去修改这些对象。

下面的例子演示了如何查询数据库中的自定义函数和存储过程。（只能看到基本信息，无法看到具体的SQL代码）。

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

~~~java
@Test
public void testFunctions() throws SQLException{
	DbMetaData meta = db.getMetaData(null);
	System.out.println("=== User Defined Functions ===");
	//打印出所有函数
	List<Function> functions=meta.getFunctions(null);
	for(Function f: functions){
		System.out.println(f);
	}
	//打印出所有存储过程
	System.out.println("=== User Defined Procedures ===");
	functions=meta.getProcedures(null);
	for(Function f: functions){
		System.out.println(f);
	}		
}
~~~

要注意的是，查询存储过程和自定义函数是较高版本的JDBC驱动才能提供的方法。在一些不够主流的数据库上，getFunctions操作可能不支持。如果您使用的是主流数据库，请注意使用较高版本的JDBC驱动。

### 13.1.7.  修改已存在的表(Alter table)

修改表------ALTER TABLE语句，可能是不同的RDBMS之间差别最大的SQL语句了。对于修改表，框架提供的接口也会稍微复杂一些。

修改表的功能被统一为refreshTable()方法，这个方法的作用的将数据库中的表结构和传入的表模型进行对比，并让数据库中的表和模型保持一致。

src\main\java\org\easyframe\tutorial\lessonc\CaseAlterTable.java

~~~java
/**
* refreshTable的效果是检查并修改数据库中的表，使其和传入的实体模型保持一致。
* 本例中，数据库中没有此表，因此变为建表操作。
*/
@Test
public void testCreateTableSimple() throws SQLException{
	db.dropTable(Student.class);
	db.refreshTable(Student.class);
}
~~~

那么，如果Student表已经存在的情况下，就需要使用ALTER TABLE的方式来维护数据库结构了。再看下面这个例子。

src\main\java\org\easyframe\tutorial\lessonc\CaseAlterTable.java

~~~java
/**
* 先用SQL语句直接建立一张类似的表。
* 然后通过refresh方法，修改已存在的表。
*/
@Test
public void testAlterTableSimple() throws SQLException{
	//准备一张结构不同的表
	db.dropTable(Student.class);
	String sql="create table STUDENT(\n"+
			   "ID int generated by default as identity  not null,\n"+
			   "GENDER varchar(6),\n"+
			   "NAME varchar(255),\n"+
			   "DATE_OF_BIRTH timestamp,\n"+
			   "constraint PK_STUDENT primary key(ID)\n"+
		")";
	db.executeSql(sql);

	//开始刷新表
	System.out.println("=== Begin refresh table ===");
	db.refreshTable(Student.class);
}
~~~

运行上述代码，就可以看到，框架会使用ALTER TABLE语句来修改已经存在的表，使其和模型保持一致。

### 13.1.8.  监听和控制表的修改过程

在修改表的过程中，我们可以传入一个事件监听器。事件监听器并不仅仅用于了解刷新表的过程，还可以直接控制这一过程。我们可以先尝试运行位于orm-tutorial中的例子，来观察这一事件。

src\main\java\org\easyframe\tutorial\lessonc\CaseAlterTable.java

~~~java
/**
* 传入一个事件监听器，从而可以监测刷新操作的步骤
* @see jef.database.support.MetadataEventListener
*/
@Test
public void testAlterTableProgress() throws SQLException{
	//准备一张结构不同的表
	db.dropTable(Student.class);
	String sql="create table STUDENT(\n"+
			   "ID int generated by default as identity  not null,\n"+
			   "GENDER varchar(6),\n"+
			   "NAME varchar(128),\n"+
			   "REV_NAME varchar(255),\n"+
			   "DATE_OF_BIRTH timestamp,\n"+
			   "constraint PK_STUDENT primary key(ID)\n"+
		")";
	db.executeSql(sql);
		
	System.out.println("=== Begin refresh table ===");
	db.refreshTable(Student.class,new ProgressSample());
}
~~~

运行上述案例，使用者可以看到有一个桌面进度条弹出，进度条中显示了当前的工作进度，包括正在执行的比较工作或者数据库SQL语句。

事实上

[此处应该插入一张流程图]()

|                       |                 |      |
| --------------------- | --------------- | ---- |
| 变更表之前                 | （可以控制是否继续刷新操作）  |      |
| 如果表不存在，               | 创建表之前（可以控制是否建表） |      |
| 如果表存在，在比较表之前          | （可以控制是否继续刷新操作）  |      |
| 数据库中的表和元数据比较完成后       |                 |      |
| 比较完成并生成SQL后，每句SQL执行之前 |                 |      |
| 在每句SQL执行成功后           |                 |      |
| 在任何一句SQL执行失败后         |                 |      |
| 在所有SQL都执行完成后          |                 |      |

按事件的时间顺序如下 

[缺失]()

### 13.1.9.  使用动态表模型来维护数据库结构

在EF-ORM中支持了动态表模型，因此可以用编程接口来构造一个表模型，并且用这个表模型来维护数据库。

src\main\java\org\easyframe\tutorial\lessonc\CaseAlterTable.java

~~~java
@Test
public void testAlterTupleTable() throws SQLException{
	TupleMetadata model=new TupleMetadata("MY_TABLE");
	model.addColumn("ID", new ColumnType.AutoIncrement(8));
	model.addColumn("NAME", new ColumnType.Varchar(64));
	model.addColumn("DATA", new ColumnType.Varchar(128));
	model.addColumn("DOB", new ColumnType.TimeStamp().notNull()
		.defaultIs(Func.current_timestamp));
	model.addColumn("MODIFIED", new ColumnType.TimeStamp().notNull());
	model.addColumn("CONTANT", new ColumnType.Clob());
	
	//第一次刷新，由于此时表还不存在，因此会创建 MY_TABLE
	db.refreshTable(model, null);
	
	//修改模型字段
	model.removeColumn("DATA");
	model.updateColumn("NAME", new ColumnType.Varchar(128).notNull());
	model.updateColumn("MODIFIED", new ColumnType.TimeStamp());
	model.addColumn("DATA1", new ColumnType.Varchar(64));
	model.addColumn("AGE", new ColumnType.Int(12));
	//第二次刷新，修改MY_TABLE的字段
	db.refreshTable(model, null);
	System.out.println("=== begin drop ===");
	db.dropTable(model);
}
~~~

## 13.2.  其他功能

### 13.2.1.  执行sql脚本文件

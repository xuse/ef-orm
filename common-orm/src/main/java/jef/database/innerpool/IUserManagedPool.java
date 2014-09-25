package jef.database.innerpool;

import java.sql.SQLException;
import java.util.Collection;

import jef.common.Callback;

/**
 * <strong>什么是IUserManagedPool</strong>
 * <p>
 * <pre>
 * 支持可重入特性的连接池。在线程或者事务反复拿连接时范围同一个连接以节约使用，因此设计了这个接口，用于返回重复的连接。

 * 目前DbClient中使用的还是IUserManagedPool。
 *  该连接池没有按照J2EE规范使用DataSource PooledConnection等实现，但JEF-ORM可以选择不启用连接池。此时可以通过外部配置的方式用DataSource来实现一个第三方的连接池。详见下面的列表。
 * </pre>
 * 
 * 以下情况下，您需要禁用嵌入式连接池，启用第三方连接池
 * <li>1、希望使用使用了c3p0等外部连接池的</li>
 * <li>2、使用了Oracle驱动内建的连接池，来实现类似于RAC FCF等自动切换功能的。</li>
 * <li>3、使用了XA来实现分布式事务的，由于XA连接本身就是连接池，也需要禁用内嵌池</li>
 * <br>
 * 
 * 目前设计的实现
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER><em>Name</em></td>
 *    <td ALIGN=CENTER><em>Implementation</em></td>
 *    <td ALIGN=CENTER><em>Using</em></td>
 *  </tr>
 *  <tr>
 *    <td><b>A1</b></td>
 *    <td>内嵌单连接池</td>
 *    <td>{@link SingleManagedConnectionPool}</td>
 *    <td>没有外部连接池时，并且只有一个数据源</td>
 *  </tr>
 *  <tr>
 *    <td><b>A2</b></td>
 *    <td>外部单连接池</td>
 *    <td>{@link SingleDummyConnectionPool}</td>
 *    <td>使用外部连接池时，并且只有一个数据源</td>
 *  </tr>
 *  <tr>
 *    <td><b>B1</b></td>
 *    <td>内嵌多连接池</td>
 *    <td>{@link RoutingManagedConnectionPool}</td>
 *    <td>使用 {@link jef.database.datasource.RoutingDataSource} 来路由到多个数据源，并且未启用XA的场景</td>
 *  </tr>
 *  <tr>
 *    <td><b>B2</b></td>
 * 	  <td>外部多连接池</td>
 *    <td>{@link RoutingDummyConnectionPool}</td>
 *    <td>使用 {@link jef.database.datasource.RoutingDataSource} 来路由到多个数据源，并且这些数据源本身已经配置成连接池</td>
 *  </tr>
 *  <tr>
 *    <td><b>C</b></td>
 *    <td>外部XA连接池</td>
 *    <td>{@link RoutingDummyConnectionPool}</td>
 *    <td>当使用外部的XA时。此时每个XA数据源本身包含了连接池，因此JEF内嵌连接池不允许使用</td>
 *  </tr>
 * </table>
 * <ul>
 *  *  在多数据库支持的场合，IConnection已经包含了多数据库的支持。因此JEF上层只要关心IConnection的状态就可以操作多个数据库。
 *  <p>
 *  针对内嵌连接池和第三方连接池，IConnection和其对应的ConnectionPool对象已经处理了这一关系。因此JEF上层也不用关心这个连接到底是新建的，还是从某个池里拿出来的。
 * 
 * 
 * IConnection针对以下5种场景，可能有不同的实现。
 * 1 .单数据库场景
 *   具备重连特性的数据库连接
 * 场景A1:使用内嵌连接池的JEF-ORM,具有连接管理，自动重连，线程复用等调度机制
 * 
 * 场景A2：使用外部连接池。相应的重连等特性不再支持，一切连接池的维护管理功能由datasource的下层来管理
 * 
 * 2 .在使用了Routing Datasource的场合下，用来封装事务用到的多个连接，使得它们能一起提交一起回滚（所谓多重JPA）
 * 
 * 场景B1:使用内嵌连接池，配置了RoutingDatasource，一个DbClient。在事务中，每次操作需要getConnection，判断本次操作的连接关键字。
 * 该操作数位于datasource之上的，如果沿用JEF-ORM内嵌连接池的API，也就意味着要为这种datasource设计专门的连接池。
 * 这个连接池内部分为许多的小池子，同时每个事务要在多个小池子中占用一个连接。而这种连接池所返回的IConnection是一个复杂实现，类似与连接缓存，会记录当前事务所用到的多个连接，
 * 如果事务要访问一个用过的数据库，那么返回缓存的连接，否则要将事务操作状态（dbKey）设计进API，来保证routingDatasource能路由出正确的次Connection对象。
 * 
 * 延续B2的设计，IConnection内部包含若干小池。（其实我们可以将B2的场景中的也用dummy池实现，即将带池的DataSource统一用实现内部简单池接口）
 * 
 * 场景B2:使用外部连接池
 * 这种设计比上一种要简单。我们将多个数据源单独包装为连接池的datasourece,然后聚合为一个routingDatasource.内部处理的时候，直接将多个datasource记录到IConnection对象上。
 * 事务对象得到IConnection，IConnection在被事务取用的时候，实际上并未为其分配任何一个连接。只有在事务用到一个连接的时候，IConnection得到这个连接，并缓存在其中。
 * 当事务提交时，提交全部使用过的连接。(注意要只读事务优化是在Transaction上的，即Transaction不会调用只读事务的commit).
 * 
 * 这个IConnection也必须要实现某个RoutingAPI，从而判断出当前应该提供的连接。(其实就是在每次create Statement时进行计算。有状态的)
 * 
 * 3. 在底层使用了其他第三方连接池的时候，（包括XA）的时候，基于XA来实现JTA事务
 * 场景C: 使用XA连接池，禁用内部连接池
 * 
 * 
 * 这种场景下，事务操作全部禁用。由JTA控制器去操作。但是连接路由管理功能还是一样不变的。
 * 因此我们参考实现是B2实现。除了事务不提交不回滚之外，其他和B2一样。（事务的提交回滚一切由Spring和JTA控制器完成.）
 * @author jiyi
 */
public interface IUserManagedPool<T extends IConnection> extends IPool<T>,MetadataService{
	/**
	 * 获取一个连接，如果连接池已经为指定的事务分配过连接，那么还是返回这个连接。
	 * 注意：这个版本开始，连接池不再负责设置AutoCommit的状态
	 * 
	 * @param transaction 事务对象
	 * @return Connection
	 * @throws SQLException
	 */
	IConnection getConnection(Object transaction) throws SQLException;
	
	/**
	 * 返回全部的datasource名称。
	 * 對於只支持但数据源的连接池实现，返回空集合即可
	 * @return
	 */
	Collection<String> getAllDatasourceNames();
	
	/**
	 * 是否为路由池
	 * @return 如果是路由的连接池返回true，反之
	 */
	boolean isRouting();
	
	/**
	 * 是否为假连接池
	 * @return 如果为非缓存的连接池实现，返回true,反之
	 */
	boolean isDummy();
	
	/**
	 * 可以注册一个回调函数，当数据库在首次初始化的时候执行
	 * @param callback 回调函数
	 */
	void registeDbInitCallback(Callback<String, SQLException> callback);
}

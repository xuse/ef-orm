package jef.orm.onetable;

import java.sql.SQLException;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;

import jef.database.DbClient;
import jef.database.DebugUtil;
import jef.database.NativeQuery;
import jef.database.QB;
import jef.database.Transaction;
import jef.database.cache.Cache;
import jef.database.jmx.JefFacade;
import jef.database.query.Query;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.onetable.model.CaAsset;
import jef.tools.string.RandomData;

/**
 * 测试一级缓存的命中和清洗等场景
 * 
 * @author jiyi
 *
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
		@DataSource(name = "oracle", url = "${oracle.url}", user = "${oracle.user}", password = "${oracle.password}"),
		@DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
		@DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"),
		@DataSource(name = "derby", url = "jdbc:derby:./db;create=true"),
		@DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
		@DataSource(name = "sqlite", url = "jdbc:sqlite:test.db?date_string_format=yyyy-MM-dd HH:mm:ss"),
		@DataSource(name = "sqlserver", url = "${sqlserver.url}", user = "${sqlserver.user}", password = "${sqlserver.password}")
		})
public class CacheTest extends org.junit.Assert {
	private DbClient db;

	@DatabaseInit
	public void prepare() throws SQLException {
		db.createTable(CaAsset.class);
		JefFacade.getOrmConfig().setCacheDebug(true);
		JefFacade.getOrmConfig().setCacheLevel1(true);
	}

	//@IgnoreOn(allButExcept = "sqlserver")
	@Test
	public void case1() throws SQLException {
		Transaction session = db.startTransaction();
		CaAsset ca = RandomData
				.newInstance(jef.orm.onetable.model.CaAsset.class);
		session.insert(ca);
		Cache cache = DebugUtil.getCache(session);

		{
			long hit = cache.getHitCount();
			CaAsset obj = session.load(ca);//2016-4-5，更改案例，目前插入时不存储缓存。
			assertEquals(hit, cache.getHitCount());
		}
		{
			long hit = cache.getHitCount();
			CaAsset obj = session.load(ca);// 第二次查询，命中
			assertEquals(hit + 1, cache.getHitCount());
		}
		CaAsset ca2 = RandomData
				.newInstance(jef.orm.onetable.model.CaAsset.class);
		{
			session.insert(ca2);
			
			CaAsset obj = session.load(ca2);//加载一次，使其缓存

			//然后更新，使其失效
			ca2.startUpdate();
			ca2.setNormal("XAA");
			ca2.setThedate(new Date());
			session.update(ca2);

			long miss = cache.getMissCount();
			session.load(ca2);// 不命中
			assertEquals(miss + 1, cache.getMissCount());
		}
		{
			CaAsset q = new CaAsset();
			q.setAcctId(ca.getAcctId());
			long miss = cache.getMissCount();
			CaAsset obj = session.load(q);// 维度变化，不会命中
			assertEquals(miss + 1, cache.getMissCount());// 验证失效
		}
		{
			CaAsset q = new CaAsset();
			q.setAssetId(ca.getAssetId());
			long hit = cache.getHitCount();
			CaAsset obj = session.load(q);
			assertEquals(hit + 1, cache.getHitCount());// 验证命中
		}

		{
			NativeQuery<?> q = session
					.createNativeQuery("update CA_ASSET set NORMAL = :normal, VALID_DATE = :vdate where ASSET_ID=:assetId");
			q.setParameter("normal", "asasa");
			q.setParameter("vdate", new Date());
			q.setParameter("assetId", ca.getAcctId());
			int count = q.executeUpdate();
			assertEquals(0, count);// 缓存无变化
			long hit = cache.getHitCount();
			session.load(ca);
			assertEquals(hit + 1, cache.getHitCount());// 验证命中,上个Update语句没有更新任何值，引起不会引起缓存变化
		}
		{
			NativeQuery<?> q = session
					.createNativeQuery("update CA_ASSET set NORMAL = :normal, VALID_DATE = :vdate where ASSET_ID=:assetId");
			q.setParameter("normal", "asasa");
			q.setParameter("vdate", new Date());
			q.setParameter("assetId", ca.getAssetId());
			int count = q.executeUpdate();
			assertEquals(1, count);// 更新了一条记录
			long hit = cache.getHitCount();
			session.load(ca2);
			assertEquals(hit + 1, cache.getHitCount());// 验证命中，update语句更新了一条记录，但不是这条记录，因此此缓存无变化
			long miss = cache.getMissCount();
			session.load(ca);
			assertEquals(miss + 1, cache.getMissCount());// 验证不命中，update语句更新了这条记录，因此缓存被清洗
		}

		{
			Query<CaAsset> q = QB.create(CaAsset.class);
			q.getInstance().prepareUpdate(CaAsset.Field.assetType, 50);
			session.update(q.getInstance());
			// 该表上所有缓存失效
			long miss = cache.getMissCount();
			session.load(ca);
			assertEquals(miss + 1, cache.getMissCount());// 验证失效
			miss = cache.getMissCount();
			session.load(ca2);
			assertEquals(miss + 1, cache.getMissCount());// 验证失效
		}
		session.commit(true);
	}

}

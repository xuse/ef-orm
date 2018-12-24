package org.easyframe.enterprise.spring;

import java.sql.SQLException;
import java.util.UUID;

import javax.persistence.PersistenceException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import jef.database.DbClient;
import jef.database.IQueryableEntity;
import jef.database.Sequence;
import jef.database.SessionFactory;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.tools.Assert;


public class UniqueIdManagerImpl implements UniqueIdManager,InitializingBean{
	private SessionFactory entityManagerFactory;
	
	/**
	 * {@inheritDoc}<br>
	 * 若序列不存在会自动创建，且序列起始值=配置项AUTO_SEQUENCE_START的值，<br>
	 * 若未配置则序列起始值=表中当前最大序列值+1.
	 */
	public long nextLong(ITableMetadata meta) {
		try{
			return getNextLong(meta);
		}catch(SQLException e){
			throw new PersistenceException(e.getMessage()+" "+e.getSQLState(),e);
		}
	}

	/**
	 * {@inheritDoc}<br>
	 * 若序列不存在会自动创建，且序列起始值=配置项AUTO_SEQUENCE_START的值，<br>
	 * 若未配置则序列起始值=表中当前最大序列值+1.
	 */
	public long nextLong(Class<? extends IQueryableEntity> clz) {
		ITableMetadata meta=MetaHolder.getMeta(clz);
		try{
			return getNextLong(meta);
		}catch(SQLException e){
			throw new PersistenceException(e.getMessage()+" "+e.getSQLState(),e);
		}
	}
	
	public long nextLong() {
		try {
			Sequence holder=getDbClient().getSqlTemplate(null).getSequence("DEFAULT_GLOBAL_SEQ",12);
			return holder.next();
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage()+" "+e.getSQLState(),e);
		}
	}

	public String nextGUID() {
		return UUID.randomUUID().toString();
	}

	/*
	 *
	 */
	public long nextLong(String dbKey,String seqName) {
		try{
			Sequence holder=getDbClient().getSqlTemplate(dbKey).getSequence(seqName,12);
			return holder.next();	
		}catch(SQLException e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * {@inheritDoc}<br>
	 * 若序列不存在会自动创建，且序列起始值=配置项AUTO_SEQUENCE_START的值，<br>
	 * 若未配置则序列起始值={@link SequenceKeyHolder#DEFAULT_SEQ_START}.
	 */
	public long nextLong(String seqName) {
		return nextLong(null,seqName);
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(entityManagerFactory,"The dbclient was not set!!");
	}
	
	@Autowired
	public void setEntityManagerFactory(SessionFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}
	
	private long getNextLong(ITableMetadata meta) throws SQLException {
		AutoIncrementMapping mapping=meta.getFirstAutoincrementDef();
		DbClient client=getDbClient();
		DatabaseDialect profile=client.getProfile(null);
		Sequence holder = client.getSqlTemplate(mapping.getSequenceDataSource(profile)).getSequence(meta.getFirstAutoincrementDef());
		return holder.next();
	}

	private DbClient getDbClient() {
		DbClient dbClient = entityManagerFactory.asDbClient();
		return dbClient;
	}

}

package jef.database.jdbc;


public final class Jdbcs {
	private Jdbcs() {
	}

//	public final UpdateReturn innerExecuteUpdate(String sql, List<Object> ps, GenerateKeyReturnOper keyOper) throws SQLException {
//		Object[] params = ps.toArray();
//
//		session.getListener().beforeSqlExecute(sql, params);
//		SqlLog sb = ORMConfig.getInstance().newLogger();
//
//		long start = System.currentTimeMillis();
//		PreparedStatement st = null;
//		UpdateReturn result;
//		long dbAccess;
//		int total;
//		sb.append(sql).append(this);
//		try {
//			st = keyOper.prepareStatement(this, sql);
//			st.setQueryTimeout(ORMConfig.getInstance().getUpdateTimeout());
//			if (!ps.isEmpty()) {
//				BindVariableContext context = new BindVariableContext(st, getProfile(), sb);
//				BindVariableTool.setVariables(context, ps);
//			}
//			total = st.executeUpdate();
//			result = new UpdateReturn(total);
//			dbAccess = System.currentTimeMillis();
//			keyOper.getGeneratedKey(result, st);
//			if (total > 0) {
//				session.checkCacheUpdate(sql, ps);
//			}
//		} catch (SQLException e) {
//			DbUtils.processError(e, sql, this);
//			throw e;
//		} finally {
//			sb.output();
//			DbUtils.close(st);
//			releaseConnection();
//		}
//		sb.directLog(StringUtils.concat("Executed:", String.valueOf(total), "\t Time cost([DbAccess]:", String.valueOf(dbAccess - start), "ms) |", getTransactionId()));
//		session.getListener().afterSqlExecuted(sql, total, params);
//		return result;
//	}

}

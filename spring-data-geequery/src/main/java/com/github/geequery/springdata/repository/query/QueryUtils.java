package com.github.geequery.springdata.repository.query;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.util.Collections;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;

import jef.database.ManagedTransactionImpl;
import jef.database.jpa.JefEntityManager;
import jef.database.jpa.JefEntityManagerFactory;

import org.easyframe.enterprise.spring.TransactionMode;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.github.geequery.springdata.annotation.IgnoreIf;

public class QueryUtils {
    static boolean isIgnore(IgnoreIf ignoreIf, Object obj) {
        switch (ignoreIf.value()) {
        case Empty:
            return obj == null || String.valueOf(obj).length() == 0;
        case Negative:
            if (obj instanceof Number) {
                return ((Number) obj).longValue() < 0;
            } else {
                throw new IllegalArgumentException("can not calcuate is 'NEGATIVE' on parameter which is not a number.");
            }
        case Null:
            return obj == null;
        case Zero:
            if (obj instanceof Number) {
                return ((Number) obj).longValue() == 0;
            } else {
                throw new IllegalArgumentException("can not calcuate is 'IS_ZERO' on parameter which is not a number.");
            }
        case ZeroOrNagative:
            if (obj instanceof Number) {
                return ((Number) obj).longValue() <= 0;
            } else {
                throw new IllegalArgumentException("can not calcuate is 'IS_ZERO_OR_NEGATIVE' on parameter which is not a number.");
            }
        default:
            throw new IllegalArgumentException("Unknown ignoreIf type:" + ignoreIf.value());
        }
    }

    public static boolean hasNamedParameter(String query) {
        return StringUtils.hasText(query) && NAMED_PARAMETER.matcher(query).find();
    }

    /**
     * 获得EntityManager
     * 
     * @return
     */
    public static final EntityManager getEntityManager(JefEntityManagerFactory jefEmf) {
        TransactionMode tx = jefEmf.getDefault().getTxType();
        EntityManager em;
        switch (tx) {
        case JPA:
        case JTA:
            em = EntityManagerFactoryUtils.doGetTransactionalEntityManager(jefEmf, null);
            if (em == null) { // 当无事务时。Spring返回null
                em = jefEmf.createEntityManager(null, Collections.EMPTY_MAP);
            }
            break;
        case JDBC:
            ConnectionHolder conn = (ConnectionHolder) TransactionSynchronizationManager.getResource(jefEmf.getDefault().getDataSource());
            if (conn == null) {// 基于数据源的Spring事务
                em = jefEmf.createEntityManager(null, Collections.EMPTY_MAP);
            } else {
                ManagedTransactionImpl session = new ManagedTransactionImpl(jefEmf.getDefault(), conn.getConnection());
                em = new JefEntityManager(jefEmf, null, session);
            }
            break;
        default:
            throw new UnsupportedOperationException(tx.name());
        }
        return em;
    }

    private static final String IDENTIFIER = "[\\p{Lu}\\P{InBASIC_LATIN}\\p{Alnum}._$]+";
    private static final Pattern NAMED_PARAMETER = Pattern.compile(":" + IDENTIFIER + "|\\#" + IDENTIFIER, CASE_INSENSITIVE);
}

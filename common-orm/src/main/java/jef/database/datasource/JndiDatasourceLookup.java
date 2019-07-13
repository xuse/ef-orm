package jef.database.datasource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.sql.DataSource;

import jef.common.log.LogUtil;
import jef.tools.StringUtils;

/**
 * 通过JNDI查找Datasource
 * 
 * 这个类也同时实现了DataSourceLookup和DataSourceInfoLookup接口
 * 
 * @author jiyi
 *
 */
public class JndiDatasourceLookup implements DataSourceLookup{
    private InitialContext ctx;
    private PasswordDecryptor passwordDecryptor;
    private String defaultKey;
    private String namePrifix = "";

    public DataSource getDataSource(String dataSourceName) {
        if (ctx == null) {
            init();
        }
        try {
            DataSource ds = (DataSource) ctx.lookup(namePrifix + dataSourceName);
            return ds;// 包装失败
        } catch (NamingException e) {
            LogUtil.exception("Can not lookup datasource from JNDI:" + dataSourceName, e);
        }
        return null;
    }

    public DataSourceInfo getDataSourceInfo(String dataSourceName) {
        if (ctx == null) {
            init();
        }
        try {
            Object ds = ctx.lookup(namePrifix + dataSourceName);
            if (ds instanceof DataSourceInfo) {
                return decrypt((DataSourceInfo) ds);
            }
            return null;// 包装失败
        } catch (NamingException e) {
            LogUtil.exception("Can not lookup datasource from JNDI:" + dataSourceName, e);
        }
        return null;
    }

    private DataSourceInfo decrypt(DataSourceInfo dsi) {
        if (passwordDecryptor != null) {
            String newpass = passwordDecryptor.decrypt(dsi.getPassword());
            if (!StringUtils.equals(dsi.getPassword(), newpass)) {
                dsi.setPassword(newpass);
            }
        }
        return dsi;
    }

    private synchronized void init() {
        if (ctx != null)
            return;
        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            throw new IllegalStateException("JNDI init error.", e);
        }
    }

    public void setPasswordDecryptor(PasswordDecryptor passwordDecryptor) {
        passwordDecryptor = this.passwordDecryptor;
    }

    public void setDefaultKey(String defaultKey) {
        this.defaultKey = defaultKey;
    }

    public String getDefaultKey() {
        return defaultKey;
    }

    public Collection<String> getAvailableKeys() {
        if (ctx == null) {
            init();
        }
        List<String> all = new ArrayList<String>();
        try {
            NamingEnumeration<NameClassPair> o = ctx.list("");
            for (; o.hasMore();) {
                NameClassPair e = o.next();
                if (isDataSource(e.getClassName())) {
                    all.add(e.getName());
                }
            }
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return all;
    }

    private boolean isDataSource(String className) {
        Class<?> c;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return false;
        }
        if (c == RoutingDataSource.class) {
            return false;
        }
        return DataSource.class.isAssignableFrom(c);
    }

    public String getNamePrifix() {
        return namePrifix;
    }

    public void setNamePrifix(String namePrifix) {
        if (namePrifix != null)
            this.namePrifix = namePrifix;
    }
}

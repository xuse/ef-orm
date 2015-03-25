package jef.database.datasource;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.json.JsonUtil;
import jef.tools.IOUtils;
import jef.tools.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 读取Json格式数据源配置的抽象实现
 * @author jiyi
 *
 */
public abstract class AbstractJsonDataSourceInfoLookup implements DataSourceInfoLookup{
	private String dataSourceKeyFieldName="id";
	private String urlFieldName="url";
	private String userFieldName="username";
	private String passwordFieldName="password";
	private String driverFieldName="driverClassName";
	protected boolean ignoreCase=false;
	private PasswordDecryptor passwordDecryptor=PasswordDecryptor.DUMMY;
	
	protected Map<String,JSONObject> cache;
	protected abstract URL getResource();
	
	public DataSourceInfo getDataSourceInfo(String dataSourceName) {
		if(ignoreCase)dataSourceName=StringUtils.upperCase(dataSourceName);
		JSONObject data=null;
		if(cache!=null){
			data=cache.get(dataSourceName);
		}
		if(data==null){
			cache=getCache();
			data=cache.get(dataSourceName);
		}
		return wrap(data);
	}
	
	/**
	 * 返回目前的解密器
	 * @return
	 */
	public PasswordDecryptor getPasswordDecryptor() {
		return passwordDecryptor;
	}


	public void setPasswordDecryptor(PasswordDecryptor passwordDecryptor) {
		this.passwordDecryptor = passwordDecryptor;
	}

	private DataSourceInfo wrap(JSONObject data) {
		if(data==null)return null;
		String url=data.getString(urlFieldName);
		if(StringUtils.isEmpty(url))return null;
		
		String user=data.getString(userFieldName);
		String password=data.getString(passwordFieldName);
		String driverClass=data.getString(driverFieldName);
	
		DataSourceInfoImpl impl=new DataSourceInfoImpl(url);
		impl.setDriverClass(driverClass);
		impl.setUser(user);
		if(StringUtils.isNotEmpty(password)){
			impl.setPassword(passwordDecryptor.decrypt(password));
		}
		return impl;
	}

	protected Map<String, JSONObject> getCache() {
		Map<String,JSONObject> result=new HashMap<String,JSONObject>();
		String data=null;
		try {
			data = IOUtils.asString(getResource(), null);
		} catch (IOException e1) {
			LogUtil.exception(e1);
		}
		if(data==null)return result;
		JSONArray objs=null;
		if(data.startsWith("{")){
			objs=new JSONArray();
			objs.add(JsonUtil.toJsonObject(data));
		}else if(data.startsWith("[")){
			objs=JsonUtil.toJsonArray(data);
		}
		if(objs==null)
			return result;
		for (Object e : objs) {
			if(e instanceof JSONObject){
				JSONObject jp=(JSONObject)e;
				String s=jp.getString(dataSourceKeyFieldName);
				if(StringUtils.isNotEmpty(s)){
					result.put(ignoreCase?StringUtils.upperCase(s):s, jp);
				}	
			}
		}
		return result;
	}
	
	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}
	
	/**
	 * 数据源配置Json中的唯一标识字段，举例<pre>
	 * [{id:"ds1", url:"jdbc:mysql://localhost:3306/test", user: "root", password:"123456",
	 *    driverClassName:"org.gjt.mm.mysql.Driver"}]
	 * </pre>
	 * 在上面这段json中，dataSourceKeyFieldName就是 'id'
	 * @param dataSourceKeyFieldName default valuie is 'url'
	 */
	public void setDataSourceKeyFieldName(String dataSourceKeyFieldName) {
		this.dataSourceKeyFieldName = dataSourceKeyFieldName;
	}

	/**
	 * 数据源配置Json中的JDBC URL字段，举例<pre>
	 * [{id:"ds1", url:"jdbc:mysql://localhost:3306/test", user: "root", password:"123456",
	 *    driverClassName:"org.gjt.mm.mysql.Driver"}]
	 * </pre>
	 * 在上面这段json中，urlFieldName就是 'url'
	 * @param urlFieldName default valuie is 'url'
	 */
	public void setUrlFieldName(String urlFieldName) {
		this.urlFieldName = urlFieldName;
	}

	/**
	 * 数据源配置Json中的用户名字段，举例<pre>
	 * [{id:"ds1", url:"jdbc:mysql://localhost:3306/test", user: "root", password:"123456",
	 *    driverClassName:"org.gjt.mm.mysql.Driver"}]
	 * </pre>
	 * 在上面这段json中，userFieldName就是 'user'
	 * @param userFieldName default valuie is 'user'
	 */
	public void setUserFieldName(String userFieldName) {
		this.userFieldName = userFieldName;
	}

	/**
	 * 数据源配置Json中的用口令字段，举例<pre>
	 * [{id:"ds1", url:"jdbc:mysql://localhost:3306/test", user: "root", password:"123456",
	 *    driverClassName:"org.gjt.mm.mysql.Driver"}]
	 * </pre>
	 * 在上面这段json中，passwordFieldName就是 'password'
	 * @param passwordFieldName default valuie is 'password'
	 */
	public void setPasswordFieldName(String passwordFieldName) {
		this.passwordFieldName = passwordFieldName;
	}
	/**
	 * 数据源配置Json中的用驱动类字段，举例<pre>
	 * [{id:"ds1", url:"jdbc:mysql://localhost:3306/test", user: "root", password:"123456",
	 *    driverClassName:"org.gjt.mm.mysql.Driver"}]
	 * </pre>
	 * 在上面这段json中，driverFieldName就是 'driverClassName'
	 * @param driverFieldName default valuie is 'driverClassName'
	 */
	public void setDriverFieldName(String driverFieldName) {
		this.driverFieldName = driverFieldName;
	}
}

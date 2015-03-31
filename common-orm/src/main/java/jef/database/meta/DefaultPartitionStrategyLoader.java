package jef.database.meta;

import java.io.BufferedReader;
import java.net.URL;

import jef.common.log.LogUtil;
import jef.database.IQueryableEntity;
import jef.database.annotation.PartitionTable;
import jef.database.annotation.PartitionTableImpl;
import jef.json.JsonUtil;
import jef.tools.IOUtils;
import jef.tools.StringUtils;

/**
 * 缺省分表规则加载器，首先查找xxx.xxx.xxx.clzname.partition文件,然后使用annotation。
 * 
 * 扩展这可以继承此类实现自定义的配置加载方式
 * 
 * 文件为json结构
 * 
 * @author jiyi
 * 
 */
public class DefaultPartitionStrategyLoader implements PartitionStrategyLoader {
	public PartitionTable get(Class<? extends IQueryableEntity> clz) {
		String data=getJsonDataConfig(clz);
		if(data!=null && data.length()>0){
			return parseJsonConfig(data);
		}
		URL url = getResource(clz);
		if (url != null) {
			return loadFromResourceUrl(url);
		}
		return loadFromClassAnnotation(clz);
	}
	
	/**
	 * 从URL中获取
	 * @param url
	 * @return
	 */
	protected PartitionTable loadFromResourceUrl(URL url) {
		BufferedReader reader = null;
		try {
			reader= IOUtils.getReader(url.openStream(), null);
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (StringUtils.isEmpty(line) || line.startsWith("--")) {
					continue;
				}
				sb.append(line);
				sb.append('\n');
			}
			String data = sb.toString();
			if (!data.startsWith("{")) {
				data = StringUtils.concat("{", data, "}");
			}
			LogUtil.info("Loading partition strategy from:{}", url.getPath());
			PartitionTableImpl table = JsonUtil.toObject(data, PartitionTableImpl.class);
			System.out.println(JsonUtil.toJson(table));
			return table;
		} catch (Exception e) {
			LogUtil.exception("The Partition rule load failure in file:" + url + ", it will be ignored!", e);
			return null;
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	private PartitionTable loadFromClassAnnotation(Class<? extends IQueryableEntity> clz) {
		PartitionTable table = clz.getAnnotation(PartitionTable.class);
		if (table != null)
			LogUtil.info(StringUtils.concat("Loading the partition strategy from annotation.[", clz.getName(), "]", JsonUtil.toJsonWithoutQuot(PartitionTableImpl.create(table))));
		return table;
	}
	
	/**
	 * 解析Json配置
	 * @param data
	 * @return
	 */
	protected PartitionTable parseJsonConfig(String data) {
		PartitionTableImpl table = JsonUtil.toObject(data, PartitionTableImpl.class);
		return table;
	}

	/**
	 * 寻找配置资源并返回
	 * @param clz
	 * @return
	 */
	protected URL getResource(Class<? extends IQueryableEntity> clz){
		String fileName = "/" + clz.getName() + ".json";
		return clz.getResource(fileName);
	}
	
	/**
	 * 得到JSON的分区配置。继承此类可以扩展配置读取的方式
	 * @param clz
	 * @return
	 */
	protected String getJsonDataConfig(Class<? extends IQueryableEntity> clz) {
		return null;
	}
}

package com.github.geequery.support.spring;

import java.sql.Connection;
import java.util.Properties;

public interface CustomConnectionProvider {
	Connection getConnectionFromDriver(Properties props); 

}

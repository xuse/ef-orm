/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.support;

import jef.database.Session;
import jef.database.DbClient;

/**
 * 任何继承此类的对象在JEF-IOC工厂出来后都会被注入DbClient
 * @author jiyi
 *
 */
public abstract class JefBaseDAO implements JefDbClientSupport{
	protected DbClient client;
	
	public void setClient(DbClient client){
		this.client=client;
	}
	
	public void setClient(Session client){
		setClient((DbClient)client);
	}
}

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
package jef.database.meta;

import com.github.geequery.entity.AbstractEntityMetadata;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.database.DebugUtil;
import jef.database.IQueryableEntity;
import jef.tools.reflect.BooleanProperty;
import jef.tools.reflect.Property;

public class TableMetadata extends AbstractEntityMetadata {
	/**
	 * 记录当前Schema所对应的实体类。
	 */
	private Class<? extends IQueryableEntity> thisType;

	/**
	 * Bean操作类
	 */
	private final BeanAccessor containerAccessor;

	/**
	 * 标准实体的构造
	 * 
	 * @param clz
	 * @param annos
	 */
	protected TableMetadata(Class<? extends IQueryableEntity> clz, AnnotationProvider annos) {
		super.checkEnhanced(clz, annos);
		this.thisType = clz;
		this.containerAccessor = FastBeanWrapperImpl.getAccessorFor(clz);
		initByAnno(clz, annos);
	}

	@Override
	public BeanAccessor getContainerAccessor() {
		return containerAccessor;
	}

	public Class<? extends IQueryableEntity> getThisType() {
		return thisType;
	}

	@Override
	public Property getTouchRecord() {
		return DebugUtil.TouchRecord;
	}

	@Override
	public BooleanProperty getTouchIgnoreFlag() {
		return DebugUtil.notTouchProperty;
	}

	public IQueryableEntity newInstance() {
		return (IQueryableEntity) containerAccessor.newInstance();
	}

	public EntityType getType() {
		return EntityType.NATIVE;
	}

	@Override
	public Property getLazyAccessor() {
		return DebugUtil.LazyContext;
	}
}

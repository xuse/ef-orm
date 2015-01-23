package jef.accelerator.bean;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.tools.IOUtils;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.FieldEx;
import jef.tools.reflect.MethodEx;
import jef.tools.reflect.UnsafeUtils;

/**
 * 用于生成动态访问者类（Accessor）的类工厂。
 * @author jiyi
 *
 */
final class ASMAccessorFactory implements BeanAccessorFactory {
	@SuppressWarnings("rawtypes")
	private static final Map<Class, BeanAccessor> map = new IdentityHashMap<Class, BeanAccessor>();
	
	public BeanAccessor getBeanAccessor(Class<?> javaBean) {
		if (javaBean.isPrimitive()) {
			throw new IllegalArgumentException(javaBean + " invalid!");
		}
		BeanAccessor ba = map.get(javaBean);
		if (ba != null)
			return ba;
		ba = generateAccessor(javaBean);
		synchronized (map) {
			map.put(javaBean, ba);
		}
		return ba;
	}

	private BeanAccessor generateAccessor(Class<?> javaClz) {
		String clzName = javaClz.getName().replace('.', '_');
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null)
			cl = BeanAccessorFactory.class.getClassLoader();

		Class<?> cls = null;
		try {
			cls = cl.loadClass(clzName);
		} catch (ClassNotFoundException e1) {
		}

		FieldInfo[] fields = getFields(javaClz);
		boolean isHashProperty=sortFields(fields);
		ClassGenerator asm;
		byte[] clzdata=null;
		if (cls == null) {
			if (isHashProperty) {
				asm= new ASMHashGenerator(javaClz, clzName, fields,cl);
			} else {
				asm = new ASMSwitcherGenerator(javaClz, clzName, fields);
			}
			clzdata = asm.generate();
			// DEBUG
//			saveClass(clzdata, clzName);
			cls= UnsafeUtils.defineClass(clzName, clzdata, 0, clzdata.length, cl);
			if (cls == null) {
				throw new RuntimeException("Dynamic class accessor for " + javaClz + " failure!");
			}
		}
		try {
			BeanAccessor ba = (BeanAccessor) cls.newInstance();
			initAnnotations(ba, fields);
			initGenericTypes(ba, fields);
			return ba;
		} catch (Error e) {
			if(clzdata!=null){
				saveClass(clzdata, clzName);
			}
			throw e;
		} catch (Exception e) {
			if(clzdata!=null){
				saveClass(clzdata, clzName);
			}
			throw new RuntimeException(e);
		}
	}
	interface ClassGenerator{
		byte[] generate(); 
	}

	/*
	 * 保存文件
	 * 
	 * @param mclz
	 * 
	 * @throws IOException
	 * 
	 * @throws CannotCompileException
	 */
	protected static void saveClass(byte[] data, String name) {
		File file = new File("c:/test/", name + ".class");
		try {
			IOUtils.saveAsFile(file, data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(file.getAbsolutePath() + " was generated to debug.");
	}

	/*
	 * 计算全部要反射代理的字段信息
	 * 
	 * @param javaBean
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static FieldInfo[] getFields(Class<?> javaBean) {
		ClassEx cw = new ClassEx(javaBean);
		FieldEx[] fs = cw.getFields();
		List<FieldInfo> result=new ArrayList<FieldInfo>(fs.length);
		for (FieldEx f : fs) {
			MethodEx getter = BeanUtils.getGetter(f);
			MethodEx setter = BeanUtils.getSetter(f);
			if (getter == null || setter == null) {
				continue;
			}
			FieldInfo fi = new FieldInfo();
			fi.setGetter(getter.getJavaMethod());
			fi.setSetter(setter.getJavaMethod());
			fi.setName(f.getName());
			fi.setType(f.getGenericType());
			

			{
				Annotation[] anno = f.getAnnotations();
				if (anno == null || anno.length == 0) {
					fi.setAnnoOnField(null);
				} else {
					IdentityHashMap<Class, Annotation> amap = new IdentityHashMap<Class, Annotation>();
					for (Annotation a : anno) {
						amap.put(a.annotationType(), a);
					}
					fi.setAnnoOnField(amap);
				}
			}

			{
				Annotation[] anno = getter.getAnnotations();
				if (anno == null || anno.length == 0) {
					fi.setAnnoOnGetter(null);
				} else {
					IdentityHashMap<Class, Annotation> amap = new IdentityHashMap<Class, Annotation>();
					for (Annotation a : anno) {
						amap.put(a.getClass(), a);
					}
					fi.setAnnoOnGetter(amap);
				}
			}

			{
				Annotation[] anno = setter.getAnnotations();
				if (anno == null || anno.length == 0) {
					fi.setAnnoOnSetter(null);
				} else {
					IdentityHashMap<Class, Annotation> amap = new IdentityHashMap<Class, Annotation>();
					for (Annotation a : anno) {
						amap.put(a.getClass(), a);
					}
					fi.setAnnoOnSetter(amap);
				}
			}
			result.add(fi);
		}
		return result.toArray(new FieldInfo[result.size()]);
	}

	private void initGenericTypes(BeanAccessor ba, FieldInfo[] fields) {
		for (int i = 0; i < fields.length; i++) {
			FieldInfo fi = fields[i];
			ba.initNthGenericType(i, fi.getRawType(), fi.getType(), fields.length, fi.getName());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initAnnotations(BeanAccessor ba, FieldInfo[] fields) {
		IdentityHashMap<Class, Annotation>[] f = new IdentityHashMap[fields.length];
		IdentityHashMap<Class, Annotation>[] g = new IdentityHashMap[fields.length];
		IdentityHashMap<Class, Annotation>[] s = new IdentityHashMap[fields.length];
		for (int n = 0; n < fields.length; n++) {
			f[n] = fields[n].getAnnoOnField();
			g[n] = fields[n].getAnnoOnGetter();
			s[n] = fields[n].getAnnoOnSetter();
		}
		ba.initAnnotations(f, g, s);
	}

	private boolean sortFields(FieldInfo[] fields) {
		if (fields == null)
			return false;

		boolean isDup=false;
		{
			Set<Integer> intSet=new HashSet<Integer>();
			for(FieldInfo fi:fields){
				boolean old=intSet.add(fi.getName().hashCode());
				if(!old){
					isDup=true;
					break;
				}
			}
		}
		
		Arrays.sort(fields, new Comparator<FieldInfo>() {
			public int compare(FieldInfo o1, FieldInfo o2) {
				int x = o1.getName().hashCode();
				int y = o2.getName().hashCode();
				return (x < y) ? -1 : ((x == y) ? 0 : 1);
			}
		});
		return isDup;
	}
}

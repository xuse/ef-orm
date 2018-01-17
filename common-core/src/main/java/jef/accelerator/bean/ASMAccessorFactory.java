package jef.accelerator.bean;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jef.accelerator.GeeField;
import jef.tools.IOUtils;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.FieldEx;
import jef.tools.reflect.MethodEx;
import jef.tools.reflect.UnsafeUtils;

/**
 * 用于生成动态访问者类（Accessor）的类工厂。
 * 
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
		synchronized (map) {
			ba = map.get(javaBean);
			if(ba==null){
				ba = generateAccessor(javaBean);
				map.put(javaBean, ba);	
			}
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
		boolean isHashProperty = sortFields(fields);
		ClassGenerator asm;
		byte[] clzdata = null;
		if (cls == null) {
			if (isHashProperty) {
				asm = new ASMHashGenerator(javaClz, clzName, fields, cl);
			} else {
				asm = new ASMSwitcherGenerator(javaClz, clzName, fields,cl);
			}
			clzdata = asm.generate();
			// DEBUG
			//saveClass(clzdata, clzName);
			cls = UnsafeUtils.defineClass(clzName, clzdata, 0, clzdata.length, cl);
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
			if (clzdata != null) {
				saveClass(clzdata, clzName);
			}
			throw e;
		} catch (Exception e) {
			if (clzdata != null) {
				saveClass(clzdata, clzName);
			}
			throw new RuntimeException(e);
		}
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
	private static FieldInfo[] getFields(Class<?> javaBean) {
		ClassEx cw = new ClassEx(javaBean);
		FieldEx[] fs = cw.getFields();
		Map<String, FieldInfo> result = new LinkedHashMap<String, FieldInfo>(fs.length);
		for (FieldEx f : fs) {
			GeeField fieldAnno = f.getAnnotation(GeeField.class);
			if (fieldAnno != null && fieldAnno.ignore()) {
				continue;
			}
			MethodEx getter = BeanUtils.getGetter(f);
			MethodEx setter = BeanUtils.getSetter(f);
			if (getter == null || setter == null) {
				continue;
			}
			FieldInfo fi = new FieldInfo();
			fi.setGetter(getter.getJavaMethod());
			fi.setSetter(setter.getJavaMethod());

			String name = (fieldAnno != null && fieldAnno.name().length() > 0) ? fieldAnno.name() : f.getName();
			fi.setName(name);
			fi.setType(f.getGenericType());
			fi.setAnnoOnField(processAnnotations(f));
			fi.setAnnoOnGetter(processAnnotations(getter));
			fi.setAnnoOnSetter(processAnnotations(setter));
			result.put(fi.getName(), fi);
		}
		MethodEx[] methods = cw.getMethods();
		for (MethodEx m : methods) {
			GeeField field = m.getAnnotation(GeeField.class);
			if (field==null || field.ignore())
				continue;
			String name = field.name();
			if (result.containsKey(name))
				continue;
			MethodEx setter;
			MethodEx getter;
			if (m.getParameterTypes().length == 1) {// set
				setter = m;
				getter = find(name, methods, true);
			} else if (m.getParameterTypes().length == 0) {// get
				getter = m;
				setter = find(name, methods, false);
			} else {
				throw new IllegalArgumentException("The @GeeField annotated on unknown method: " + m.getName());
			}
			if (setter == null || getter == null) {
				continue;
			}
			FieldInfo fi = new FieldInfo();
			fi.setGetter(getter.getJavaMethod());
			fi.setSetter(setter.getJavaMethod());
			fi.setName(name);
			fi.setType(getter.getGenericReturnType());
			fi.setAnnoOnGetter(processAnnotations(getter));
			fi.setAnnoOnSetter(processAnnotations(setter));
		}
		return result.values().toArray(new FieldInfo[result.size()]);
	}

	private static IdentityHashMap<Class<?>, Annotation> processAnnotations(MethodEx getter) {
		Annotation[] anno = getter.getAnnotations();
		if (anno == null || anno.length == 0) {
			return null;
		} else {
			IdentityHashMap<Class<?>, Annotation> amap = new IdentityHashMap<Class<?>, Annotation>();
			for (Annotation a : anno) {
				amap.put(a.getClass(), a);
			}
			return amap;
		}
	}

	private static IdentityHashMap<Class<?>, Annotation> processAnnotations(FieldEx f) {
		Annotation[] anno = f.getAnnotations();
		if (anno == null || anno.length == 0) {
			return null;
		} else {
			IdentityHashMap<Class<?>, Annotation> amap = new IdentityHashMap<Class<?>, Annotation>();
			for (Annotation a : anno) {
				amap.put(a.annotationType(), a);
			}
			return amap;
		}
	}

	private static MethodEx find(String name, MethodEx[] methods, boolean getter) {
		for (MethodEx m : methods) {
			GeeField gf = m.getAnnotation(GeeField.class);
			if (gf != null && gf.name().equals(name)) {
				if (m.getParameterTypes().length == (getter ? 0 : 1)) {
					return m;
				}
			}
		}
		return null;
	}

	private void initGenericTypes(BeanAccessor ba, FieldInfo[] fields) {
		for (int i = 0; i < fields.length; i++) {
			FieldInfo fi = fields[i];
			ba.initNthGenericType(i, fi.getRawType(), fi.getType(), fields.length, fi.getName());
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void initAnnotations(BeanAccessor ba, FieldInfo[] fields) {
		IdentityHashMap<Class<?>, Annotation>[] f = new IdentityHashMap[fields.length];
		IdentityHashMap<Class<?>, Annotation>[] g = new IdentityHashMap[fields.length];
		IdentityHashMap<Class<?>, Annotation>[] s = new IdentityHashMap[fields.length];
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

		boolean isDup = false;
		{
			Set<Integer> intSet = new HashSet<Integer>();
			for (FieldInfo fi : fields) {
				boolean old = intSet.add(fi.getName().hashCode());
				if (!old) {
					isDup = true;
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

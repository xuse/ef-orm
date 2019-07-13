package jef;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.ReflectionException;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.accelerator.cglib.beans.BeanCopier;
import jef.common.log.LogUtil;
import jef.tools.Foo;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.BeanWrapper;

import  org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Test;

public class PerformanceTest {
	private static final int LOOP_NUMBER = 200000;

	// copy测试
	@Test
	public void copyPropertiesTest() throws IllegalAccessException, InvocationTargetException, InterruptedException, ReflectionException, NoSuchMethodException {
		Map<String, Long> cost = new LinkedHashMap<String, Long>();

		PromotionPO source = new PromotionPO();
		Timestamp now = new Timestamp(System.currentTimeMillis());
		source.setCode("code");
		source.setDescription("haha");
		source.setDiscount(5.4);
		source.setEndTime(new Date());
		source.setID(39578395L);
		source.setPriority(1);
		source.setPromotionType(3);
		source.setStartTime(now);
		source.setSupplierID("123245L");
		source.setField1("dsds");
		source.setField2("fdsfsdfds");
		source.setField3("sdff33");
		source.setField4("dfsef4344");
		source.setField5("cfer4344");
		source.setField6("dssfsfdsf");
		source.setField7("cdedfewfdsf");

		{
			PromotionPO target = new PromotionPO();
			BeanCopier bc = BeanCopier.create(PromotionPO.class, PromotionPO.class, false);
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < LOOP_NUMBER; i++) {
				bc.copy(source, target, null);
			}
			long endTime = System.currentTimeMillis();
			cost.put("Spring BeanCopier", endTime - startTime);

		}

		{// 案例0：CGLib （原生）
			PromotionPO po = new PromotionPO();
			jef.accelerator.cglib.beans.BeanCopier bc = jef.accelerator.cglib.beans.BeanCopier.create(PromotionPO.class, PromotionPO.class, false);
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < LOOP_NUMBER; i++) {
				bc.copy(source, po, null);
			}
			long endTime = System.currentTimeMillis();
			cost.put("CGLib BeanCopier", endTime - startTime);
		}

		{// 案例1：Apache commons (最慢)
			PromotionPO po = new PromotionPO();
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < LOOP_NUMBER; i++) {
//				org.apache.commons.beanutils.PropertyUtils.copyProperties(po, source);
			}
			long endTime = System.currentTimeMillis();
			cost.put("Apache PropertyUtils", endTime - startTime);// (Apache
																	// 的BeanUtils.copyProperties性能差不多是PropertyUtils的1/2，时间开销加倍实在太慢了!)
		}
		{// 案例2：Spring的beanutils
			// spring
			PromotionPO po = new PromotionPO();
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < LOOP_NUMBER; i++) {
				org.springframework.beans.BeanUtils.copyProperties(source, po);
			}
			long endTime = System.currentTimeMillis();
			cost.put("Spring BeanUtils", endTime - startTime);

		}

		{// 案例3：Spring的BeanWrapper
			// spring
			PromotionPO po = new PromotionPO();
			long startTime = System.currentTimeMillis();

			for (int i = 0; i < LOOP_NUMBER; i++) {
				org.springframework.beans.BeanWrapper bws = new org.springframework.beans.BeanWrapperImpl(source);
				org.springframework.beans.BeanWrapper bwt = new org.springframework.beans.BeanWrapperImpl(po);
				for (PropertyDescriptor property : bws.getPropertyDescriptors()) {
					if (bwt.isWritableProperty(property.getName())) {
						bwt.setPropertyValue(property.getName(), bws.getPropertyValue(property.getName()));
					}
				}
			}
			long endTime = System.currentTimeMillis();
			cost.put("Spring BeanWrapper", endTime - startTime);

		}

		{// 案例4: jef.tools.reflect.BeanUtils (含动态类创建时间)
			PromotionPO po = new PromotionPO();
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < LOOP_NUMBER; i++) {
				jef.tools.reflect.BeanUtils.copyProperties(source, po);
			}
			long endTime = System.currentTimeMillis();
			cost.put("Jef BeanUtils(include ASM)", endTime - startTime);
		}

		{// 案例5：jef.tools.reflect.BeanUtils
			BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(PromotionPO.class);
			PromotionPO po = new PromotionPO();
			long startTime = System.currentTimeMillis();
			// 动态类生成时间不计入反射操作时间
			for (int i = 0; i < LOOP_NUMBER; i++) {
				ba.copy(source, po);
			}
			long endTime = System.currentTimeMillis();
			cost.put("Jef BeanAccessor", endTime - startTime);

		}

		{// 案例0:直接get/set
			PromotionPO po = new PromotionPO();
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < LOOP_NUMBER; i++) {
				po.setCode(source.getCode());
				po.setDescription(source.getDescription());
				po.setDiscount(source.getDiscount());
				po.setEndTime(source.getEndTime());
				po.setID(source.getID());
				po.setPriority(source.getPriority());
				po.setPromotionType(source.getPromotionType());
				po.setStartTime(source.getStartTime());
				po.setSupplierID(source.getSupplierID());
				po.setField1(source.getField1());
				po.setField2(source.getField2());
				po.setField3(source.getField3());
				po.setField4(source.getField4());
				po.setField5(source.getField5());
				po.setField6(source.getField6());
				po.setField7(source.getField7());
			}
			long endTime = System.currentTimeMillis();
			cost.put("Hard Coding", endTime - startTime);
		}
		LogUtil.show(cost);
	}

	// 随机存取测试
	@Test
	public void getterSetterTest() throws IllegalAccessException, InvocationTargetException, InterruptedException, ReflectionException, NoSuchMethodException {
		PromotionPO source = new PromotionPO();
		long springCopyPropertiesTime;
		long commonsCopyPropertiesTime;
		long fastCopyWithClassGenerate;
		long fastCopy;
		long traditionalCopyTIme;

		{// 案例1：Apache commons
			PromotionPO po = new PromotionPO();
			long startTime = System.currentTimeMillis();
//			for (int i = 0; i < LOOP_NUMBER; i++) {
//				org.apache.commons.beanutils.PropertyUtils.getProperty(po, "code");
//				org.apache.commons.beanutils.PropertyUtils.getProperty(po, "ID");
//				org.apache.commons.beanutils.PropertyUtils.getProperty(po, "field1");
//				org.apache.commons.beanutils.PropertyUtils.setProperty(po, "code", "code123456789");
//				org.apache.commons.beanutils.PropertyUtils.setProperty(po, "ID", 100L);
//				org.apache.commons.beanutils.PropertyUtils.setProperty(po, "field1", "field1123456789");
//			}
			long endTime = System.currentTimeMillis();
			commonsCopyPropertiesTime = endTime - startTime;
		}
		{// 案例2：Spring的beanutils
			// spring
			PromotionPO po = new PromotionPO();
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < LOOP_NUMBER; i++) {
				org.springframework.beans.BeanWrapperImpl bw = new org.springframework.beans.BeanWrapperImpl(po);
				bw.getPropertyValue("code");
				bw.getPropertyValue("ID");
				bw.getPropertyValue("field1");
				bw.setPropertyValue("code", "code123456789");
				bw.setPropertyValue("ID", 100L);
				bw.setPropertyValue("field1", "field1123456789");
			}
			long endTime = System.currentTimeMillis();
			springCopyPropertiesTime = endTime - startTime;

		}
		{// 案例3: jef.tools.reflect.BeanUtils (含动态类创建时间)
			PromotionPO po = new PromotionPO();
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < LOOP_NUMBER; i++) {
				jef.tools.reflect.BeanWrapper bw = new FastBeanWrapperImpl(po);
				bw.getPropertyValue("code");
				bw.getPropertyValue("ID");
				bw.getPropertyValue("field1");
				bw.setPropertyValue("code", "code123456789");
				bw.setPropertyValue("ID", 100L);
				bw.setPropertyValue("field1", "field1123456789");
			}
			long endTime = System.currentTimeMillis();
			fastCopyWithClassGenerate = endTime - startTime;
		}
		{// 案例4：jef.tools.reflect.BeanUtils
			PromotionPO po = new PromotionPO();
			long startTime = System.currentTimeMillis();
			// 动态类生成时间不计入反射操作时间
			for (int i = 0; i < LOOP_NUMBER; i++) {
				jef.tools.reflect.BeanWrapper bw = new FastBeanWrapperImpl(po);
				bw.getPropertyValue("code");
				bw.getPropertyValue("ID");
				bw.getPropertyValue("field1");
				bw.setPropertyValue("code", "code123456789");
				bw.setPropertyValue("ID", 100L);
				bw.setPropertyValue("field1", "field1123456789");
			}
			long endTime = System.currentTimeMillis();
			fastCopy = endTime - startTime;

		}

		{// 案例0:直接get/set
			PromotionPO po = new PromotionPO();
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < LOOP_NUMBER; i++) {
				po.getCode();
				po.getID();
				po.getField1();
				po.setCode("code123456789");
				po.setID(100L);
				po.setField1("field1123456789");
			}
			long endTime = System.currentTimeMillis();
			traditionalCopyTIme = endTime - startTime;
		}
		LogUtil.info("Apache Commons get/set Time: " + commonsCopyPropertiesTime + "ms.");
		LogUtil.info("Spring BeanWrapper get/set Time: " + springCopyPropertiesTime + "ms.");
		LogUtil.info("Easyframe BeanUtil get/set Time: " + fastCopyWithClassGenerate + "ms.");
		LogUtil.info("Easyframe BeanUtil get/set Time(without first): " + fastCopy + "ms.");
		LogUtil.info("Java get/set Time: " + traditionalCopyTIme + "ms.");
	}

	@Test
	public void test3() {
		BeanWrapper bw = BeanWrapper.wrap(new PromotionPO());
		for (String s : bw.getPropertyNames()) {
			System.out.println(s + ":" + s.hashCode());
		}
		System.out.println(bw.getPropertyNames().size());
	}

	@Test
	public void sdfsa() {
		{
			BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(PromotionPO.class);
			PromotionPO source = new PromotionPO();
			Timestamp now = new Timestamp(System.currentTimeMillis());
			source.setCode("code");
			source.setDescription("haha");
			source.setDiscount(5.4);
			source.setEndTime(new Date());
			source.setID(39578395L);
			source.setPriority(1);
			source.setPromotionType(3);
			source.setStartTime(now);
			source.setSupplierID("123245L");
			source.setField1("dsds");
			source.setField2("fdsfsdfds");
			source.setField3("sdff33");
			source.setField4("dfsef4344");
			source.setField5("cfer4344");
			source.setField6("dssfsfdsf");
			source.setField7("cdedfewfdsf");
			Map map = ba.convert(source);
			ba.getAnnotationOnField("code");
			ba.getAnnotationOnGetter("code");
			ba.getAnnotationOnSetter("code");
			PromotionPO source2 = (PromotionPO) ba.fromMap(map);
			System.out.println(map);
			System.out.println(ToStringBuilder.reflectionToString(source));
			System.out.println(ToStringBuilder.reflectionToString(source2));
		}
		{
			BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(Foo.class);
			Foo foo = new Foo();
			foo.setId(1);
			foo.setName("aaa");
			foo.setScore(123D);
			foo.setCreated(new Date());
			Map map = ba.convert(foo);
			ba.getAnnotationOnField("name");
			ba.getAnnotationOnGetter("name");
			ba.getAnnotationOnSetter("name");
			map.remove("score");
			Foo foo2 = (Foo) ba.fromMap(map);
			System.out.println(map);
			System.out.println(ToStringBuilder.reflectionToString(foo));
			System.out.println(ToStringBuilder.reflectionToString(foo2));

			map.put("id", "3");
			Foo foo3 = BeanUtils.undescribeSafe(map, Foo.class);
			System.out.println(ToStringBuilder.reflectionToString(foo3));

		}

	}

	@Test
	public void testConcurrent() throws InterruptedException {
		for (int i = 0; i < 100; i++) {
			new Thread() {
				@Override
				public void run() {
					work();
				}
			}
			.start();
			;
		}
		Thread.sleep(2000);
	}

	private void work() {
		FastBeanWrapperImpl.getAccessorFor(PromotionPO.class);
	}

}

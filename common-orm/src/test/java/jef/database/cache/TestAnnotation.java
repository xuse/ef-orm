package jef.database.cache;

import java.lang.reflect.Method;

import javax.persistence.Entity;

@Entity
public class TestAnnotation {
    public void test_performance() {
    	Entity e=TestAnnotation.class.getAnnotation(Entity.class);
    }
     
    public static void test1() {
        long start = System.currentTimeMillis();
        TestAnnotation test = new TestAnnotation();
         
        for(int i = 0; i < 1000000000L; i++) {
            test.test_performance();
        }
        long end = System.currentTimeMillis();
        System.out.println("直接调用10亿次耗时: " + (end - start) + "ms");
    }
     
    public static void test2() throws Exception {
        long start = System.currentTimeMillis();
        TestAnnotation test = new TestAnnotation();
         
        Class clazz = test.getClass();
        Method m = clazz.getDeclaredMethod("test_performance", null);
        for(int i = 0; i < 1000000000L; i++) {
            m.invoke(test, null);
        }
        long end = System.currentTimeMillis();
        System.out.println("反射调用10亿次耗时: " + (end - start) + "ms");
    }
     
    public static void test3() throws Exception {
        long start = System.currentTimeMillis();
        TestAnnotation test = new TestAnnotation();
         
        Class clazz = test.getClass();
        Method m = clazz.getDeclaredMethod("test_performance", null);
        m.setAccessible(true);
        for(int i = 0; i < 1000000000L; i++) {
            m.invoke(test, null);
        }
        long end = System.currentTimeMillis();
        System.out.println("不检查安全性反射调用10亿次耗时: " + (end - start) + "ms");
    }
     
    public static void main(String[] args) throws Exception {
        test1();
        test2();
        test3();
    }
}
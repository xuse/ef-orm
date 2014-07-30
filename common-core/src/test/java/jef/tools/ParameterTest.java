package jef.tools;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import jef.tools.MathUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
  
  
/** 
 * 参数化设置 
 *  
 * 1 测试类必须由parameterized测试运行器修饰 
 * 2 准备数据，数据的准备需要在一个方法中进行，该方法需要满足一定的要求 
 *   1)该方法必须有parameters注解修饰 
 *   2)该方法必须为public static的 
 *   3)该方法必须返回Collection类型 
 *   4)该方法的名字不作要求 
 *   5)该方法没有参数 
 *    
 *   int.class == Integer.TYPE != Integer.class 
 */  
// 测试运行器  
@RunWith(Parameterized.class)  
public class ParameterTest {  
    private int expeted;  
    private int input1;  
    private int input2;  
      
    @Parameters  
    public static Collection perpareData() {  
        Object[][] objects = { {3,1,2}, {0,0,0}, {-4,-1,-3} };  
        return Arrays.asList(objects);  
    }  
      
    public ParameterTest(int expected, int input1, int input2){  
        this.expeted = expected;  
        this.input1 = input1;  
        this.input2 = input2;  
    }  
      
    @Test public void testAdd() {  
        assertEquals(expeted, MathUtils.add(input1, input2));  
    }  
} 

package jef.entity;

import junit.framework.Assert;

import org.junit.Test;

/**
 * {@code CommonMaskStrategy}单元测试类
 * 
 * @see CommonMaskStrategy
 * 
 * @Company Asiainfo-Linkage Technologies (China), Inc.
 * @author luolp@asiainfo-linkage.com
 * @Date 2012-7-12
 */
public class TestCommonMaskStrategy {
	/**
	 * 通用模糊化
	 */
	@Test
	public void testMask() {
		//通用模糊化
		CommonMaskStrategy maskStategy = new CommonMaskStrategy();
		Assert.assertEquals("***********", maskStategy.eval("only测试信息模糊化", null));
		
		Assert.assertEquals("", maskStategy.eval("", null));
		Assert.assertNull(maskStategy.eval(null, null));

		maskStategy = new CommonMaskStrategy("*(3,8)");
		Assert.assertEquals("onl*****模糊化", maskStategy.eval("only测试信息模糊化", null));

		maskStategy = new CommonMaskStrategy("(,15)");
		Assert.assertEquals("***********", maskStategy.eval("only测试信息模糊化", null));
	}

	/**
	 * 模糊化为指定长度
	 */
	@Test
	public void testMaskToFixed() {
		CommonMaskStrategy maskStategy = new CommonMaskStrategy("*3(,)");
		Assert.assertEquals("***", maskStategy.eval("only测试信息模糊化", null));

		maskStategy = new CommonMaskStrategy("*20(,)");
		Assert.assertEquals("********************", maskStategy.eval("only测试信息模糊化", null));
	}

	@Test
	public void testMaskFromStart() {
		/**
		 * 从第二个字符开始模糊
		 */
		CommonMaskStrategy maskStategy = new CommonMaskStrategy("*(1,)");
		Assert.assertEquals("张*", maskStategy.eval("张三", null));
		Assert.assertEquals("李**", maskStategy.eval("李小明", null));

		/**
		 * 模糊倒数四位
		 */
		maskStategy = new CommonMaskStrategy("*(-4,)");
		Assert.assertEquals("1380571****", maskStategy.eval("13805710000", null));

		maskStategy = new CommonMaskStrategy("*(-4,6)");
		Assert.assertEquals("1380571****", maskStategy.eval("13805710000", null));
	}

	@Test
	public void testMaskToFixedFromStart() {
		CommonMaskStrategy maskStategy = new CommonMaskStrategy("*3(3,)");
		Assert.assertEquals("138***", maskStategy.eval("13805710000", null));

		maskStategy = new CommonMaskStrategy("*3(-4,)");
		Assert.assertEquals("1380571***", maskStategy.eval("13805710000", null));

		maskStategy = new CommonMaskStrategy("*3(-4,6)");
		Assert.assertEquals("1380571***", maskStategy.eval("13805710000", null));
	}

	@Test
	public void testMaskFromEnd() {
		CommonMaskStrategy maskStategy = new CommonMaskStrategy("*(,6)");
		Assert.assertEquals("******10000", maskStategy.eval("13805710000", null));

		maskStategy = new CommonMaskStrategy("*(,-4)");
		Assert.assertEquals("*******0000", maskStategy.eval("13805710000", null));
	}

	@Test
	public void testMaskToFixedFromEnd() {
		CommonMaskStrategy maskStategy = new CommonMaskStrategy("*3(,-4)");
		Assert.assertEquals("***0000", maskStategy.eval("13805710000", null));
	}

	@Test
	public void testMaskFromStartAndEnd() {
		CommonMaskStrategy maskStategy = new CommonMaskStrategy("*(5,-4)");
		Assert.assertEquals("95588**********2334", maskStategy.eval("9558801202106562334", null));
	}

	@Test
	public void testMaskToFixedFromStartAndEnd() {
		CommonMaskStrategy maskStategy = new CommonMaskStrategy("*3(5,-4)");
		Assert.assertEquals("95588***2334", maskStategy.eval("9558801202106562334", null));
	}

	@Test
	public void testExpressionIllegal() {
		try {
			new CommonMaskStrategy("^3(5,-4)");
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
		
		try {
			new CommonMaskStrategy("#(aa,-4)");
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

}

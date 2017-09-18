package jef.codegen;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Date;

import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import jef.accelerator.asm.Attribute;
import jef.accelerator.asm.ClassReader;
import jef.accelerator.asm.ClassVisitor;
import jef.accelerator.asm.ClassWriter;
import jef.accelerator.asm.Opcodes;
import jef.database.DataObject;
import jef.database.VarObject;
import jef.database.dialect.ColumnType;
import jef.database.meta.MetaHolder;
import jef.database.meta.TupleMetadata;
import jef.json.JsonUtil;
import jef.orm.onetable.model.TestEntity;
import jef.tools.DateFormats;
import jef.tools.IOUtils;
import jef.tools.reflect.BeanWrapper;
import jef.tools.string.RandomData;

import org.junit.Test;
public class EntityEnhancerTest {

	@Test
	public void testEntityEnhancer() {
		EntityEnhancer e = new EntityEnhancer();
		e.setOut(System.out);
		e.setExcludePatter(new String[] { "java.lang.*" });
		e.setIncludePattern("javax.xml");
		e.getExcludePatter();
		e.getIncludePattern();
		e.enhance("jef");
	}

	@Test
	public void testEnhanceTask() throws Exception {
		EnhanceTaskASM asm = new EnhanceTaskASM();
		ClassReader reader = new ClassReader("jef.orm.multitable.model.Person");
		URL url=ClassLoader.getSystemResource("jef.orm.multitable.model.Person$Field".replace('.', '/') + ".class");
		
		ClassWriter writer = new ClassWriter(0);
		reader.accept(new ClassVisitor(Opcodes.ASM5,writer){
			@Override
			public void visitAttribute(Attribute attr) {
				if(!"jefd".equals(attr.type)){
					super.visitAttribute(attr);
				}
			}
		}, 0);
		
		 byte[] bytes=asm.doEnhance( writer.toByteArray(),IOUtils.toByteArray(url));
		 assertNotNull(bytes);
		 
		 byte[] bytes2=asm.doEnhance(bytes,IOUtils.toByteArray(url));
		 assertTrue(bytes2.length==0);
		 
	}
	
	@Test
	public void testWrapperSettingPrimitiveNull(){
		BeanWrapper bw=BeanWrapper.wrap(new FieldParent());
		bw.setPropertyValue("id", 1);
		bw.setPropertyValue("id", null);
	}
	
	/**
	 * 1 可以增强内部类
	 * 
	 * 增强操作队显示为对一切IQuerableEnrity生效，但对任意类来说，都只按Field中的枚举来增强属性。(可以吗？)
	 * 
	 * 
	 * 
	 * 
	 *
	 * 现在支持，父子类之间继承field属性。
	 * 2 有Field的子类 (OK)
	 * 3 无Field的子类 （OK）
	 * 
	 * 4 父类没有Field，子类有全部的Field
	 * 4 父类没有Field，子类有部分的Field  (结果：仅在子类中属性得到了增强，父类的属性未增强：解决办法是在父类中也将该字段定义为Field.
	 * 考虑制作功能用于增强父类，但是——
	 *   a 父类可能在JAR包中，不能直接修改。
	 *   b 如果在子类中通过覆盖方法来实现，也有问题，因为ASM中去解析父类并查找同名方法较为复杂。在增强前，不能调用类实现反射，因此相当于要自行用ASM实现父子类解析的JAVA逻辑，太麻烦了……
	 *   
	 *      办法，在增强子类时，查找哪些子类中定义了元模型，但并未定义的属性，然后到父类中去找，直到找到其getter setter方法，再然后编写一个增强的方法覆盖父类方法……
	 *      用CG lib倒是可以很方便的实现，但是用ASM就悲剧了。
	 *      此外，如果父类本身也定义了该元模型，子类覆盖父类元模型，此时也很悲剧——延迟加载和等植入代码将被执行两遍，子类一遍父类一遍……
	 *      
	 *      因此，我们还是要尽可能避免这种父类定义属性，子类定义元模型的方式。
	 *      
	 *   
	 * )
	 */
	@Test
	public void testInnerClass()throws Exception {
		System.out.println(MetaHolder.getMeta(NoFieldParent.class));
		System.out.println(MetaHolder.getMeta(ExtendsNoFIeldClass.class)); //此处如果仅输出一个列，是不对的
		System.out.println(MetaHolder.getMeta(FieldParent.class));
		System.out.println(MetaHolder.getMeta(OverrideParent.class));
		System.out.println(MetaHolder.getMeta(NoFIeldChild.class));
		System.out.println(MetaHolder.getMeta(Ts.class));
	}
	
	public static class NoFieldParent extends DataObject{
		private int noFid;
		private String noFname;
		public int getNoFid() {
			return noFid;
		}
		public void setNoFid(int noFid) {
			this.noFid = noFid;
		}
		public String getNoFname() {
			return noFname;
		}
		public void setNoFname(String noFname) {
			this.noFname = noFname;
		}
		public enum Field implements jef.database.Field{
			noFid
		}
	}

	public static class FieldParent extends DataObject{
		private int id;
		private String name;
		private Date date;
		private String notDefine;
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Date getDate() {
			return date;
		}
		public void setDate(Date date) {
			this.date = date;
		}
		public String getNotDefine() {
			return notDefine;
		}
		public void setNotDefine(String notDefine) {
			this.notDefine = notDefine;
		}


		public enum Field implements jef.database.Field{
			id,name,date,notDefine
		}
	}
	
	/**
	 * 将父类中的属性（非列）定义为数据库列
	 * 父类无列，子类有列
	 * @author jiyi
	 *
	 */
	public static class ExtendsNoFIeldClass extends NoFieldParent{
		private String desc;
		private String noFname; 
		
		public String getNoFname() {
			return noFname;
		}
		public void setNoFname(String noFname) {
			this.noFname = noFname;
		}
		public String getDesc() {
			return desc;
		}
		public void setDesc(String desc) {
			this.desc = desc;
		}
		public enum Field implements jef.database.Field{
			noFid,noFname,desc
		}
	}
	/**
	 * 父类有列，子类无列
	 * @author jiyi
	 *
	 */
	public static class NoFIeldChild extends FieldParent{
		private String ext;
		
		@OneToOne
		@JoinColumn(name="id",referencedColumnName="noFid")
		private ExtendsNoFIeldClass extend;

		public ExtendsNoFIeldClass getExtend() {
			return extend;
		}

		public void setExtend(ExtendsNoFIeldClass extend) {
			this.extend = extend;
		}
		
		public String getExt() {
			return ext;
		}

		public void setExt(String ext) {
			this.ext = ext;
		}
	}
	
	
	public static class OverrideParent extends FieldParent{
		private String comment;
		public enum Field implements jef.database.Field{
			comment,notDefine
		}
		public String getComment() {
			return comment;
		}
		public void setComment(String comment) {
			this.comment = comment;
		}
		
		
	}
	
	/**
	 * 测试动态Entity和静态Entity的 bean被序列化时的表现
	 */
	@Test
	public void testDataObjectAsJson(){
		TestEntity t1=RandomData.newInstance(TestEntity.class);
		t1.prepareUpdate(TestEntity.Field.boolField, "false");
		t1.setBinaryData(null);
		t1.setTt1(null);
		t1.setTt2(null);
		t1.setTt3(null);
		String s=JsonUtil.toJson(t1, DateFormats.DATE_TIME_CS.get());
		System.out.println(s);
		TupleMetadata GroupTable = new TupleMetadata("URM_GROUP");
		GroupTable.addColumn("id", new ColumnType.AutoIncrement(8));
		GroupTable.addColumn("serviceId", new ColumnType.Int(8));
		GroupTable.addColumn("name", new ColumnType.Varchar(100));
		VarObject var=new VarObject(GroupTable);
		var.set("id", 123);
		var.set("serviceId",456);
		var.set("name","addfvdf");
		s=JsonUtil.toJson(var, DateFormats.DATE_TIME_CS.get());
		System.out.println(s);
	}
	
	@Test
	public void test123(){
		EntityEnhancer e=new EntityEnhancer();
		e.enhance("com.hikvision");
	}
}

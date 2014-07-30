package jef.database.query;

/**
 * 描述一个绑定变量的值，实现jef.database.Field接口
 * @author Administrator
 *
 */
public class BindVariableField implements jef.database.Field{
	public BindVariableField(Object value){
		this.value=value;
	}
	private static final long serialVersionUID = -8768583442196655587L;
	public Object value;
	public String name() {
		return String.valueOf(value);
	}
	
}
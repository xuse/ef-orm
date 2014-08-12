package jef.database.routing.function;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jef.database.query.RegexpDimension;
import jef.tools.StringUtils;

/**
 * 分表字段处理函数：ModulusFunction
 * 对指定字段取摸
 * 
 * @author Administrator
 *
 */
public class ModulusFunction extends AbstractNumberFunction{
	private BigInteger mod;
	
	public ModulusFunction(String string) {
		long l=StringUtils.toLong(string, 0L);
		this.mod=BigInteger.valueOf(l);
	}
	public ModulusFunction(int i) {
		this.mod=BigInteger.valueOf(i);
	}
	public String eval(Number value) {
		BigInteger l=BigInteger.valueOf(value.longValue());
		return l.mod(mod).toString();
	}
	public List<Number> iterator(Number minn, Number maxn, boolean left, boolean right) {
		if(minn==null || maxn==null){
			return allModulus(); //任何一侧没有限制的话，都返回无限的取模区间.
			//但是如果考虑到一般来说，分表维度值都是正数的话，那么这里还能优化：比如 where a<5，此时实际上只要查0,1,2,3,4等五张表就够了
		}
		List<Number> ret=new ArrayList<Number>();
		if(minn instanceof Integer){//按int计算
			int min=minn.intValue();
			int max=maxn.intValue();
			if (!left)
				min++;			
			if (!right)
				max--;
			for (int i = min; i <= max; i++) {
				ret.add(i);
			}		
		}else{
			long min = minn.longValue();
			long max = maxn.longValue();
			if (!left)
				min++;			
			if (!right)
				max--;
			for (long i = min; i <= max; i++) {
				ret.add(i);
			}
		}
		return ret;
	}
	
	private List<Number> allModulus() {
		List<Number> ret=new ArrayList<Number>();
		for(int i=0;i<mod.intValue();i++){
			ret.add(i);
		}
		return ret;
	}

	private static ModulusFunction DEFAULT;
	public static ModulusFunction getDefault(){
		if(DEFAULT==null){
			DEFAULT=new ModulusFunction(10);
		}
		return DEFAULT;
	}
	public boolean acceptRegexp() {
		return false;
	}
	public Collection<Number> iterator(RegexpDimension regexp) {
		throw new UnsupportedOperationException();
	}
}

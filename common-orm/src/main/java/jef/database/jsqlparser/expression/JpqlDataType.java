package jef.database.jsqlparser.expression;


/**
 * 描述在NamedQuery中允许出现的数据类型
 * <h3>作用</h3>
 * 标准SQL中定义绑定变量的方式如下{@code
 *    select * from foo where id=? and name=?
 * }
 * EF框架中，{@linkplain jef.database.NativeQuery 对SQL语句进行了增强}，
 * 其中有一项重要特性就是，可以在SQL语句中指定参数(绑定变量)的类型。
 * 
 *  <h3>枚举一览</h3>
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <tr><td>类型名</td><td>效果</td></tr>
 * <tr><td>{@link #DATE}</td><td>参数将被转换为java.sql.Date</td></tr>
 * <tr><td>{@link #TIMESTAMP}</td><td>参数将被转换为java.sql.Timestamp</td></tr>
 * <tr><td>{@link #INT}</td><td>参数将被转换为int</td></tr>
 * <tr><td>{@link #STRING}</td><td>参数将被转换为string</td></tr>
 * <tr><td>{@link #LONG}</td><td>参数将被转换为long</td></tr>
 * <tr><td>{@link #SHORT}</td><td>参数将被转换为short</td></tr>
 * <tr><td>{@link #FLOAT}</td><td>参数将被转换为float</td></tr>
 * <tr><td>{@link #DOUBLE}</td><td>参数将被转换为double</td></tr>
 * <tr><td>{@link #BOOLEAN}</td><td>参数将被转换为boolean</td></tr>
 * <tr><td>{@link #STRING$}</td><td>参数将被转换为string,并且后面加上%，一般用于like xxx% 的场合</td></tr>
 * <tr><td>{@link #$STRING$}</td><td>参数将被转换为string,并且两端加上%，一般用于like %xxx% 的场合</td></tr>
 * <tr><td>{@link #$STRING}</td><td>参数将被转换为string,并且前面加上%，一般用于like %xxx 的场合</td></tr>
 * <tr><td>{@link #SQL}</td><td>SQL片段。参数将直接作为SQL语句的一部分，而不是作为SQL语句的绑定变量处理</td></tr>
 * </table>
 * 上面的STRING$、$STRING$、$STRING三种参数转换，其效果是将$符号替换为%，主要用于从WEB页面传输模糊匹配的查询条件到后台。使用该数据类型后，%号的添加交由框架自动处理。<br>
 * 
 * <h3>用法1: 自动将参数转换为SQL语句需要的类型</h3>
 * 可以编写这样一句SQL。
 * <pre><tt>NativeQuery&lt;Person&gt; query=session.createNativeQuery(
 *  "select * from person where age between :minAge&lt;int&gt; and :maxAge&lt;int&gt;", Person.class);</tt></pre>
 * 如果不提示{@code minAge}和{@code maxAge}的 数据类型，那么在使用该查询时，我们必须<p>
 * {@code query.setParam("minAge", Integer.parseInt(maxString))}; //必须确保设置的参数类型为数值类型。
 * <p>
 * 但如果在SQL语句中指定了参数的类型，那么EF_ORM在执行语句时就会检查参数类型，并自动将传入的参数转换为需要的SQL类型，这将在一定程度上简化开发工作。
 * 
 * <h3>用法2: Like语句中，自动补充条件两端的百分号</h3>
 * <p>
 * <pre><tt>NativeQuery&lt;Person&gt; query=session.createNativeQuery(
 *    "select * from person where name like :name<$string$>", Person.class);
 *  query.setParam("name","Smith");
 *   //执行时的实际SQL为select * from person where name like '%Smith%'。即解决了Like语句通配符的问题。
 *  query.getResultList(); 
 *  </tt></pre>
 *  详见<ul>
 *  <li>{@linkplain JpqlDataType#$STRING $STRING类型}</li>
 *  <li>{@linkplain JpqlDataType#$STRING$ $STRING$类型}</li>
 *  <li>{@linkplain JpqlDataType#STRING$ STRING$类型}</li>
 * </ul>
 * 
 * <h3>用法3: SQL片段，将参数直接拼接到SQL语句中</h3>
 * {@code
 *    select from :tablename<sql>  where id > 100
 * }
 * <p>当输入参数为keyword="FOO"时，实际产生的SQL语句为<p>
 * {@code
 *    select * from FOO where id > 100 
 * }<br>
 * 也就是说，在SQL语句中的表名、列名、Join条件等非绑定变量，也可以动态的拼接到SQL语句当中。<br>
 * 参见 {@link #SQL}
 */
public enum JpqlDataType {
	/**
	 * java.sql.Date类型的SQL参数
	 */
	DATE,
	/**
	 * java.sql.Timestamp类型的SQL参数
	 */
	TIMESTAMP,
	/**
	 * int类型的SQL参数
	 */
	INT,
	/**
	 * string类型的SQL参数
	 */
	STRING,
	/**
	 * long类型的SQL参数
	 */
	LONG,
	/**
	 * short类型的SQL参数
	 */
	SHORT,
	/**
	 * Float类型的sql参数
	 */
	FLOAT,
	/**
	 * Bouble类型的SQL参数
	 */
	DOUBLE,
	/**
	 * Boolean类型的SQL参数
	 */
	BOOLEAN,
	/**
	 * SQL表达式类型的参数，传入参数必须是合法的SQL表达式。
	 * <h3>示例1</h3>
	 * {@code
	 *    select * from foo where name like :keyword<sql> 
	 * }
	 * <p>当输入参数为keyword="'Sm%ith'"时，实际产生的SQL语句为<p>
	 * {@code
	 *    select * from foo where name like 'Sm%ith' 
	 * }
	 * 
	 * <h3>示例2</h3>
	 * {@code
	 *    select from :tablename<sql>  where id > 100
	 * }
	 * <p>当输入参数为keyword="FOO"时，实际产生的SQL语句为<p>
	 * {@code
	 *    select * from FOO where id > 100 
	 * }
	 * 也就是说，在SQL语句中的表名、列名、Join条件等非绑定变量，也可以动态的拼接到SQL语句当中。<br>
	 * 这个特性让命名查询可以将固定不变的SQL部分确定下来，进一步提高了框架的灵活性。
	 * 
	 */
	SQL,
	/**
	 * String类型的参数，并且会自动在String后面增加%,形成 'string%'这样的表达式。<br>
	 * 一般用与描述Like条件。
	 * <h3>示例</h3>
	 * {@code
	 *    select * from foo where name like :keyword<string$> 
	 * }
	 * <p>当输入参数为keyword="Smith"时，实际产生的SQL语句为<p>
	 * {@code
	 *    select * from foo where name like 'Smith%' 
	 * }
	 */
	STRING$,
	/**
	 * String类型的参数，并且会自动在String前面和后面增加%,形成 '%string%'这样的表达式。<br>
	 * 一般用与描述Like条件。
	 * <h3>示例</h3>
	 * {@code
	 *    select * from foo where name like :keyword<$string$> 
	 * }
	 * <p>当输入参数为keyword="Smith"时，实际产生的SQL语句为<p>
	 * {@code
	 *    select * from foo where name like '%Smith%' 
	 * }
	 */
	$STRING$,
	/**
	 * String类型的参数，并且会自动在String前面增加%,形成 '%string'这样的表达式。<br>
	 * 一般用与描述Like条件。
	 * <h3>示例</h3>
	 * {@code
	 *    select * from foo where name like :keyword<$string> 
	 * }
	 * <p>当输入参数为keyword="Smith"时，实际产生的SQL语句为<p>
	 * {@code
	 *    select * from foo where name like '%Smith' 
	 * }
	 */
	$STRING
}

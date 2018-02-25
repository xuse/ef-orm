package jef.tools;

import java.io.File;
import java.util.function.Supplier;

/**
 * 使用装饰者模式对FileName进行修改。 实现了Supplier<String>接口。使用{@linkplain #get()}得到文件名。
 * 所有FileName实现均为不可变对象。
 *
 * 
 * @author Administrator
 *
 */
public interface FileName extends Supplier<String> {

	String getMainPart();

	String getExtPart();

	public static FileName valueOf(String name) {
		return new FN(name);
	}

	/**
	 * 将文件名拆成名称和扩展名两部分
	 * 
	 * @param name
	 * @return
	 */
	public static String[] splitExt(String name) {
		int n = name.lastIndexOf('.');
		if (n == -1) {
			return new String[] { name, "" };
		} else {
			return new String[] { name.substring(0, n), name.substring(n + 1).toLowerCase() };
		}
	}

	@Override
	default String get() {
		String ext = getExtPart();
		if (StringUtils.isEmpty(ext)) {
			return getMainPart();
		}
		return getMainPart() + "." + ext;
	}

	default boolean existsInDirectory(File directory) {
		return new File(directory, get()).exists();
	}

	default File asFileInDirectory(File directory) {
		return new File(directory, get());
	}

	/**
	 * 得到扩展名
	 * 
	 * @return 总是小写
	 */
	default String getExt() {
		return getExtPart().toLowerCase();
	}

	/**
	 * 在文件名的主体部分（不含扩展名）后面添加文字
	 * 
	 * @param text
	 * @return
	 */
	default FileName append(String text) {
		return new AppendMain(this, text);
	}

	/**
	 * 在文件名前面添加文字
	 * 
	 * @param text
	 * @return
	 */
	default FileName addBefore(String text) {
		return new AddBefore(this, text);
	}

	/**
	 * 在文件名的主体部分（不含扩展名）中查找替换
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	default FileName replace(String from, String to) {
		return new Replace(this, from, to);
	}

	/**
	 * 替换扩展名
	 * 
	 * @param ext
	 * @return
	 */
	default FileName asExt(String ext) {
		return new ExtPart(this, ext);
	}

	/**
	 * 替换文件名主体部分
	 * 
	 * @param main
	 * @return
	 */
	default FileName asMain(String main) {
		return new MainPart(this, main);
	}

	static final class Replace implements FileName {
		final private FileName raw;
		final private String from;
		final private String to;

		Replace(FileName raw, String from, String to) {
			this.raw = raw;
			this.from = from;
			this.to = to;
		}

		@Override
		public String getMainPart() {
			return raw.getMainPart().replace(from, to);
		}

		@Override
		public String getExtPart() {
			return raw.getExtPart();
		}
	}

	static final class AppendMain implements FileName {
		final private FileName raw;
		final private String append;

		AppendMain(FileName raw, String append) {
			this.raw = raw;
			this.append = append;
		}

		@Override
		public String getMainPart() {
			return raw.getMainPart() + append;
		}

		@Override
		public String getExtPart() {
			return raw.getExtPart();
		}
	}

	static final class AddBefore implements FileName {
		final private FileName raw;
		final private String append;

		AddBefore(FileName raw, String append) {
			this.raw = raw;
			this.append = append;
		}

		@Override
		public String getMainPart() {
			return append + raw.getMainPart();
		}

		@Override
		public String getExtPart() {
			return raw.getExtPart();
		}
	}

	static final class ExtPart implements FileName {
		final private FileName raw;
		final private String ext;

		ExtPart(FileName raw, String ext) {
			this.raw = raw;
			this.ext = ext;
		}

		@Override
		public String getMainPart() {
			return raw.getMainPart();
		}

		@Override
		public String getExtPart() {
			return ext;
		}
	}

	static final class MainPart implements FileName {
		final private FileName raw;
		final private String main;

		MainPart(FileName raw, String ext) {
			this.raw = raw;
			this.main = ext;
		}

		@Override
		public String getMainPart() {
			return main;
		}

		@Override
		public String getExtPart() {
			return raw.getExtPart();
		}
	}

	static final class FN implements FileName {
		/**
		 * 文件名
		 */
		final private String name;
		/**
		 * 扩展名前的.的位置
		 */
		final private int index;

		/**
		 * 构造
		 * 
		 * @param name
		 */
		public FN(String name) {
			this.name = name;
			int index = name.lastIndexOf('.');
			if (index == -1)
				index = name.length();
			this.index = index;
		}

		/**
		 * 得到文件名主体部分
		 * 
		 * @return
		 */
		public String getMainPart() {
			return name.substring(0, index);
		}

		/**
		 * 得到原始扩展名，包含点，并且保留原始大小写
		 */
		public String getExtPart() {
			return name.substring(index);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public String toString() {
			return name;
		}

		public String get() {
			return this.name;
		}
	}
}

package jef.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * 总是被继承，用于描述对一个文本文件的修改处理步骤。 通过继承这个类可以形成对文本文件处理的详细控制。
 * 
 * @author Jiyi
 * @since 1.0
 * @Date 2011-5-10
 */
public class TextFileCallback {
	Throwable lastException;
	protected File sourceFile;

	private String sourceCharset;
	private String tCharset;
	private Dealwith dealwith = Dealwith.NONE;

	public TextFileCallback() {
	}

	public TextFileCallback(String sourceCharset) {
		this.sourceCharset = sourceCharset;
	}

	public TextFileCallback(String sourceCharset, String targetCharset, Dealwith dealwith) {
		this.sourceCharset = sourceCharset;
		this.tCharset = targetCharset;
		this.dealwith = dealwith;
	}

	/**
	 * 指定输出文件，如果返回null，将不产生输出文件<br>
	 * 阶段：在一个文件处理开始前<br>
	 * 影响：控制输出文件路径，也可以不输出文件<br>
	 * 
	 * @param source
	 * @return 输出文件file，返回null则不输出文件，但行处理依然进行。
	 */
	protected File getTarget(File source) {
		if(dealwith==Dealwith.NO_OUTPUT)return null;
		return new File(source.getPath().concat(".tmp"));
	}

	/**
	 * 处理每行，返回null表示删除此行<br>
	 * 阶段：在处理过程中，每行都执行<br>
	 * 影响：控制输出文件的内容<br>
	 * 
	 * @param line
	 * @return
	 */
	protected String processLine(String line){
		return line;
	}

	/**
	 * 控制是否换行<br>
	 * 阶段：在每行的{@link #processLine(String)}方法执行完成后<br>
	 * 影响： 控制在刚才{@link #processLine(String)}返回的内容后要不要换行。<br>
	 * 
	 * @return
	 */
	protected boolean wrapLine() {
		return true;
	}

	/**
	 * 询问是否要中断处理过程<br>
	 * 阶段：在每行处理完成之后<br>
	 * 影响：如果返回true，就可以中断处理，即源文件的后续行不再读取。用于中断大文件的处理。
	 * 
	 * @return
	 */
	protected boolean breakProcess() {
		return lastException != null;
	}

	/**
	 * 指定输出文件的字符集，默认和输入文件相同<br>
	 * 阶段：在文件处理开始前，getTarget方法之前执行。<br>
	 * 影响：控制输出文件的编码，输出null表示输出文件和输入文件编码一致<br>
	 * 
	 * @return
	 */
	protected String targetCharset() {
		return tCharset==null?sourceCharset:tCharset;
	};

	/**
	 * 返回源文件读取编码，null表示默认
	 * @param source
	 * @return
	 */
	protected String sourceCharset(File source) {
		return sourceCharset;
	}

	/**
	 * 在处理文件每行内容之前触发，用于在处理前操作输出目标<br>
	 * 阶段：在输出文件确认之后，正式处理每一行之前<br>
	 * 影响：允许在处理源文件的每行之前，在输出文件中添加内容<br>
	 * 
	 * @param w
	 * @throws IOException
	 */
	protected void beforeProcess(File source, File target, BufferedWriter w) throws IOException {
	};

	/**
	 * 允许继承类在处理后操作输出目标<br>
	 * 阶段：在输入文件行处理完后，或者中断后触发。每个文件触发一次<br>
	 * 影响：允许在输出文件关闭之前，在文件中添加内容。<br>
	 * 
	 * @param w
	 * @throws IOException
	 */
	protected void afterProcess(File source, File target, BufferedWriter w) throws IOException {
	}

	/**
	 * 询问是否处理成功，如果返回false，则不修改和删除源文件，并删除临时文件<br>
	 * 阶段：在一个文件处理完成后<br>
	 * 影响：如果成功，会触发删除或替换源文件操作，如果失败会删除错误的输出文件。<br>
	 * 
	 * @return
	 */
	public boolean isSuccess() {
		return true;
	}

	/**
	 * 是否替换源文件<br>
	 * 阶段: 操作成功后(参见 {@link #isSuccess})<br>
	 * 影响：控制是否替换源文件
	 * 
	 * @return
	 */
	protected Dealwith dealwithSourceOnSuccess(File source) {
		return dealwith;
	}

	/**
	 * 在文件开始处理前判断要不要处理
	 * @param source
	 * @return
	 */
	protected boolean accept(File source) {
		return true;
	}
	
	/**
	 * 在文件开始处理前询是否要debug输出
	 * @param source
	 * @return
	 */
	protected boolean debug(File source){
		return false;
	}
	
	public static enum Dealwith {
		DELETE, REPLACE, BACKUP_REPLACE, NONE,NO_OUTPUT
	}


}

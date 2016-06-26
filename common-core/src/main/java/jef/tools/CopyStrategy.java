package jef.tools;

import java.io.File;

/**
 * 描述文件夹拷贝策略，除了默认提供的几个策略外，一般供应用继承。
 * 
 * @Date 2011-5-9
 */
public class CopyStrategy {
	/**
	 * 复制策略，不覆盖已存在的文件（默认） 1.跳过.开头的文件夹和文件 2.不覆盖已存在的文件
	 */
	public static final CopyStrategy NO_OVERWRITE = new CopyStrategy();
	/**
	 * 复制策略，总是覆盖已存在的文件： 1.跳过.开头的文件夹和文件 2.总是覆盖已存在的文件
	 */
	public static final CopyStrategy ALLWAYS_OVERWRITE = new CopyStrategy() {
		public boolean canOverWritten(File file, File target) {
			return true;
		}
	};
	/**
	 * 复制策略，当文件内容不同时 覆盖已存在的文件： 1.跳过.开头的文件夹和文件 2.当文件不同时覆盖已存在的文件(根据文件大小和CRC判断)
	 */
	public static final CopyStrategy OVERWRITE_IF_DIFFERENT = new CopyStrategy() {
		private FileComparator comparator=FileComparator.LENGTH_SKIP;
		
		public boolean canOverWritten(File file, File target) {
			if (!file.exists())
				return false;
			if (!target.exists())
				return true;
			return !comparator.equals(file,target);
		}
	};

	/**
	 * 供子类覆盖，描述拷贝行为: 是否拷贝该文件夹的内容
	 * 
	 * @param file
	 *            源文件夹
	 * @param newFile
	 *            目标文件夹
	 * @return true表示该文件夹需要拷贝，false表示不拷贝
	 */
	public boolean processFolder(File file, File newFile) {
		if (file.getName().startsWith("."))
			return false;
		return true;
	}

	/**
	 * 供子类覆盖，描述拷贝行为: 给出目标文件名
	 * 
	 * @param file
	 *            源文件
	 * @param targetFolder
	 *            目标文件夹
	 * @return File，要拷贝的目标文件，null将不拷贝源文件。
	 */
	public File getTargetFile(File file, File targetFolder) {
		File target= new File(targetFolder, file.getName());
		return target;
	}

	/**
	 * 供子类覆盖，描述拷贝行为: 目标文件已经存在时，可否覆盖
	 * 
	 * @param source
	 *            源文件
	 * @param target
	 *            目标文件，注意一定是文件
	 * @return true表示允许覆盖目标文件,false反之
	 */
	public boolean canOverWritten(File source, File target) {
		return false;
	}
	
	/**
	 * 如果源是一个文件而目标是一个目录，是否允许将源复制到目录下。
	 * 
	 * @return true表示允许；false表示不允许，此时将抛出异常。
	 */
	public boolean allowFileIntoFolder() {
		return true;
	}

	/**
	 * 返回true，表示是移动文件，源文件将被删除
	 * @return
	 */
	public boolean isMove() {
		return false;
	}

	/**
	 * 该策略用于合并两个文件夹。
	 * <li>源目录有而目标目录没有的文件，移动/复制到目标目录</li>
	 * <li>两边相同的文件，删除源目录下的（move模式下）</li>
	 * <li>两边不同的文件，不作操作，打印出文件名</li>
	 */
	public static class MergeStrategy extends CopyStrategy {
		private FileComparator comparator;
		private int normalCount;		//不冲突的文件数，（在源有而目标没有的文件）复制。
		private int duplicateCount;	    //两边完全相同的文件数。
		private int conflictCount;		//两边冲突的文件数
		private int deleteSuccessCount;//在两边完全相同时，如果是move指令，则会删除源文件夹中的重复文件，这里记录成功删除的数量
		private int mergeFailure;      //在源是目录而目标是文件，或者源为文件目标为目录，此时无法实现合并
		
		private boolean isMove=true;

		public MergeStrategy() {
			this(FileComparator.LENGTH_SKIP);
		}
		public MergeStrategy(FileComparator compare) {
			super();
			this.comparator = compare;
		}

		/**
		 * 设置move模式
		 * @param isMove
		 */
		public void setMove(boolean isMove) {
			this.isMove = isMove;
		}
		@Override
		public File getTargetFile(File file, File targetFolder) {
			File f = new File(targetFolder, file.getName());
			if (f.isFile()) {
				if(file.isDirectory()){
					System.out.println("the target " + f.getPath() + " has exist, and is not folder.");
					mergeFailure++;
					return null;
				}
				if (comparator.equals(file, f)) { //源于目标相同文件
					duplicateCount++;
					if(isMove){					//Move的场合直接删除源文件。
						if (file.delete()) {
							deleteSuccessCount++;
						} else {
							System.out.println("删除" + file.getAbsolutePath() + "失败.");
						}	
					}
					return null;//该文件无需复制
				} else {						//源和目标不同文件
					System.out.println("文件" + file.getAbsolutePath() + " 已经存在不同版本！！(内容不同)");
					conflictCount++;
					return null;//版本不同，不覆盖
				}
			}else if (f.exists() && file.isFile()){
				System.out.println("Target is directory and source is a file:"+file.getAbsolutePath());
				mergeFailure++;
				return null;
			}else{
				normalCount++;
			}
			return f;
		}

		@Override
		public boolean isMove() {
			return isMove;
		}

		public boolean canOverWritten(File file, File target) {
			throw new IllegalArgumentException();
		}
		/**
		 * 不冲突的文件数（在源有而目标没有的文件）
		 * @return
		 */
		public int getNormalCount() {
			return normalCount;
		}
		/**
		 * 
		 * @return 两边完全相同的文件数。
		 */
		public int getDuplicateCount() {
			return duplicateCount;
		}
		/**
		 * @return 两边冲突的文件数
		 */
		public int getConflictCount() {
			return conflictCount;
		}
		/**
		 * 在两边完全相同时，如果是move指令，成功删除的文件数
		 * @return
		 */
		public int getDeleteSuccessCount() {
			return deleteSuccessCount;
		}
		public String getSummary(){
			StringBuilder sb=new StringBuilder();
			sb.append(isMove?"Moved:":"Copyed:").append(this.normalCount).append("\r\n");
			sb.append("Duplicate:").append(this.duplicateCount);
			if(isMove){
				sb.append(" Deleted:").append(this.deleteSuccessCount);
			}
			sb.append(" MergeFailure:").append(this.mergeFailure);
			sb.append("\r\n");
			sb.append("ConflictFiles:").append(this.conflictCount);
			return sb.toString();
		}
	};
}

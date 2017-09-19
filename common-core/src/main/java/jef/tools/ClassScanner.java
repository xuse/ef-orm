package jef.tools;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jef.tools.reflect.ClassLoaderUtil;
import jef.tools.resource.IResource;

/**
 * 扫描指定包（包括jar）下的class文件 <br>
 * 
 * @author jiyi
 */
public class ClassScanner {
    /**
     * 是否排除内部类
     */
    private boolean excludeInnerClass = true;

    /**
     * class path根路径，如果不指定那么就在所有ClassPath下寻找
     * 
     * @return
     */
    private URL rootClasspath;

    public boolean isExcludeInnerClass() {
        return excludeInnerClass;
    }

    public ClassScanner excludeInnerClass(boolean excludeInnerClass) {
        this.excludeInnerClass = excludeInnerClass;
        return this;
    }

    public ClassScanner rootClasspath(URL rootClasspath) {
        this.rootClasspath = rootClasspath;
        return this;
    }

    /**
     * 扫描包
     * 
     * @param basePackage
     *            基础包
     * @param recursive
     *            是否递归搜索子包
     * @return Set
     */
    public IResource[] scan(String... packages) {
     // 这里设置父classloader为null
        URLClassLoader cl = rootClasspath == null ? null : new URLClassLoader(new URL[] { rootClasspath }, null);

        boolean scanAll = packages.length == 0;
        if (!scanAll) {
            for (String pkg : packages) {
                if (StringUtils.isEmpty(pkg)) {
                    scanAll = true;
                    break;
                }
            }
        }
        String prifix = rootClasspath == null ? "classpath*:" : "classpath:";
        if (scanAll) {
            return ResourceUtils.findResources(cl, prifix + "**/*.class", excludeInnerClass);
        }
        // Scan multiple packages.
        Set<IResource> result = new HashSet<IResource>();
        for (String packageName : packages) {
            if (StringUtils.isBlank(packageName)) {
                continue;
            }
            String keystr = prifix + packageName.replace('.', '/') + "/**/*.class";
            IResource[] res = ResourceUtils.findResources(cl, keystr, excludeInnerClass);
            if (packages.length == 1) {
                return res;
            }
            result.addAll(Arrays.asList(res));
        }
        IOUtils.closeQuietly(cl);
        return result.toArray(new IResource[result.size()]);
    }

    /**
     * 用相同类的一个已经加载的类来设置要搜索的classpath
     * 
     * @param rootCls
     */
    public void setRootBySameUrlClass(Class<?> rootCls) {
        if (rootCls == null)
            return;
        this.rootClasspath = ClassLoaderUtil.getCodeSource(rootCls);
        if (rootClasspath == null)
            rootClasspath = rootCls.getResource("/");
    }

    public static IResource[] listClassNameInPackage(URL root, String[] pkgNames, boolean includeInner) {
        ClassScanner cs = new ClassScanner().excludeInnerClass(!includeInner);
        cs.rootClasspath(root);
        return cs.scan(pkgNames);
    }

}

package jef.accelerator.bean;

public final class ClassLoaderAccessor extends ClassLoader {
	public ClassLoaderAccessor(ClassLoader parent) {
		super(parent);
	}

	public ClassLoaderAccessor(String name, ClassLoader parent) {
		super(name, parent);
	}

	public Class<?> defineClz(String name, byte[] b) {
		return super.defineClass(name, b, 0, b.length);
	}
}

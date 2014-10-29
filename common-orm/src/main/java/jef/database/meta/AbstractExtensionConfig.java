package jef.database.meta;


public abstract class AbstractExtensionConfig implements ExtensionConfig {
	protected String name;
	protected MetadataAdapter parent;
	private TupleMetadata extension;

	protected AbstractExtensionConfig(String name, MetadataAdapter parent) {
		this.name = name;
		this.parent = parent;
		init();
	}

	private void init() {
		if (extension == null) {
			extension = MetaHolder.getDynamicMeta(name);
			if (extension == null) {
				throw new IllegalArgumentException("Can not found extension definition [" + name + "] for " + this.parent.getName());
			}
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public TupleMetadata getExtensionMeta() {
		return extension;
	}

	private MetadataAdapter mergedMeta;

	@Override
	public MetadataAdapter getMeta() {
		if (mergedMeta == null) {
			mergedMeta = merge();
		}
		return mergedMeta;
	}

	protected abstract MetadataAdapter merge();

	@Override
	public void flush(DynamicMetadata meta) {
		mergedMeta = null;
	}
}

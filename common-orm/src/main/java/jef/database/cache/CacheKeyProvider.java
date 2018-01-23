package jef.database.cache;

/**
 * CacheKey的提供者
 * @author jiyi
 *
 */
@FunctionalInterface
public interface CacheKeyProvider {
	CacheKey getCacheKey();
}

/*
 *    Copyright 2009-2014 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * SPI for cache providers.
 * <p>
 * One instance of cache will be created for each namespace.
 * <p>
 * The cache implementation must have a constructor that receives the cache id as an String parameter.
 * <p>
 * MyBatis will pass the namespace as id to the constructor.
 *
 * <pre>
 * public MyCache(final String id) {
 *  if (id == null) {
 *    throw new IllegalArgumentException("Cache instances require an ID");
 *  }
 *  this.id = id;
 *  initialize();
 * }
 * </pre>
 *
 * @author Clinton Begin
 */

public interface Cache {

    /**
     * 获取缓存对象的唯一标识
     *
     * @return The identifier of this cache
     */
    String getId();

    /**
     * key可以是任何对象，但一般是CacheKey对象
     * value是查询结果，为List类型
     *
     * @param key   Can be any object but usually it is a {@link CacheKey}
     * @param value The result of a select.
     */
    void putObject(Object key, Object value);

    /**
     * 从缓存对象中获取key对应的value
     *
     * @param key The key
     * @return The object stored in the cache.
     */
    Object getObject(Object key);

    /**
     * Optional. It is not called by the core.
     * 可选的方法，没有被核心框架调用，移除key对应的value
     *
     * @param key The key
     * @return The object that was removed
     */
    Object removeObject(Object key);

    /**
     * 清空缓存
     * Clears this cache instance
     */
    void clear();

    /**
     * 获取缓存对象中存储的键/值对的数量,可选的方法，没有被框架核心调用
     * Optional. This method is not called by the core.
     *
     * @return The number of elements stored in the cache (not its capacity).
     */
    int getSize();

    /**
     * 获取读写锁,可选的方法，从3.2.6起这个方法不再被框架核心调用,任何需要的锁，都必须由缓存供应商提供
     * Optional. As of 3.2.6 this method is no longer called by the core.
     * <p>
     * Any locking needed by the cache must be provided internally by the cache provider.
     *
     * @return A ReadWriteLock
     */
    ReadWriteLock getReadWriteLock();

}
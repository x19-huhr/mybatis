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
package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * Lru (first in, first out) cache decorator
 * 最近最少使用算法，缓存回收策略
 *
 * @author Clinton Begin
 */
public class LruCache implements Cache {

    /**
     * 代理的缓存对象
     */
    private final Cache delegate;
    /**
     * 此Map的key和value都是要添加的键值对的键
     */
    private Map<Object, Object> keyMap;
    /**
     * 最老的key
     */
    private Object eldestKey;

    public LruCache(Cache delegate) {
        this.delegate = delegate;
        setSize(1024);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    /**
     * 在每次调用setSize方法时，都会创建一个新的该类型的对象，同时指定其容量大小。
     * 第三个参数为true代表Map中的键值对列表要按照访问顺序排序，每次被方位的键值对都会被移动到列表尾部（值为false时按照插入顺序排序）。
     *
     * @param size
     */
    public void setSize(final int size) {
        keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
            private static final long serialVersionUID = 4267176411845948333L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
                boolean tooBig = size() > size;
                if (tooBig) {
                    eldestKey = eldest.getKey();
                }
                return tooBig;
            }
        };
    }

    /**
     * 只是修改了缓存的添加方式
     *
     * @param key   Can be any object but usually it is a {@link CacheKey}
     * @param value The result of a select.
     */
    @Override
    public void putObject(Object key, Object value) {
        delegate.putObject(key, value);
        cycleKeyList(key);
    }

    @Override
    public Object getObject(Object key) {
        keyMap.get(key); //touch
        return delegate.getObject(key);
    }

    @Override
    public Object removeObject(Object key) {
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        delegate.clear();
        keyMap.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    private void cycleKeyList(Object key) {
        // 把刚刚给代理缓存对象中添加的key，同时添加到keyMap中
        keyMap.put(key, key);
        // 如果eldestKey不为null，则代表keyMap内部删除了eldestKey这个key
        if (eldestKey != null) {
            // 同样把代理缓存对象中key为eldestKey的键值对删除即可
            delegate.removeObject(eldestKey);
            eldestKey = null;
        }
    }

}

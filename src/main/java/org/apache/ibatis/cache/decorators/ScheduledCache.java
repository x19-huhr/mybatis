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

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 调度缓存，负责定时清空缓存
 *
 * @author Clinton Begin
 */
public class ScheduledCache implements Cache {

    private Cache delegate;
    /**
     * 调用clear()清空缓存的时间间隔，单位毫秒，默认1小时
     */
    protected long clearInterval;
    /**
     * 最后一次清空缓存的时间，单位毫秒
     */
    protected long lastClear;

    public ScheduledCache(Cache delegate) {
        this.delegate = delegate;
        this.clearInterval = 60 * 60 * 1000; // 1 hour
        this.lastClear = System.currentTimeMillis();
    }

    public void setClearInterval(long clearInterval) {
        this.clearInterval = clearInterval;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        clearWhenStale();
        return delegate.getSize();
    }

    @Override
    public void putObject(Object key, Object object) {
        clearWhenStale();
        delegate.putObject(key, object);
    }

    @Override
    public Object getObject(Object key) {
        if (clearWhenStale()) {
            return null;
        } else {
            return delegate.getObject(key);
        }
    }

    @Override
    public Object removeObject(Object key) {
        clearWhenStale();
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        lastClear = System.currentTimeMillis();
        delegate.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    /**
     * 当缓存过期时调用clear方法进行清空
     *
     * @return 成功清理返回true，否则返回false
     */
    private boolean clearWhenStale() {
        // 如果当前时间-最后一次清空时间>指定的时间间隔，则调用clear()进行清空
        if (System.currentTimeMillis() - lastClear > clearInterval) {
            clear();
            return true;
        }
        return false;
    }

}

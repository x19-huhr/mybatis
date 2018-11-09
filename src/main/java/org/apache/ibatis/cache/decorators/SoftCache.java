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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * Soft Reference cache decorator
 * Thanks to Dr. Heinz Kabutz for his guidance here.
 * 基于软引用实现的缓存管理策略,借助垃圾回收器进行缓存对象的回收
 * 如：Object obj = new Object();只要引用变量obj不为null，那么new出来的Object对象永远不会被垃圾回收器回收
 *
 * @author Clinton Begin
 */
public class SoftCache implements Cache {
    /**
     * 强引用集合，最近一此查询命中的对象，其引用会被加入此集合的头部
     * 集合采取先进先出策略，当长度超出指定size时，删除尾部元素
     */
    private final LinkedList<Object> hardLinksToAvoidGarbageCollection;
    /**
     * 此队列保存被垃圾回收器回收的对象所在的Reference对象
     * 垃圾回收器在进行内存回收时，会把Reference对象内的引用变量置为null，同时将Reference对象加入队列中
     */
    private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;
    /**
     * 缓存对象
     */
    private final Cache delegate;
    /**
     * 强引用集合长度限制，可通过setSize方法设置，默认为256
     */
    private int numberOfHardLinks;

    public SoftCache(Cache delegate) {
        this.delegate = delegate;
        this.numberOfHardLinks = 256;
        this.hardLinksToAvoidGarbageCollection = new LinkedList<Object>();
        this.queueOfGarbageCollectedEntries = new ReferenceQueue<Object>();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        removeGarbageCollectedItems();
        return delegate.getSize();
    }


    public void setSize(int size) {
        this.numberOfHardLinks = size;
    }

    @Override
    public void putObject(Object key, Object value) {
        // 删除已经被垃圾回收器回收的key/value
        removeGarbageCollectedItems();
        delegate.putObject(key, new SoftEntry(key, value, queueOfGarbageCollectedEntries));
    }

    /**
     * SoftCache在读缓存时，是直接读取的，
     * 这样存在一个问题：缓存的value对象已经被垃圾回收器回收，但是该对象的软引用对象还存在，这种情况下要删除缓存对象中，软引用对象对应的key。
     * 另外，每次调用getObject查询到缓存对象中的value还未被回收时，会把此对象的引用临时加入强引用集合，确保该对象不会被回收。
     * 这种机制保证了访问频次越搞的value对象，被回收的几率越小。
     *
     * @param key The key
     * @return
     */
    @Override
    public Object getObject(Object key) {
        Object result = null;
        @SuppressWarnings("unchecked") // assumed delegate cache is totally managed by this cache
                SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
        // 引用变量为null，说明不存在key指定的键值对
        if (softReference != null) {
            // 键值对存在的情况下，获取软引用变量引用的对象
            result = softReference.get();
            // 如果引用的value已经被回收，则删除缓存对象中的key
            if (result == null) {
                delegate.removeObject(key);
            } else {
                // See #586 (and #335) modifications need more than a read lock
                // 缓存的value对象没有被回收，且这次访问到了，则把此对象引用加入强引用集合
                // 使其不会被回收
                synchronized (hardLinksToAvoidGarbageCollection) {
                    hardLinksToAvoidGarbageCollection.addFirst(result);
                    if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
                        hardLinksToAvoidGarbageCollection.removeLast();
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Object removeObject(Object key) {
        removeGarbageCollectedItems();
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        synchronized (hardLinksToAvoidGarbageCollection) {
            hardLinksToAvoidGarbageCollection.clear();
        }
        removeGarbageCollectedItems();
        delegate.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    /**
     * 从缓存对象中删除已经被垃圾回收器回收的value对象对应的key
     */
    private void removeGarbageCollectedItems() {
        SoftEntry sv;
        while ((sv = (SoftEntry) queueOfGarbageCollectedEntries.poll()) != null) {
            delegate.removeObject(sv.key);
        }
    }

    private static class SoftEntry extends SoftReference<Object> {
        private final Object key;

        private SoftEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
            super(value, garbageCollectionQueue);
            this.key = key;
        }
    }

}
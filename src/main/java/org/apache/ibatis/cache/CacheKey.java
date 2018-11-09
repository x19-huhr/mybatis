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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Cache缓存对象的key
 * 在HashMap中， 通过hashCode()和equals()标识key的唯一性
 * 计算公式：hashcode = multiplier * hashcode + object.hashCode()*count
 *
 * @author Administrator
 */
public class CacheKey implements Cloneable, Serializable {

    private static final long serialVersionUID = 1146682552656046210L;

    public static final CacheKey NULL_CACHE_KEY = new NullCacheKey();

    private static final int DEFAULT_MULTIPLYER = 37;
    private static final int DEFAULT_HASHCODE = 17;

    /**
     * 乘数，固定初始值质数37，不会变
     */
    private int multiplier;
    /**
     * 当前hashCode值，初始值是质数17，计算公式：hashcode=hashcode * multiplier  + object.hashCode()*count
     */
    private int hashcode;
    /**
     * 所有更新对象的初始hashCode的和
     */
    private long checksum;
    /**
     * 更新的对象总数
     */
    private int count;
    /**
     * 更新的对象集合
     */
    private List<Object> updateList;

    public CacheKey() {
        this.hashcode = DEFAULT_HASHCODE;
        this.multiplier = DEFAULT_MULTIPLYER;
        this.count = 0;
        this.updateList = new ArrayList<Object>();
    }

    public CacheKey(Object[] objects) {
        this();
        updateAll(objects);
    }

    public int getUpdateCount() {
        return updateList.size();
    }

    public void update(Object object) {
        if (object != null && object.getClass().isArray()) {
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(object, i);
                doUpdate(element);
            }
        } else {
            doUpdate(object);
        }
    }

    private void doUpdate(Object object) {
        // 如果object为null则hashCode为1，不为null则调用hashCode()获取
        int baseHashCode = object == null ? 1 : object.hashCode();
        // count+1
        count++;
        // checksum+此对象hashCode
        checksum += baseHashCode;
        baseHashCode *= count;
        // 重新计算hashCode
        hashcode = multiplier * hashcode + baseHashCode;
        // 更新列表中添加此对象
        updateList.add(object);
    }

    public void updateAll(Object[] objects) {
        for (Object o : objects) {
            update(o);
        }
    }

    /**
     * 对比顺序hashCode-->checksum-->count-->updateList
     *
     * @param object
     * @return
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof CacheKey)) return false;

        final CacheKey cacheKey = (CacheKey) object;

        if (hashcode != cacheKey.hashcode) return false;
        if (checksum != cacheKey.checksum) return false;
        if (count != cacheKey.count) return false;

        for (int i = 0; i < updateList.size(); i++) {
            Object thisObject = updateList.get(i);
            Object thatObject = cacheKey.updateList.get(i);
            if (thisObject == null) {
                if (thatObject != null) return false;
            } else {
                if (!thisObject.equals(thatObject)) return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String toString() {
        StringBuilder returnValue = new StringBuilder().append(hashcode).append(':').append(checksum);
        for (int i = 0; i < updateList.size(); i++) {
            returnValue.append(':').append(updateList.get(i));
        }

        return returnValue.toString();
    }

    @Override
    public CacheKey clone() throws CloneNotSupportedException {
        CacheKey clonedCacheKey = (CacheKey) super.clone();
        clonedCacheKey.updateList = new ArrayList<Object>(updateList);
        return clonedCacheKey;
    }

}

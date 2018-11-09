/*
 *    Copyright 2009-2012 the original author or authors.
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
package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * @author Clinton Begin
 */
public class PropertyNamer {

    public static String methodToProperty(String name) {
        //根据java常用语法规则将一个函数转化为属性，如果参数不符合java常用语法规则将会抛出ReflectionException
        if (name.startsWith("is")) {
            name = name.substring(2);
        } else if (name.startsWith("get") || name.startsWith("set")) {
            name = name.substring(3);
        } else {
            throw new ReflectionException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
        }

        //对于这个判断为什么这么写，没有彻底弄明白。也许是对于字符串长度大于1且全为大写的数据不做处理吧
        if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
            name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
        }

        return name;
    }

    /**
     * 根据传入的参数判断这个参数是不是应包含属性
     * 判断的依据是这个参数是不是以get|set|is开头的。但这个函数的判断依据是比较简单的，这一个必然条件。
     * 也就是说如果这个函数返回false，则这个参数肯定部包含属性；反之，如果这个函数返回true,则只能说明这个参数可能包含属性
     *
     * @param name
     * @return boolean
     */
    public static boolean isProperty(String name) {
        return name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
    }

    public static boolean isGetter(String name) {
        return name.startsWith("get") || name.startsWith("is");
    }

    public static boolean isSetter(String name) {
        return name.startsWith("set");
    }

}

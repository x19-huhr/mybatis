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
package org.apache.ibatis.parsing;

import java.util.Properties;

/**
 * @author Clinton Begin
 */
public class PropertyParser {

    public static String parse(String string, Properties variables) {
        //先初始化一个handler
        VariableTokenHandler handler = new VariableTokenHandler(variables);
        //在初始化GenericTokenParser对象，设置openToken为${,endToken为}
        //有没有对${}比较熟悉，这个符号就是mybatis配置文件中的占位符，例如定义datasource时用到的 <property name="driverClassName" value="${driver}" />
        //同时也可以解释在VariableTokenHandler中的handleToken时，如果content在properties中不存在时，返回的内容要加上${}了。
        GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
        return parser.parse(string);
    }

    private static class VariableTokenHandler implements TokenHandler {
        private Properties variables;

        public VariableTokenHandler(Properties variables) {
            this.variables = variables;
        }

        @Override
        public String handleToken(String content) {
            if (variables != null && variables.containsKey(content)) {
                return variables.getProperty(content);
            }
            return "${" + content + "}";
        }
    }
}

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

/**
 * @author Clinton Begin
 */
public class GenericTokenParser {

    private final String openToken;
    private final String closeToken;
    private final TokenHandler handler;

    public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
        this.openToken = openToken;
        this.closeToken = closeToken;
        this.handler = handler;
    }

    public String parse(String text) {

        StringBuilder builder = new StringBuilder();
        if (text != null && text.length() > 0) {
            //将字符串转为字符数组
            char[] src = text.toCharArray();
            int offset = 0;
            //判断openToken在text中的位置，注意indexOf函数的返回值-1表示不存在，0表示在在开头的位置
            int start = text.indexOf(openToken, offset);
            while (start > -1) {
                if (start > 0 && src[start - 1] == '\\') {
                    //如果text中在openToken前存在转义符就将转义符去掉。如果openToken前存在转义符，start的值必然大于0，最小也为1
                    //因为此时openToken是不需要进行处理的，所以也不需要处理endToken。接着查找下一个openToken
                    builder.append(src, offset, start - 1).append(openToken);

                    offset = start + openToken.length();
                } else {
                    int end = text.indexOf(closeToken, start);
                    //如果不存在openToken，则直接将offset位置后的字符添加到builder中
                    if (end == -1) {
                        builder.append(src, offset, src.length - offset);

                        offset = src.length;
                    } else {
                        //添加openToken前offset后位置的字符到bulider中
                        builder.append(src, offset, start - offset);

                        offset = start + openToken.length();
                        //获取openToken和endToken位置间的字符串
                        String content = new String(src, offset, end - offset);
                        //调用handler进行处理
                        builder.append(handler.handleToken(content));

                        offset = end + closeToken.length();
                    }
                }

                //开始下一个循环
                start = text.indexOf(openToken, offset);
            }

            //只有当text中不存在openToken且text.length大于0时才会执行下面的语句
            if (offset < src.length) {
                builder.append(src, offset, src.length - offset);

            }
        }

        return builder.toString();
    }

}

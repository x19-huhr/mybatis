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
package org.apache.ibatis.scripting.xmltags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Clinton Begin
 */
public class XMLScriptBuilder extends BaseBuilder {

    private XNode context;
    private boolean isDynamic;
    private Class<?> parameterType;

    public XMLScriptBuilder(Configuration configuration, XNode context) {
        this(configuration, context, null);
    }

    public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType) {
        super(configuration);
        this.context = context;
        this.parameterType = parameterType;
    }

    public SqlSource parseScriptNode() {
        // 解析XNode成一系列SqlNode对象，并封装成MixedSqlNode对象，并会判断此SQL是否为动态
        List<SqlNode> contents = parseDynamicTags(context);
        MixedSqlNode rootSqlNode = new MixedSqlNode(contents);
        SqlSource sqlSource = null;
        if (isDynamic) {
            // 动态SQL则创建DynamicSqlSource
            sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
        } else {

            // 静态SQL则创建RawSqlSource
            sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
        }
        return sqlSource;
    }

    private List<SqlNode> parseDynamicTags(XNode node) {
        // node是我们要解析的SQL语句: <select resultType="org.apache.ibatis.domain.blog.Author" id="selectAllAuthors">select * from author</select>
        List<SqlNode> contents = new ArrayList<SqlNode>();
        // 获取SQL下面的子节点
        NodeList children = node.getNode().getChildNodes();
        // 遍历子节点，解析成对应的sqlNode类型，并添加到contents中
        for (int i = 0; i < children.getLength(); i++) {
            // 第一个child节点就是SQL中的文本数据：select * from author
            XNode child = node.newXNode(children.item(i));
            //如果是文本节点，则先解析成TextSqlNode对象
            if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE || child.getNode().getNodeType() == Node.TEXT_NODE) {
                // 获取文本信息
                String data = child.getStringBody("");
                // 创建TextSqlNode对象
                TextSqlNode textSqlNode = new TextSqlNode(data);
                // 判断是否是动态Sql，其过程会调用GenericTokenParser判断文本中是否含有"${"字符
                if (textSqlNode.isDynamic()) {
                    // 如果是动态SQL,则直接使用TextSqlNode类型，并将isDynamic标识置为true
                    contents.add(textSqlNode);
                    isDynamic = true;
                } else {
                    // 不是动态sql，则创建StaticTextSqlNode对象，表示静态SQL
                    contents.add(new StaticTextSqlNode(data));
                }

            } else if (child.getNode().getNodeType() == Node.ELEMENT_NODE) {
                // 其他类型的节点，由不同的节点处理器来对应处理成本成不同的SqlNode类型
                // issue #628
                String nodeName = child.getNode().getNodeName();
                NodeHandler handler = nodeHandlers.get(nodeName);
                if (handler == null) {
                    throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement.");
                }
                handler.handleNode(child, contents);
                isDynamic = true;
            }
        }
        return contents;
    }

    private Map<String, NodeHandler> nodeHandlers = new HashMap<String, NodeHandler>() {
        private static final long serialVersionUID = 7123056019193266281L;

        {
            put("trim", new TrimHandler());
            put("where", new WhereHandler());
            put("set", new SetHandler());
            put("foreach", new ForEachHandler());
            put("if", new IfHandler());
            put("choose", new ChooseHandler());
            put("when", new IfHandler());
            put("otherwise", new OtherwiseHandler());
            put("bind", new BindHandler());
        }
    };

    private interface NodeHandler {

        void handleNode(XNode nodeToHandle, List<SqlNode> targetContents);
    }

    private class BindHandler implements NodeHandler {
        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            final String name = nodeToHandle.getStringAttribute("name");
            final String expression = nodeToHandle.getStringAttribute("value");
            final VarDeclSqlNode node = new VarDeclSqlNode(name, expression);
            targetContents.add(node);
        }
    }

    private class TrimHandler implements NodeHandler {
        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            String prefix = nodeToHandle.getStringAttribute("prefix");
            String prefixOverrides = nodeToHandle.getStringAttribute("prefixOverrides");
            String suffix = nodeToHandle.getStringAttribute("suffix");
            String suffixOverrides = nodeToHandle.getStringAttribute("suffixOverrides");
            TrimSqlNode trim = new TrimSqlNode(configuration, mixedSqlNode, prefix, prefixOverrides, suffix, suffixOverrides);
            targetContents.add(trim);
        }
    }

    private class WhereHandler implements NodeHandler {
        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            WhereSqlNode where = new WhereSqlNode(configuration, mixedSqlNode);
            targetContents.add(where);
        }
    }

    private class SetHandler implements NodeHandler {
        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            SetSqlNode set = new SetSqlNode(configuration, mixedSqlNode);
            targetContents.add(set);
        }
    }

    private class ForEachHandler implements NodeHandler {
        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            String collection = nodeToHandle.getStringAttribute("collection");
            String item = nodeToHandle.getStringAttribute("item");
            String index = nodeToHandle.getStringAttribute("index");
            String open = nodeToHandle.getStringAttribute("open");
            String close = nodeToHandle.getStringAttribute("close");
            String separator = nodeToHandle.getStringAttribute("separator");
            ForEachSqlNode forEachSqlNode = new ForEachSqlNode(configuration, mixedSqlNode, collection, index, item, open, close, separator);
            targetContents.add(forEachSqlNode);
        }
    }

    private class IfHandler implements NodeHandler {
        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            String test = nodeToHandle.getStringAttribute("test");
            IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
            targetContents.add(ifSqlNode);
        }
    }

    private class OtherwiseHandler implements NodeHandler {
        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            targetContents.add(mixedSqlNode);
        }
    }

    private class ChooseHandler implements NodeHandler {
        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> whenSqlNodes = new ArrayList<SqlNode>();
            List<SqlNode> otherwiseSqlNodes = new ArrayList<SqlNode>();
            handleWhenOtherwiseNodes(nodeToHandle, whenSqlNodes, otherwiseSqlNodes);
            SqlNode defaultSqlNode = getDefaultSqlNode(otherwiseSqlNodes);
            ChooseSqlNode chooseSqlNode = new ChooseSqlNode(whenSqlNodes, defaultSqlNode);
            targetContents.add(chooseSqlNode);
        }

        private void handleWhenOtherwiseNodes(XNode chooseSqlNode, List<SqlNode> ifSqlNodes, List<SqlNode> defaultSqlNodes) {
            List<XNode> children = chooseSqlNode.getChildren();
            for (XNode child : children) {
                String nodeName = child.getNode().getNodeName();
                NodeHandler handler = nodeHandlers.get(nodeName);
                if (handler instanceof IfHandler) {
                    handler.handleNode(child, ifSqlNodes);
                } else if (handler instanceof OtherwiseHandler) {
                    handler.handleNode(child, defaultSqlNodes);
                }
            }
        }

        private SqlNode getDefaultSqlNode(List<SqlNode> defaultSqlNodes) {
            SqlNode defaultSqlNode = null;
            if (defaultSqlNodes.size() == 1) {
                defaultSqlNode = defaultSqlNodes.get(0);
            } else if (defaultSqlNodes.size() > 1) {
                throw new BuilderException("Too many default (otherwise) elements in choose statement.");
            }
            return defaultSqlNode;
        }
    }

}

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.parser.helper;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.camel.parser.model.CamelNodeDetails;
import org.apache.camel.parser.model.CamelNodeDetailsFactory;

public final class CamelXmlTreeParserHelper {

    private CamelXmlTreeParserHelper() {
    }

    public static List<CamelNodeDetails> parseCamelRouteTree(Node route, String routeId, CamelNodeDetails parent,
                                                             String baseDir, String fullyQualifiedFileName) {

        CamelNodeDetailsFactory nodeFactory = CamelNodeDetailsFactory.newInstance();
        List<CamelNodeDetails> answer = new ArrayList<>();

        walkXmlTree(nodeFactory, route, parent);
        answer.add(parent);
        return answer;
    }

    private static void walkXmlTree(CamelNodeDetailsFactory nodeFactory, Node node, CamelNodeDetails parent) {
        CamelNodeDetails newNode = null;

        String name = node.getNodeName();
        // skip route as we just keep from
        if (!"route".equals(name)) {
            String lineNumber = (String) node.getUserData(XmlLineNumberParser.LINE_NUMBER);
            String lineNumberEnd = (String) node.getUserData(XmlLineNumberParser.LINE_NUMBER_END);
            newNode = nodeFactory.newNode(parent, name);
            newNode.setRouteId(parent.getRouteId());
            newNode.setFileName(parent.getFileName());
            newNode.setLineNumber(lineNumber);
            newNode.setLineNumberEnd(lineNumberEnd);

            parent.addOutput(newNode);
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                walkXmlTree(nodeFactory, child, newNode != null ? newNode : parent);
            }
        }

    }

}

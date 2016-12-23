/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.parser.helper.AdvancedCamelJavaParserHelper;
import org.apache.camel.parser.helper.CamelJavaParserHelper;
import org.apache.camel.parser.model.CamelNodeDetails;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * TODO: Merge this to {@link RouteBuilderParser}
 */
public class AdvancedRouteBuilderParser {

    // TODO: list of details, on per route
    public static CamelNodeDetails parseRouteBuilder(JavaClassSource clazz, boolean includeInlinedRouteBuilders) {
        AdvancedCamelJavaParserHelper parser = new AdvancedCamelJavaParserHelper();

        List<MethodSource<JavaClassSource>> methods = new ArrayList<>();
        MethodSource<JavaClassSource> method = CamelJavaParserHelper.findConfigureMethod(clazz);
        if (method != null) {
            methods.add(method);
        }
        if (includeInlinedRouteBuilders) {
            List<MethodSource<JavaClassSource>> inlinedMethods = CamelJavaParserHelper.findInlinedConfigureMethods(clazz);
            if (!inlinedMethods.isEmpty()) {
                methods.addAll(inlinedMethods);
            }
        }

        for (MethodSource<JavaClassSource> configureMethod : methods) {
            CamelNodeDetails details = parser.parseCamelRoute(clazz, configureMethod);
            return details;
        }

        return null;
    }
}

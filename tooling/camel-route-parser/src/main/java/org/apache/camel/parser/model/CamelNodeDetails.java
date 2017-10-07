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
package org.apache.camel.parser.model;

import java.util.ArrayList;
import java.util.List;

public class CamelNodeDetails {

    private final CamelNodeDetails parent;
    private final String name;
    private final int order;
    private List<CamelNodeDetails> outputs;

    public CamelNodeDetails(CamelNodeDetails parent, String name, int order) {
        this.parent = parent;
        this.name = name;
        this.order = order;
    }

    public void addPreliminaryOutput(CamelNodeDetails output) {
        if (outputs == null) {
            outputs = new ArrayList<>();
        }
        // the parser walks the EIPs backwards so add from the top
        outputs.add(0, output);
    }

    public void addOutput(CamelNodeDetails output) {
        if (outputs == null) {
            outputs = new ArrayList<>();
        }
        outputs.add(output);
    }

    public CamelNodeDetails getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public List<CamelNodeDetails> getOutputs() {
        return outputs;
    }

    public String toString() {
        return name;
    }

    public String dump(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append(order);
        sb.append("\t");
        sb.append(padString(level));
        sb.append(name);
        if (outputs != null) {
            level++;
            for (CamelNodeDetails child : outputs) {
                String text = child.dump(level);
                sb.append("\n");
                sb.append(text);
            }
        }
        return sb.toString();
    }

    private static String padString(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }
}

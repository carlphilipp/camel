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
package org.apache.camel.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.maven.helper.EndpointHelper;
import org.apache.camel.parser.RouteBuilderParser;
import org.apache.camel.parser.model.CamelEndpointDetails;
import org.apache.camel.parser.model.CamelNodeDetails;
import org.apache.camel.parser.model.CamelRouteDetails;
import org.apache.camel.parser.model.CamelSimpleExpressionDetails;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.exec.AbstractExecMojo;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;

/**
 * Performs route coverage reports after running Camel unit tests with camel-test modules
 *
 * @goal coverage
 * @threadSafe
 */
public class CoverageMojo extends AbstractExecMojo {

    /**
     * The maven project.
     *
     * @parameter property="project"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Whether to fail if a route was not fully covered
     *
     * @parameter property="camel.failOnError"
     *            default-value="false"
     */
    private boolean failOnError;

    /**
     * Whether to include test source code
     *
     * @parameter property="camel.includeTest"
     *            default-value="false"
     */
    private boolean includeTest;

    /**
     * To filter the names of java and xml files to only include files matching any of the given list of patterns (wildcard and regular expression).
     * Multiple values can be separated by comma.
     *
     * @parameter property="camel.includes"
     */
    private String includes;

    /**
     * To filter the names of java and xml files to exclude files matching any of the given list of patterns (wildcard and regular expression).
     * Multiple values can be separated by comma.
     *
     * @parameter property="camel.excludes"
     */
    private String excludes;

    // CHECKSTYLE:OFF
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        List<CamelEndpointDetails> endpoints = new ArrayList<>();
        List<CamelSimpleExpressionDetails> simpleExpressions = new ArrayList<>();
        List<CamelRouteDetails> routeIds = new ArrayList<>();
        Set<File> javaFiles = new LinkedHashSet<File>();
        Set<File> xmlFiles = new LinkedHashSet<File>();

        // find all java route builder classes
        List list = project.getCompileSourceRoots();
        for (Object obj : list) {
            String dir = (String) obj;
            findJavaFiles(new File(dir), javaFiles);
        }
        // find all xml routes
        list = project.getResources();
        for (Object obj : list) {
            Resource dir = (Resource) obj;
            findXmlFiles(new File(dir.getDirectory()), xmlFiles);
        }

        if (includeTest) {
            list = project.getTestCompileSourceRoots();
            for (Object obj : list) {
                String dir = (String) obj;
                findJavaFiles(new File(dir), javaFiles);
            }
            list = project.getTestResources();
            for (Object obj : list) {
                Resource dir = (Resource) obj;
                findXmlFiles(new File(dir.getDirectory()), xmlFiles);
            }
        }

        List<CamelNodeDetails> routeTrees = new ArrayList<>();

        for (File file : javaFiles) {
            if (matchFile(file)) {
                try {

                    // parse the java source code and find Camel RouteBuilder classes
                    String fqn = file.getPath();
                    String baseDir = ".";
                    JavaType out = Roaster.parse(file);
                    // we should only parse java classes (not interfaces and enums etc)
                    if (out != null && out instanceof JavaClassSource) {
                        JavaClassSource clazz = (JavaClassSource) out;
                        List<CamelNodeDetails> result = RouteBuilderParser.parseRouteBuilderTree(clazz, baseDir, fqn, true);
                        routeTrees.addAll(result);
                    }
                } catch (Exception e) {
                    getLog().warn("Error parsing java file " + file + " code due " + e.getMessage(), e);
                }
            }
        }
        for (File file : xmlFiles) {
            if (matchFile(file)) {
                try {
                    // TODO: implement me
                } catch (Exception e) {
                    getLog().warn("Error parsing xml file " + file + " code due " + e.getMessage(), e);
                }
            }
        }

        getLog().info("Discovered " + routeTrees.size() + " routes");

        // skip any routes which has no route id assigned

        long anonymous = routeTrees.stream().filter(t -> t.getRouteId() == null).count();
        if (anonymous > 0) {
            getLog().warn("Discovered " + anonymous + " anonymous routes. Add route ids to these routes for route coverage support");
        }

        routeTrees = routeTrees.stream().filter(t -> t.getRouteId() != null).collect(Collectors.toList());

        routeTrees.forEach(t -> {
            String routeId = t.getRouteId();
            String fileName = asRelativeFile(t.getFileName());
            String tree = t.dump(4);

            getLog().info("Route " + routeId + " discovered in file " + fileName);
            getLog().info("\n" + tree + "\n");
        });

        int notCovered = 0;

        if (failOnError && (notCovered > 0)) {
            throw new MojoExecutionException("Some routes are not fully covered");
        }
    }
    // CHECKSTYLE:ON

    private static int countRouteId(List<CamelRouteDetails> details, String routeId) {
        int answer = 0;
        for (CamelRouteDetails detail : details) {
            if (routeId.equals(detail.getRouteId())) {
                answer++;
            }
        }
        return answer;
    }

    private void findJavaFiles(File dir, Set<File> javaFiles) {
        File[] files = dir.isDirectory() ? dir.listFiles() : null;
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                } else if (file.isDirectory()) {
                    findJavaFiles(file, javaFiles);
                }
            }
        }
    }

    private void findXmlFiles(File dir, Set<File> xmlFiles) {
        File[] files = dir.isDirectory() ? dir.listFiles() : null;
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".xml")) {
                    xmlFiles.add(file);
                } else if (file.isDirectory()) {
                    findXmlFiles(file, xmlFiles);
                }
            }
        }
    }

    private boolean matchFile(File file) {
        if (excludes == null && includes == null) {
            return true;
        }

        // exclude take precedence
        if (excludes != null) {
            for (String exclude : excludes.split(",")) {
                exclude = exclude.trim();
                // try both with and without directory in the name
                String fqn = stripRootPath(asRelativeFile(file.getAbsolutePath()));
                boolean match = EndpointHelper.matchPattern(fqn, exclude) || EndpointHelper.matchPattern(file.getName(), exclude);
                if (match) {
                    return false;
                }
            }
        }

        // include
        if (includes != null) {
            for (String include : includes.split(",")) {
                include = include.trim();
                // try both with and without directory in the name
                String fqn = stripRootPath(asRelativeFile(file.getAbsolutePath()));
                boolean match = EndpointHelper.matchPattern(fqn, include) || EndpointHelper.matchPattern(file.getName(), include);
                if (match) {
                    return true;
                }
            }
            // did not match any includes
            return false;
        }

        // was not excluded nor failed include so its accepted
        return true;
    }

    private String asRelativeFile(String name) {
        String answer = name;

        String base = project.getBasedir().getAbsolutePath();
        if (name.startsWith(base)) {
            answer = name.substring(base.length());
            // skip leading slash for relative path
            if (answer.startsWith(File.separator)) {
                answer = answer.substring(1);
            }
        }
        return answer;
    }

    private String stripRootPath(String name) {
        // strip out any leading source / resource directory

        List list = project.getCompileSourceRoots();
        for (Object obj : list) {
            String dir = (String) obj;
            dir = asRelativeFile(dir);
            if (name.startsWith(dir)) {
                return name.substring(dir.length() + 1);
            }
        }
        list = project.getTestCompileSourceRoots();
        for (Object obj : list) {
            String dir = (String) obj;
            dir = asRelativeFile(dir);
            if (name.startsWith(dir)) {
                return name.substring(dir.length() + 1);
            }
        }
        List resources = project.getResources();
        for (Object obj : resources) {
            Resource resource = (Resource) obj;
            String dir = asRelativeFile(resource.getDirectory());
            if (name.startsWith(dir)) {
                return name.substring(dir.length() + 1);
            }
        }
        resources = project.getTestResources();
        for (Object obj : resources) {
            Resource resource = (Resource) obj;
            String dir = asRelativeFile(resource.getDirectory());
            if (name.startsWith(dir)) {
                return name.substring(dir.length() + 1);
            }
        }

        return name;
    }

    private static String asPackageName(String name) {
        return name.replace(File.separator, ".");
    }

    private static String asSimpleClassName(String className) {
        int dot = className.lastIndexOf('.');
        if (dot > 0) {
            return className.substring(dot + 1);
        } else {
            return className;
        }
    }
}

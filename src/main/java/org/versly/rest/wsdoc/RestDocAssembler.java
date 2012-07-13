/*
 * Copyright 2011 TaskDock, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.versly.rest.wsdoc;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class RestDocAssembler {
    private final String _outputFileName;

    public static void main(String... args)
        throws IOException, ClassNotFoundException, TemplateException {
        Arguments arguments = new Arguments();
        new JCommander(arguments, args);

        List<RestDocumentation> docs = new LinkedList<RestDocumentation>();
        for (String input : arguments.inputs) {
            File inputFile = new File(input);
            if (inputFile.isDirectory()) {
                System.err.println("adding web service docs from classes directory " + input);
                File resourceFile = new File(inputFile, Utils.SERIALIZED_RESOURCE_LOCATION);
                docs.add(RestDocumentation.fromStream(new FileInputStream(resourceFile))); // TODO resource management
            } else if (input.toLowerCase().endsWith(".war")) {
                System.err.println("adding web service docs from WAR " + input);
                JarFile jar = new JarFile(input);
                ZipEntry e = jar.getEntry("WEB-INF/classes/" + Utils.SERIALIZED_RESOURCE_LOCATION);
                docs.add(RestDocumentation.fromStream(jar.getInputStream(e)));
                jar.close();
            } else {
                System.err.println("adding web service docs from serialized input " + input);
                docs.add(RestDocumentation.fromStream(new FileInputStream(inputFile))); // TODO resource management
            }
        }

        if (docs.size() > 0)
            new RestDocAssembler(arguments.outputFileName).writeDocumentation(docs);
    }

    public RestDocAssembler(String outputFileName) {
        _outputFileName = outputFileName;
    }

    void writeDocumentation(List<RestDocumentation> docs)
        throws IOException, ClassNotFoundException, TemplateException {
        Configuration conf = new Configuration();
        conf.setClassForTemplateLoading(RestDocAssembler.class, "");
        conf.setObjectWrapper(new DefaultObjectWrapper());
        Writer out = null;
        try {
            Template template = conf.getTemplate("RestDocumentation.ftl");
            Map root = new HashMap();
            root.put("docs", docs);
            File file = getOutputFile();
            out = new FileWriter(file);
            template.process(root, out);
            out.flush();
            System.err.printf("Wrote REST docs to %s\n", file.getAbsolutePath());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                    // ignored
                }
            }
        }
    }

    File getOutputFile() {
        return new File(_outputFileName);
    }

    static class Arguments {
        @Parameter
        List<String> inputs = Lists.newArrayList();

        @Parameter(names = { "-o", "--out" }, description = "File to write HTML documentation to")
        String outputFileName = "web-service-api.html";
    }
}

/*
 * MIT License
 *
 * Copyright 2019 Crown Copyright
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.kscs.util.test;

import com.sun.tools.xjc.Driver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class GenerateClasses {


    private static final String TEST_MODULE_NAME = "test-module";

    /**
     * Useful for debugging the plugin
     */
    public static void main(String[] args) throws Exception {

        Path rootDir = Paths.get(".").normalize().toAbsolutePath();
        if (!rootDir.endsWith(TEST_MODULE_NAME)) {
            rootDir = Paths.get("." + File.separator + TEST_MODULE_NAME).normalize().toAbsolutePath();
        }
        Path resourcesDir = rootDir.resolve("src").resolve("main").resolve("resources");
        Path schemaFile = resourcesDir.resolve("jaxb2-plugin-test.xsd");
        Path bindingFile = resourcesDir.resolve("binding-config.xjb");
        Path outputDir = rootDir.resolve("target").resolve("GenerateClassesOutput");

        System.out.println("rootDir: " + rootDir.toAbsolutePath().toString());
        System.out.println("schemaFile: " + schemaFile.toAbsolutePath().toString());
        System.out.println("bindingFile: " + bindingFile.toAbsolutePath().toString());
        System.out.println("outputDir: " + outputDir.toAbsolutePath().toString());

        Files.createDirectories(outputDir);

        final String[] xjcOptions = new String[]{
                "-xmlschema",
                "-nv",
                "-extension",
                "-verbose",
//                "-p", PACKAGE_NAME,
                "-d", outputDir.toAbsolutePath().toString(),
                "-b", bindingFile.toAbsolutePath().toString(),
//                "-quiet",
                schemaFile.toAbsolutePath().toString(), //the source schema to gen classes from
                "-Ximmutable",
                "-Xfluent-builder",
                "-generateJavadocFromAnnotations=true",
                "-Xclone",
                // TODO Not sure how to handle the episodes file that group-contract needs
//                "-Xgroup-contract",
        };

        System.out.println("Running XJC with arguments:");
        Arrays.stream(xjcOptions)
                .map(str -> "  " + str)
                .forEach(System.out::println);

        final int exitStatus = Driver.run(xjcOptions, System.out, System.out);

        if (exitStatus != 0) {
            System.out.print("Executing xjc failed");
            System.exit(1);
        }
    }
}

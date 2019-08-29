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

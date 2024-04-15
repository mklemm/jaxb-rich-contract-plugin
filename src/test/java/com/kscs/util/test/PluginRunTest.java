/*
 * MIT License
 *
 * Copyright (c) 2014 Klemm Software Consulting, Mirko Klemm
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import com.sun.tools.xjc.Driver;

public class PluginRunTest {
	final Path outputDir = Paths.get("target/generated-test-sources/xjc");
	final Path xsdDir = Paths.get("src/test/resources");

	private String inFile(final String name) {
		return xsdDir.resolve(name).toString();
	}
	private String outFile(final String name) {
		return outputDir.resolve(name).toString();
	}
	/**
	 * Useful for debugging the plugin
	 */
	void runPlugin(final String... pluginArgs) throws Exception {
		System.setProperty("javax.xml.accessExternalDTD", "all");
		System.setProperty("javax.xml.accessExternalSchema", "all");
		// Use the same dir that the maven plugin uses
		System.out.println("outputDir: " + outputDir.toAbsolutePath().toString());
		clearDirectory(outputDir);
		// Ensure the full path exists
		Files.createDirectories(outputDir);
		final var xjcBaseOptions = new ArrayList<>(List.of(
				"-xmlschema",
				"-nv",
				"-verbose",
				//                "-p", PACKAGE_NAME,
				"-d", outputDir.toString(),
				"-catalog", inFile("catalog.xml"),
				"-extension"
		));
		xjcBaseOptions.addAll(List.of(pluginArgs));
		final String[] xjcOptions = xjcBaseOptions.toArray(new String[0]);
		System.out.println("Running XJC with arguments:");
		Arrays.stream(xjcOptions)
				.map(str -> "  " + str)
				.forEach(System.out::println);
		// Run the xjc code generation process
		final int exitStatus = Driver.run(xjcOptions, System.out, System.err);
		if (exitStatus != 0) {
			System.out.print("Executing xjc failed");
			System.exit(1);
		}
	}

	private static void clearDirectory(final Path rootDir) {
		try {
			if (Files.isDirectory(rootDir)) {
				Files.walk(rootDir)
						.sorted(Comparator.reverseOrder())
						.peek(path -> System.out.println("Deleting " + path))
						.forEach(PluginRunTest::deleteFileQuiet);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error deleting path " + rootDir.toAbsolutePath().toString(), e);
		}
	}

	private static void deleteFileQuiet(final Path p) {
		try {
			Files.delete(p);
		} catch (final IOException iox) {
			throw new RuntimeException("Error deleting path " + p, iox);
		}
	}

	@Test
	public void testGenerateAll() throws Exception {
		runPlugin("-b", inFile("binding-config.xjb"),
				"-b", inFile("binding-config-xhtml.xjb"),
				inFile("jaxb2-plugin-test.xsd"),
				inFile("xml-ns.xsd"),
				inFile("math.xsd"),
				inFile("svg.xsd"),
				inFile("xhtml5.xsd"),
				"-Xclone",
				"-meta.generateTools=n",
				"-fluent-builder.generateTools=n",
				"-Xfluent-builder",
				"-Xgroup-contract",
				"-group-contract.declareSetters=n",
				"-Ximmutable",
				"-Xmodifier",
				"-Xmeta",
				"-meta.extended=y",
				"-meta.camelCase=y"
				);
	}

	@Test
	public void testGenerateIdRef() throws Exception {
		runPlugin("-b", inFile("binding-config-idrefs.xjb"),
				inFile("idrefs-test.xsd"),
				"-Xclone",
				"-meta.generateTools=n",
				"-fluent-builder.generateTools=n",
				"-Xfluent-builder",
				"-Xgroup-contract",
				"-group-contract.declareSetters=n",
				"-Ximmutable",
				"-Xmodifier",
				"-Xmeta",
				"-meta.extended=y",
				"-meta.camelCase=y"
				);
	}
}

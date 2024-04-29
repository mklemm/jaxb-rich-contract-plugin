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

import javax.tools.ToolProvider;

import org.junit.Assert;
import org.junit.Test;

import com.kscs.util.plugins.xjc.GroupInterfaceDirectStrategy;
import com.kscs.util.plugins.xjc.GroupInterfaceDummyStrategy;
import com.kscs.util.plugins.xjc.GroupInterfaceModelProcessingStrategy;
import com.sun.tools.xjc.Driver;

import io.github.classgraph.ClassGraph;

public class PluginRunTest {
	final Path compiledCodeDir = Paths.get("target/test/classes");
	final Path generatedSourcesDir = Paths.get("target/test/generated-sources/xjc");
	final Path xsdDir = Paths.get("src/test/resources");

	private String inFile(final String name) {
		return xsdDir.resolve(name).toString();
	}

	/**
	 * Useful for debugging the plugin
	 */
	void runPlugin(final String subDirectory, final String... pluginArgs) throws Exception {
		final var outputDir = generatedSourcesDir.resolve(subDirectory);
		System.setProperty("javax.xml.accessExternalDTD", "all");
		System.setProperty("javax.xml.accessExternalSchema", "all");
		// Use the same dir that the maven plugin uses
		System.out.println("outputDir: " + outputDir);
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
				try(final var stream = Files.walk(rootDir)) {
					stream.sorted(Comparator.reverseOrder())
							.peek(path -> System.out.println("Deleting " + path))
							.forEach(PluginRunTest::deleteFileQuiet);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Error deleting path " + rootDir.toAbsolutePath(), e);
		}
	}

	private void clearOutputFor(final String subDir){
		clearDirectory(this.compiledCodeDir.resolve(subDir));
		clearDirectory(this.generatedSourcesDir.resolve(subDir));
	}

	private static void deleteFileQuiet(final Path p) {
		try {
			Files.delete(p);
		} catch (final IOException iox) {
			throw new RuntimeException("Error deleting path " + p, iox);
		}
	}

	private void compileTestCode(final String sourceSubDirectory) throws IOException {
		final var classpath = new ClassGraph().getClasspathFiles();

		final var args = new ArrayList<>(Arrays.asList(
				"-classpath", classpath.stream().map(java.io.File::toString).reduce((s1, s2) -> s1 + ":" + s2).orElse(""),
				"-d", this.compiledCodeDir.resolve(sourceSubDirectory).toString()
		));
		try(final var s = Files.walk(this.generatedSourcesDir.resolve(sourceSubDirectory))) {
			s.map(Path::toString).filter(p -> p.endsWith(".java")).forEach(args::add);
		}
		final var compiler = ToolProvider.getSystemJavaCompiler();
		Assert.assertEquals("Test code compiled with errors", 0, compiler.run(System.in, System.out, System.err, args.toArray(new String[0])));
	}

	public void generateAndCompile(final String subDir, final String... pluginArgs) throws Exception {
		clearOutputFor(subDir);
		runPlugin(subDir, pluginArgs);
		compileTestCode(subDir);
	}

	@Test
	public void testGenerateAll() throws Exception {
		generateAndCompile("all","-b", inFile("binding-config.xjb"),
				"-b", inFile("binding-config-xhtml.xjb"),
				inFile("jaxb2-plugin-test.xsd"),
				inFile("xml-ns.xsd"),
				inFile("math.xsd"),
				inFile("svg.xsd"),
				inFile("xhtml5.xsd"),
				"-Xclone",
				"-Xfluent-builder",
				"-fluent-builder.generateTools=n",
				"-Xgroup-contract",
				"-group-contract.declareSetters=n",
				"-Ximmutable",
				"-Xmodifier",
				"-Xmeta",
				"-meta.generateTools=n",
				"-meta.extended=y",
				"-meta.camelCase=y"
				);
	}

	@Test
	public void testGenerateIdRef() throws Exception {
		generateAndCompile("idrefs", "-b", inFile("binding-config-idrefs.xjb"),
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
	@Test
	public void testGenerateCustomList() throws Exception {
		generateAndCompile("customList","-b", inFile("binding-config-custom-list.xjb"),
				inFile("custom-list-test.xsd"),
				"-Xclone",
				"-meta.generateTools=n",
				"-Xfluent-builder",
				"-fluent-builder.generateTools=y",
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
	public void testGenerateDefaultValue() throws Exception {
		generateAndCompile("defaultValue","-b", inFile("binding-config-default-value.xjb"),
				inFile("default-value-test.xsd"),
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
	public void testGroupInterfaceDummy() throws Exception {
		System.setProperty(GroupInterfaceModelProcessingStrategy.class.getName(), GroupInterfaceDummyStrategy.class.getName());
		generateAndCompile("gidummy",
				//"-b", inFile("binding-config-group-interface.xjb"),
				inFile("group-interface-test.xsd"),
				"-Xgroup-contract",
				"-group-contract.declareSetters=n"
				);
	}
	@Test
	public void testGroupInterfaceDirect() throws Exception {
		System.setProperty(GroupInterfaceModelProcessingStrategy.class.getName(), GroupInterfaceDirectStrategy.class.getName());
		generateAndCompile("gidirect",
				//"-b", inFile("binding-config-group-interface.xjb"),
				inFile("group-interface-test.xsd"),
				"-Xgroup-contract",
				"-group-contract.declareSetters=n"
				);
	}
	@Test
	public void testGroupInterfaceDirectFull() throws Exception {
		System.setProperty(GroupInterfaceModelProcessingStrategy.class.getName(), GroupInterfaceDirectStrategy.class.getName());
		generateAndCompile("gidifu",
				"-b", inFile("binding-config-group-interface.xjb"),
				inFile("group-interface-test.xsd"),
				"-Xgroup-contract",
				"-group-contract.declareSetters=n",
				"-Xclone",
				"-meta.generateTools=n",
				"-fluent-builder.generateTools=n",
				"-Xfluent-builder",
				"-Ximmutable",
				"-Xmodifier",
				"-Xmeta",
				"-meta.extended=y",
				"-meta.camelCase=y"
				);
	}
	@Test
	public void testGroupInterfaceCustom() throws Exception {
		System.setProperty(GroupInterfaceModelProcessingStrategy.class.getName(), GroupInterfaceDirectStrategy.class.getName());
		generateAndCompile("gicustom",
				"-b", inFile("binding-config-group-interface.xjb"),
				inFile("group-interface-test.xsd"),
				"-verbose",
				"-Xgroup-contract",
				"-group-contract.declareSetters=n"
				);
	}

	@Test
	public void testGenerateChoices() throws Exception {
		generateAndCompile("choices","-b", inFile("binding-config.xjb"),
				inFile("jaxb2-plugin-test.xsd"),
				"-Xclone",
				"-Xfluent-builder",
				"-fluent-builder.generateTools=n",
				"-Xgroup-contract",
				"-group-contract.declareSetters=n",
				"-Ximmutable",
				"-Xmodifier",
				"-Xmeta",
				"-meta.generateTools=n",
				"-meta.extended=y",
				"-meta.camelCase=y"
				);
	}

}

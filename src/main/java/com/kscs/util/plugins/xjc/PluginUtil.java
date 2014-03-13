package com.kscs.util.plugins.xjc;

import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;

import java.io.*;

/**
 * Created by klemm0 on 13/03/14.
 */
public final class PluginUtil {
	public static void writeSourceFile(final Class<?> thisClass, final File targetDir, final String resourceName) {
		try {
			final String resourcePath = "/" + resourceName.replace('.', '/') + ".java";
			final File targetFile = new File(targetDir.getPath()+resourcePath);
			final File packageDir = targetFile.getParentFile();
			final boolean created = packageDir.mkdirs();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(thisClass.getResourceAsStream(resourcePath)));
			final BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile));
			try {
				String line;
				while((line = reader.readLine()) != null) {
					writer.write(line+"\n");
				}
			} finally {
				reader.close();
				writer.close();
			}
		} catch(final IOException iox) {
			throw new RuntimeException(iox);
		}
	}

	public static final class Arg<T> {
		private final T value;
		private final int argsParsed;

		Arg(T value, int argsParsed) {
			this.value = value;
			this.argsParsed = argsParsed;
		}

		public T getValue() {
			return value;
		}

		public int getArgsParsed() {
			return argsParsed;
		}
	}

	public static Arg<Boolean> parseBooleanArgument(final String name, final boolean defaultValue, final Options opt, final String[] args, final int i) throws BadCommandLineException {
		final String arg = args[i].toLowerCase();
		if (arg.startsWith("-"+name.toLowerCase()+"=")) {
			final boolean argTrue = isTrue(arg);
			final boolean argFalse = isFalse(arg);
			if (!argTrue && !argFalse) {
				throw new BadCommandLineException("-"+name.toLowerCase()+" " + DeepClonePlugin.BOOLEAN_OPTION_ERROR_MSG);
			} else {
				return new Arg<Boolean>(argTrue, 1);
			}
		}
		return new Arg<Boolean>(defaultValue, 0);
	}

	private static boolean isTrue(final String arg) {
		return arg.endsWith("y") || arg.endsWith("true") || arg.endsWith("on") || arg.endsWith("yes");
	}

	private static boolean isFalse(final String arg) {
		return arg.endsWith("n") || arg.endsWith("false") || arg.endsWith("off") || arg.endsWith("no");
	}


}

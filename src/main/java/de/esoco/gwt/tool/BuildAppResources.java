//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.gwt.tool;

import com.google.gwt.i18n.tools.I18NSync;
import com.google.gwt.resources.css.InterfaceGenerator;
import de.esoco.ewt.app.EWTEntryPoint;
import de.esoco.lib.collection.CollectionUtil;
import de.esoco.lib.logging.Log;
import de.esoco.lib.text.TextConvert;
import de.esoco.lib.text.TextUtil;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Creates the resources for a GEWT application by concatenating all string
 * properties and CSS files into single files and then generates the code files
 * by invoking the tools I18NSync and {@link CssInterfaceGenerator} on them.
 *
 * @author eso
 */
public class BuildAppResources {

	private static final String ARG_APP_CLASS = "-app";

	private static final String ARG_PROJECT_DIR = "-project";

	private static final String ARG_SOURCE_DIR = "-src";

	private static final String ARG_EXTRA_DIRS = "-extra_dirs";

	private static final String ARG_WEBAPP_DIR = "-webapp";

	private static final String ARG_MAX_PROPERTY_LINES = "-max_lines";

	private static final String ARG_HIERARCHICAL = "-hierarchical";

	private static final String DEFAULT_MAX_PROPERTY_LINES = "2500";

	private static final String GENERATED_DIR = "res/generated/";

	private static final String GENERATED_PACKAGE = ".res.generated.";

	private static final PatternFilter CSS_FILES_FILTER =
		new PatternFilter(".*\\.css");

	private static final PatternFilter PROPERTY_FILES_FILTER =
		new PatternFilter(".*\\.properties");

	private static final List<String> EXCLUDED_DIRS =
		Arrays.asList("generated", "img");

	private static final Map<String, String> SUPPORTED_ARGS =
		new LinkedHashMap<>();

	private static final Set<String> FLAG_ARGS =
		CollectionUtil.setOf(ARG_HIERARCHICAL);

	private static final Map<String, String> projectDirMap = new HashMap<>();

	private static Map<String, String> params;

	static {
		SUPPORTED_ARGS.put(ARG_APP_CLASS,
			"(Class Name) The full name of the application class");
		SUPPORTED_ARGS.put(ARG_PROJECT_DIR,
			"(Directory) The name of the project directory [app_class_name]");
		SUPPORTED_ARGS.put(ARG_SOURCE_DIR,
			"(Directory) The project-relative directory to read source files" +
				" " + "from [src/main/java]");
		SUPPORTED_ARGS.put(ARG_WEBAPP_DIR,
			"(Directory) The project-relative webapp directory to store " +
				"server" + " resources in [src/main/webapp/data/res]");
		SUPPORTED_ARGS.put(ARG_EXTRA_DIRS,
			"(Directories, comma-separated) Additional project-relative " +
				"directories to recursively read source files from");
		SUPPORTED_ARGS.put(ARG_MAX_PROPERTY_LINES,
			"(Integer) Maximum line count for generated resource property " +
				"files before splitting [2500]");
		SUPPORTED_ARGS.put(ARG_HIERARCHICAL,
			"(Flag) Search the project hierarchy");
	}

	/**
	 * Creates a new instance.
	 */
	private BuildAppResources() {
	}

	/**
	 * Appends a properties file for a certain locale to a target file and
	 * creates the corresponding writer if necessary.
	 *
	 * @param source  The source file for the given locale
	 * @param target  The base name (!) of the target file
	 * @param locale  The locale (will be appended to the target name)
	 * @param writers The mapping from locales to writers for lookup and
	 *                   storing
	 *                of the locale writer
	 * @throws IOException If an I/O operation fails
	 */
	private static void appendLocaleProperties(String source, String target,
		String locale, Map<String, Writer> writers) throws IOException {
		Writer localeWriter = writers.get(locale);

		if (localeWriter == null) {
			localeWriter = createPropertiesWriter(target, locale, writers);
		}

		appendProperties(source, localeWriter);
	}

	/**
	 * Appends a certain properties file to a writer.
	 *
	 * @param fileName     The name of the properties file
	 * @param targetWriter The target writer
	 * @throws IOException If an I/O operation fails
	 */
	private static void appendProperties(String fileName, Writer targetWriter)
		throws IOException {
		writePropertiesHeader(fileName, targetWriter);
		writeFile(fileName, targetWriter);
	}

	/**
	 * Closes all elements of a collection.
	 *
	 * @param closeables A collection of {@link Closeable} elements
	 */
	private static void closeAll(Collection<? extends Closeable> closeables) {
		for (Closeable closeable : closeables) {
			try {
				closeable.close();
			} catch (IOException e) {
				System.err.printf("Error closing collection: %s\n", e);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Adds the paths of all directories in the hierarchy of a root
	 * directory to
	 * a collection.
	 *
	 * @param rootDir     The root directory to scan for directories
	 * @param directories The collection to add the directories to
	 */
	private static void collectDirectories(File rootDir,
		Collection<String> directories) {
		if (rootDir.exists()) {
			directories.add(rootDir.getPath() + File.separatorChar);

			for (File file : rootDir.listFiles()) {
				if (file.isDirectory()) {
					collectDirectories(file, directories);
				}
			}
		}
	}

	/**
	 * Concatenates the CSS files of the target application into a single large
	 * file.
	 *
	 * @param directories    The resource directories from which to read the
	 *                         CSS
	 *                       files
	 * @param targetBaseName The base name for the target files to write to
	 * @throws IOException If reading or writing a file fails
	 */
	private static void concatenateCssFiles(Collection<String> directories,
		String targetBaseName) throws IOException {
		String targetFile = targetBaseName + ".css";
		Writer cssWriter = new BufferedWriter(new FileWriter(targetFile));
		boolean firstFile = true;

		try {
			for (String dir : directories) {
				File directory = new File(dir);

				PatternFilter filter = CSS_FILES_FILTER;
				String[] cssFiles = directory.list(filter);

				for (String cssFile : cssFiles) {
					if (firstFile) {
						System.out.printf("Writing %s\n", targetFile);
						firstFile = false;
					}

					String header = String.format(
						"\n/*--------------------------------------------------------------------\n" +
							"  --- %s\n" +
							"  --------------------------------------------------------------------*/\n\n",
						cssFile);

					cssWriter.write(header);
					writeFile(directory + cssFile, cssWriter);
					System.out.printf(" + %s%s\n", directory, cssFile);
				}
			}
		} finally {
			cssWriter.close();
		}
	}

	/**
	 * Concatenates the string properties files containing the resource strings
	 * of the target application into a single files, separated by locale. If a
	 * certain size is exceeded the file will be split into multiple parts to
	 * prevent GWT compile errors. If locale-specific files exists they will be
	 * handled analog to the default files that don't have a locale in their
	 * file name.
	 *
	 * @param appName        appClass appName The application name
	 * @param directories    The resource directories from which to read the
	 *                       property files
	 * @param targetBaseName The base name for the target files to write to
	 * @throws IOException If reading or writing a file fails
	 */
	private static void concatenatePropertyFiles(String appName,
		Collection<String> directories, String targetBaseName)
		throws IOException {
		Map<String, Writer> writers = new HashMap<>();
		Map<String, Writer> serverWriters = new HashMap<>();

		try {
			Set<String> processedFiles = new HashSet<>();
			Writer defaultWriter = null;
			String target = null;

			String serverFileBaseName =
				String.format("%s/%sStrings", params.get(ARG_WEBAPP_DIR),
					appName);

			int totalLines = -1;
			int fileCount = 0;
			int maxLines =
				Integer.parseInt(params.get(ARG_MAX_PROPERTY_LINES));

			Writer serverWriter =
				createPropertiesWriter(serverFileBaseName, null,
					serverWriters);

			System.out.printf("Writing %s\n", serverFileBaseName);

			for (String directory : directories) {
				Collection<String> defaultFiles =
					getDefaultPropertyFiles(directory);

				// first concatenate the default files
				for (String inputFile : defaultFiles) {
					String file = directory + inputFile;

					// don't process the same file twice
					if (!processedFiles.contains(file)) {
						processedFiles.add(file);

						int lines = countLines(file);

						if (totalLines < 0 || totalLines + lines > maxLines) {
							target = targetBaseName;
							totalLines = lines;

							closeAll(writers.values());
							writers.clear();

							if (++fileCount > 1) {
								target += fileCount;
							}

							defaultWriter =
								createPropertiesWriter(target, null, writers);
							System.out.printf("Writing %s\n", target);
						} else {
							totalLines += lines;
						}

						appendProperties(file, defaultWriter);
						appendProperties(file, serverWriter);
						System.out.printf(" + %s%s\n", directory, file);

						// then lookup the locale-specific files for the
						// current
						// default file and concatenate them for each locale if
						// they exist
						int dotPos = inputFile.indexOf('.');

						String localePattern =
							inputFile.substring(0, dotPos) + "_.*" +
								inputFile.substring(dotPos);

						String[] localeFiles = new File(directory).list(
							new PatternFilter(localePattern));

						for (String localeFile : localeFiles) {
							int localeIndex = localeFile.indexOf('_');

							String locale =
								localeFile.substring(localeIndex + 1,
									localeFile.indexOf('.'));

							String source = directory + localeFile;

							appendLocaleProperties(source, target, locale,
								writers);

							appendLocaleProperties(source, serverFileBaseName,
								locale, serverWriters);

							System.out.printf(" + %s%s\n", directory,
								localeFile);
						}
					}
				}
			}
		} finally {
			closeAll(writers.values());
			closeAll(serverWriters.values());
		}
	}

	/**
	 * Counts the lines in a text file.
	 *
	 * @param file The name of the file to count the lines of
	 * @return The number of lines in the file
	 * @throws IOException If reading from the file fails
	 */
	private static int countLines(String file) throws IOException {
		int lines;

		try (LineNumberReader reader = new LineNumberReader(
			new FileReader(file))) {
			while (reader.skip(Long.MAX_VALUE) > 0) {
			}

			lines = reader.getLineNumber() + 1;
		}

		return lines;
	}

	/**
	 * Creates a writer for the output of property files.
	 *
	 * @param baseName  The base name
	 * @param locale    The locale or NULL for none
	 * @param writerMap A map to store the writer in with the locale as the key
	 *                  (DEFAULT for NULL keys)
	 * @return The new writer
	 */
	private static Writer createPropertiesWriter(String baseName,
		String locale,
		Map<String, Writer> writerMap) throws IOException {
		StringBuilder fileName = new StringBuilder(baseName);

		if (locale != null) {
			fileName.append('_').append(locale);
		}

		fileName.append(".properties");

		Writer writer =
			new BufferedWriter(new FileWriter(fileName.toString()));

		writerMap.put(locale != null ? locale : "DEFAULT", writer);

		return writer;
	}

	/**
	 * Generates the application CSS classes by invoking
	 * {@link InterfaceGenerator}.
	 *
	 * @param appClass The application class
	 * @throws IOException If accessing files fails
	 */
	@SuppressWarnings("unused")
	private static void generateCssClasses(Class<?> appClass)
		throws IOException {
		String appPackage = appClass.getPackage().getName();
		String directory = getBaseDir(appClass);

		String[] cssFiles = new File(directory).list(CSS_FILES_FILTER);

		assert cssFiles != null;

		for (String file : cssFiles) {
			String target = file.substring(0, file.indexOf('.'));

			target = TextConvert.capitalizedIdentifier(target);

			StringBuilder targetName = new StringBuilder(appPackage);

			targetName.append('.');
			targetName.append(target);

			String[] args =
				new String[] { "-standalone", "-css", directory + file,
					"-typeName", targetName.toString() };

			System.out.printf("Generating %s.java from %s\n", targetName,
				file);

			ByteArrayOutputStream generatedData = new ByteArrayOutputStream();

			PrintStream captureOut = new PrintStream(generatedData);
			PrintStream standardOut = System.out;

			System.setOut(captureOut);
			CssInterfaceGenerator.main(args);
			System.setOut(standardOut);
			captureOut.flush();

			target = directory + target + ".java";

			try (OutputStream cssFile = new BufferedOutputStream(
				Files.newOutputStream(Paths.get(target)))) {
				generatedData.writeTo(cssFile);
			}
		}
	}

	/**
	 * Generates the application strings classes by invoking {@link I18NSync}.
	 *
	 * @param appClass The application class
	 * @throws IOException If accessing files fails
	 */
	private static void generateStringClasses(Class<?> appClass) {
		String appPackage =
			appClass.getPackage().getName() + GENERATED_PACKAGE;

		Collection<String> propertyFiles =
			getDefaultPropertyFiles(getBaseDir(appClass) + GENERATED_DIR);

		for (String file : propertyFiles) {
			String targetClass =
				appPackage + file.substring(0, file.indexOf('.'));

			String[] args =
				new String[] { targetClass, "-out", params.get(ARG_SOURCE_DIR),
					"-createConstantsWithLookup" };

			System.out.printf("Generating %s.java from %s\n", targetClass,
				file);
			I18NSync.main(args);
		}
	}

	/**
	 * Generates the name of the base directory of a GEWT application. Always
	 * ends with a directory separator.
	 *
	 * @param appClass The application class
	 * @return The base directory of the app
	 */
	private static String getBaseDir(Class<?> appClass) {
		String appPackage = appClass.getPackage().getName();
		String rootDir = appClass.getSimpleName();
		StringBuilder baseDir = new StringBuilder();
		char dirSep = File.separatorChar;

		if (projectDirMap.containsKey(rootDir)) {
			rootDir = projectDirMap.get(rootDir);
		}

		// ../<app-name>/<src-dir>/<package-path>/
		baseDir.append("..").append(dirSep);
		baseDir.append(rootDir).append(dirSep);
		baseDir.append(params.get(ARG_SOURCE_DIR)).append(dirSep);
		baseDir.append(appPackage.replace('.', dirSep)).append(dirSep);

		return baseDir.toString();
	}

	/**
	 * Returns a collection of the property files for the default locale (i.e.
	 * without a locale suffix) in a certain directory.
	 *
	 * @param directoryName The name of the directory to read the files from
	 * @return A new collection of property files
	 */
	private static Collection<String> getDefaultPropertyFiles(
		String directoryName) {
		File directory = new File(directoryName);
		List<String> files = new ArrayList<>();

		for (String file : Objects.requireNonNull(
			directory.list(PROPERTY_FILES_FILTER))) {
			if (file.indexOf('_') == -1) {
				files.add(file);
			}
		}

		return files;
	}

	/**
	 * Returns the application resource directories to be searched for resource
	 * files.
	 *
	 * @param appClass     The class hierarchy
	 * @param extraDirs    Optional extra directories to add to the returned
	 *                     collection
	 * @param includeRoots TRUE to include the root resource directories, FALSE
	 *                     to only include the sub-directories
	 * @return The class hierarchy
	 */
	private static Collection<String> getResourceDirs(Class<?> appClass,
		Collection<String> extraDirs, boolean hierarchical,
		boolean includeRoots) {
		Deque<String> directories = new ArrayDeque<>();

		do {
			String resourceDir =
				getBaseDir(appClass) + "res" + File.separatorChar;

			File directory = new File(resourceDir);

			if (directory.exists()) {
				if (includeRoots) {
					directories.push(resourceDir);
				}

				for (File file : directory.listFiles()) {
					String filename = file.getName();

					if (file.isDirectory() &&
						!EXCLUDED_DIRS.contains(filename)) {
						directories.push(file.getPath() + File.separatorChar);
					}
				}
			}
		} while (hierarchical &&
			(appClass = appClass.getSuperclass()) != EWTEntryPoint.class);

		if (extraDirs != null) {
			for (String dir : extraDirs) {
				File directory = new File(dir);

				collectDirectories(directory, directories);
			}
		}

		return directories;
	}

	/**
	 * Executes this application.
	 *
	 * @param args The app arguments
	 */
	public static void main(String[] args) {
		params = parseCommandLine(args);

		if (!params.containsKey(ARG_SOURCE_DIR)) {
			params.put(ARG_SOURCE_DIR, "src/main/java");
		}

		if (!params.containsKey(ARG_WEBAPP_DIR)) {
			params.put(ARG_WEBAPP_DIR, "src/main/webapp/data/res");
		}

		if (!params.containsKey(ARG_MAX_PROPERTY_LINES)) {
			params.put(ARG_MAX_PROPERTY_LINES, DEFAULT_MAX_PROPERTY_LINES);
		}

		try {
			String projectDir = params.get(ARG_PROJECT_DIR);
			boolean hierarchical = params.containsKey(ARG_HIERARCHICAL);

			Class<?> appClass = Class.forName(params.get(ARG_APP_CLASS));

			if (projectDir != null) {
				projectDirMap.put(appClass.getSimpleName(), projectDir);
			}

			String targetDir = getBaseDir(appClass) + GENERATED_DIR;
			String baseName = appClass.getSimpleName();
			String extraDirs = params.get(ARG_EXTRA_DIRS);
			Set<String> extraDirSet = new LinkedHashSet<>();

			if (extraDirs != null) {
				Collections.addAll(extraDirSet, extraDirs.split(","));
			}

			String propertiesBase = targetDir + baseName + "Strings";
			String cssBase = targetDir + baseName + "Css";

			Collection<String> stringDirs =
				getResourceDirs(appClass, extraDirSet, hierarchical, true);
			Collection<String> cssDirs =
				getResourceDirs(appClass, extraDirSet, hierarchical, false);

			concatenatePropertyFiles(appClass.getSimpleName(), stringDirs,
				propertiesBase);
			concatenateCssFiles(cssDirs, cssBase);
			generateStringClasses(appClass);

			System.out.print("OK\n");
		} catch (Exception e) {
			Log.error("Error: " + e.getMessage(), e);
			printUsageAndStop(null);
		}
	}

	/**
	 * Parses the command line arguments into a map.
	 *
	 * @param args The raw command line arguments
	 * @return A mapping from parameter names to parameter values
	 */
	private static Map<String, String> parseCommandLine(String[] args) {
		Map<String, String> arguments = new HashMap<>();

		if (args.length == 0) {
			printUsageAndStop(null);
		}

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if ("-?".equals(arg) || "--help".equals(arg)) {
				String helpArg = "help";

				if (i < args.length - 1 &&
					SUPPORTED_ARGS.containsKey(helpArg)) {
					helpArg = args[i + 1];
				}

				printUsageAndStop(helpArg);
			} else if (SUPPORTED_ARGS.containsKey(arg)) {
				String argValue = null;

				if (FLAG_ARGS.contains(arg)) {
					argValue = Boolean.TRUE.toString();
				} else if (i < args.length - 1) {
					argValue = args[++i];

					if (argValue.startsWith("-")) {
						printUsageAndStop(arg);
					}
				} else {
					printUsageAndStop(null);
				}

				arguments.put(arg, argValue);
			}
		}

		return arguments;
	}

	/**
	 * Prints usage information to the console and terminates this application
	 * by invoking {@link System#exit(int)}.
	 *
	 * @param argument The argument to display information for
	 */
	private static void printUsageAndStop(String argument) {
		System.out.printf("USAGE: %s [OPTIONS]\n",
			BuildAppResources.class.getSimpleName());

		if (argument != null) {
			Collection<String> helpArgs;

			if ("help".equals(argument)) {
				helpArgs = SUPPORTED_ARGS.keySet();
			} else {
				helpArgs = Collections.singletonList("-" + argument);
			}

			for (String helpArg : helpArgs) {
				System.out.printf("   %s%s\n",
					TextUtil.padRight(helpArg, 15, ' '),
					SUPPORTED_ARGS.get(helpArg));
			}
		}

		System.exit(1);
	}

	/**
	 * Reads a certain file and writes it to the given output stream.
	 *
	 * @param file   The name of the file to write
	 * @param writer The target output stream
	 * @throws IOException If creating the input stream or transferring data
	 *                     fails
	 */
	private static void writeFile(String file, Writer writer)
		throws IOException {
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			String line;

			while ((line = in.readLine()) != null) {
				writer.write(line);
				writer.write("\n");
				writer.flush();
			}
		}
	}

	/**
	 * Writes a separating header into a target stream.
	 *
	 * @param text   The header Text
	 * @param writer The output stream
	 * @throws IOException If writing data fails
	 */
	private static void writePropertiesHeader(String text, Writer writer)
		throws IOException {
		String header = String.format(
			"\n#--------------------------------------------------------------------\n" +
				"#--- %s\n" +
				"#--------------------------------------------------------------------\n\n",
			text);

		writer.write(header);
	}

	/**
	 * A filename filter based on regular expressions.
	 *
	 * @author eso
	 */
	static class PatternFilter implements FilenameFilter {

		private final Pattern pattern;

		/**
		 * Creates a new instance.
		 *
		 * @param regex The regular expression pattern
		 */
		public PatternFilter(String regex) {
			pattern = Pattern.compile(regex);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean accept(File dir, String name) {
			return pattern.matcher(name).matches();
		}
	}
}

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

import de.esoco.ewt.app.EWTEntryPoint;

import de.esoco.lib.collection.CollectionUtil;
import de.esoco.lib.text.TextConvert;
import de.esoco.lib.text.TextUtil;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;

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
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gwt.i18n.tools.I18NSync;
import com.google.gwt.resources.css.InterfaceGenerator;

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

	private static Set<String> FLAG_ARGS =
		CollectionUtil.setOf(ARG_HIERARCHICAL);

	private static Map<String, String> aParams;

	private static Map<String, String> aProjectDirMap = new HashMap<>();

	static {
		SUPPORTED_ARGS.put(ARG_APP_CLASS,
			"(Class Name) The full name of the application class");
		SUPPORTED_ARGS.put(ARG_PROJECT_DIR,
			"(Directory) The name of the project directory [app_class_name]");
		SUPPORTED_ARGS.put(ARG_SOURCE_DIR,
			"(Directory) The project-relative directory to read source files" +
				" " +
				"from [src/main/java]");
		SUPPORTED_ARGS.put(ARG_WEBAPP_DIR,
			"(Directory) The project-relative webapp directory to store " +
				"server" +
				" resources in [src/main/webapp/data/res]");
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
	 * @param sSource  The source file for the given locale
	 * @param sTarget  The base name (!) of the target file
	 * @param sLocale  The locale (will be appended to the target name)
	 * @param aWriters The mapping from locales to writers for lookup and
	 *                 storing of the locale writer
	 * @throws IOException If an I/O operation fails
	 */
	private static void appendLocaleProperties(String sSource, String sTarget,
		String sLocale, Map<String, Writer> aWriters) throws IOException {
		Writer aLocaleWriter = aWriters.get(sLocale);

		if (aLocaleWriter == null) {
			aLocaleWriter = createPropertiesWriter(sTarget, sLocale, aWriters);
		}

		appendProperties(sSource, aLocaleWriter);
	}

	/**
	 * Appends a certain properties file to a writer.
	 *
	 * @param sFileName     The name of the properties file
	 * @param aTargetWriter The target writer
	 * @throws IOException If an I/O operation fails
	 */
	private static void appendProperties(String sFileName,
		Writer aTargetWriter)
		throws IOException {
		writePropertiesHeader(sFileName, aTargetWriter);
		writeFile(sFileName, aTargetWriter);
	}

	/**
	 * Closes all elements of a collection.
	 *
	 * @param rCloseables A collection of {@link Closeable} elements
	 */
	private static void closeAll(Collection<? extends Closeable> rCloseables) {
		for (Closeable rCloseable : rCloseables) {
			try {
				rCloseable.close();
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
	 * @param rRootDir     The root directory to scan for directories
	 * @param rDirectories The collection to add the directories to
	 */
	private static void collectDirectories(File rRootDir,
		Collection<String> rDirectories) {
		if (rRootDir.exists()) {
			rDirectories.add(rRootDir.getPath() + File.separatorChar);

			for (File rFile : rRootDir.listFiles()) {
				if (rFile.isDirectory()) {
					collectDirectories(rFile, rDirectories);
				}
			}
		}
	}

	/**
	 * Concatenates the CSS files of the target application into a single large
	 * file.
	 *
	 * @param rDirectories    The resource directories from which to read the
	 *                        CSS files
	 * @param sTargetBaseName The base name for the target files to write to
	 * @throws IOException If reading or writing a file fails
	 */
	private static void concatenateCssFiles(Collection<String> rDirectories,
		String sTargetBaseName) throws IOException {
		String sTargetFile = sTargetBaseName + ".css";
		Writer aCssWriter = new BufferedWriter(new FileWriter(sTargetFile));
		boolean bFirstFile = true;

		try {
			for (String sDirectory : rDirectories) {
				File aDirectory = new File(sDirectory);

				PatternFilter rFilter = CSS_FILES_FILTER;
				String[] aCssFiles = aDirectory.list(rFilter);

				for (String sCssFile : aCssFiles) {
					if (bFirstFile) {
						System.out.printf("Writing %s\n", sTargetFile);
						bFirstFile = false;
					}

					String sHeader = String.format(
						"\n/*--------------------------------------------------------------------\n" +
							"  --- %s\n" +
							"  --------------------------------------------------------------------*/\n\n",
						sCssFile);

					aCssWriter.write(sHeader);
					writeFile(sDirectory + sCssFile, aCssWriter);
					System.out.printf(" + %s%s\n", sDirectory, sCssFile);
				}
			}
		} finally {
			aCssWriter.close();
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
	 * @param sAppName        rAppClass sAppName The application name
	 * @param rDirectories    The resource directories from which to read the
	 *                        property files
	 * @param sTargetBaseName The base name for the target files to write to
	 * @throws IOException If reading or writing a file fails
	 */
	private static void concatenatePropertyFiles(String sAppName,
		Collection<String> rDirectories, String sTargetBaseName)
		throws IOException {
		Map<String, Writer> aWriters = new HashMap<>();
		Map<String, Writer> aServerWriters = new HashMap<>();

		try {
			Set<String> aProcessedFiles = new HashSet<>();
			Writer aDefaultWriter = null;
			String sTarget = null;

			String sServerFileBaseName =
				String.format("%s/%sStrings", aParams.get(ARG_WEBAPP_DIR),
					sAppName);

			int nTotalLines = -1;
			int nFileCount = 0;
			int nMaxLines =
				Integer.parseInt(aParams.get(ARG_MAX_PROPERTY_LINES));

			Writer aServerWriter =
				createPropertiesWriter(sServerFileBaseName, null,
					aServerWriters);

			System.out.printf("Writing %s\n", sServerFileBaseName);

			for (String sDirectory : rDirectories) {
				Collection<String> aDefaultFiles =
					getDefaultPropertyFiles(sDirectory);

				// first concatenate the default files
				for (String sInputFile : aDefaultFiles) {
					String sFile = sDirectory + sInputFile;

					// don't process the same file twice
					if (!aProcessedFiles.contains(sFile)) {
						aProcessedFiles.add(sFile);

						int nLines = countLines(sFile);

						if (nTotalLines < 0 ||
							nTotalLines + nLines > nMaxLines) {
							sTarget = sTargetBaseName;
							nTotalLines = nLines;

							closeAll(aWriters.values());
							aWriters.clear();

							if (++nFileCount > 1) {
								sTarget += nFileCount;
							}

							aDefaultWriter =
								createPropertiesWriter(sTarget, null,
									aWriters);
							System.out.printf("Writing %s\n", sTarget);
						} else {
							nTotalLines += nLines;
						}

						appendProperties(sFile, aDefaultWriter);
						appendProperties(sFile, aServerWriter);
						System.out.printf(" + %s%s\n", sDirectory, sFile);

						// then lookup the locale-specific files for the
						// current
						// default file and concatenate them for each locale if
						// they exist
						int nDotPos = sInputFile.indexOf('.');

						String sLocalePattern =
							sInputFile.substring(0, nDotPos) + "_.*" +
								sInputFile.substring(nDotPos);

						String[] aLocaleFiles = new File(sDirectory).list(
							new PatternFilter(sLocalePattern));

						for (String sLocaleFile : aLocaleFiles) {
							int nLocaleIndex = sLocaleFile.indexOf('_');

							String sLocale =
								sLocaleFile.substring(nLocaleIndex + 1,
									sLocaleFile.indexOf('.'));

							String sSource = sDirectory + sLocaleFile;

							appendLocaleProperties(sSource, sTarget, sLocale,
								aWriters);

							appendLocaleProperties(sSource,
								sServerFileBaseName,
								sLocale, aServerWriters);

							System.out.printf(" + %s%s\n", sDirectory,
								sLocaleFile);
						}
					}
				}
			}
		} finally {
			closeAll(aWriters.values());
			closeAll(aServerWriters.values());
		}
	}

	/**
	 * Counts the lines in a text file.
	 *
	 * @param sFile The name of the file to count the lines of
	 * @return The number of lines in the file
	 * @throws IOException If reading from the file fails
	 */
	private static int countLines(String sFile) throws IOException {
		int nLines;

		try (LineNumberReader aReader = new LineNumberReader(
			new FileReader(sFile))) {
			while (aReader.skip(Long.MAX_VALUE) > 0) {
			}

			nLines = aReader.getLineNumber() + 1;
		}

		return nLines;
	}

	/**
	 * Creates a writer for the output of property files.
	 *
	 * @param sBaseName  The base name
	 * @param sLocale    The locale or NULL for none
	 * @param rWriterMap A map to store the writer in with the locale as the
	 *                     key
	 *                   (DEFAULT for NULL keys)
	 * @return The new writer
	 */
	private static Writer createPropertiesWriter(String sBaseName,
		String sLocale, Map<String, Writer> rWriterMap) throws IOException {
		StringBuilder aFileName = new StringBuilder(sBaseName);

		if (sLocale != null) {
			aFileName.append('_').append(sLocale);
		}

		aFileName.append(".properties");

		Writer aWriter =
			new BufferedWriter(new FileWriter(aFileName.toString()));

		rWriterMap.put(sLocale != null ? sLocale : "DEFAULT", aWriter);

		return aWriter;
	}

	/**
	 * Generates the application CSS classes by invoking
	 * {@link InterfaceGenerator}.
	 *
	 * @param rAppClass The application class
	 * @throws IOException If accessing files fails
	 */
	@SuppressWarnings("unused")
	private static void generateCssClasses(Class<?> rAppClass)
		throws IOException {
		String sPackage = rAppClass.getPackage().getName();
		String sDirectory = getBaseDir(rAppClass);

		String[] aCssFiles = new File(sDirectory).list(CSS_FILES_FILTER);

		for (String sFile : aCssFiles) {
			String sTarget = sFile.substring(0, sFile.indexOf('.'));

			sTarget = TextConvert.capitalizedIdentifier(sTarget);

			StringBuilder aTargetName = new StringBuilder(sPackage);

			aTargetName.append('.');
			aTargetName.append(sTarget);

			String[] aArgs =
				new String[] { "-standalone", "-css", sDirectory + sFile,
					"-typeName", aTargetName.toString() };

			System.out.printf("Generating %s.java from %s\n", aTargetName,
				sFile);

			ByteArrayOutputStream aGeneratedData = new ByteArrayOutputStream();

			PrintStream aCaptureOut = new PrintStream(aGeneratedData);
			PrintStream rStandardOut = System.out;

			System.setOut(aCaptureOut);
			CssInterfaceGenerator.main(aArgs);
			System.setOut(rStandardOut);
			aCaptureOut.flush();

			sTarget = sDirectory + sTarget + ".java";

			try (OutputStream aCssFile = new BufferedOutputStream(
				new FileOutputStream(sTarget))) {
				aGeneratedData.writeTo(aCssFile);
			}
		}
	}

	/**
	 * Generates the application strings classes by invoking {@link I18NSync}.
	 *
	 * @param rAppClass The application class
	 * @throws IOException If accessing files fails
	 */
	private static void generateStringClasses(Class<?> rAppClass) {
		String sPackage = rAppClass.getPackage().getName() + GENERATED_PACKAGE;

		Collection<String> aPropertyFiles =
			getDefaultPropertyFiles(getBaseDir(rAppClass) + GENERATED_DIR);

		for (String sFile : aPropertyFiles) {
			String sTargetClass =
				sPackage + sFile.substring(0, sFile.indexOf('.'));

			String[] aArgs = new String[] { sTargetClass, "-out",
				aParams.get(ARG_SOURCE_DIR), "-createConstantsWithLookup" };

			System.out.printf("Generating %s.java from %s\n", sTargetClass,
				sFile);
			I18NSync.main(aArgs);
		}
	}

	/**
	 * Generates the name of the base directory of a GEWT application. Always
	 * ends with a directory separator.
	 *
	 * @param rAppClass The application class
	 * @return The base directory of the app
	 */
	private static String getBaseDir(Class<?> rAppClass) {
		String sPackage = rAppClass.getPackage().getName();
		String sRootDir = rAppClass.getSimpleName();
		StringBuilder aBaseDir = new StringBuilder();
		char cDirSep = File.separatorChar;

		if (aProjectDirMap.containsKey(sRootDir)) {
			sRootDir = aProjectDirMap.get(sRootDir);
		}

		// ../<app-name>/<src-dir>/<package-path>/
		aBaseDir.append("..").append(cDirSep);
		aBaseDir.append(sRootDir).append(cDirSep);
		aBaseDir.append(aParams.get(ARG_SOURCE_DIR)).append(cDirSep);
		aBaseDir.append(sPackage.replace('.', cDirSep)).append(cDirSep);

		return aBaseDir.toString();
	}

	/**
	 * Returns a collection of the property files for the default locale (i.e.
	 * without a locale suffix) in a certain directory.
	 *
	 * @param sDirectory The name of the directory to read the files from
	 * @return A new collection of property files
	 */
	private static Collection<String> getDefaultPropertyFiles(
		String sDirectory) {
		File aDirectory = new File(sDirectory);
		List<String> aFiles = new ArrayList<>();

		for (String sFile : aDirectory.list(PROPERTY_FILES_FILTER)) {
			if (sFile.indexOf('_') == -1) {
				aFiles.add(sFile);
			}
		}

		return aFiles;
	}

	/**
	 * Returns the application resource directories to be searched for resource
	 * files.
	 *
	 * @param rAppClass     The class hierarchy
	 * @param rExtraDirs    Optional extra directories to add to the returned
	 *                      collection
	 * @param bIncludeRoots TRUE to include the root resource directories,
	 *                         FALSE
	 *                      to only include the sub-directories
	 * @return The class hierarchy
	 */
	private static Collection<String> getResourceDirs(Class<?> rAppClass,
		Collection<String> rExtraDirs, boolean bHierarchical,
		boolean bIncludeRoots) {
		Deque<String> aDirectories = new ArrayDeque<>();

		do {
			String sResourceDir =
				getBaseDir(rAppClass) + "res" + File.separatorChar;

			File aDirectory = new File(sResourceDir);

			if (aDirectory.exists()) {
				if (bIncludeRoots) {
					aDirectories.push(sResourceDir);
				}

				for (File rFile : aDirectory.listFiles()) {
					String sFilename = rFile.getName();

					if (rFile.isDirectory() &&
						!EXCLUDED_DIRS.contains(sFilename)) {
						aDirectories.push(rFile.getPath() + File.separatorChar);
					}
				}
			}
		} while (bHierarchical &&
			(rAppClass = rAppClass.getSuperclass()) != EWTEntryPoint.class);

		if (rExtraDirs != null) {
			for (String sDir : rExtraDirs) {
				File aDirectory = new File(sDir);

				collectDirectories(aDirectory, aDirectories);
			}
		}

		return aDirectories;
	}

	/**
	 * Executes this application.
	 *
	 * @param rArgs The app arguments
	 */
	public static void main(String[] rArgs) {
		aParams = parseCommandLine(rArgs);

		if (!aParams.containsKey(ARG_SOURCE_DIR)) {
			aParams.put(ARG_SOURCE_DIR, "src/main/java");
		}

		if (!aParams.containsKey(ARG_WEBAPP_DIR)) {
			aParams.put(ARG_WEBAPP_DIR, "src/main/webapp/data/res");
		}

		if (!aParams.containsKey(ARG_MAX_PROPERTY_LINES)) {
			aParams.put(ARG_MAX_PROPERTY_LINES, DEFAULT_MAX_PROPERTY_LINES);
		}

		try {
			String sProjectDir = aParams.get(ARG_PROJECT_DIR);
			boolean bHierarchical = false;

			if (aParams.containsKey(ARG_HIERARCHICAL)) {
				bHierarchical = true;
			}

			Class<?> rAppClass = Class.forName(aParams.get(ARG_APP_CLASS));

			if (sProjectDir != null) {
				aProjectDirMap.put(rAppClass.getSimpleName(), sProjectDir);
			}

			String sTargetDir = getBaseDir(rAppClass) + GENERATED_DIR;
			String sBaseName = rAppClass.getSimpleName();
			String sExtraDirs = aParams.get(ARG_EXTRA_DIRS);
			Set<String> aExtraDirs = new LinkedHashSet<>();

			if (sExtraDirs != null) {
				for (String sDir : sExtraDirs.split(",")) {
					aExtraDirs.add(sDir);
				}
			}

			String sPropertiesBase = sTargetDir + sBaseName + "Strings";
			String sCssBase = sTargetDir + sBaseName + "Css";

			Collection<String> aStringDirs =
				getResourceDirs(rAppClass, aExtraDirs, bHierarchical, true);
			Collection<String> aCssDirs =
				getResourceDirs(rAppClass, aExtraDirs, bHierarchical, false);

			concatenatePropertyFiles(rAppClass.getSimpleName(), aStringDirs,
				sPropertiesBase);
			concatenateCssFiles(aCssDirs, sCssBase);
			generateStringClasses(rAppClass);

			System.out.printf("OK\n");
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			printUsageAndStop(null);
		}
	}

	/**
	 * Parses the command line arguments into a map.
	 *
	 * @param rArgs The raw command line arguments
	 * @return A mapping from parameter names to parameter values
	 */
	private static Map<String, String> parseCommandLine(String[] rArgs) {
		Map<String, String> aArguments = new HashMap<>();

		if (rArgs.length == 0) {
			printUsageAndStop(null);
		}

		for (int i = 0; i < rArgs.length; i++) {
			String sArg = rArgs[i];

			if ("-?".equals(sArg) || "--help".equals(sArg)) {
				String rHelpArg = "help";

				if (i < rArgs.length - 1 &&
					SUPPORTED_ARGS.containsKey(rHelpArg)) {
					rHelpArg = rArgs[i + 1];
				}

				printUsageAndStop(rHelpArg);
			} else if (SUPPORTED_ARGS.containsKey(sArg)) {
				String sArgValue = null;

				if (FLAG_ARGS.contains(sArg)) {
					sArgValue = Boolean.TRUE.toString();
				} else if (i < rArgs.length - 1) {
					sArgValue = rArgs[++i];

					if (sArgValue.startsWith("-")) {
						printUsageAndStop(sArg);
					}
				} else {
					printUsageAndStop(null);
				}

				aArguments.put(sArg, sArgValue);
			}
		}

		return aArguments;
	}

	/**
	 * Prints usage information to the console and terminates this application
	 * by invoking {@link System#exit(int)}.
	 *
	 * @param sArgument The argument to display information for
	 */
	private static void printUsageAndStop(String sArgument) {
		System.out.printf("USAGE: %s [OPTIONS]\n",
			BuildAppResources.class.getSimpleName());

		if (sArgument != null) {
			Collection<String> rHelpArgs;

			if ("help".equals(sArgument)) {
				rHelpArgs = SUPPORTED_ARGS.keySet();
			} else {
				rHelpArgs = Collections.singletonList("-" + sArgument);
			}

			for (String sHelpArg : rHelpArgs) {
				System.out.printf("   %s%s\n",
					TextUtil.padRight(sHelpArg, 15, ' '),
					SUPPORTED_ARGS.get(sHelpArg));
			}
		}

		System.exit(1);
	}

	/**
	 * Reads a certain file and writes it to the given output stream.
	 *
	 * @param sFile   The name of the file to write
	 * @param rWriter The target output stream
	 * @throws IOException If creating the input stream or transferring data
	 *                     fails
	 */
	private static void writeFile(String sFile, Writer rWriter)
		throws IOException {
		try (BufferedReader aIn = new BufferedReader(new FileReader(sFile))) {
			String sLine;

			while ((sLine = aIn.readLine()) != null) {
				rWriter.write(sLine);
				rWriter.write("\n");
				rWriter.flush();
			}
		}
	}

	/**
	 * Writes a separating header into a target stream.
	 *
	 * @param sText   The header Text
	 * @param rWriter The output stream
	 * @throws IOException If writing data fails
	 */
	private static void writePropertiesHeader(String sText, Writer rWriter)
		throws IOException {
		String sHeader = String.format(
			"\n#--------------------------------------------------------------------\n" +
				"#--- %s\n" +
				"#--------------------------------------------------------------------\n\n",
			sText);

		rWriter.write(sHeader);
	}

	/**
	 * A filename filter based on regular expressions.
	 *
	 * @author eso
	 */
	static class PatternFilter implements FilenameFilter {

		private Pattern aPattern;

		/**
		 * Creates a new instance.
		 *
		 * @param sRegex The regular expression pattern
		 */
		public PatternFilter(String sRegex) {
			aPattern = Pattern.compile(sRegex);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean accept(File rDir, String sName) {
			return aPattern.matcher(sName).matches();
		}
	}
}

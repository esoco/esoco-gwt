//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2017 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.PrintWriter;

import java.net.MalformedURLException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.css.DefsCollector;
import com.google.gwt.resources.css.ExtractClassNamesVisitor;
import com.google.gwt.resources.css.GenerateCssAst;
import com.google.gwt.resources.css.InterfaceGenerator;
import com.google.gwt.resources.css.ast.CssStylesheet;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;
import com.google.gwt.util.tools.ArgHandlerFile;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;

/**
 * Variant of {@link InterfaceGenerator} that creates public interfaces and adds
 * a Jalopy exclusion comment (//J-) to the resulting file.
 */
public class CssInterfaceGenerator extends ToolBase {

	private static final Comparator<String> NAME_COMPARATOR =
		new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}
		};

	private static final TreeLogger.Type LOG_LEVEL = TreeLogger.WARN;

	private String interfaceName;

	private File inputFile;

	private TreeLogger logger;

	private boolean standaloneFile;

	/**
	 * @see InterfaceGenerator#InterfaceGenerator()
	 */
	private CssInterfaceGenerator() {
		registerHandler(new ArgHandlerAddPackageHeader());
		registerHandler(new ArgHandlerString() {
			@Override
			public String getPurpose() {
				return "The name of the generated CssResource subtype";
			}

			@Override
			public String getTag() {
				return "-typeName";
			}

			@Override
			public String[] getTagArgs() {
				return new String[] { "some.package.MyCssResource" };
			}

			@Override
			public boolean isRequired() {
				return true;
			}

			@Override
			public boolean setString(String str) {
				if (str.length() == 0) {
					return false;
				}

				if (!Character.isJavaIdentifierStart(str.charAt(0))) {
					return false;
				}

				for (int i = 1, j = str.length(); i < j; i++) {
					char c = str.charAt(i);

					if (!(Character.isJavaIdentifierPart(c) || c == '.')) {
						return false;
					}
				}

				interfaceName = str;

				if (logger.isLoggable(TreeLogger.DEBUG)) {
					logger.log(TreeLogger.DEBUG,
						"interfaceName = " + interfaceName);
				}

				return true;
			}
		});

		// -css in.css
		registerHandler(new ArgHandlerFile() {
			@Override
			public String getPurpose() {
				return "The input CSS file to process";
			}

			@Override
			public String getTag() {
				return "-css";
			}

			@Override
			public boolean isRequired() {
				return true;
			}

			@Override
			public void setFile(File file) {
				inputFile = file;

				if (logger.isLoggable(TreeLogger.DEBUG)) {
					logger.log(TreeLogger.DEBUG,
						"inputFile = " + file.getAbsolutePath());
				}
			}
		});
	}

	/**
	 * @see InterfaceGenerator#main(String[])
	 */
	public static void main(String[] rArgs) {
		(new CssInterfaceGenerator()).execImpl(rArgs);
	}

	/**
	 * Returns a description string
	 *
	 * @return The description
	 */
	@Override
	protected String getDescription() {
		return "Create a CssResource interface based on a CSS file";
	}

	/**
	 * @see InterfaceGenerator#execImpl(String[])
	 */
	private void execImpl(String[] args) {
		// Set up logging
		PrintWriter logWriter = new PrintWriter(System.err);

		logger = new PrintWriterTreeLogger(logWriter);

		((PrintWriterTreeLogger) logger).setMaxDetail(LOG_LEVEL);

		if (processArgs(args)) {
			try {
				System.out.println(process());
			} catch (MalformedURLException e) {
				logger.log(TreeLogger.ERROR, "Unable to load CSS", e);
			} catch (UnableToCompleteException e) {
				logger.log(TreeLogger.ERROR, "Unable to process CSS", e);
			} finally {
				// Make sure the logs are emitted
				logWriter.flush();
			}
		}
	}

	/**
	 * @see InterfaceGenerator#methodName(String)
	 */
	private String methodName(String className) {
		StringBuilder sb = new StringBuilder();
		char c = className.charAt(0);
		boolean nextUpCase = false;

		if (Character.isJavaIdentifierStart(c)) {
			sb.append(Character.toLowerCase(c));
		}

		for (int i = 1, j = className.length(); i < j; i++) {
			c = className.charAt(i);

			if (!Character.isJavaIdentifierPart(c)) {
				nextUpCase = true;

				continue;
			}

			if (nextUpCase) {
				nextUpCase = false;
				c = Character.toUpperCase(c);
			}

			sb.append(c);
		}

		return sb.toString();
	}

	/**
	 * @see InterfaceGenerator#process()
	 */
	private String process()
		throws MalformedURLException, UnableToCompleteException {
		// Create AST
		CssStylesheet sheet =
			GenerateCssAst.exec(logger, inputFile.toURI().toURL());

		// Sort all names
		Set<String> names = new TreeSet<String>(NAME_COMPARATOR);

		names.addAll(ExtractClassNamesVisitor.exec(sheet));

		DefsCollector defs = new DefsCollector();

		defs.accept(sheet);
		names.addAll(defs.getDefs());

		// Deduplicate method names
		Set<String> methodNames = new HashSet<String>();

		// Build the interface
		SourceWriter sw = new StringSourceWriter();

		int lastDot = interfaceName.lastIndexOf('.');

		if (standaloneFile) {
			sw.println("// DO NOT EDIT");
			sw.println("// Automatically generated by " +
				InterfaceGenerator.class.getName());
			sw.println("//J-");
			sw.println("package " + interfaceName.substring(0, lastDot) + ";");
			sw.println("import " + CssResource.class.getCanonicalName() + ";");
//			sw.println("import " + ClassName.class.getCanonicalName() + ";");
		}

		sw.println("public interface " + interfaceName.substring(lastDot + 1) +
			" extends CssResource {");
		sw.indent();

		for (String className : names) {
			String methodName = methodName(className);

			while (!methodNames.add(methodName)) {
				// Unusual, handles foo-bar and foo--bar
				methodName += "_";
			}

			sw.println();

			if (!methodName.equals(className)) {
				sw.println(
					"@ClassName(\"" + Generator.escape(className) + "\")");
			}

			sw.println("String " + methodName + "();");
		}

		sw.outdent();
		sw.println("}");

		return sw.toString();
	}

	/**
	 * @see InterfaceGenerator.ArgHandlerAddPackageHeader
	 */
	private class ArgHandlerAddPackageHeader extends ArgHandlerFlag {

		/**
		 * Creates a new instance.
		 */
		public ArgHandlerAddPackageHeader() {
			addTagValue("-standalone", true);
		}

		/**
		 * Returns the default value.
		 *
		 * @return The default value
		 */
		@Override
		public boolean getDefaultValue() {
			return standaloneFile;
		}

		/**
		 * Returns the label.
		 *
		 * @return The label
		 */
		@Override
		public String getLabel() {
			return "addPackageHeader";
		}

		/**
		 * Returns the purpose snippet.
		 *
		 * @return The purpose snippet
		 */
		@Override
		public String getPurposeSnippet() {
			return
				"Add package and import statements to generated interface so" +
					" " +
					"that " +
					"they are still functional when they stand alone.";
		}

		/**
		 * Sets the flag.
		 *
		 * @param value The new flag
		 * @return Always true
		 */
		@Override
		public boolean setFlag(boolean value) {
			standaloneFile = value;
			logger.log(TreeLogger.DEBUG,
				value ? "Not creating" : "Creating" + " a standalone file");

			return true;
		}
	}
}

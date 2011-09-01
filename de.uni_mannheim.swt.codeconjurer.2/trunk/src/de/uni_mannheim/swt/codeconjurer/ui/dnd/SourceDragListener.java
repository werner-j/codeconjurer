/*
 * Copyright (c) 2007-2011
 * University of Mannheim, Chair for Software-Engineering
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Werner Janjic -- initial development and documentation
 */
package de.uni_mannheim.swt.codeconjurer.ui.dnd;

import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.ui.view.PluginUI;

/**
 * @author Werner Janjic
 * 
 */
public class SourceDragListener implements DragSourceListener {

	private TreeViewer viewer;
	private Logger logger = Logger.getLogger(SourceDragListener.class);

	private TreeItem selection;
	private BodyDeclaration selectedElement;
	private String transferString;

	public SourceDragListener(TreeViewer viewer) {
		logger.debug("SourceDragListener registered!");
		this.viewer = viewer;
	}

	/**
	 * If something is selected a drag can start
	 */
	@Override
	public void dragStart(DragSourceEvent event) {
		if (CodeConjurer.getInstance().isBackgroundAgentEnabled()) {
			CodeConjurer.getInstance().getBackgroundAgentListener()
					.ignoreNextEvent(true);
		}
		selection = viewer.getTree().getSelection()[0];
		if (selection != null) {
			selectedElement = (BodyDeclaration) selection.getData();
			event.doit = (selectedElement != null);
		}
	}

	/**
	 * The source code of the selection
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
		/*
		 * String licText = selectedElement.getProperty(ResultProperty.LICENSE
		 * .name()) + ""; String license = ""; if
		 * (!licText.equals("no license")) { license =
		 * "// Code released under the terms of the " + licText + "\r\n"; }
		 * transferString = license + "// " +
		 * selectedElement.getProperty(ResultProperty.SHORT_URL.name()) + "\r\n"
		 * + selectedElement.toString(); if
		 * (TextTransfer.getInstance().isSupportedType(event.dataType)) {
		 * event.data = transferString; } logger.debug("Transfer data:\r\n" +
		 * event.data);
		 */
		event.data = new BodyDeclaration[] { selectedElement };
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		if (event.doit) {
			logger.debug("Drag of "
					+ viewer.getTree().getSelection()[0].getText()
					+ " finished. Format the Sourcecode properly...");
			// setContents();
		}
	}

	/**
	 * Sets the provided String as the editor's content
	 * 
	 * @param content
	 */
	private void setEditorContent(String content) {
		ICompilationUnit icu = (ICompilationUnit) JavaUI
				.getEditorInputJavaElement(PluginUI.getActiveEditor()
						.getEditorInput());
		try {
			icu.becomeWorkingCopy(null);
			icu.getBuffer().setContents(content);
			icu.reconcile(ICompilationUnit.NO_AST, false, null, null);
			icu.commitWorkingCopy(true, null);
			icu.discardWorkingCopy();
		} catch (JavaModelException e) {
			logger.debug("JavaModelException during editor clearance.\r\n"
					+ e.getLocalizedMessage());
		}
	}

	/**
	 * Set the content of the editor and format it if necessary
	 */
	private void setContents() {
		// TODO: this kind of code formatting seems very smelly.
		// Source:
		// http://help.eclipse.org/indigo/topic/org.eclipse.jdt.doc.isv/guide/jdt_api_manip.htm
		ICompilationUnit icu = (ICompilationUnit) JavaUI
				.getEditorInputJavaElement(PluginUI.getActiveEditor()
						.getEditorInput());
		String source = "";

		// If we inserted a whole class -- clear the editor's content
		if (selectedElement.getNodeType() == BodyDeclaration.TYPE_DECLARATION
				|| selectedElement.getNodeType() == BodyDeclaration.ENUM_DECLARATION) {
			setEditorContent(transferString);
		}

		if (selectedElement.getNodeType() == BodyDeclaration.METHOD_DECLARATION
				&& Activator.getDefault().getPreferenceStore()
						.getString(PreferenceConstants.P_OVERWRITE_ON_INSERT)
						.equals("true")) {
			MethodDeclaration selection = (MethodDeclaration) selectedElement;
			String signature = getMethodSignature(selection);
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setSource(icu);
			CompilationUnit cpu = (CompilationUnit) parser.createAST(null);
			Iterator<?> it = cpu.types().iterator();
			while (it.hasNext()) {
				TypeDeclaration type = (TypeDeclaration) it.next();
				for (MethodDeclaration m : type.getMethods()) {
					if (signature.equals(getMethodSignature(m))) {
						logger.debug("Found same method declaration!");
					}
				}
			}

		}

		// If we shouldn't format the source code -- exit.
		if (!Activator.getDefault().getPreferenceStore()
				.getString(PreferenceConstants.P_FORMAT).equals("true")) {
			return;
		}

		try {
			source = icu.getSource();
		} catch (JavaModelException e1) {
			logger.debug(e1.getLocalizedMessage());
		}

		// Take default Eclipse formatting options
		@SuppressWarnings("unchecked")
		Map<String, String> options = DefaultCodeFormatterConstants
				.getEclipseDefaultSettings();

		// initialize the compiler settings to be able to format 1.6
		// code
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
				JavaCore.VERSION_1_6);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);

		// change the option to wrap each enum constant on a new line
		options.put(
				DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT,
				DefaultCodeFormatterConstants.TRUE);

		// instantiate the default code formatter with the given options
		final CodeFormatter codeFormatter = ToolFactory
				.createCodeFormatter(options);
		final TextEdit editCode = codeFormatter.format(
				CodeFormatter.K_COMPILATION_UNIT
						| CodeFormatter.F_INCLUDE_COMMENTS, source, 0,
				source.length(), 0, System.getProperty("line.separator"));

		IDocument document = new Document(source);

		if (icu != null && document != null && editCode != null) {
			try {
				editCode.apply(document);
			} catch (MalformedTreeException e) {
				e.printStackTrace();
			} catch (BadLocationException e) {
				e.printStackTrace();
			}

			try {
				icu.becomeWorkingCopy(null);
				icu.getBuffer().setContents(document.get());
				icu.reconcile(ICompilationUnit.NO_AST, false, null, null);
				icu.commitWorkingCopy(true, null);
				icu.discardWorkingCopy();
			} catch (JavaModelException e) {
				logger.debug("Reconcile operation on ICompilationUnit failed.\r\n"
						+ e.getLocalizedMessage());
			}
		}
	}

	/**
	 * Creates a signature for the given MethodDeclaration
	 * 
	 * @param selection
	 * @return
	 */
	private String getMethodSignature(MethodDeclaration selection) {
		StringBuilder signature = new StringBuilder();
		signature.append(selection.getName());
		Iterator<?> it = selection.parameters().iterator();
		while (it.hasNext()) {
			signature.append(it.next().toString());
		}
		signature.append(selection.getReturnType2());
		return signature.toString();
	}
}

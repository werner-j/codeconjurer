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

import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;
import de.uni_mannheim.swt.codeconjurer.ui.view.PluginUI;

/**
 * @author Werner Janjic
 * 
 */
public class SourceDragListener implements DragSourceListener {

	private TreeViewer viewer;
	private Logger logger = Logger.getLogger(SourceDragListener.class);

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
		event.doit = (viewer.getTree().getSelection() != null);
	}

	/**
	 * The source code of the selection
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
		TreeItem selection = viewer.getTree().getSelection()[0];
		BodyDeclaration selectionData = (BodyDeclaration) selection.getData();
		if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
			event.data = selectionData.toString();
		}
		logger.debug("Transfer data:\r\n" + event.data);
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		logger.debug("Drag of " + viewer.getTree().getSelection()[0].getText()
				+ " finished. Format the Sourcecode properly...");

		// TODO: this kind of code formatting seems very smelly.
		// Source:
		// http://help.eclipse.org/indigo/topic/org.eclipse.jdt.doc.isv/guide/jdt_api_manip.htm
		ICompilationUnit icu = (ICompilationUnit) JavaUI
				.getEditorInputJavaElement(PluginUI.getActiveEditor()
						.getEditorInput());
		String source = "";
		try {
			source = icu.getSource();
		} catch (JavaModelException e) {
			CrashReporter.reportException(e);
			logger.debug(e.getLocalizedMessage());
		}

		// If we shouldn't format the source code -- exit.
		if (!Activator.getDefault().getPreferenceStore()
				.getString(PreferenceConstants.P_FORMAT).equals("true")) {
			return;
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
				CrashReporter.reportException(e);
			} catch (BadLocationException e) {
				CrashReporter.reportException(e);
			}

			try {
				icu.becomeWorkingCopy(null);
				icu.getBuffer().setContents(document.get());
				icu.reconcile(ICompilationUnit.NO_AST, false, null, null);
				icu.commitWorkingCopy(true, null);
				icu.discardWorkingCopy();
			} catch (JavaModelException e) {
				CrashReporter.reportException(e);
				logger.debug("Reconcile operation on ICompilationUnit failed.\r\n"
						+ e.getLocalizedMessage());
			}
		}
	}
}

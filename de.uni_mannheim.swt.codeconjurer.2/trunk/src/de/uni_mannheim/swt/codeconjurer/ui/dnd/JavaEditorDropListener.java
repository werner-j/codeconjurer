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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;
import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;
import de.uni_mannheim.swt.codeconjurer.ui.view.PluginUI;

/**
 * @author Werner Janjic
 * 
 */
public class JavaEditorDropListener implements DropTargetListener {

	private Logger logger = Logger.getLogger(JavaEditorDropListener.class);
	private static JavaEditorDropListener instance;

	private JavaEditorDropListener() {
		logger.debug("Listener created!");
	}

	/**
	 * Returns the <code>JavaEditorDropListener</code> of Code Conjurer that
	 * takes action on drop events on the Java Editor
	 * 
	 * @return
	 */
	public static JavaEditorDropListener getInstance() {
		if (instance == null) {
			instance = new JavaEditorDropListener();
		}
		return instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DropTargetListener#dragEnter(org.eclipse.swt.dnd.
	 * DropTargetEvent)
	 */
	@Override
	public void dragEnter(DropTargetEvent event) {
		logger.debug("Drag enter: " + event.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DropTargetListener#dragLeave(org.eclipse.swt.dnd.
	 * DropTargetEvent)
	 */
	@Override
	public void dragLeave(DropTargetEvent event) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DropTargetListener#dragOperationChanged(org.eclipse
	 * .swt.dnd.DropTargetEvent)
	 */
	@Override
	public void dragOperationChanged(DropTargetEvent event) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.dnd.DropTargetListener#dragOver(org.eclipse.swt.dnd.
	 * DropTargetEvent)
	 */
	@Override
	public void dragOver(DropTargetEvent event) {
		// TODO: auto generated
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.dnd.DropTargetListener#drop(org.eclipse.swt.dnd.
	 * DropTargetEvent)
	 */
	@Override
	public void drop(DropTargetEvent event) {
		if (event.data instanceof BodyDeclaration[]) {
			BodyDeclaration[] declarations = (BodyDeclaration[]) event.data;
			logger.debug("Dropped: \r\n" + declarations[0].toString());

			ICompilationUnit icu = (ICompilationUnit) JavaUI
					.getEditorInputJavaElement(PluginUI.getActiveEditor()
							.getEditorInput());

			for (BodyDeclaration decl : declarations) {
				try {
					// Insert a class into the editor
					switch (decl.getNodeType()) {
					case BodyDeclaration.TYPE_DECLARATION:
						insertDeclaration(icu, (TypeDeclaration) decl);
						break;
					case BodyDeclaration.METHOD_DECLARATION:
						insertDeclaration(icu, (MethodDeclaration) decl);
						break;
					default:
						break;
					}
				} catch (Exception e) {
					CrashReporter.reportException(e);
					logger.debug("Could not modify editor content: "
							+ e.getLocalizedMessage());
					e.printStackTrace();
				}
				logger.debug("Dropping finished");
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DropTargetListener#dropAccept(org.eclipse.swt.dnd
	 * .DropTargetEvent)
	 */
	@Override
	public void dropAccept(DropTargetEvent event) {
		// TODO Auto-generated method stub

	}

	private void insertDeclaration(ICompilationUnit target,
			MethodDeclaration methodDeclaration) throws JavaModelException,
			MalformedTreeException, BadLocationException {
		boolean overwrite = Activator.getDefault().getPreferenceStore()
				.getBoolean(PreferenceConstants.P_OVERWRITE_ON_INSERT);

		ICompilationUnit cpu = target.getWorkingCopy(null);
		// creation of DOM/AST from an ICompilationUnit
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(cpu);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

		// creation of ASTRewrite
		astRoot.recordModifications();
		IMethod method = null;
		String content = createPreambule(methodDeclaration) + "\r\n"
				+ methodDeclaration.toString();
		try {
			method = astRoot.getTypeRoot().findPrimaryType()
					.createMethod(content, null, overwrite, null);
		} catch (JavaModelException e) {
			CrashReporter.reportException(e);
			logger.debug("Method could not be created: "
					+ e.getLocalizedMessage());
		}

		if (method != null) {
			if (overwrite) {
				IMethod[] methods = astRoot.getTypeRoot().findPrimaryType()
						.findMethods(method);
				for (int i = 0; i < methods.length - 1; i++) {
					methods[i].delete(false, null);
				}
			}
		}

		// update of the compilation unit
		cpu.getBuffer().setContents(
				astRoot.getTypeRoot().findPrimaryType().getCompilationUnit()
						.getSource());
		cpu.reconcile(ICompilationUnit.NO_AST, false, null, null);
		cpu.commitWorkingCopy(false, null);
	}

	/**
	 * Inserts a TypeDeclaration into the target ICompilationUnit
	 * 
	 * @param target
	 * @param typeDeclaration
	 * @throws JavaModelException
	 * @throws MalformedTreeException
	 * @throws BadLocationException
	 */
	private void insertDeclaration(ICompilationUnit target,
			TypeDeclaration typeDeclaration) throws JavaModelException,
			MalformedTreeException, BadLocationException {
		ICompilationUnit icu = target.getWorkingCopy(null);

		// creation of DOM/AST from an ICompilationUnit
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(icu);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

		TypeDeclaration dropTypeDec = (TypeDeclaration) ASTNode.copySubtree(
				astRoot.getAST(), typeDeclaration);

		// creation of ASTRewrite
		AST ast = astRoot.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);

		// description of the change
		for (Object typeObj : astRoot.types()) {
			if (typeObj instanceof TypeDeclaration) {
				TypeDeclaration typeDec = (TypeDeclaration) typeObj;
				if (typeDec.getName().toString()
						.equals(dropTypeDec.getName().toString())) {
					logger.debug("Replace existing type declaration");
					if (Activator
							.getDefault()
							.getPreferenceStore()
							.getString(
									PreferenceConstants.P_OVERWRITE_ON_INSERT)
							.equals("true")) {
						rewrite.remove(typeDec, null);
					}
				}
			}
		}

		ASTNode a = ASTNode.copySubtree(ast, dropTypeDec);
		@SuppressWarnings("unchecked")
		List<TypeDeclaration> types = astRoot.types();
		ArrayList<TypeDeclaration> newTypes = new ArrayList<TypeDeclaration>();
		newTypes.add((TypeDeclaration) a);
		if (types.addAll(newTypes)) {
			if (!target.getElementName().equals(
					dropTypeDec.getName().toString() + ".java")) {
				List<?> modifiers = dropTypeDec.modifiers();
				for (Object mod : modifiers) {
					if (mod instanceof Modifier) {
						Modifier modifier = (Modifier) mod;
						if (modifier.getKeyword().toString().equals("public")) {
							dropTypeDec.modifiers().remove(modifier);
							break;
						}
					}
				}
			}
			String preambule = createPreambule(typeDeclaration) + "\r\n";
			icu.createType(preambule + dropTypeDec.toString(), null, true, null);
			logger.debug("New type added successfully");
		}

		String source = icu.getSource();
		Document document = new Document(source);

		// computation of the text edits
		TextEdit edits = rewrite.rewriteAST(document, icu.getJavaProject()
				.getOptions(true));

		// computation of the new source code
		if (edits != null) {
			edits.apply(document);
		}

		String newSource = document.get();

		// update of the compilation unit
		icu.getBuffer().setContents(newSource);
		icu.reconcile(ICompilationUnit.NO_AST, false, null, null);
		icu.commitWorkingCopy(false, null);
		icu.discardWorkingCopy();
	}

	/**
	 * Creates a preambule string for the insertion with source and license
	 * information
	 * 
	 * @param bodyDeclaration
	 */
	private String createPreambule(BodyDeclaration bodyDeclaration) {
		String content = "";
		String licenseType = (String) bodyDeclaration
				.getProperty(ResultProperty.LICENSE.name());
		if (licenseType != null && !licenseType.equals("no license")) {
			content += "// Released under the terms of the " + licenseType
					+ "\r\n";
		}
		content += "// Origin: "
				+ bodyDeclaration.getProperty(ResultProperty.SHORT_URL.name());
		return content;
	}

}

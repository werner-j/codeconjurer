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

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.text.edits.TextEdit;

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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub

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

			ICompilationUnit cpu = (ICompilationUnit) JavaUI
					.getEditorInputJavaElement(PluginUI.getActiveEditor()
							.getEditorInput());

			for (BodyDeclaration dec : declarations) {
				try {
					// Insert a class into the editor
					if (dec.getNodeType() == BodyDeclaration.TYPE_DECLARATION) {
						ICompilationUnit icu = cpu.getWorkingCopy(null);
						TypeDeclaration dropTypeDec = (TypeDeclaration) dec;
						String source = icu.getSource();
						Document document = new Document(source);

						// creation of DOM/AST from an ICompilationUnit
						ASTParser parser = ASTParser.newParser(AST.JLS3);
						parser.setSource(icu);
						CompilationUnit astRoot = (CompilationUnit) parser
								.createAST(null);

						// creation of ASTRewrite
						ASTRewrite rewrite = ASTRewrite
								.create(astRoot.getAST());

						// description of the change
						boolean exists = false;
						for (Object typeObj : astRoot.types()) {
							if (typeObj instanceof TypeDeclaration) {
								TypeDeclaration typeDec = (TypeDeclaration) typeObj;
								if (typeDec
										.getName()
										.toString()
										.equals(dropTypeDec.getName()
												.toString())) {
									logger.debug("Replace existing type declaration");
									rewrite.replace(typeDec, dropTypeDec, null);
									exists = true;
								}
								if (!exists) {

								}
							}
						}

						// computation of the text edits
						TextEdit edits = rewrite.rewriteAST(document, icu
								.getJavaProject().getOptions(true));

						// computation of the new source code
						edits.apply(document);
						String newSource = document.get();

						// update of the compilation unit
						icu.getBuffer().setContents(newSource);
						icu.reconcile(ICompilationUnit.NO_AST, false, null,
								null);
						icu.commitWorkingCopy(false, null);
						icu.discardWorkingCopy();
					}
				} catch (Exception e) {
					logger.debug("Could not modify editor content: "
							+ e.getMessage());
					e.printStackTrace();
				}
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

}

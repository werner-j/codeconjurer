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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultItem;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;
import de.uni_mannheim.swt.codeconjurer.domain.search.Search;
import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;

/**
 * 
 * The class <code>BodyDeclarationTransfer</code> provides the possibility to
 * transfer classes or methods from a result view to the Eclipse java editor.
 * 
 * Therefore the <code>SHORT_URL</code> attribute of the
 * <code>BodyDeclaration</code> selected in the result view is transferred and
 * then looked up in the results to insert it into the editor.
 * 
 * @author Werner Janjic
 * 
 */
public class BodyDeclarationTransfer extends ByteArrayTransfer {

	private Logger logger = Logger.getLogger(BodyDeclarationTransfer.class);

	private static final String MYTYPENAME = "body_declaration_tranfer";
	private static final int MYTYPEID = registerType(MYTYPENAME);
	private static BodyDeclarationTransfer _instance = new BodyDeclarationTransfer();

	private BodyDeclarationTransfer() {
		logger.debug("Instantiated BodyDeclarationTransfer");
	}

	public static BodyDeclarationTransfer getInstance() {
		return _instance;
	}

	public void javaToNative(Object object, TransferData transferData) {
		if (object == null || !(object instanceof BodyDeclaration[]))
			return;

		if (isSupportedType(transferData)) {
			logger.debug("Transfer type is supported -- beam to destination");
			BodyDeclaration[] myTypes = (BodyDeclaration[]) object;
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream writeOut = new DataOutputStream(out);
				for (int i = 0, length = myTypes.length; i < length; i++) {
					String uri = myTypes[i].getProperty(
							ResultProperty.URI.name()).toString();
					logger.debug("Transfer " + uri);
					byte[] buffer = uri.getBytes();
					writeOut.writeInt(buffer.length);
					writeOut.write(buffer);
				}
				byte[] buffer = out.toByteArray();
				writeOut.close();
				super.javaToNative(buffer, transferData);
			} catch (IOException e) {
				CrashReporter.reportException(e);
				logger.debug("Failed to convert to native.");
			}
		}
	}

	public Object nativeToJava(TransferData transferData) {

		if (isSupportedType(transferData)) {

			byte[] buffer = (byte[]) super.nativeToJava(transferData);
			if (buffer == null)
				return null;

			BodyDeclaration[] bodyDec = new BodyDeclaration[1];
			try {
				ByteArrayInputStream in = new ByteArrayInputStream(buffer);
				DataInputStream readIn = new DataInputStream(in);
				while (readIn.available() > 20) {
					Search search = CodeConjurer.getInstance()
							.getActiveEditorSearch();
					int size = readIn.readInt();
					byte[] uri = new byte[size];
					readIn.read(uri);
					String uriString = new String(uri);
					logger.debug("Rematerialize " + uri);
					// The end of the URI substring depends on whether there is
					// a URI delimiter or not
					int endIdx = uriString.length();
					if (uriString.contains(CodeConjurer.URI_DELIMITER)) {
						endIdx = uriString.indexOf(CodeConjurer.URI_DELIMITER);
					}
					ResultItem result = search.getSearchResult().getResultItem(
							uriString.substring(0, endIdx));
					bodyDec[0] = result.find(uriString);
					logger.debug("Rematerialized \r\n" + bodyDec.toString());
				}
				readIn.close();
			} catch (IOException e) {
				CrashReporter.reportException(e);
				logger.debug("Could not materialize transfer data!");
				return null;
			}
			return bodyDec;
		}

		return null;
	}

	protected String[] getTypeNames() {
		return new String[] { MYTYPENAME };
	}

	protected int[] getTypeIds() {
		return new int[] { MYTYPEID };
	}

}

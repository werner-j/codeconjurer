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
package de.uni_mannheim.swt.codeconjurer.techsrv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;

import com.merotronics.merobase.ws.action.IOException_Exception;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;

/**
 * This class is used to send crash reports of Code Conjurer to the maintainers
 * to detect bugs.
 * 
 * @author Werner Janjic
 */
public class CrashReporter {

	private static Logger logger = Logger.getLogger(CrashReporter.class);

	public static boolean reportException(IOException_Exception ex) {
		return reportException(ex.getCause());
	}

	/**
	 * Report an exception
	 * 
	 * @param ex
	 * @return
	 */
	public static boolean reportException(Throwable ex) {
		return reportException(ex, null);
	}

	/**
	 * Report an exception and accompany it with an arbitrary cause string and
	 * supplementary information
	 * 
	 * @param ex
	 * @param cause
	 * @param suppl
	 * @return
	 */
	public static boolean reportException(Throwable ex, String cause,
			HashMap<String, String> suppl) {
		suppl.put("Cause", cause);
		return reportException(ex, suppl);
	}

	/**
	 * Send a stacktrace of the exception to the report-collection system
	 * 
	 * @param ex
	 * @return
	 */
	public static boolean reportException(Throwable ex,
			HashMap<String, String> suppl) {

		// Check if we should send reports
		if (Activator.getDefault().getPreferenceStore()
				.getBoolean(PreferenceConstants.P_CRASH_REPORTING)) {
			logger.debug("Report exception to devs...");
			// Build parameter string
			String data = "reportType=exception&" + "message="
					+ ex.getMessage();
			data += "&build="
					+ Platform.getBundle("de.uni_mannheim.swt.codeconjurer")
							.getHeaders().get("Bundle-Version");
			int ln = 0;
			for (StackTraceElement el : ex.getStackTrace()) {
				data += "&st_line_" + ++ln + "=" + el.getClassName() + "#"
						+ el.getMethodName() + "<" + el.getLineNumber() + ">";
			}
			data += "&lines=" + ln;
			data += "&Suppl-Eclipse-Version="
					+ Activator.getDefault().getPreferenceStore()
							.getString(PreferenceConstants.P_PLATFORM_VERSION);
			data += "&Suppl-Description=" + ex.toString();
			data += "&Suppl-Server="
					+ Activator.getDefault().getPreferenceStore()
							.getString(PreferenceConstants.P_SERVER);
			data += "&Suppl-User="
					+ Activator.getDefault().getPreferenceStore()
							.getString(PreferenceConstants.P_USERNAME);
			if (suppl != null) {
				for (String key : suppl.keySet()) {
					data += "&Suppl-" + key + "=" + suppl.get(key);
				}
			}

			try {

				// Send the request
				URL url = new URL(
						"http://www.merobase.com:7777/org.code_conjurer.udc/CrashReport");
				URLConnection conn = url.openConnection();
				conn.setDoOutput(true);
				OutputStreamWriter writer = new OutputStreamWriter(
						conn.getOutputStream());

				// Send parameters to server
				writer.write(data);
				writer.flush();

				// Get the server's response
				StringBuffer answer = new StringBuffer();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(conn.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					answer.append(line + "\r\n");
				}
				writer.close();
				reader.close();

				// Log the response
				logger.debug(answer.toString());

			} catch (Exception e) {
				logger.debug("Could not report exception");
				return false;
			}

			return true;
		} else {
			logger.debug("Reporting not wished!");
			return false;
		}

	}
}

/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.boot.maven.analyzer.server;

public class TypeGraphResult {

	public final byte[] xmlData;
	public final byte[] logData;

	public TypeGraphResult(byte[] xmlData, byte[] logData) {
		this.xmlData = xmlData;
		this.logData = logData;
	}

}

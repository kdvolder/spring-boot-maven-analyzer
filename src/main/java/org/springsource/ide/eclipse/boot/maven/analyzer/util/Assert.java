/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.boot.maven.analyzer.util;

/**
 * Some 'assert' methods similar the ones in org.eclipse.core.runtime.Assert class.
 * So we can take the code that uses them out of eclipse context without changing it too 
 * much.
 */
public class Assert {

	public static void isNotNull(Object x) {
		if (x==null) {
			throw new IllegalArgumentException("Shoud not be null");
		}
	}

	public static void isLegal(boolean check) {
		if (!check) {
			throw new AssertionError();
		}
	}

	public static void isTrue(boolean check) {
		if (!check) {
			throw new AssertionError();
		}
	}

}

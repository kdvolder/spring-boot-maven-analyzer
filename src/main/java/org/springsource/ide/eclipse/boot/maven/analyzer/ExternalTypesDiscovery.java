package org.springsource.ide.eclipse.boot.maven.analyzer;

import org.springsource.ide.eclipse.boot.maven.analyzer.util.Requestor;

public interface ExternalTypesDiscovery {

	void getTypes(Requestor<ExternalTypeEntry> requestor);

}

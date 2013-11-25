package org.springsource.ide.eclipse.boot.maven.analyzer.util;


public interface ExternalTypesDiscovery {

	void getTypes(Requestor<ExternalTypeEntry> requestor);

}

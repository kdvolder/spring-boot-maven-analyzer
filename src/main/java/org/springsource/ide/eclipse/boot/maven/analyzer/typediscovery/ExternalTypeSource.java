package org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery;

public interface ExternalTypeSource {

	ExternalTypeSource UNKNOWN = new ExternalTypeSource() {
		public String toString() { return "UNKNOWN"; };
	};

}

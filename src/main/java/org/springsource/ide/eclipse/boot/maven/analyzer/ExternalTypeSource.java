package org.springsource.ide.eclipse.boot.maven.analyzer;

public interface ExternalTypeSource {

	ExternalTypeSource UNKNOWN = new ExternalTypeSource() {
		public String toString() { return "UNKNOWN"; };
	};

}

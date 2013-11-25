package org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery;

import org.springsource.ide.eclipse.boot.maven.analyzer.util.ExternalTypeEntry;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.ExternalTypesDiscovery;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Requestor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class TypeCollector {
	
	Multimap<ExternalType, ExternalTypeSource> types = HashMultimap.create();
	
	public void addTypesFrom(ExternalTypesDiscovery discoverer) {
		discoverer.getTypes(new Requestor<ExternalTypeEntry>() {
			public boolean receive(ExternalTypeEntry element) {
				types.put(element.getType(), element.getSource());
				return true; //want more
			}
		});
	}

	public int getTypeCount() {
		return types.keySet().size();
	}

	public Multimap<ExternalType, ExternalTypeSource> getIndex() {
		return types;
	}
	
}

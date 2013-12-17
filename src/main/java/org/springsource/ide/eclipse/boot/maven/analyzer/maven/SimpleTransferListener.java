package org.springsource.ide.eclipse.boot.maven.analyzer.maven;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sonatype.aether.transfer.TransferCancelledException;
import org.sonatype.aether.transfer.TransferEvent;
import org.sonatype.aether.transfer.TransferListener;

public class SimpleTransferListener implements TransferListener {

	static Log log = LogFactory.getLog(SimpleTransferListener.class);
	
	@Override
	public void transferInitiated(TransferEvent event)
			throws TransferCancelledException {
		// TODO Auto-generated method stub
	}

	@Override
	public void transferStarted(TransferEvent event)
			throws TransferCancelledException {
//		System.out.println("Transferring: "+event.getResource());
	}

	@Override
	public void transferProgressed(TransferEvent event)
			throws TransferCancelledException {
		// TODO Auto-generated method stub

	}

	@Override
	public void transferCorrupted(TransferEvent event)
			throws TransferCancelledException {
		// TODO Auto-generated method stub

	}

	@Override
	public void transferSucceeded(TransferEvent event) {
		log.info("Transferred: "+event.getResource());
	}

	@Override
	public void transferFailed(TransferEvent event) {
//		System.out.println("      FAILED: "+event.getResource());
//		System.out.println("       "+event.getException());
	}

}

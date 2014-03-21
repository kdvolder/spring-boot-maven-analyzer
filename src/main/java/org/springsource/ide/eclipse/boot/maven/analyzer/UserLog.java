package org.springsource.ide.eclipse.boot.maven.analyzer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Process that compute some stuff for a rest end point may also write some info
 * to a kind of log. The log will be exposed via a related end point.
 * <p>
 * This class just implements some convenenience methods to write info to the log.
 * 
 * @author Kris De Volder
 */
public class UserLog {
	
	/**
	 * Server log... not accessible via end points only on the server itself.
	 * Yeah... the log has a log. It's weird. But we need a place to write errors
	 * that happen writing to the log. The log's log is for debugging the app.
	 * The UserLog is for information the user of the app may care about instead.
	 */
	static Log log = LogFactory.getLog(UserLog.class);
	
	private OutputStreamWriter writer;

	public UserLog(OutputStream out) {
		try {
			this.writer = new OutputStreamWriter(out, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("Unexpected error", e);
		}
	}

	public void println(String str) {
		try {
			writer.write(str);
			writer.write("\n");
		} catch (IOException e){
			log.error(e);
		}
	}

	public synchronized void dispose() {
		if (writer!=null) {
			try {
				writer.close();
			} catch (IOException e) {
			} finally {
				writer = null;
			}
		}
	}

}

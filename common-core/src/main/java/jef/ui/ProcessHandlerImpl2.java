package jef.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import jef.tools.Exceptions;
import jef.ui.Commands.Interacts;

final class ProcessHandlerImpl2 implements ProcessHandler {
	private static Logger log = Logger.getLogger(ProcessHandlerImpl2.class.getName());
	private final Process proc;
	private final BufferedWriter bw;
	private final InputStream stdOut;
	private final Thread stdThread;
	private Interacts reveiver = DYMMY;

	private static final Interacts DYMMY = new Interacts() {
		public void onStdOut(String line) {
			System.out.println(line);
		}

		public void onStdErr(String line) {
			System.out.println("[ERR]" + line);
		}
	};

	final class Reader extends Thread {
		private InputStream is;
		private boolean stdout;

		public Reader(InputStream b, boolean stdout) {
			this.is = b;
			this.stdout = stdout;
		}

		public final void run() {
			if (is == null) {
				throw new IllegalStateException("The input stream is null.");
			}
			doTask();
		}

		protected void doTask() {
			try {
				BufferedReader isr = new BufferedReader(new InputStreamReader(is));
				String line;
				while ((line = isr.readLine()) != null) {
					reveiver.handler.set(ProcessHandlerImpl2.this);
					if (stdout) {
						reveiver.onStdOut(line);
					} else {
						reveiver.onStdErr(line);
					}
				}
			} catch (IOException ioe) {
				log.log(Level.SEVERE, "send command error.", ioe);
			}
		}
	}

	public ProcessHandlerImpl2(ProcessBuilder pb, Interacts transact) {
		if (transact != null)
			this.reveiver = transact;
		try {
			pb.redirectErrorStream(true);
			this.proc = pb.start();
		} catch (IOException e) {
			throw Exceptions.toIllegalArgument(e);
		}
		this.bw = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
		this.stdOut = proc.getInputStream();
		stdThread = new Reader(stdOut, true);
		stdThread.start();
	}

	public void kill() {
//		if (proc.isAlive()) {
			proc.destroy();
//		}
	}

	/**
	 * 向进程发送命令
	 * 
	 * @param command
	 * @return
	 */
	public void sendCommand(String command) {
		try {
			bw.write(command + "\n");
			bw.flush();
		} catch (IOException e) {
			log.log(Level.SEVERE, "send command error.", e);
		}
	}

	public void close() {
		closeQuietly(this.bw);
		closeQuietly(this.stdOut);
	}

	private void closeQuietly(Closeable c) {
		try {
			c.close();
		} catch (IOException e) {
			log.log(Level.WARNING, "close error.", e);
			e.printStackTrace();
		}
	}

	public int waitfor() {
		try {
			int status = proc.waitFor();
			reveiver.handler.remove();
			return status;
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		} finally {
			close();
		}
	}
}
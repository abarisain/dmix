package org.a0z.mpd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.a0z.mpd.exception.MPDConnectionException;
import org.a0z.mpd.exception.MPDServerException;

import android.util.Log;

/**
 * Class representing a connection to MPD Server.
 * 
 * @version $Id: MPDConnection.java 2941 2005-02-09 02:34:21Z galmeida $
 */
public class MPDConnection {
	private static final int CONNECTION_TIMEOUT = 1000;
	
	private static final String MPD_RESPONSE_ERR = "ACK";
	private static final String MPD_RESPONSE_OK = "OK";
	private static final String MPD_CMD_START_BULK = "command_list_begin";
	private static final String MPD_CMD_END_BULK = "command_list_end";

	private InetAddress hostAddress;
	private int hostPort;
	
	private Socket sock;
	private InputStreamReader inputStream;
	private OutputStreamWriter outputStream;

	private int[] mpdVersion;
	private StringBuffer commandQueue;
	
	
	MPDConnection(InetAddress server, int port) throws MPDServerException {
		hostPort = port;
		hostAddress = server;
		commandQueue = new StringBuffer();
		
		// connect right away and setup streams
		try {
			mpdVersion = this.connect();
			
			// Use UTF-8 when needed
			if (mpdVersion[0] > 0 || mpdVersion[1] >= 10) {
				outputStream = new OutputStreamWriter(sock.getOutputStream(), "UTF-8");
				inputStream = new InputStreamReader(sock.getInputStream(), "UTF-8");
			} else {
				outputStream = new OutputStreamWriter(sock.getOutputStream());
				inputStream = new InputStreamReader(sock.getInputStream());
			}
		} catch(IOException e) {
			throw new MPDServerException(e.getMessage(), e);
		}
	}

	final synchronized private int[] connect() throws MPDServerException, IOException {
		sock = new Socket();
		sock.connect(new InetSocketAddress(hostAddress, hostPort), CONNECTION_TIMEOUT);
		BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()), 1024);
		String line = in.readLine();
		
		if (line == null) {
			throw new MPDServerException("No response from server");
		} else if (line.startsWith(MPD_RESPONSE_OK)) {
			String[] tmp = line.substring((MPD_RESPONSE_OK + " MPD ").length(), line.length()).split("\\.");
			int[] result = new int[tmp.length];
			
			for (int i = 0; i < tmp.length; i++)
				result[i] = Integer.parseInt(tmp[i]);
			
			return result;
		} else if (line.startsWith(MPD_RESPONSE_ERR)) {
			throw new MPDServerException("Server error: " + line.substring(MPD_RESPONSE_ERR.length()));
		} else {
			throw new MPDServerException("Bogus response from server");
		}
	}
	
	synchronized void disconnect() throws MPDServerException {
		if(isConnected())
			try {
				sock.close();
			} catch(IOException e) {
				throw new MPDServerException(e.getMessage(), e);
			}
	}

	boolean isConnected() {
		return (sock != null && sock.isConnected() && !sock.isClosed());
	}
	
	int[] getMpdVersion() {
		return mpdVersion;
	}
	
	
	synchronized List<String> sendCommand(String command, String... args) throws MPDServerException {
		return sendRawCommand(commandToString(command, args));
	}
	synchronized List<String> sendCommand(boolean expectAnswer,String command, String... args) throws MPDServerException {
		return sendRawCommand(expectAnswer,commandToString(command, args));
	}
	synchronized void queueCommand(String command, String ... args) {
		commandQueue.append(commandToString(command, args));
	}

	synchronized List<String> sendCommandQueue() throws MPDServerException {
		String command = MPD_CMD_START_BULK + "\n" + commandQueue.toString() + MPD_CMD_END_BULK + "\n";
		commandQueue = new StringBuffer();
		return sendRawCommand(command);
	}

	private synchronized List<String> sendRawCommand(String command) throws MPDServerException {
		return this.sendRawCommand(true,command);
	}
	
	private synchronized List<String> sendRawCommand(boolean expectAnswer,String command) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("No connection to server");

		try {
			ArrayList<String> result = new ArrayList<String>();
			
			// send command
			outputStream.write(command);
			outputStream.flush();
			if (!expectAnswer) {
				return result;
			}
			// wait for answer
			BufferedReader in = new BufferedReader(inputStream, 1024);
			boolean anyResponse = false;
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				anyResponse = true;
				if (line.startsWith(MPD_RESPONSE_OK))
					break;
				
				if (line.startsWith(MPD_RESPONSE_ERR))
					throw new MPDServerException("Server error: " + line.substring(MPD_RESPONSE_ERR.length()));
				
				result.add(line);
			}
			
			// Close socket if there is no response... Something is wrong (e.g. MPD shutdown..)
			if (!anyResponse) {
				sock.close();
				throw new MPDConnectionException("Connection lost");
			}

			return result;
		} catch (SocketException e) {
			//this.sock = null; // isn't it too dangerous ?
			try{
				this.sock.close();//trying to close nicely
			}catch (IOException er) {
				throw new MPDServerException(e.getMessage(), er);
			}
			throw new MPDConnectionException("Connection lost", e);
		} catch (IOException e) {
			throw new MPDServerException(e.getMessage(), e);
		}
	}

	private static String commandToString(String command, String[] args) {
		StringBuffer outBuf = new StringBuffer();
		outBuf.append(command);
		for (String arg : args) {
			if(arg == null)
				continue;
			outBuf.append(" \"" + arg + "\"");
		}
		outBuf.append("\n");
		final String outString = outBuf.toString();
		Log.d("JMPDComm", "Mpd command : " + (outString.startsWith("password ") ? "password **censored**" : outString));
		return outString;
	}

}

package org.a0z.mpd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a connection to MPD Server.
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDConnection.java 2941 2005-02-09 02:34:21Z galmeida $
 */
public class MPDConnection {

    private static final String MPD_RESPONSE_ERR = "ACK";

    private static final String MPD_RESPONSE_OK = "OK";

    private static final String MPD_CMD_START_BULK = "command_list_begin";

    private static final String MPD_CMD_END_BULK = "command_list_end";

    private InetAddress hostAddress;

    private int hostPort;

    private Socket sock;

    private int[] mpdVersion;

    private StringBuffer commandQueue;

    MPDConnection(String server, int port) throws MPDServerException {
        try {
            this.hostPort = port;
            this.hostAddress = InetAddress.getByName(server);
            this.mpdVersion = this.connect();
            this.commandQueue = new StringBuffer();
        } catch (UnknownHostException e) {
            throw new MPDServerException(e.getMessage(), e);
        }
    }

    final synchronized int[] connect() throws MPDServerException {
        try {

            this.sock = new Socket();
            sock.connect(new InetSocketAddress(hostAddress, hostPort), 1000);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()), 1024);
            String line = in.readLine();
            if (line == null) {
                throw new MPDServerException("No response from server");
            } else if (line.startsWith(MPD_RESPONSE_OK)) {
                String[] tmp = line.substring((MPD_RESPONSE_OK + " MPD ").length(), line.length()).split("\\.");
                int[] result = new int[tmp.length];
                for (int i = 0; i < tmp.length; i++) {
                    result[i] = Integer.parseInt(tmp[i]);
                }
                return result;
            } else if (line.startsWith(MPD_RESPONSE_ERR)) {
                throw new MPDServerException("Server error: " + line.substring(MPD_RESPONSE_ERR.length()));
            } else {
                throw new MPDServerException("Bogus response from server");
            }
        } catch (IOException e) {
            throw new MPDServerException(e.getMessage(), e);
        }
    }

    List<String> sendCommand(String command, String[] args) throws MPDServerException {
        return sendRawCommand(commandToString(command, args));
    }

    private synchronized List<String> sendRawCommand(String command) throws MPDServerException {
        if (sock == null || !sock.isConnected()) {
            throw new MPDServerException("No connection to server");
        }

        try {
            ArrayList<String> list = new ArrayList<String>();
            //TODO debug
            //System.out.print("> " + command);
            InputStreamReader inputStreamReader;

            //Use UTF-8 when needed
            if (mpdVersion[0] > 0 || mpdVersion[1] >= 10) {
                sock.getOutputStream().write(command.getBytes("UTF-8"));
                inputStreamReader = new InputStreamReader(sock.getInputStream(), "UTF-8");
            } else {
                sock.getOutputStream().write(command.getBytes());
                inputStreamReader = new InputStreamReader(sock.getInputStream());
            }
            BufferedReader in = new BufferedReader(inputStreamReader,1024);
            boolean anyResponse = false;
            for (String line = in.readLine(); line != null; line = in.readLine()) {
            	anyResponse = true;
                if (line.startsWith(MPD_RESPONSE_OK)) {
                    break;
                }
                if (line.startsWith(MPD_RESPONSE_ERR)) {
                    throw new MPDServerException("Server error: " + line.substring(MPD_RESPONSE_ERR.length()));
                }
                list.add(line);
                //TODO debug
                //System.out.println("> " + line);
            }
            // Close socket if there is no response... Something is wrong (e.g. MPD shutdown..)
            if(!anyResponse)
            {
            	sock.close();
                throw new MPDConnectionException("Connection lost");
            }
            
            return list;
        } catch (SocketException e) {
            this.sock = null;
            throw new MPDConnectionException("Connection lost", e);
        } catch (IOException e) {
            throw new MPDServerException(e.getMessage(), e);
        }
    }

    List<String> sendCommand(String command) throws MPDServerException {
        return sendCommand(command, null);
    }

    synchronized void disconnect() throws IOException {
        sock.close();
    }

    boolean isConnected() {
        return (sock != null && sock.isConnected() && !sock.isClosed());
    }

    int[] getMpdVersion() {
        return mpdVersion;
    }

    synchronized void queueCommand(String command, String[] args) {
        commandQueue.append(commandToString(command, args));
    }

    synchronized List sendCommandQueue() throws MPDServerException {
        String command = MPD_CMD_START_BULK + "\n" + commandQueue.toString() + MPD_CMD_END_BULK + "\n";
        this.commandQueue = new StringBuffer();
        return sendRawCommand(command);
    }

    private static String commandToString(String command, String[] args) {
        //TODO testar se o commando eh valido
        StringBuffer outBuf = new StringBuffer();
        outBuf.append(command);
        for (int i = 0; args != null && i < args.length; i++) {
            outBuf.append(" \"" + args[i] + "\"");
        }
        outBuf.append("\n");
        return outBuf.toString();
    }

}

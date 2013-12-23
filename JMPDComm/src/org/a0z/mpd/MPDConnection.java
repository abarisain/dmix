package org.a0z.mpd;

import org.a0z.mpd.exception.MPDConnectionException;
import org.a0z.mpd.exception.MPDNoResponseException;
import org.a0z.mpd.exception.MPDServerException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.util.Log.e;
import static android.util.Log.w;

/**
 * Class representing a connection to MPD Server.
 *
 * @version $Id: MPDConnection.java 2941 2005-02-09 02:34:21Z galmeida $
 */
public abstract class MPDConnection {
    private static final int CONNECTION_TIMEOUT = 10000;

    private static final String MPD_RESPONSE_ERR = "ACK";
    private static final String MPD_RESPONSE_OK = "OK";
    private static final String MPD_CMD_START_BULK = "command_list_begin";
    private static final String MPD_CMD_START_BULK_OK = "command_list_ok_begin";
    private static final String MPD_CMD_BULK_SEP = "list_OK";
    private static final String MPD_CMD_END_BULK = "command_list_end";
    public static final String POOL_THREAD_NAME_PREFIX = "pool";

    private InetAddress hostAddress;
    private int hostPort;

    private int[] mpdVersion;
    private List<MPDCommand> commandQueue;
    private int readWriteTimeout;

    private ExecutorService executor;
    private int maxThreads;

    protected boolean cancelled = false;

    private final static int MAX_CONNECT_RETRY = 3;
    private final static int MAX_REQUEST_RETRY = 3;

    private String password = null;

    MPDConnection(InetAddress server, int port, String password, int readWriteTimeout) throws MPDServerException {
        this(server, port, readWriteTimeout, 1, password);
    }

    protected abstract OutputStreamWriter getOutputStream();

    protected abstract void setOutputStream(OutputStreamWriter outputStream);

    protected abstract InputStreamReader getInputStream();

    protected abstract void setInputStream(InputStreamReader inputStream);

    protected abstract Socket getSocket();

    protected abstract void setSocket(Socket socket);

    MPDConnection(InetAddress server, int port, int readWriteTimeout, int maxConnections, String password) throws MPDServerException {
        this.readWriteTimeout = readWriteTimeout;
        hostPort = port;
        hostAddress = server;
        commandQueue = new ArrayList<MPDCommand>();
        maxThreads = maxConnections;
        executor = Executors.newFixedThreadPool(maxThreads);
        this.password = password;
    }

    final protected int[] connect() throws MPDServerException {

        int[] result = null;
        int retry = 0;
        MPDServerException lastException = null;

        while (result == null && retry < MAX_CONNECT_RETRY && !cancelled) {
            try {
                result = innerConnect();
            } catch (MPDServerException e1) {
                lastException = e1;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //Nothing to do
                }
            } catch (Exception e2) {
                lastException = new MPDServerException(e2);
            }
            retry++;
        }

        if (result != null) {
            mpdVersion = result;
            return result;
        } else {
            if (lastException == null) {
                lastException = new MPDServerException("Connection request cancelled");
            }
            throw new MPDServerException(lastException);
        }
    }


    final private int[] innerConnect() throws MPDServerException {

        if (getSocket() != null) { //Always release existing socket if any before creating a new one
            try {
                innerDisconnect();
            } catch (MPDServerException e) {
                //ok, don't care about any exception here
            }
        }
        try {
            setSocket(new Socket());
            getSocket().setSoTimeout(readWriteTimeout);
            getSocket().connect(new InetSocketAddress(hostAddress, hostPort), CONNECTION_TIMEOUT);
            BufferedReader in = new BufferedReader(new InputStreamReader(getSocket().getInputStream()), 1024);
            String line = in.readLine();

            if (line == null) {
                throw new MPDServerException("No response from server");
            } else if (line.startsWith(MPD_RESPONSE_OK)) {
                String[] tmp = line.substring((MPD_RESPONSE_OK + " MPD ").length(), line.length()).split("\\.");
                int[] result = new int[tmp.length];

                for (int i = 0; i < tmp.length; i++)
                    result[i] = Integer.parseInt(tmp[i]);

                // Use UTF-8 when needed
                if (result[0] > 0 || result[1] >= 10) {
                    setOutputStream(new OutputStreamWriter(getSocket().getOutputStream(), "UTF-8"));
                    setInputStream(new InputStreamReader(getSocket().getInputStream(), "UTF-8"));
                } else {
                    setOutputStream(new OutputStreamWriter(getSocket().getOutputStream()));
                    setInputStream(new InputStreamReader(getSocket().getInputStream()));
                }

                if (password != null) {
                    password(password);
                }
                return result;
            } else if (line.startsWith(MPD_RESPONSE_ERR)) {
                throw new MPDServerException("Server error: " + line.substring(MPD_RESPONSE_ERR.length()));
            } else {
                throw new MPDServerException("Bogus response from server");
            }


        } catch (IOException e) {
            throw new MPDConnectionException(e);
        }
    }

    void disconnect() throws MPDServerException {
        this.cancelled = true;
        //executor.shutdown();
        innerDisconnect();
    }

    void innerDisconnect() throws MPDServerException {
        if (innerIsConnected())
            try {
                getSocket().close();
                setSocket(null);
            } catch (IOException e) {
                throw new MPDConnectionException(e.getMessage(), e);
            }
    }

    public boolean innerIsConnected() {
        return (getSocket() != null && getSocket().isConnected() && !getSocket().isClosed());
    }

    int[] getMpdVersion() {
        return mpdVersion;
    }

    public InetAddress getHostAddress() {
        return hostAddress;
    }

    public int getHostPort() {
        return hostPort;
    }

    public List<String> sendCommand(MPDCommand command) throws MPDServerException {
        return sendRawCommand(command);
    }

    public List<String> sendCommand(String command, String... args) throws MPDServerException {
        return sendCommand(new MPDCommand(command, args));
    }

    public void queueCommand(String command, String... args) {
        queueCommand(new MPDCommand(command, args));
    }

    public void queueCommand(MPDCommand command) {
        commandQueue.add(command);
    }

    static List<String[]> separatedQueueResults(List<String> lines) {
        List<String[]> result = new ArrayList<String[]>();
        ArrayList<String> lineCache = new ArrayList<String>();

        for (String line : lines) {
            if (line.equals(MPD_CMD_BULK_SEP)) { // new part
                if (lineCache.size() != 0) {
                    result.add((String[]) lineCache.toArray(new String[0]));
                    lineCache.clear();
                }
            } else
                lineCache.add(line);
        }
        if (lineCache.size() != 0) {
            result.add((String[]) lineCache.toArray(new String[0]));
        }
        return result;
    }

    public List<String[]> sendCommandQueueSeparated() throws MPDServerException {
        return separatedQueueResults(sendCommandQueue(true));
    }

    public List<String> sendCommandQueue() throws MPDServerException {
        return sendCommandQueue(false);
    }

    List<String> sendCommandQueue(boolean withSeparator) throws MPDServerException {
        String commandstr = (withSeparator ? MPD_CMD_START_BULK_OK : MPD_CMD_START_BULK) + "\n";
        for (MPDCommand command : commandQueue) {
            commandstr += command.toString();
        }
        commandstr += MPD_CMD_END_BULK + "\n";
        commandQueue = new ArrayList<MPDCommand>();
        return sendRawCommand(new MPDCommand(commandstr));
    }

    public List<String> sendRawCommand(MPDCommand command) throws MPDServerException {
        return syncedWriteRead(command);
    }


    List<String> sendAsyncCommand(MPDCommand command)
            throws MPDServerException {
        return syncedWriteAsyncRead(command);
    }

    List<String> sendAsyncCommand(String command, String... args)
            throws MPDServerException {
        return sendAsyncCommand(new MPDCommand(command, args));
    }

    private void writeToServer(MPDCommand command) throws IOException {
        getOutputStream().write(command.toString());
        getOutputStream().flush();
        command.setSentToServer(true);
    }

    private ArrayList<String> readFromServer() throws MPDServerException, IOException {
        ArrayList<String> result = new ArrayList<String>();
        BufferedReader in = new BufferedReader(getInputStream(), 1024);


        boolean dataReaded = false;
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            dataReaded = true;
            if (line.startsWith(MPD_RESPONSE_OK))
                break;
            if (line.startsWith(MPD_RESPONSE_ERR)) {

                if (line.contains("permission")) {
                    throw new MPDConnectionException("MPD Permission failure : "
                            + line.substring(MPD_RESPONSE_ERR.length()));
                } else {
                    throw new MPDServerException("Server error: "
                            + line.substring(MPD_RESPONSE_ERR.length()));
                }
            }
            result.add(line);
        }
        if (!dataReaded) {
            // Close socket if there is no response... Something is wrong
            // (e.g.
            // MPD shutdown..)
            throw new MPDNoResponseException("Connection lost");
        }
        return result;
    }

    private List<String> innerSyncedWriteRead(MPDCommand command)
            throws MPDServerException {
        ArrayList<String> result = new ArrayList<String>();
        if (!isConnected())
            throw new MPDConnectionException("No connection to server");

        // send command
        try {
            writeToServer(command);
        } catch (IOException e1) {
            throw new MPDConnectionException(e1);
        }
        try {
            result = readFromServer();
            return result;
        } catch (MPDConnectionException e) {
            if (command.command.equals(MPDCommand.MPD_CMD_CLOSE))
                return result;// we sent close command, so don't care about Exception while ryong to read response
            else
                throw e;
        } catch (IOException e) {
            throw new MPDConnectionException(e);
        }
    }

    private List<String> syncedWriteAsyncRead(MPDCommand command) throws MPDServerException {
        command.setSynchronous(false);
        return processRequest(command);
    }

    private List<String> syncedWriteRead(MPDCommand command) throws MPDServerException {
        command.setSynchronous(true);
        return processRequest(command);
    }

    private List<String> innerSyncedWriteAsyncRead(MPDCommand command)
            throws MPDServerException {
        ArrayList<String> result = new ArrayList<String>();
        try {
            writeToServer(command);
        } catch (IOException e) {
            throw new MPDConnectionException(e);
        }
        boolean dataReaded = false;
        while (!dataReaded) {
            try {
                result = readFromServer();
                dataReaded = true;
            } catch (SocketTimeoutException e) {
                w(MPDConnection.class.getSimpleName(), "Socket timeout while reading server response : " + e);
            } catch (IOException e) {
                throw new MPDConnectionException(e);
            }
        }
        return result;
    }

    private List<String> processRequest(MPDCommand command) throws MPDServerException {

        MPDCommandResult result;

        try {
            // Bypass thread pool queue if the thread already comes from the pool
            // to avoid deadlock
            if (Thread.currentThread().getName().startsWith(POOL_THREAD_NAME_PREFIX)) {
                result = new MpdCallable(command).call();
            } else {
                result = executor.submit(new MpdCallable(command)).get();
            }
        } catch (Exception e) {
            throw new MPDServerException(e);
        }
        if (result.getResult() != null) {
            return result.getResult();
        } else if (cancelled) {
            throw new MPDConnectionException("The MPD request has been canceled");
        } else {
            throw result.getLastexception();
        }
    }

    class MpdCallable extends MPDCommand implements Callable<MPDCommandResult> {

        private int retry = 0;

        public MpdCallable(MPDCommand mpdCommand) {
            super(mpdCommand.command, mpdCommand.args, mpdCommand.isSynchronous());
        }

        @Override
        public MPDCommandResult call() throws Exception {
            boolean retryable = true;
            MPDCommandResult result = new MPDCommandResult();

            while (result.getResult() == null && retry < MAX_REQUEST_RETRY && !cancelled && retryable) {
                try {
                    if (!innerIsConnected()) {
                        innerConnect();
                    }
                    if (isSynchronous()) {
                        result.setResult(innerSyncedWriteRead(this));
                    } else {
                        result.setResult(innerSyncedWriteAsyncRead(this));
                    }
                    // Do not fail when the IDLE response has not been read (to improve connection failure robustness)
                    // Just send the "changed playlist" result to force the MPD status to be refreshed.
                } catch (MPDNoResponseException ex0) {
                    this.setSentToServer(false);
                    handleConnectionFailure(result, ex0);
                    if (command.equals(MPDCommand.MPD_CMD_IDLE)) {
                        result.setResult(Arrays.asList("changed: playlist"));
                    }
                } catch (MPDServerException ex1) {
                    handleConnectionFailure(result, ex1);
                }
                retryable = isRetryable(command) || !this.isSentToServer();
                retry++;
            }

            if (result.getResult() == null) {
                if (cancelled) {
                    result.setLastexception(new MPDConnectionException("MPD request has been cancelled for disconnection"));
                }
                e(MpdCallable.class.getSimpleName(), "MPD command " + command + " failed after " + retry + " attempts : " + result.getLastexception().getMessage());
            }
            return result;
        }
    }

    private void handleConnectionFailure(MPDCommandResult result, MPDServerException ex) {
        try {
            result.setLastexception(ex);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //Nothing to do
            }
            innerConnect();
            refreshAllConnections();
        } catch (MPDServerException e) {
            result.setLastexception(e);
        }
    }

    class MPDCommandResult {
        private MPDServerException lastexception = null;
        private List<String> result = null;

        public MPDServerException getLastexception() {
            return lastexception;
        }

        public void setLastexception(MPDServerException lastexception) {
            this.lastexception = lastexception;
        }

        public List<String> getResult() {
            return result;
        }

        public void setResult(List<String> result) {
            this.result = result;
        }
    }

    /**
     * Authenticate using password.
     *
     * @param password password.
     * @throws MPDServerException if an error occur while contacting server.
     */
    protected void password(String password) throws MPDServerException {
        this.password = password;
        sendCommand(MPDCommand.MPD_CMD_PASSWORD, password);
    }

    public boolean isConnected() {
        return !cancelled;
    }

    private void refreshAllConnections() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < maxThreads; i++) {
                    try {
                        processRequest(new MPDCommand(MPDCommand.MPD_CMD_PING));
                    } catch (MPDServerException e) {
                        w(MPDConnection.class.getSimpleName(), "All connection refresh failure : " + e);
                    }
                }
            }
        }).start();
    }
}
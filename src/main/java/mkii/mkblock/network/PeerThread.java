package mkii.mkblock.network;

import java.net.Socket;

import static mkii.mkblock.common.Util.OUTPRT;

public class PeerThread extends Thread {
    private Socket socket;
    public InputThread inputThread;
    public OutputThread outputThread;

    public PeerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        OUTPRT("Get connection from : " + socket.getInetAddress());
        inputThread = new InputThread(socket);
        inputThread.start();
        outputThread = new OutputThread(socket);
        outputThread.start();
    }

    public void send(String data) {
        if( outputThread == null ) {
            OUTPRT("Unable to send " + data + " to the network!");
        } else {
            outputThread.write(data);
        }
    }
}

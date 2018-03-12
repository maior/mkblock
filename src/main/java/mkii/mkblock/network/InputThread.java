package mkii.mkblock.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

import static mkii.mkblock.common.Util.OUTPRT;

public class InputThread extends Thread {
    private Socket socket;
    private ArrayList<String> receivedData = new ArrayList<>();

    public InputThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String input;
            while((input = in.readLine()) != null) {
                receivedData.add(input);
            }
        } catch (Exception e) {
            OUTPRT("Peer : " + socket.getInetAddress() + " disconnected.");
        }
    }

    public ArrayList<String> readData() {
        ArrayList<String> inputBuffer = new ArrayList<>(receivedData);
        if( inputBuffer == null ) {
            inputBuffer = new ArrayList<>();
        }
        receivedData = new ArrayList<>();
        return inputBuffer;
    }
}

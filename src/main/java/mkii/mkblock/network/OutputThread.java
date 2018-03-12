package mkii.mkblock.network;

import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import static mkii.mkblock.common.Util.OUTPRT;

public class OutputThread extends Thread {
    private Socket socket;

    private ArrayList<String> outputBuffer;
    private boolean shouldContinue = true;

    public OutputThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            outputBuffer = new ArrayList<String>();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            while(shouldContinue) {
                if(outputBuffer.size() > 0) {
                    if(outputBuffer.get(0) != null) {
                        for(int i=0; i<outputBuffer.size(); i++) {
                            if( outputBuffer.get(i).length() > 20) {
                                OUTPRT("Sending " + outputBuffer.get(i).substring(0,20) + " to " + socket.getInetAddress());
                            } else {
                                OUTPRT("Sending " + outputBuffer.get(i) + " to " + socket.getInetAddress());
                            }
                            out.println(outputBuffer.get(i));
                        }
                        outputBuffer = new ArrayList<String>();
                        outputBuffer.add(null);
                    }
                }
                Thread.sleep(100);
            }
            OUTPRT("WHY AM I HERE???");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write(String data) {
        if(data.length() > 20) {
            OUTPRT("PUTTING INTO WRITE BUFFER : " + data.substring(0, 20) + "...");
        } else {
            OUTPRT("PUTTING INTO WRITE BUFFER : " + data);
        }
        File f = new File("writebuffer");
        try {
            PrintWriter out = new PrintWriter(f);
            out.println("SEDING : " + data);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(outputBuffer.size() > 0) {
            if(outputBuffer.get(0) == null) {
                outputBuffer.remove(0);
            }
        }
        outputBuffer.add(data);
    }

    public void shutdown() {
        shouldContinue = false;
    }
}

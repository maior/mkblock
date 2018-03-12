package mkii.mkblock.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static mkii.mkblock.common.Util.OUTPRT;

public class RPCThread extends Thread {
    private Socket socket;
    public String res;
    public String req;

    public RPCThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            req = null;
            res = null;
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String input = "";
            out.println("RPC Daemon!!!!");
            while((input = in.readLine()) != null) {
                if(input.equalsIgnoreCase("HELP")) {
                    out.println("Command : ");
                    out.println("send <amount> <dest> - Send <amount> coins to <dest>");
                    out.println("getinfo - Gets basic info about this node.");
                    out.println("getbalance <address> - Gets this current balance of <address>");
                    out.println("submit_tx <rawTx> - Submits a transaction to the network.");
                    out.println("submit_cert <address> - Submits a certificate to the network.");
                    out.println("get_history <address> - Gets the tx history of <address>.");
                    out.println("get_pending <address> - Gets the pending balance of the default address.");
                    out.println("trypos - Attempts of PoS block.");
                    out.println();
                } else {
                    req = input;
                    while(res == null) {
                        Thread.sleep(25);
                    }

                    out.println(res + "\n</>");
                    out.println();
                    req = null;
                    res = null;
                }
            }
        } catch (Exception e) {
            OUTPRT("An RPC client has disconnected.");
        }
    }
}

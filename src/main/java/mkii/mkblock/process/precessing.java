package mkii.mkblock.process;

import static mkii.mkblock.common.Constants.*;
import static mkii.mkblock.common.Util.OUTPRT;
import static mkii.mkblock.process.PeerNetworkProcessing.PNProcessing;
import static mkii.mkblock.process.RPCProcessing.ResponseProcess;

public class precessing {

    public static void blockProcessing() {

        OUTPRT("Sending REQUEST_NET_STATE out to network");
        peerNetwork.broadcast("REQUEST_NET_STATE");

        while(true) {

            // Processing PeerNetwork
            PNProcessing();

            // Processing RPC response
            ResponseProcess();

            try{
                Thread.sleep(100);
            } catch (Exception e){}
        }
    }
}

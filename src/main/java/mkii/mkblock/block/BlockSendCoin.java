package mkii.mkblock.block;

import mkii.mkblock.common.TransactionUtility;

import static mkii.mkblock.common.Constants.*;
import static mkii.mkblock.common.Constants.rpcAgent;
import static mkii.mkblock.common.Util.OUTPRT;

public class BlockSendCoin {
    public static void sendCoin(int i, String[] parts) {
        try{
            double amount = Double.parseDouble(parts[1]);
            String destAddr = parts[2];
            String addr = addressManager.getDefaultAddress();
            String fullTx = addressManager.getSignedTransaction(destAddr, amount, databaseMaster.getAddressSignatureIndex(addr) + addressManager.getDefaultOffset());
            addressManager.incrementDefaultOffset();
            OUTPRT("Trying to verify transaction... " + TransactionUtility.isTransactionValid(fullTx));
            if (TransactionUtility.isTransactionValid(fullTx)){
                pendingTransactionContainer.addTransaction(fullTx);
                peerNetwork.broadcast("TRANSACTION " + fullTx);
                OUTPRT("Sending " + amount + " to " + destAddr + " from " + addr);
                rpcAgent.rpcThreads.get(i).res = "Sent " + amount + " from " + addr + " to " + destAddr;
            } else {
                rpcAgent.rpcThreads.get(i).res = "Unable to send: invalid transaction :(";
            }
        } catch (Exception e){
            rpcAgent.rpcThreads.get(i).res = "Syntax (no '<' or '>'): send <amount> <dest>";
        }
    }
}

package mkii.mkblock.block;

import java.util.ArrayList;

import static mkii.mkblock.common.Constants.databaseMaster;
import static mkii.mkblock.common.Constants.rpcAgent;

public class BlockHistory {

    public static void getBlockHistory(int i, String[] parts) {
        if (parts.length > 1){
            ArrayList<String> allTransactions = databaseMaster.getAllTransactionsInvolvingAddress(parts[1]);
            String allTransactionsFlat = "";
            for (int j = 0; j < allTransactions.size(); j++) {
                allTransactionsFlat += allTransactions.get(j) + "\n";
            }
            rpcAgent.rpcThreads.get(i).res = allTransactionsFlat;
        } else {
            rpcAgent.rpcThreads.get(i).res = "Syntax: get_history <address>";
        }
    }
}

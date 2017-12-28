package ch.ethz.inf.vs.fstreun.finance;

import android.support.v4.util.Pair;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created by fabio on 12/28/17.
 *
 * Algorithm info: http://www.settleup.info/files/master-thesis-david-vavra.pdf
 */

public class DebtSolver {

    private final String TAG = "## DebtSolver";

    private Group group;
    private UUID creator;

    public double TOLERANCE = 0.001;


    public DebtSolver(Group group, UUID creator){
        if (group == null){
            throw new IllegalArgumentException("Illegal Null argument");
        }
        this.group = group;
        this.creator = creator;
    }

    /**
     * O(N), but not the most optimal one for user
     * @return
     */
    public List<Transaction> solvePrimitive(long timestamp){

        LinkedList<FinalParticipant> participants = new LinkedList<>();
        for (Participant p : group.getParticipants()){
            participants.add(new FinalParticipant(p.name, p.getCredit()));
        }
        Collections.sort(participants, new ParticipantComparator());

        List<Transaction> transactions = new ArrayList<>();

        while (participants.size() > 1){
            Log.d(TAG, "while run");
            FinalParticipant big = participants.getLast();
            FinalParticipant small = participants.getFirst();

            Log.d(TAG, "big credit: " + big.credit);
            Log.d(TAG, "small credit: " + small.credit);

            if (Math.abs(big.credit) < TOLERANCE){
                participants.removeLast();
                continue;
            }
            if (Math.abs(small.credit) < TOLERANCE){
                participants.removeFirst();
                continue;
            }

            double amount = absMin(big.credit, small.credit);
            ArrayList<String> involved = new ArrayList<>(1);
            involved.add(big.name);

            Transaction transaction = new Transaction(creator, small.name, involved, amount, timestamp, "SETTLE");
            transactions.add(transaction);

            big.credit -= amount;
            small.credit += amount;
        }

        return transactions;
    }

    private double absMin(double x, double y){
        return Math.min(Math.abs(x), Math.abs(y));
    }

    class FinalParticipant{
        final String name;
        double credit;

        FinalParticipant(String name, double credit){
            this.name = name;
            this.credit = credit;
        }
    }

    static class ParticipantComparator implements Comparator<FinalParticipant>{
        @Override
        public int compare(FinalParticipant o1, FinalParticipant o2) {
            return Double.compare(o1.credit, o2.credit);
        }
    }
}

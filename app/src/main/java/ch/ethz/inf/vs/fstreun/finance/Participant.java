package ch.ethz.inf.vs.fstreun.finance;

/**
 * Created by fabio on 12/12/17.
 */

public class Participant {

    public final String name;
    private final Group group;

    public Participant(String name, Group group) {
        this.name = name;
        this.group = group;
    }

    /*
    public double getToPay(){
        return group.toPay(name);
    }
    */
    public double getCredit(){
        return group.credit(name);
    }

    public double getSpent(){
        return group.spent(name);
    }

    public double getTotalInvolved(){
        return group.owes(name);
    }

    /*
    @Override
    public boolean equals(Participant obj) {
        return Transaction.caseInsensitiveEquals(name, obj.name);
    }
    */
}

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

    public double getToPay(){
        return group.toPay(name);
    }
}

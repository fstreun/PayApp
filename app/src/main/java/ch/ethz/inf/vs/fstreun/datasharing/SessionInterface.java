package ch.ethz.inf.vs.fstreun.datasharing;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by fabio on 11/28/17.
 */

public interface SessionInterface<C extends Chain>{

    public Map<UUID, C> getData();

    public Map<UUID, C> getDataAfter(Map<UUID, Integer> start);

    public Map<UUID, Integer> put (Map<UUID, Chain> chainMap, Map<UUID, Integer> expected);

    public int put(UUID userID, Chain chain, int expected);

    public int put(UUID userID, Block block, int expected);

    public Map<UUID, Integer> getLength();

    public Set<UUID> getAllUserID();

    public UUID getSessionID();


}

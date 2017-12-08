package ch.ethz.inf.vs.fstreun.datasharing;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by fabio on 11/28/17.
 */

public interface SessionInterface{

    public Map<UUID, Chain> getData();

    public Map<UUID, Chain> getData(Map<UUID, Integer> start);

    public Map<UUID, Integer> putData(Map<UUID, Chain> mapData, Map<UUID, Integer> expected);

    public Integer putChain(UUID id, Chain chain, int expected);

    public Integer putBlock(UUID id, Block block, int expected);

    public Map<UUID, Integer> getLength();

    public Set<UUID> getAllUserID();

    public UUID getSessionID();


}

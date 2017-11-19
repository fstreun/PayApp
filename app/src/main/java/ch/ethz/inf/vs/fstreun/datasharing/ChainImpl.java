package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by fabio on 11/12/17.
 * possibly chain implementation
 */

public class ChainImpl implements ChainFactory<ChainImpl>, Chain {

    List<Block> chain = new ArrayList<>();


    @Override
    public boolean append(Block block) {
        chain.add(block);
        return true;
    }

    @Override
    public Block get(int position) {
        return chain.get(position);
    }

    @Override
    public List<Block> getBlocks(int start) {
        return new ArrayList<>(chain.subList(start, chain.size()));
    }

    @Override
    public List<Block> getBlocks(int start, int end) {
        return new ArrayList<>(chain.subList(start, end));
    }

    @Override
    public List<Block> getBlocks() {
        return new ArrayList<>(chain);
    }

    @Override
    public int size() {
        return chain.size();
    }


    @Override
    public Iterator<Block> iterator() {
        return chain.iterator();
    }


    @Override
    public ChainImpl createEmpty() {
        return null;
    }

    @Override
    public ChainImpl createFromJSON(JSONObject object) {
        return null;
    }

    @Override
    public JSONObject createJSON(ChainImpl chain) {
        return null;
    }

}

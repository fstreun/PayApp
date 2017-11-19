package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by fabio on 11/12/17.
 * possibly data implementation
 */

public class ChainImpl implements ChainFactory<ChainImpl>, Chain {

    private List<Block> data = new ArrayList<>();

    public ChainImpl(){}
    private ChainImpl(List<Block> blocks){
        data.addAll(blocks);
    }


    @Override
    public boolean append(Block block) {
        data.add(block);
        return true;
    }

    @Override
    public Block get(int position) {
        return data.get(position);
    }

    @Override
    public Chain getSubChain(int start) {
        return new ChainImpl (data.subList(start, data.size()));
    }

    @Override
    public List<Block> getBlocks() {
        return new ArrayList<>(data);
    }

    @Override
    public int length() {
        return data.size();
    }


    @Override
    public Iterator<Block> iterator() {
        return data.iterator();
    }



    @Override
    public ChainImpl createEmpty() {
        return new ChainImpl();
    }

    @Override
    public ChainImpl createFromJSON(JSONArray object) throws JSONException {
        ChainImpl chain = new ChainImpl();
        int length = object.length();
        for (int i = 0; i < length; i++){
            chain.append(Block.createFromJSON(object.getJSONObject(i)));
        }
        return chain;
    }

    @Override
    public ChainImpl createFromBlocks(List<Block> blocks) {
        return new ChainImpl(blocks);
    }

    @Override
    public JSONArray createJSON(ChainImpl chain) throws JSONException {
        JSONArray array = new JSONArray();
        if (chain != null){
            for (Block b : chain.data){
                array.put(b.getJSONObject());
            }
        }
        return array;
    }

}

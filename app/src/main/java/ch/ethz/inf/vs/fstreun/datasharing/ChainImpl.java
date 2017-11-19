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

public class ChainImpl implements ChainFactory<ChainImpl>, Chain<BlockImpl> {

    private List<BlockImpl> data = new ArrayList<>();

    public final BlockFactory<BlockImpl> blockFactory;

    public ChainImpl(BlockFactory<BlockImpl> blockFactory){
        this.blockFactory = blockFactory;
    }

    private ChainImpl(List<BlockImpl> blocks, BlockFactory<BlockImpl> blockFactory){
        this.blockFactory = blockFactory;
        data.addAll(blocks);
    }


    @Override
    public boolean append(BlockImpl block) {
        data.add(block);
        return true;
    }

    @Override
    public BlockImpl get(int position) {
        return data.get(position);
    }

    @Override
    public Chain getSubChain(int start) {
        return new ChainImpl (data.subList(start, data.size()), blockFactory);
    }

    @Override
    public List<BlockImpl> getBlocks() {
        return new ArrayList<>(data);
    }

    @Override
    public int length() {
        return data.size();
    }


    @Override
    public Iterator<BlockImpl> iterator() {
        return data.iterator();
    }



    @Override
    public ChainImpl createEmpty() {
        return new ChainImpl(blockFactory);
    }

    @Override
    public ChainImpl createFromJSON(JSONArray object) throws JSONException {
        ChainImpl chain = new ChainImpl(blockFactory);
        int length = object.length();
        for (int i = 0; i < length; i++){
            chain.append(blockFactory.createFromJSON(object.getJSONObject(i)));
        }
        return chain;
    }

    @Override
    public JSONArray createJSON(ChainImpl chain) throws JSONException {
        JSONArray array = new JSONArray();
        if (chain != null){
            for (BlockImpl b : chain.data){
                array.put(blockFactory.getJSONObject(b));
            }
        }
        return array;
    }

}

package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fabio on 11/12/17.
 * possibly data implementation
 */

public class Chain implements ChainJSON, ChainInterface {

    private ArrayList<Block> data = new ArrayList<>();

    /**
     * simple creation of chain
     */
    public Chain(){}

    public Chain(JSONArray object) throws JSONException {
        int length = object.length();
        for (int i = 0; i < length; i++){
            data.add((Block.createFromJSON(object.getJSONObject(i))));
        }
    }

    public JSONArray toJSON() throws JSONException {
        JSONArray array = new JSONArray();
        for (Block b : data){
            array.put(b.getJSONObject());
        }
        return array;
    }


    public String toString(){
        try {
            return toJSON().toString();
        } catch (JSONException e) {
            return "Failed to create String of Chain";
        }
    }


    @Override
    public int append(Chain chain, int elength) {
        int length = data.size();
        for (int i = length - elength; i < chain.length(); i++){
            data.add(chain.data.get(i).clone());
        }
        return length;
    }

    @Override
    public int append(Block block, int elength) {
        int length = data.size();
        if (length == elength){
            data.add(block.clone());
        }
        return length;
    }

    @Override
    public Chain getSubChain(int start){
        Chain res = new Chain();
        int size = data.size();
        for (int i = start; i < size; i++){
            res.data.add(data.get(i).clone());
        }
        return res;
    }


    @Override
    public Block getBlock(int position) {
        return data.get(position).clone();
    }

    @Override
    public int length() {
        return data.size();
    }


    @Override
    public Chain clone() {
        Chain res = new Chain();
        for (Block b : data){
            res.data.add(b.clone());
        }
        return res;
    }



    @Override
    public List<Block> getBlocks() {
        ArrayList<Block> res = new ArrayList<>(data.size());
        for (Block b : data){
            res.add(b.clone());
        }
        return res;
    }

    @Override
    public List<Block> getBlocks(int start) {
        ArrayList<Block> res = new ArrayList<>();
        int i = data.size() - start;
        for (;i < data.size(); i++){
            res.add(data.get(i).clone());
        }
        return res;
    }



    @Override
    public Integer appendJSON(JSONArray chain, int expected) throws JSONException {
        int actual = data.size();

        if (actual < expected) {
            // block misses
            return actual;
        }

        // first element to be added from chain
        int start = actual - expected;

        List<Block> toAppend = new ArrayList<>();
        for (int i = start; i < chain.length(); i++){
            // append as many blocks as possible
            Block b = Block.createFromJSON(chain.getJSONObject(i));
            data.add(b);
        }
        return actual;
    }

    @Override
    public Integer appendJSON(JSONObject block, int expected) {
        return null;
    }

    @Override
    public JSONArray subChainJSON(int start) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = start; i < data.size(); i++){
            array.put(data.get(i).getJSONObject());
        }
        return array;
    }

    @Override
    public JSONObject getBlockJSON(int position) {
        return null;
    }
}

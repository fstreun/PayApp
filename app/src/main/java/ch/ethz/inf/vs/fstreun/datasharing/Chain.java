package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;

/**
 * Created by fabio on 11/12/17.
 * Basic interface of a chain.
 */

public interface Chain extends Iterable<Block>{


    /**
     * Appends block to the end of the chain if length == elength
     * @param ablock to be appended
     * @param elength expected length of the chain (elength >= 0)
     * @return actual length before appending
     */
    public int append(Block ablock, int elength);

    /**
     * Appends chain to the end of the chain if elength <= length
     * @param achain chain to be appended
     * @param elength expected length of chain (elength >= 0)
     * @return actuall length before appending
     */
    public int append(Chain achain, int elength);

    /**
     * To access a block in the chain
     * @param position of the block in the chain
     * @return a read only block
     */
    public Block get(int position);

    /**
     * To access a contiguous list of blocks.
     * NOT THE ACTUAL DATA REPRESENTATION OF THE CHAIN!
     * @param start position of the first block to be in the list
     * @return a copy of the sub chain
     */
    public Chain getSubChain(int start);

    /**
     * To access a contiguous list of all blocks of the chain
     * NOT THE ACTUAL DATA REPRESENTATION OF THE CHAIN!
     * @return a copy of the data
     */
    public List<Block> getBlocks();


    /**
     * number of blocks in the chain
     * @return length
     */
    public int length();

}

package ch.ethz.inf.vs.fstreun.datasharing;

import java.util.List;

/**
 * Created by fabio on 11/12/17.
 * Basic interface of a chain.
 */

public interface ChainInterface {

    public int append(Chain chain, int elength);

    /**
     * Appends block to the end of the chain if length == elength
     * @param block to be appended
     * @param elength expected length of the chain (elength >= 0)
     * @return actual length before appending
     */
    public int append(Block block, int elength);


    public Chain getSubChain(int start);

    /**
     * To access a block in the chain
     * @param position of the block in the chain
     * @return a read only block
     */
    public Block getBlock(int position);


    /**
     * To access a contiguous list of all blocks of the chain
     * NOT THE ACTUAL DATA REPRESENTATION OF THE CHAIN!
     * @return a copy of the data
     */
    public List<Block> getBlocks();

    public List<Block> getBlocks(int start);


    /**
     * number of blocks in the chain
     * @return length
     */
    public int length();


    /**
     *
     * @return a copy of the chain
     */
    public ChainInterface clone();

}

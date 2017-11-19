package ch.ethz.inf.vs.fstreun.datasharing;

import java.util.List;

/**
 * Created by fabio on 11/12/17.
 * Basic interface of a chain with Blocks of type B
 */

public interface Chain<B extends Block> extends Iterable<B>{

    /**
     * Appends block to the end of the chain.
     * @param block to be appended
     * @return success
     */
    public boolean append(B block);

    /**
     * To access a block in the chain
     * @param position of the block in the chain
     * @return a read only block
     */
    public B get(int position);

    /**
     * To access a contiguous list of blocks.
     * NOT THE ACTUAL DATA REPRESENTATION OF THE CHAIN!
     * @param start position of the first block to be in the list
     * @return
     */
    public Chain getSubChain(int start);


    /**
     * To access a contiguous list of all blocks of the chain
     * NOT THE ACTUAL DATA REPRESENTATION OF THE CHAIN!
     * @return
     */
    public List<B> getBlocks();



    /**
     * number of blocks in the chain
     * @return length
     */
    public int length();

}

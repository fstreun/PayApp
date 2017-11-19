package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by fabio on 11/12/17.
 * Minimal interface of a basic block.
 */

public abstract class Block {

    private final JSONObject content;

    public Block(JSONObject content){
        this.content = content;
    }

    /**
     *
     * TODO: only return copy of object
     * @return
     */
    public final JSONObject getContent() {
        return content;
    }

}

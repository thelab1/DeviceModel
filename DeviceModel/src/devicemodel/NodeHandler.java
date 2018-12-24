package devicemodel;

/**
 *
 * @author root
 */
public abstract class NodeHandler {    
    // return a true/false if the node should also follow its
    // normal update routine as well; unused by set
    public abstract boolean handle(DeviceNode node);
}

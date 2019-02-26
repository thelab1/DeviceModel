package devicemodel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeviceNode implements PropertyChangeListener, Comparable<DeviceNode> {

    public static final String PROPERTY_CHANGE_NAME = "update";
    // listeners will get updates fired when this node's value or children's values change
    // note that the public method setValue() does not fire an event, but allows 
    // access for the user to set the value at initialization, etc
    // usage of the update(DeviceNode) method is preferred as it will fire updates 
    // and handle all update change aggregation and parent recursion
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    protected DeviceNode parent;
    protected String name;
    protected String value;
    protected final CopyOnWriteArrayList<DeviceNode> children = new CopyOnWriteArrayList<>();
    protected final ConcurrentHashMap<String, String> attributes = new ConcurrentHashMap<>();

    // these are access handlers for the node
    // SET: called at set(DeviceNode) when requesting this node's value to change
    //      by default this does nothing, but add in a handle to handle the logic
    //      for setting some value this device node model represents
    //
    // UPDATE: called at update(DeviceNode) when updating this node's value, children, or attributes
    //      by default, this updates the node's value and attributes accordingly, but
    //      add in a handle to change this behavior (e.g. send data to a log file instead)
    private NodeHandler setHandle;
    private NodeHandler updateHandle;
    private NodeGetHandler getHandle;

    public DeviceNode(String name) {
        this(name, null);
    }

    public DeviceNode(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public class DeviceNodeBuilder {
        private DeviceNode rootNode;
        private DeviceNode currentNodePointer;

        public DeviceNodeBuilder() {}

        public DeviceNodeBuilder addDeviceNode(String name) {
            return this.addDeviceNode(name, null);
        }

        public DeviceNodeBuilder addDeviceNode(String name, String value) {
            if (this.rootNode == null) {
                this.rootNode = new DeviceNode(name, value);
                this.currentNodePointer = this.rootNode;
            }
            else {
                DeviceNode newNode = new DeviceNode(name, value);
                this.currentNodePointer.addChild(newNode);
                this.currentNodePointer = newNode;
            }

            return this;
        }
    }

// *********************************************** //
// Setter/getter methods
// *********************************************** //
    protected void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // using this will NOT fire an event, use update() for that
    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // should only be used internally; add/remove child methods should be used
    protected void setParent(DeviceNode parent) {
        this.parent = parent;
        this.changeSupport.addPropertyChangeListener(parent);
    }

    public DeviceNode getParent() {
        return parent;
    }

    public DeviceNode addAttribute(String name, String attribute) {
        this.attributes.put(name, attribute);
        return this;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    public NodeHandler getSetHandle() {
        return setHandle;
    }

    public void setSetHandle(NodeHandler setHandle) {
        this.setHandle = setHandle;
    }

    public NodeHandler getUpdateHandle() {
        return updateHandle;
    }

    public void setUpdateHandle(NodeHandler updateHandle) {
        this.updateHandle = updateHandle;
    }

    public NodeGetHandler getGetHandle() {
        return getHandle;
    }

    public void setGetHandle(NodeGetHandler getHandle) {
        this.getHandle = getHandle;
    }

    public DeviceModelProto.DeviceNode.Builder getDeviceNodeProtoBuf() {
        return this.getDeviceNodeProtoBuf(null);
    }

    public DeviceModelProto.DeviceNode.Builder getDeviceNodeProtoBuf(DeviceModelProto.DeviceNode.Builder nodeParent) {
        DeviceModelProto.DeviceNode.Builder devNodeBuilder = DeviceModelProto.DeviceNode.newBuilder();

        if (nodeParent != null) devNodeBuilder.setNodeParent(nodeParent);
        devNodeBuilder.setName(this.getName());
        devNodeBuilder.setValue(this.getValue());
        for (String key : this.attributes.keySet()) {
            devNodeBuilder.getAttributesMap().put(key, this.attributes.get(key));
        }
        for (DeviceNode child : this.children) {
            devNodeBuilder.getChildrenList().add(child.getDeviceNodeProtoBuf().build());
        }

        return devNodeBuilder;
    }

// *********************************************** //
// Child methods
// *********************************************** //
    public boolean hasChild(DeviceNode node) {
        return indexOfChild(node) != -1;
    }

    public boolean hasChild(String name) {
        return indexOfChild(name) != -1;
    }

    public int indexOfChild(DeviceNode node) {
        return indexOfChild(node.getName());
    }

    public int indexOfChild(String name) {
        synchronized (this.children) {
            DeviceNode child;
            for (int idx = 0; idx < this.children.size(); idx++) {
                child = this.children.get(idx);
                if (child.getName().equals(name)) {
                    return idx;
                }
            }
        }

        return -1;
    }
    public void addChild(DeviceNode child) {
        this.addChild(child, false);
    }

    public void addChild(DeviceNode child, boolean fireUpdate) {
        if (fireUpdate) {
            // Copy this node, add the child
            // and then update this node.
            DeviceNode wrapper = this.cloneShallow();
            child.setParent(wrapper);
            this.update(wrapper);
        }
        else {
            // Update the child to hold this node as it's parent.
            child.setParent(this);

            // Add the child to the list of children.
            synchronized (children) {
                this.children.add(child);
            }
        }
    }

    // read-only; adding should go through addChild()
    public List<DeviceNode> getChildren() {
        return Collections.unmodifiableList(this.children);
    }

    public List<DeviceNode> getChildren(String name) {
        synchronized (this.children) {
            List<DeviceNode> ret = new ArrayList<>();
            for (DeviceNode node : this.children) {
                if (node.getName().equals(name)) {
                    ret.add(node);
                }
            }

            return ret;
        }
    }

    public ConcurrentHashMap<String, DeviceNode> getAllChildren() {
        ConcurrentHashMap<String, DeviceNode> allChildren = new ConcurrentHashMap<>();
        collectAllChildren(allChildren, this);
        return allChildren;
    }

    private void collectAllChildren(ConcurrentHashMap<String, DeviceNode> allChildren, DeviceNode node) {
        allChildren.put(node.getNodePath(), node.cloneShallow());

        for (DeviceNode child : node.getChildren()) {
            collectAllChildren(allChildren, child);
        }
    }

    public DeviceNode getChild(int idx) {
        if (this.children.size() > idx) {
            return this.children.get(idx);
        }

        return null;
    }

    public DeviceNode getChild(String name) {
        List<DeviceNode> childs = this.getChildren(name);
        return childs != null && childs.size() > 0 ? childs.get(0) : null;
    }

    public DeviceNode getChild(String name, String id) {
        // Get a list of this node's children.
        List<DeviceNode> childs = this.getChildren(name);

        // If an id was passed in, look for that child.
        if (id != null && id.trim().length() > 0) {
            for (DeviceNode child : childs) {
                // Check the id against this child.
                if (child.getAttribute("_id") != null && child.getAttribute("_id").trim().equals(id)) {
                    // Found it.
                    return child;
                }
            }
        }

        // Default to just returning the first one.
        return childs != null && childs.size() > 0 ? childs.get(0) : null;
    }

    public DeviceNode getChild(DeviceNode node) {
        List<DeviceNode> nodes = this.getChildren(node.getName());
        for (DeviceNode child : nodes) {
            if (child.compareTo(node) == 0) {
                return child;
            }
        }

        return null;
    }

    public DeviceNode getChildById(int id) {
        synchronized (this.children) {
            for (DeviceNode child : this.children) {
                if (child.getAttribute("_id") != null && child.getAttribute("_id").equals(String.valueOf(id))) {
                    return child;
                }
            }

            return null;
        }
    }

    public List<DeviceNode> getChildrenSorted() {
        List<DeviceNode> leaves = new ArrayList<>();
        List<DeviceNode> branches = new ArrayList<>();
        synchronized (this.children) {
            for (DeviceNode node : this.children) {
                if (node.getChildren().isEmpty()) {
                    leaves.add(node);
                } else {
                    branches.add(node);
                }
            }
        }
        Collections.sort(leaves);
        Collections.sort(branches);
        leaves.addAll(branches);
        return leaves;
    }

    public List<String> getChildrenNamesSorted() {
        List<String> leaves = new ArrayList<>();
        List<String> branches = new ArrayList<>();
        synchronized (this.children) {
            for (DeviceNode node : this.children) {
                if (node.getChildren().isEmpty()) {
                    leaves.add(node.getName());
                } else {
                    branches.add(node.getName());
                }
            }
        }
        Collections.sort(leaves);
        Collections.sort(branches);
        leaves.addAll(branches);
        return leaves;
    }

    // update to be called from external classes; this calls the recursive loop
    // to update all the children (if applicable) and fire the aggregated events
    public void update(DeviceNode updatedNode) {
        // do recursive update & fire events as needed
        DeviceNode change = updateNode(updatedNode);

        // At this point we've updated everything and the end of update() fired
        // a change event for this node (if any); if there was a change, continue
        // up the tree for all the parents
        if (change != null) {
            if (parent != null) {
                parent.childEventFired(change);
            }
        }
    }

    // do not use this one
    private DeviceNode updateNode(DeviceNode updatedNode) {
        // keep track if anything changed and should fire event
        DeviceNode changeEvent = null;

        // make sure it's this node
        if (this.compareTo(updatedNode) == 0) {
            boolean handleHere = true;

            // fire updateHandle, if it's attached
            if (this.updateHandle != null) {
                handleHere = updateHandle.handle(updatedNode);
            }

            if (handleHere) {
                // update attributes
                boolean attributeChange = this.attributes.size() < updatedNode.getAttributes().size();
                this.attributes.putAll(updatedNode.getAttributes());

                // Check to see if there was an attribute change.
                if (attributeChange) {
                    if (changeEvent == null) {
                        changeEvent = this.cloneShallow();
                    }
                }

                // set value, if needed
                if (updatedNode.getValue() != null) {
                    if (this.getValue() == null || (this.getValue() != null && !this.getValue().equals(updatedNode.getValue()))) {
                        this.setValue(updatedNode.getValue());

                        if (changeEvent == null) {
                            changeEvent = this.cloneShallow();
                        }
                    }
                }
            }

            // merge children; update or add
            for (DeviceNode child : updatedNode.getChildren()) {
                boolean added = false;

                // if child does not exist yet, add it
                if (this.getChild(child) == null) {
                    try {
                        this.addChild(child.cloneShallow());
                    } catch (Exception ex) {
                        // we just made this child; it'll always have a null parent
                    }
                    added = true;
                }

                // update child
                DeviceNode childUpdate = this.getChild(child).updateNode(child);

                // either updated child or added (if added, won't get childUpdate)
                if (childUpdate != null || added) {
                    if (changeEvent == null) {
                        changeEvent = this.cloneShallow();
                    }

                    // add the child update if there was one
                    if (childUpdate != null) {
                        try {
                            changeEvent.addChild(childUpdate);
                        } catch (Exception ex) {
                            // we just made this child; it'll always have a null parent
                        }
                    } // if the child was added, but no grandchildren changed, still fire event
                    else {
                        try {
                            changeEvent.addChild(child.cloneShallow());
                        } catch (Exception ex) {
                            // we just made this child; it'll always have a null parent
                        }
                    }
                }
            }
        }

        // fire event for this node if it or any children changed
        if (changeEvent != null) {
//            System.out.println(this.getString());
            changeSupport.firePropertyChange(PROPERTY_CHANGE_NAME, null, changeEvent);
        }

        return changeEvent;
    }

    public void removeChildren(DeviceNode child) {
        this.removeChildren(child.getName());
    }

    public void removeChildren(String name) {
        if (name == null || name.trim().length() == 0) {
            return;
        }

        synchronized (this.children) {
            DeviceNode node;
            List<Integer> idxes = new ArrayList<>();
            for (int idx = 0; idx < this.children.size(); idx++) {
                node = this.children.get(idx);
                if (node.getName().equals(name)) {
                    idxes.add(idx);
                }
            }

            Collections.sort(idxes);
            Collections.reverse(idxes);
            for (Integer idx : idxes) {
                removeChild(idx);
            }
        }
    }

    public void removeChild(int idx) {
        synchronized (this.children) {
            DeviceNode n = this.children.get(idx).cloneShallow();
            n.addAttribute("_action", "remove");
            this.children.get(idx).update(n);
            this.children.remove(idx);
        }
    }

    public void removeChild(String name) {
        this.removeChildren(name);
    }

    public void removeChild(String name, String id) {
        synchronized (this.children) {
            // Default to removing all children with this name if
            // id is not valid.
            if (id == null || id.trim().length() == 0) {
                this.removeChildren(name);
            } // If an id was passed in, look for that child.
            else {
                // Get a list of this node's children.
                List<DeviceNode> childs = this.getChildren(name);

                for (DeviceNode child : childs) {
                    // Check the id against this child.
                    if (child.getAttribute("_id") != null && child.getAttribute("_id").trim().equals(id)) {
                        // Found it.
                        DeviceNode n = child.cloneShallow();
                        n.addAttribute("_action", "remove");
                        child.update(n);
                        this.children.remove(child);
                        return;
                    }
                }
            }

            // No child found.
            return;
        }
    }

    public DeviceNode getChildByPath(String path) {
        List<DeviceNode> childs = this.searchTree(path);
        return childs != null && childs.size() > 0 ? childs.get(0) : null;
    }

    public List<DeviceNode> getChildrenByPath(String path) {
        return this.searchTree(path);
    }

    public List<DeviceNode> searchTree(String path) {
        // Always return a list.
        List<DeviceNode> ret = new ArrayList<>();

        // No path, empty list.
        if (path == null || (path = path.trim()).length() == 0) {
            return ret;
        }

        // Strip off any beginning "/" character.
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        // If the path is empty, return this node.
        if (path.isEmpty()) {
            ret.add(this);
        } else {
            // Check to see if the root matches this node.
            if (!path.split("/")[0].equals(this.getName())) {
                path = this.getName() + "/" + path;
            }

            // Recursively search the tree.
            this.searchTree(ret, path.split("/"), 0);
        }

        // Return the list.
        return ret;
    }

    private void searchTree(List<DeviceNode> ret, String[] path, int idx) {
        // Check this device node against the current index of the path.
        if (this.getName().equals(path[idx])) {
            // Check to see if there are more path elements.
            if (path.length - 1 > idx) {
                // Check to see if we want a specific id.
                if (path[idx + 1].startsWith("_")) {
                    // Make sure we even have an id to check against.
                    if (this.getAttribute("_id") == null || this.getAttribute("_id").trim().length() == 0) {
                        // No ID so return.
                        return;
                    }

                    // Check to see if the id is valid.
                    String id = path[idx + 1].substring(1);
                    if (id.trim().length() > 0 && this.getAttribute("_id") != null) {
                        // Check to see if we have the id.
                        if (!this.getAttribute("_id").trim().equals(id)) {
                            // The id did not match.
                            return;
                        }
                    } else {
                        return;
                    }

                    idx++;
                }

                if (path.length - 1 > idx) {
                    synchronized (this.children) {
                        // Loop over this node's children and search them for the next path element.
                        for (DeviceNode node : this.children) {
                            node.searchTree(ret, path, idx + 1);
                        }
                    }
                } else {
                    // We found the node, add it.
                    ret.add(this);
                }
            } else {
                // We found the node, add it.
                ret.add(this);
            }
        }
    }

// *********************************************** //
// Property change methods
// *********************************************** //
    private void childEventFired(DeviceNode n) {
        // this node will be the root node for the event
        DeviceNode change = this.cloneShallow();

        // add child event
        change.addChild(n);

        // fire event
        changeSupport.firePropertyChange(PROPERTY_CHANGE_NAME, null, change);

        // notify parent, if applicable
        if (parent != null) {
            parent.childEventFired(change);
        }
    }

    public PropertyChangeSupport getChangeSupport() {
        return changeSupport;
    }

// *********************************************** //
// Event handler methods
// *********************************************** //
    public DeviceNode get() {
        return get(new HashMap());
    }

    public DeviceNode get(HashMap<String, String> queryParameters) {
        DeviceNode ret = null;

        // fire getHandle, if it's attached
        if (this.getHandle != null) {
            ret = getHandle.handle(queryParameters);
        } else {
            // otherwise do a shallow clone on this node
            ret = this.cloneShallow();

            // loop through all children 
            if (this.children.size() > 0) {
                synchronized (this.children) {
                    for (DeviceNode child : this.children) {
                        try {
                            ret.addChild(child.get());
                        } catch (Exception ex) {
                            Logger.getLogger(DeviceNode.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }

        return ret;
    }

    public void set(DeviceNode newNode) {
        // make sure it's this node
        if (this.compareTo(newNode) == 0) {
            // fire setHandle, if it's attached
            if (this.setHandle != null) {
                setHandle.handle(newNode);
            }

            // loop through all children
            for (DeviceNode child : newNode.getChildren()) {
                if (this.getChild(child) != null) {
                    this.getChild(child).set(child);
                }
            }
        }
    }

    // shallow clone, mostly for event generation purposes
    public DeviceNode cloneShallow() {
        DeviceNode n = new DeviceNode(this.getName(), this.getValue());
        for (String str : this.getAttributes().keySet()) {
            n.addAttribute(str, this.getAttribute(str));
        }
        return n;
    }

    public String getNodePath() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.parent != null ? this.parent.getNodePath() : "").append("/").append(this.getName());
        if (this.getAttribute("_id") != null && this.getAttribute("_id").trim().length() > 0) {
            sb.append("/_").append(this.getAttribute("_id").trim());
        }

        return sb.toString();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // we don't do anything here yet...
        // the updateNode() and childEventFired() methods take care of recursion
    }

    public static String trimPath(String str, int levels) {
        StringBuilder sb = new StringBuilder(str);

        while (levels != 0) {
            int end = sb.indexOf("/", 1);
            sb.delete(0, end);
            levels--;
        }
        return sb.toString();
    }

    public String getString() {
        return getString(1, -1);
    }

    public String getString(int level, int maxDepth) {
        StringBuilder ret = new StringBuilder();
        ret.append(this.getName());
        if (this.getValue() != null) {
            ret.append(": ").append(this.getValue().toString());
        }
        for (String key : this.getAttributes().keySet()) {
            ret.append(" [").append(key).append(" = ").append(this.attributes.get(key)).append("]");
        }
        ret.append("\n");
        String spaces = "";
        for (int idx = 0; idx < level; idx++) {
            spaces += "    ";
        }

        if (maxDepth == -1 || level < maxDepth) {
            synchronized (this.children) {
                for (DeviceNode child : this.children) {
                    ret.append(spaces).append(child.getString(level + 1, maxDepth));
                }
            }
        }
        return ret.toString();
    }

    @Override
    public int compareTo(DeviceNode node2) {
        if (node2 == null) {
            return -1;
        }
        if (this.getName().equals(node2.getName())) {
            if (this.getAttribute("_id") != null && node2.getAttribute("_id") != null) {
                return this.getAttribute("_id").compareTo(node2.getAttribute("_id"));
            } else {
                return 0;
            }
        }

        return this.getName().compareTo(node2.getName());
    }
}
/*
MIT License

Copyright (c) 2020 earlygrey

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package space.earlygrey.simplegraphs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;


public abstract class Graph<V> {


    //================================================================================
    // Members
    //================================================================================

    //final Internals<V> internals = new Internals<>(this);

    final NodeMap<V> nodeMap;
    final LinkedHashMap<Connection<V>, Connection<V>> edgeMap;

    //================================================================================
    // Constructors
    //================================================================================

    protected Graph() {
        nodeMap = new NodeMap<>(this);
        edgeMap = new LinkedHashMap<>();
    }

    protected Graph(Collection<V> vertices) {
        this();
        for (V v : vertices) {
            addVertex(v);
        }
    }

    //================================================================================
    // Graph Builders
    //================================================================================

    //--------------------
    //  Abstract Methods
    //--------------------

    protected abstract Connection<V> obtainEdge();
    abstract Graph<V> createNew();
    public abstract Algorithms algorithms();

    //--------------------
    //  Public Methods
    //--------------------

    /**
     * Adds a vertex to the graph.
     * @param v the vertex to be added
     * @return true if the vertex was not already in the graph, false otherwise
     */

    public boolean addVertex(V v) {
        Node<V> node = nodeMap.put(v);
        return node != null;
    }

    /**
     * Adds all the vertices in the collection to the graph.
     * @param vertices a collection of vertices to be added
     */
    public void addVertices(Collection<V> vertices) {
        for (V v : vertices) {
            addVertex(v);
        }
    }

    public void addVertices(V... vertices) {
        for (V v : vertices) {
            addVertex(v);
        }
    }

    /**
     * Removes a vertex from the graph, and any adjacent edges.
     * @param v the vertex to be removed
     * @return true if the vertex was in the graph, false otherwise
     */
    public boolean removeVertex(V v) {
        Node<V> existing = getNode(v);
        if (existing==null) return false;
        removeNode(existing);
        return true;
    }

    /**
     * Removes all the vertices in the collection from the graph, and any adjacent edges.
     * @param vertices vertices a collection of vertices to be removed
     */
    public void removeVertices(Collection<V> vertices) {
        for (V v : vertices) {
            removeVertex(v);
        }
    }

    /**
     * Add an edge to the graph, from v to w. The edge will have a default weight of 1.
     * If there is already an edge between v and w, its weight will be set to 1.
     * @param v the source vertex of the edge
     * @param w the destination vertex of the edge
     * @return the edge
     */
    public Connection<V> addEdge(V v, V w) {
        return addEdge(v, w, Connection.DEFAULT_WEIGHT);
    }

    /**
     * Add an edge to the graph, from v to w and with the specified weight.
     * If there is already an edge between v and w, its weight will be set to the specified weight.
     * @param v the source vertex of the edge
     * @param w the destination vertex of the edge
     * @param weight the weight of the edge
     * @return the edge
     */
    public Connection<V> addEdge(V v, V w, float weight) {
        if (v == null || w == null) Errors.throwNullVertexException();
        if (v.equals(w)) Errors.throwSameVertexException();
        Node<V> a = getNode(v);
        Node<V> b = getNode(w);
        if (a == null  || b == null) Errors.throwVertexNotInGraphVertexException();
        return addConnection(a, b, weight);
    }

    /**
     * Removes the edge from v to w from the graph.
     * @param v the source vertex of the edge
     * @param w the destination vertex of the edge
     * @return the edge if there exists an edge from v to w, or null if there is no edge
     */
    public boolean removeEdge(V v, V w) {
        Node<V> a = getNode(v), b = getNode(w);
        if (a == null  || b == null) Errors.throwVertexNotInGraphVertexException();
        return removeConnection(a, b);
    }

    public boolean removeEdge(Edge<V> edge) {
        return removeConnection(edge.getInternalNodeA(), edge.getInternalNodeB());
    }

    /**
     * Removes all edges from the graph.
     */
    public void removeAllEdges() {
        for (Node<V> v : getNodes()) {
            v.disconnect();
        }
        edgeMap.clear();
    }

    /**
     * Removes all vertices and edges from the graph.
     */
    public void removeAllVertices() {
        edgeMap.clear();
        nodeMap.clear();
    }

    /**
     * Sort the vertices using the provided comparator. This is reflected in the iteration order of the collection returned
     * by {@link #getVertices()}, as well as algorithms which involve iterating over all vertices.
     * @param comparator a comparator for comparing vertices
     */
    public void sortVertices(Comparator<V> comparator) {
        /*List<Entry<V, Node<V>>> entryList = new ArrayList<>(vertexMap.entrySet());
        entryList.sort(Entry.comparingByKey(comparator));
        vertexMap.clear();
        for (Entry<V, Node<V>> entry : entryList) {
            vertexMap.put(entry.getKey(), entry.getValue());
        }*/
        nodeMap.sort(comparator);
    }

    /**
     * Sort the edges using the provided comparator. This is reflected in the iteration order of the collection returned
     * by {@link #getEdges()}, as well as algorithms which involve iterating over all edges.
     * @param comparator a comparator for comparing edges
     */
    public void sortEdges(Comparator<Connection<V>> comparator) {
        List<Entry<Connection<V>, Connection<V>>> entryList = new ArrayList<>(edgeMap.entrySet());
        Collections.sort(entryList, Entry.comparingByKey(comparator));
        edgeMap.clear();
        for (Entry<Connection<V>, Connection<V>> entry : entryList) {
            edgeMap.put(entry.getKey(), entry.getValue());
        }
    }

    //--------------------
    //  Internal Methods
    //--------------------

    void removeNode(Node<V> node) {
        for (int i = node.outEdges.size()-1; i >= 0; i--) {
            removeConnection(node.outEdges.get(i).b, node);
        }
        node.disconnect();
        nodeMap.remove(node.object);
    }

    Connection<V> addConnection(Node<V> a, Node<V> b) {
        Connection<V> e = a.addEdge(b, Connection.DEFAULT_WEIGHT);
        edgeMap.put(e, e);
        return e;
    }

    Connection<V> addConnection(Node<V> a, Node<V> b, float weight) {
        Connection<V> e = a.addEdge(b, weight);
        edgeMap.put(e, e);
        return e;
    }

    boolean removeConnection(Node<V> a, Node<V> b) {
        Connection<V> e = a.removeEdge(b);
        if (e == null) return false;
        edgeMap.remove(e);
        return true;
    }

    //================================================================================
    // Getters
    //================================================================================

    //--------------------
    //  Public Getters
    //--------------------

    /**
     * Check if the graph contains a vertex.
     * @param v the vertex with which to check
     * @return true if the graph contains the vertex, false otherwise
     */
    public boolean contains(V v) {
        return nodeMap.contains(v);
    }

    /**
     * Retrieve the edge which is from v to w.
     * @param v the source vertex of the edge
     * @param w the destination vertex of the edge
     * @return the edge if it is in the graph, otherwise null
     */
    public Edge<V> getEdge(V v, V w) {
        Node<V> a = getNode(v), b = getNode(w);
        if (a == null  || b == null) Errors.throwVertexNotInGraphVertexException();
        Connection<V> edge = getEdge(a, b);
        if (edge == null) return null;
        return edge;
    }

    /**
     * Check if the graph contains an edge from v to w.
     * @param v the source vertex of the edge
     * @param w the destination vertex of the edge
     * @return true if the edge is in the graph, false otherwise
     */
    public boolean edgeExists(V v, V w) {
        Node<V> a = getNode(v), b = getNode(w);
        if (a == null  || b == null) Errors.throwVertexNotInGraphVertexException();
        return connectionExists(a, b);
    }

    /**
     * Get a collection containing all the edges which have v as a source.
     * @param v the source vertex of all the edges
     * @return an unmodifiable collection of edges
     */
    public Collection<Edge<V>> getEdges(V v) {
        Node<V> node = getNode(v);
        if (node==null) return null;
        return Collections.unmodifiableCollection(node.outEdges);
    }

    /**
     * Get a collection containing all the edges in the graph.
     * @return an unmodifiable collection of all the edges in the graph
     */
    public Collection<Edge<V>> getEdges() {
        return Collections.unmodifiableCollection(edgeMap.keySet());
    }

    /**
     * Get a collection containing all the vertices in the graph.
     * @return an unmodifiable collection of all the vertices in the graph
     */
    public Collection<V> getVertices() {
        return nodeMap.vertexCollection;
    }


    /**
     * Check if the graph is directed, that is whether the edges form an ordered pair or a set.
     * @return whether the graph is directed
     */
    public boolean isDirected() {
        return true;
    }

    /**
     * Get the number of vertices in the graph.
     * @return the number of vertices
     */
    public int size() {
        return nodeMap.size;
    }

    /**
     * Get the number of edges in the graph.
     * @return the number of edges
     */
    public int getEdgeCount() {
        return edgeMap.size();
    }


    //--------------------
    //  Internal Getters
    //--------------------

    Node<V> getNode(V v) {
        return nodeMap.get(v);
    }

    Collection<Node<V>> getNodes() {
        return nodeMap.nodeCollection;
    }

    boolean connectionExists(Node<V> u, Node<V> v) {
        return u.getEdge(v) != null;
    }

    Connection<V> getEdge(Node<V> a, Node<V> b) {
        Connection<V> edge = a.getEdge(b);
        if (edge == null) return null;
        return edge;
    }



}

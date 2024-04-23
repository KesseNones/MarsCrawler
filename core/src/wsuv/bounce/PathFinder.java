package wsuv.bounce;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.HashMap;

public class PathFinder {
    TileMap tiles;
    Vertex[] graph;
    HashMap<Integer, Integer> visitedNodes;
    PriorityQueue<Vertex> queue;

    public PathFinder(TileMap t){
        tiles = t;
        //Sets up initial graph with non-relaxed vertices.
        graph = new Vertex[tiles.numRows * tiles.tilesInRow];
        for (int i = 0; i < graph.length; i++){
            graph[i] = new Vertex(i);
        }
        //Establishes visitedNodes and priority queue to be used by class.
        visitedNodes = new HashMap<>(tiles.numRows * tiles.tilesInRow);
        queue = new PriorityQueue<>(Comparator.comparingInt(v -> v.distanceToSource));
    }

    private boolean checkIndex(int index){
        return (index > -1 && index < tiles.tileData.length);
    }

    //Runs Dijkstra's algorithm on graph to fill out all vertices.
    public void dsAlg(int roverIndex){
        //Clears out visited nodes and queue from previous run.
        visitedNodes.clear();
        queue.clear();

        //Total cost from source so far.
        int costAcc = 1;

        int neighborIndex = -1;

        int [] neighborOffsets = new int[] {1, tiles.tilesInRow, -1, -tiles.tilesInRow};

        //Sets all nodes in graph with effective cost infinity.
        for (Vertex vertex : graph) {
            vertex.distanceToSource = 999999999;
            vertex.closerVert = -1;
        }

        //Establishes starting node in graph.
        graph[roverIndex].distanceToSource = 0;
        queue.add(graph[roverIndex]);

        while (!queue.isEmpty()){
            Vertex curr = queue.remove();

            visitedNodes.put(curr.location, curr.distanceToSource);

            //Explores all neighbor nodes of graph.
            for (int i = 0; i < neighborOffsets.length ; i ++){
                neighborIndex = curr.location + neighborOffsets[i];
                //Continues forward if neighbor is valid.
                if (checkIndex(neighborIndex) && !visitedNodes.containsKey(neighborIndex)){
                    graph[neighborIndex].closerVert = curr.location;

                    //Calculates new tile cost of given tile.
                    int tileCost;
                    if (tiles.tileData[neighborIndex] != 1){
                        tileCost = 999999;
                    }else{
                        tileCost = 1;
                    }
                    graph[neighborIndex].distanceToSource = curr.distanceToSource + tileCost;

                    queue.add(graph[neighborIndex]);
                }
            }
        }
    }

}

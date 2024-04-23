package wsuv.bounce;

//This class will be used in pathfinding.
public class Vertex {
    int distanceToSource;
    int closerVert;
    int location;

    public Vertex(int loc){
        distanceToSource = 999999999;
        closerVert = -1;
        location = loc;
    }
}

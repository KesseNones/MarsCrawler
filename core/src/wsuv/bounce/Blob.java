package wsuv.bounce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class Blob extends Sprite {
    enum BlobState {WANDERING, CHASE, VAPEROUS}
    BlobState state;
    boolean isAlive;
    float moveSpeed;
    TileMap tiles;
    PathFinder path;
    int distanceThresholdInit;
    int currentDistanceThreshold;
    private Texture[] blobTextures;

    private int gasTimer;
    private int gasTimerStartVal;

    public Blob(MarsGame game, float startX, float startY, TileMap map, PathFinder p){
        super(game.am.get(MarsGame.RSC_BLOB, Texture.class));
        isAlive = true;
        moveSpeed = 1.2f;
        tiles = map;
        path = p;
        state = BlobState.WANDERING;
        distanceThresholdInit = 5;
        gasTimerStartVal = 500;
        gasTimer = gasTimerStartVal;
        currentDistanceThreshold = distanceThresholdInit;

        //Loads valid blob textures for future use.
        blobTextures = new Texture[3];
        blobTextures[0] = new Texture(MarsGame.RSC_BLOB);
        blobTextures[1] = new Texture(MarsGame.RSC_CHASE_BLOB);
        blobTextures[2] = new Texture(MarsGame.RSC_GAS_BLOB);

        setCenter(startX + (getWidth() / 2f), startY + (getHeight() / 2f));
    }

    public float[] getCenter(){
        float[] coords = new float[2];
        coords[0] = getX() + getWidth();
        coords[1] = getY() + getHeight();
        return coords;
    }

    public int[] findRowAndCol(float x, float y){
        int[] rowCol = new int[2];
        rowCol[0] = (int)(y) / tiles.tileSize;
        rowCol[1] = (int)(x) / tiles.tileSize;
        return rowCol;
    }

    public void move(boolean roverHasShield, float[] roverCoords){
        float[] center = getCenter();
        int[] idealCoords = tiles.indexToCoords(tiles.coordsToIndex(center[0] -
                (tiles.tileSize / 2f), center[1] - (tiles.tileSize / 2f)));
        int[] rowAndCol = findRowAndCol(getX(), getY());
        double tau = Math.PI * 2;

        if (state != BlobState.VAPEROUS){
            gasTimer = gasTimerStartVal;
            //If blob is close enough to player, chase the player,
            // otherwise randomly wander by acting like a roomba.
            if (!roverHasShield){
                float[] blobCoords = this.getCenter();
                float crowDist = (float)Math.sqrt(
                        Math.pow(Math.abs(roverCoords[0] - blobCoords[0]), 2)
                                +
                        Math.pow(Math.abs(roverCoords[1] - blobCoords[1]), 2));
                if (crowDist < (currentDistanceThreshold * tiles.tileSize)){
                    state = BlobState.CHASE;
                    setTexture(blobTextures[1]);
                }else{
                    state = BlobState.WANDERING;
                    setTexture(blobTextures[0]);
                }
            }else{
                state = BlobState.WANDERING;
                setTexture(blobTextures[0]);
            }

            //In the chase state, the blob is using pathfinding to get to the player directly.
            if (state == BlobState.CHASE ){
                moveSpeed = 1.35f;
                //Fetches index of map to move to from pathfinding graph.
                int moveToIndex = path.graph[tiles.coordsToIndex(center[0] - (tiles.tileSize / 2f),
                        center[1] - (tiles.tileSize / 2f))].closerVert;

                //Does nothing if blob has basically reached player.
                if (moveToIndex == -1) {return;}

                int[] moveToRC = new int[] {moveToIndex / tiles.tilesInRow, moveToIndex % tiles.tilesInRow};

                //Sets rotation and coords based on intended move direction.
                if (moveToRC[0] - rowAndCol[0] > 0){
                    setRotation(90);
                    setX(idealCoords[0]);
                }
                else if (moveToRC[0] - rowAndCol[0] < 0){
                    setRotation(270);
                    setX(idealCoords[0]);
                }
                else if (moveToRC[1] - rowAndCol[1] > 0){
                    setRotation(0);
                    setY(idealCoords[1]);
                }
                else if (moveToRC[1] - rowAndCol[1] < 0){
                    setRotation(180);
                    setY(idealCoords[1]);
                }

                //Moves in appropriate direction.
                double currentRotation = (getRotation() / 360) * tau;
                setX(getX() + ((float)Math.cos(currentRotation) * moveSpeed));
                setY(getY() + ((float)Math.sin(currentRotation) * moveSpeed));

            }

            //In wandering state, the blobs randomly wander around
            // by going forward until hitting a wall, then randomly turning left or right.
            if (state == BlobState.WANDERING){
                moveSpeed = 1.2f;

                //Tries to move forward.
                double currentRotation = (getRotation() / 360) * tau;
                setX(getX() + ((float)Math.cos(currentRotation) * moveSpeed));
                setY(getY() + ((float)Math.sin(currentRotation) * moveSpeed));

                //Fetches coords and index of where it's trying to move.
                float[] nextCoords = new float[] {
                        idealCoords[0] + (tiles.tileSize * ((float)Math.cos(currentRotation))),
                        idealCoords[1] + (tiles.tileSize * ((float)Math.sin(currentRotation))) };
                int moveToIndex = tiles.coordsToIndex(nextCoords[0], nextCoords[1]);

                //If the tile it's trying to move to is a wall,
                // turn randomly left or right relative to blob's forward.
                if ( tiles.tileData[moveToIndex] != 1 ){
                    setX(idealCoords[0]);
                    setY(idealCoords[1]);
                    if (Math.random() >= 0.5){
                        setRotation(getRotation() + 90);
                    }else{
                        setRotation(getRotation() - 90);
                    }
                }
            }
        }else{
            moveSpeed = 7;
            gasTimer -= 1;
            setTexture(blobTextures[2]);

            //Blob condenses back to regular if time is out.
            if (gasTimer < 0){
                state = BlobState.WANDERING;
            }

            //Tries to move forward.
            double currentRotation = (getRotation() / 360) * tau;
            setX(getX() + ((float)Math.cos(currentRotation) * moveSpeed));
            setY(getY() + ((float)Math.sin(currentRotation) * moveSpeed));

            //Fetches coords and index of where it's trying to move.
            float[] nextCoords = new float[] {
                    idealCoords[0] + (tiles.tileSize * ((float)Math.cos(currentRotation))),
                    idealCoords[1] + (tiles.tileSize * ((float)Math.sin(currentRotation))) };
            int moveToIndex = tiles.coordsToIndex(nextCoords[0], nextCoords[1]);

            //If the tile it's trying to move to is a wall,
            // turn randomly left or right relative to blob's forward.
            if ( tiles.tileData[moveToIndex] != 1 ){
                setX(idealCoords[0]);
                setY(idealCoords[1]);
                if (Math.random() >= 0.5){
                    setRotation(getRotation() + 90);
                }else{
                    setRotation(getRotation() - 90);
                }
            }
        }
    }
}

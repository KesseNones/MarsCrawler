package wsuv.bounce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import wsuv.bounce.MarsGame;

public class TileMap {
    byte[] tileData;
    int tileSize;
    int tilesInRow;
    int numRows;

    public TileMap(){
        tileSize = 30;
        tilesInRow = 20;
        numRows = 30;

        tileData = new byte[tilesInRow * numRows];
    }

    //Takes in an index to the tiles
    // and converts it to world x and y coords.
    public int[] indexToCoords(int index){
        int[] coords = new int[2];
        coords[0] = ((index % tilesInRow) * tileSize);
        coords[1] = ((index / tilesInRow) * tileSize);
        return coords;
    }

    //Takes in an input set of coordinates
    // and turns them into an index into the tile data.
    public int coordsToIndex(float x, float y){
        return ( ((int)(y / tileSize)) * tilesInRow ) + ((int)(x / tileSize));
    }

    //Reads in a file name from assets and interpolates
    // the data as what each tile should be set to.
    public void readFileToTileData(String fileName){
        FileHandle input = Gdx.files.internal(fileName);
        String[] fileStrs = input.readString().split("\n");

        for (int i = 0; i < numRows; i ++){
            for (int j = 0; j < tilesInRow; j++){
                tileData[(i * tilesInRow) + j] = (byte)(fileStrs[i].charAt(j) - '0');
            }
        }
    }

}

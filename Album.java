package sample;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import javafx.scene.image.Image;
import sample.Main;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class Album extends Main {

    private String albumname;
    Mp3File mp3file;
    int trackamt = 0;
    Mp3File[] tracks = new Mp3File[trackamt+1];

    public Album(String albumname){

        this.albumname = albumname;
    }

    public String getAlbumTitle(){
        return albumname;
    }

    public void updateTracks(){
        Mp3File[] tempfile = tracks;
        tracks = new Mp3File[trackamt+1];
        for(int i = 0; i <= tempfile.length-1; i++){
            tracks[i] = tempfile[i];
        }
        //trackamt += 1;
    }

}

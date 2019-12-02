package sample;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.fxml.FXML;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.farng.mp3.MP3File;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Main extends Application {

    private int albumamount = 0;

    private BorderPane root = new BorderPane();
    private VBox albuminfo = new VBox();
    private VBox albumlistpane = new VBox();
    //private VBox[] tracklisting;
    private ScrollPane tracklistingscroll = new ScrollPane();
    private Rectangle infobox = new Rectangle();
    private StackPane infopane = new StackPane();
    private Background infobg = new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.8), CornerRadii.EMPTY, Insets.EMPTY));
    private Font infofont = new Font(30);
    private double xOffSet, yOffSet;
    private BufferedWriter writer;
    private MediaPlayer player;
    private Slider volumeslider = new Slider();
    private Slider progressbar = new Slider();
    private File filepath = new File(System.getenv("APPDATA") + "/MusicBE Songs.txt");
    private Album[] album = new Album[1000];
    private BufferedReader reader;
    private NavigableMap<String, NavigableMap<String, Mp3File>> albummap = new TreeMap<String, NavigableMap<String, Mp3File>>();
    private Media media;
    private File[] queue;
    private int trackno;
    private int testno = 0;
    private Label progresstime = new Label();
    private ChangeListener<Duration> progresslistener;
    //private Map<String, Mp3File> trackmap = new HashMap<String, Mp3File>();



    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.setTitle("MusicBE");
        primaryStage.initStyle(StageStyle.UNDECORATED);

        if(filepath.exists()){
            System.out.println("file detected");
            reader = new BufferedReader(new FileReader(filepath));
            String line = reader.readLine();
            while(line != null){
                File file = new File(line);
                try {
                    Mp3File m3pfile = new Mp3File(file);
                    if(m3pfile.getId3v2Tag().getAlbumImageMimeType().equals("image/bmp")){
                        m3pfile.getId3v2Tag().setAlbumImage(null, "image/jpeg");
                    }
                    String trackname = m3pfile.getId3v2Tag().getTitle();
                    String albumname = m3pfile.getId3v2Tag().getAlbum();
                    if (albummap.containsKey(albumname)){
                        if (albummap.get(albumname).containsKey(m3pfile.getId3v2Tag().getTitle())) {
                            System.out.println(trackname + " already exists");
                        }
                        else{
                            albummap.get(m3pfile.getId3v2Tag().getAlbum()).put(m3pfile.getId3v2Tag().getTitle(), m3pfile);
                            System.out.println(trackname + " added to " + albumname);
                            testno++;
                        }
                    }
                    else{
                        albummap.put(m3pfile.getId3v2Tag().getAlbum(), new TreeMap<String, Mp3File>());
                        albummap.get(m3pfile.getId3v2Tag().getAlbum()).put(m3pfile.getId3v2Tag().getTitle(), m3pfile);
                        System.out.println(trackname + " added to new album " + albumname);
                        testno++;
                    }
                }
                catch(UnsupportedTagException | IOException | InvalidDataException e){
                    e.printStackTrace();
                }
                line = reader.readLine();
            }
            reader.close();
        }
        drawAlbums();
        // Filechooser
        FileChooser fileChooser = new FileChooser();

        progresstime.setText("00:00");

        BackgroundFill bgfill = new BackgroundFill(Color.rgb(50, 50, 50, 0), CornerRadii.EMPTY, Insets.EMPTY);
        Image img = new Image("https://i.imgur.com/msTcSMc.png");
        BackgroundImage bgimg = new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
        Background bg = new Background(bgimg);

        root.setBackground(bg);

        //toolbar
        volumeslider.setValue(100);
        volumeslider.setMax(100);
        volumeslider.setMin(0);
        volumeslider.setMaxWidth(100);
        volumeslider.setTranslateX(80);
        volumeslider.getStyleClass().addAll("slider thumb");
        volumeslider.valueProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if(player != null){
                    player.setVolume(volumeslider.getValue()/100);
                }
            }
        });
        progressbar.setMax(1);
        progressbar.setMin(0);
        progressbar.setMaxWidth(500);
        progressbar.setTranslateX(180);
        progressbar.valueProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if(player != null){
                    //player.seek(Duration.seconds(progressbar.getValue()));
                    //progressbar.setValue(player.getCurrentTime().toSeconds());
                }
            }
        });

        progresslistener = new ChangeListener<Duration>() {
            @Override
            public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
                progressbar.setValue(player.getCurrentTime().toSeconds());
                long seconds = (long)player.getCurrentTime().toSeconds();
                String time = String.format(
                        "%02d:%02d",
                        /*seconds / 3600,*/
                        (seconds % 3600) / 60,
                        seconds % 60);
                progresstime.setText(time);
            }
        };

        GridPane toolbar = new GridPane();
        // square for toolbar
        Rectangle toolbarrec = new Rectangle();
        toolbarrec.setHeight(20);
        toolbarrec.setWidth(800);
        toolbarrec.setFill(Color.rgb(20, 20, 20));
        toolbarrec.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffSet = primaryStage.getX() - event.getScreenX();
                yOffSet = primaryStage.getY() - event.getScreenY();
            }
        });
        toolbarrec.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                primaryStage.setX(event.getScreenX() - primaryStage.getWidth());
                primaryStage.setY(event.getScreenY() + yOffSet);
            }
        });

        progresstime.setTextFill(Color.WHITE);
        progresstime.setTranslateX(680);

        toolbar.getChildren().addAll(toolbarrec, volumeslider, progressbar);
        // close button
        Label closebtn = new Label();
        closebtn.setText("X");
        closebtn.setTextFill(Color.WHITE);
        closebtn.setTranslateX(780);
        closebtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                    saveSession();
                    primaryStage.close();
            }
        });
        toolbar.getChildren().add(closebtn);
        tracklistingscroll.setFitToWidth(true);
        // add button
        Label addbtn = new Label();
        addbtn.setText("+");
        addbtn.setTextFill(Color.WHITE);
        // adds albums
        addbtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                List<File> list = fileChooser.showOpenMultipleDialog(primaryStage);
                if(list != null) {
                    for(File file: list){
                        try {
                            Mp3File m3pfile = new Mp3File(file);
                            String trackname = m3pfile.getId3v2Tag().getTitle();
                            String albumname = m3pfile.getId3v2Tag().getAlbum();
                            if (albummap.containsKey(albumname)){
                                if (albummap.get(albumname).containsKey(m3pfile.getId3v2Tag().getTitle())) {
                                    System.out.println(trackname + " already exists");
                                }
                                else{
                                    albummap.get(m3pfile.getId3v2Tag().getAlbum()).put(m3pfile.getId3v2Tag().getTitle(), m3pfile);
                                    System.out.println(trackname + " added to " + albumname);
                                }
                            }
                            else{
                                albummap.put(m3pfile.getId3v2Tag().getAlbum(), new TreeMap<String, Mp3File>());
                                albummap.get(m3pfile.getId3v2Tag().getAlbum()).put(m3pfile.getId3v2Tag().getTitle(), m3pfile);
                                System.out.println(trackname + " added to new album " + albumname);
                            }
                        }
                        catch(UnsupportedTagException | IOException | InvalidDataException e){
                            e.printStackTrace();
                        }
                    }
                }
                drawAlbums();
            }
        });
        Label name = new Label();
        name.setText("MUSICBE");
        name.setTextFill(Color.WHITE);
        name.setTranslateX(20);
        toolbar.getChildren().addAll(name, addbtn, progresstime);
        root.setTop(toolbar);

        infobox.setWidth(240);
        infobox.setHeight(600);
        infobox.setFill(Color.rgb(0, 0, 0, .8));
        infopane.getChildren().addAll(infobox, albumlistpane);

        //Scene for album squares
        albuminfo.setAlignment(Pos.TOP_CENTER);
        albuminfo.setPadding(new Insets(30, 30, 30, 30));
        albuminfo.setSpacing(50);
        albumlistpane.setPadding(new Insets(15, 0, 15, 10));
        albumlistpane.setSpacing(20);
        albumlistpane.setMaxWidth(infobox.getWidth());
        //tracklisting.setAlignment(Pos.TOP_CENTER);
        //tracklistingscroll.setStyle("-fx-background: rgba(0,0,0, .8); -fx-padding: -1;");
        tracklistingscroll.setOpacity(.8);
        root.setCenter(albuminfo);
        root.setRight(infopane);
        //primaryStage.setScene(new Scene(root, Screen.getPrimary().getVisualBounds().getWidth(), Screen.getPrimary().getVisualBounds().getHeight()));
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add("stylesheet.css");
        primaryStage.setScene(scene);

        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                saveSession();
            }
        });
        System.out.println(testno);
    }

    private void drawAlbums(){
        ImageView[] covers = new ImageView[albummap.size()];
        Label[] albumlist = new Label[albummap.size()];
        Label[] albumtitle = new Label[albummap.size()];
        VBox[] tracklisting = new VBox[albummap.size()];
        albumlistpane.getChildren().clear();
        albuminfo.getChildren().clear();
        int albumno = 0;
        for(Map.Entry<String, NavigableMap<String, Mp3File>> i: albummap.entrySet()){
            trackno = 0;
            int annoyingno = albumno;
            tracklisting[albumno] = new VBox();
            Label[][] tracklist = new Label[albummap.size()][i.getValue().size()];
            for(Map.Entry<String, Mp3File> j: i.getValue().entrySet()){
                int track = trackno;
                covers[albumno] = new ImageView();
                covers[albumno].setImage(getAlbumCover(j.getValue()));
                covers[albumno].setFitWidth(200);
                covers[albumno].setFitHeight(200);
                covers[albumno].setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 30, 0, 0, 0);");
                tracklist[albumno][trackno] = new Label();
                tracklist[albumno][trackno].setText(j.getKey());
                tracklist[albumno][trackno].setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        if(player != null){
                            player.stop();
                        }
                        queue = new File[i.getValue().size()-track];
                        queue[0] = toFile(j.getValue());
                        Mp3File next = j.getValue();
                        System.out.println(next.getId3v2Tag().getTrack());
                        System.out.println(next.getId3v2Tag().getAlbumImageMimeType());
                        System.out.println("-------");
                        if(albummap.get(i.getKey()).higherEntry(j.getValue().getId3v2Tag().getTitle()) != null) {
                            next = albummap.get(i.getKey()).higherEntry(j.getValue().getId3v2Tag().getTitle()).getValue();
                            queue[1] = toFile(next);
                        }
                        for(int x = 2; x < i.getValue().size()-track; x++){
                            next = albummap.get(i.getKey()).higherEntry(next.getId3v2Tag().getTitle()).getValue();
                            queue[x] = toFile(next);
                        }

                        /*for(File names: queue){
                            System.out.println(names);
                        }*/

                        media = new Media(toFile(j.getValue()).toURI().toString());
                        player = new MediaPlayer(media);
                        player.currentTimeProperty().addListener(progresslistener);
                        player.setVolume(volumeslider.getValue()/100);
                        player.setOnEndOfMedia(() -> {
                            for(int x = 1; x < queue.length; x++){
                                media = new Media(queue[x].getAbsolutePath());
                                player = new MediaPlayer(media);
                                player.play();
                            }
                        });
                        player.setOnReady(() -> {
                            progressbar.setMax(player.getMedia().getDuration().toSeconds());
                            progressbar.setValue(0);
                        });
                        player.play();
                    }
                });
                tracklisting[albumno].setAlignment(Pos.TOP_CENTER);
                tracklisting[albumno].getChildren().add(tracklist[albumno][trackno]);
                trackno++;
            }
            albumtitle[albumno] = new Label();
            albumtitle[albumno].setTextFill(Color.WHITE);
            albumtitle[albumno].setText(i.getKey());
            albumtitle[albumno].setBackground(infobg);
            albumtitle[albumno].setFont(infofont);
            albumtitle[albumno].setPadding(new Insets(5, 10, 5, 10));
            albumlist[albumno] = new Label();
            albumlist[albumno].setTextFill(Color.WHITE);
            albumlist[albumno].setText(i.getKey());
            albumlist[albumno].setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    albuminfo.getChildren().clear();
                    TranslateTransition tranTransition = new TranslateTransition();
                    tranTransition.setDuration(Duration.millis(500));
                    tranTransition.setNode(covers[annoyingno]);
                    tranTransition.setByX(500);
                    tranTransition.setCycleCount(0);
                    covers[annoyingno].setTranslateX(-500);
                    tranTransition.play();
                    tracklistingscroll.setContent(tracklisting[annoyingno]);
                    albuminfo.getChildren().addAll(covers[annoyingno], albumtitle[annoyingno], tracklistingscroll);
                }
            });
            albumlistpane.getChildren().add(albumlist[albumno]);
            albumno++;
        }
    }

    private Image getAlbumCover(Mp3File mp3file){
        byte[] imageData;
        Image img = new Image("https://previews.123rf.com/images/arcady31/arcady311705/arcady31170500009/77165345-oops-vector-banner-with-emoji.jpg");
        try {
            imageData = mp3file.getId3v2Tag().getAlbumImage();
            if(imageData != null) {
                img = SwingFXUtils.toFXImage(ImageIO.read(new ByteArrayInputStream(imageData)), null);
            }
            return img;

        }
        catch(IOException n){
            n.printStackTrace();
            return img;
        }
    }

    private void saveSession(){
        try{
            writer = new BufferedWriter(new FileWriter(filepath));
            for(Map.Entry<String, NavigableMap<String, Mp3File>> i: albummap.entrySet()){
                for(Map.Entry<String, Mp3File> j: i.getValue().entrySet()){
                    String filename = j.getValue().getFilename();
                    writer.append(filename);
                    writer.newLine();
                }
            }
            writer.close();

        }
        catch(IOException i){
            i.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private File toFile(Mp3File mp3file){
        return new File(mp3file.getFilename());
    }

}

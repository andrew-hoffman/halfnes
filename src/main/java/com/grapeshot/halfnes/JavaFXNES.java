package com.grapeshot.halfnes;

import com.grapeshot.halfnes.ui.ControllerImpl;
import com.grapeshot.halfnes.ui.GUIInterface;
import com.grapeshot.halfnes.ui.OnScreenMenu;
import com.grapeshot.halfnes.video.NesColors;
import java.nio.ByteBuffer;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

/**
 * @author Stephen Chin - steveonjava@gmail.com
 */
public class JavaFXNES extends Application implements GUIInterface {


    // Set the overscan insets to match your config
    // And make sure your framebuffer is set to:
    // * screen.width + overscan.right
    // * screen.height + overscan.bottom
    
    //overscan for PC
    private static final Insets overscan = new Insets(0, 0, 0, 0);
    
    //overscan for Pi screen
    //private static final Insets overscan = new Insets(-59, 160, 150, 0);
    private static final Insets extraOverscan = new Insets(8, 0, 8, 0);

    private NES nes;
    private Canvas gameCanvas;
    private Stage stage;
    private OnScreenMenu menu;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        //Rectangle2D bounds = Screen.getPrimary().getBounds();
        Rectangle2D bounds = new Rectangle2D(0,0,640,480);
        gameCanvas = new Canvas(256, 240);
        stage.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, e -> nes.quit());
        menu = new OnScreenMenu(this);
        //menu.setPadding(extraOverscan);
        menu.setPrefWidth(256);
        menu.setPrefHeight(240);
        Group root = new Group(gameCanvas, menu);
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.BLACK);
        stage.setScene(scene);
        //stage.setFullScreen(true);
        stage.setFullScreenExitKeyCombination(KeyCombination.valueOf("F11"));
        stage.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                menu.show();
            }
        });
        root.setLayoutX(overscan.getRight() - overscan.getLeft() - extraOverscan.getLeft() * bounds.getWidth() / 256);
        root.setLayoutY(overscan.getBottom() - overscan.getTop() - extraOverscan.getTop() * bounds.getHeight() / 240);
        root.getTransforms().add(new Scale(
            (bounds.getWidth() - (overscan.getRight() - overscan.getLeft())) / (256 - extraOverscan.getLeft() - extraOverscan.getRight()),
            (bounds.getHeight() - (overscan.getBottom() - overscan.getTop())) / (240 - extraOverscan.getTop() - extraOverscan.getBottom())));
        nes = new NES(this);
        ControllerImpl padController1 = new ControllerImpl(scene, 0);
        ControllerImpl padController2 = new ControllerImpl(scene, 1);
        padController1.startEventQueue();
        padController2.startEventQueue();
        nes.setControllers(padController1, padController2);
        final List<String> params = getParameters().getRaw();
        new Thread(() -> {
            if (params.isEmpty()) {
                nes.run();
            } else {
                nes.run(params.get(0));
            }
        }, "Game Thread").start();
    }

    public static void main(String[] args) {
        JInputHelper.setupJInput();
        launch(args);
    }

    @Override
    public NES getNes() {
        return nes;
    }

    @Override
    public void setNES(NES nes) {
        this.nes = nes;
    }

    final byte[] buffer = new byte[256 * 240 * 4];
    final WritablePixelFormat<ByteBuffer> format = WritablePixelFormat.getByteBgraPreInstance();

    private final long[] frametimes = new long[60];
    private int frametimeptr = 0;
    private double fps;

    @Override
    public void setFrame(int[] nespixels, int[] bgcolor, boolean dotcrawl) {
        Platform.runLater(() -> {
            frametimes[frametimeptr] = nes.getFrameTime();
            ++frametimeptr;
            frametimeptr %= frametimes.length;

            if (frametimeptr == 0) {
                long averageframes = 0;
                for (long l : frametimes) {
                    averageframes += l;
                }
                averageframes /= frametimes.length;
                fps = 1E9 / averageframes;
                stage.setTitle(String.format("HalfNES %s, %2.2f fps",
                    //                    + ((nes.frameskip > 0) ? " frameskip " + nes.frameskip : ""),
                    NES.VERSION,
                    //                    nes.getCurrentRomName(),
                    fps));
            }
            PixelWriter writer = gameCanvas.getGraphicsContext2D().getPixelWriter();
            for (int i = 0; i < nespixels.length; i++) {
                byte[] colbytes = NesColors.colbytes[(nespixels[i] & 0x1c0) >> 6][nespixels[i] & 0x3f];
                System.arraycopy(colbytes, 0, buffer, i * 4, 3);
            }
            writer.setPixels(0, 0, 256, 240, format, buffer, 0, 256 * 4);
        });
    }

    @Override
    public void messageBox(String message) {
        System.out.println("message = " + message);
    }

    @Override
    public void run() {
        Platform.runLater(() -> {
            stage.show();
            menu.show();
        });
    }

    @Override
    public void render() {
        // whatever...
    }

    public void loadROMs(String path) {
        menu.loadROMs(path);
    }
}

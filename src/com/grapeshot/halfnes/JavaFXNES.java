package com.grapeshot.halfnes;

import com.grapeshot.halfnes.ui.ControllerImpl;
import com.grapeshot.halfnes.ui.GUIInterface;
import com.grapeshot.halfnes.video.NesColors;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class JavaFXNES extends Application implements GUIInterface {

  private NES nes;
  private Canvas gameCanvas;
  private Stage stage;

  // Set the overscan insets to match your config
  // And make sure your framebuffer is set to:
  // * screen.width + overscan.right
  // * screen.height + overscan.bottom
  Insets overscan = new Insets(-59, 160, 150, 0);
  Insets extraOverscan = new Insets(8, 0, 8, 0);

  @Override
  public void start(Stage stage) throws Exception {
    this.stage = stage;
    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
    gameCanvas = new Canvas(256, 240);
    Group root = new Group(gameCanvas);
    Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.BLACK);
    stage.setScene(scene);
    stage.setX(overscan.getRight() - overscan.getLeft() - extraOverscan.getLeft());
    stage.setY(overscan.getBottom() - overscan.getTop() - extraOverscan.getTop());
    gameCanvas.getTransforms().add(new Scale(
        (bounds.getWidth() - (overscan.getRight() - overscan.getLeft())) / (256 - extraOverscan.getLeft() - extraOverscan.getRight()),
        (bounds.getHeight() - (overscan.getBottom() - overscan.getTop())) / (240 - extraOverscan.getTop() - extraOverscan.getBottom())));
    nes = new NES(this);
    ControllerImpl padController1 = new ControllerImpl(null, 0);
    ControllerImpl padController2 = new ControllerImpl(null, 1);
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
    launch(args);
  }

  @Override
  public void setNES(NES nes) {
    this.nes = nes;
  }

  final byte[] buffer = new byte[256 * 240 * 4];
  final WritablePixelFormat<ByteBuffer> format = WritablePixelFormat.getByteBgraPreInstance();

  private long[] frametimes = new long[60];
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
      System.out.println(stage.getTitle());
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
    Platform.runLater(stage::show);
  }

  @Override
  public void render() {
    // whatever...
  }

  @Override
  public void loadROM(String path) {
    if (path.endsWith(".zip") || path.endsWith(".ZIP")) {
      try {
        loadRomFromZip(path);
      } catch (IOException ex) {
        this.messageBox("Could not load file:\nFile does not exist or is not a valid NES game.\n" + ex.getMessage());
      }
    } else {
      nes.loadROM(path);
    }
  }

  private void loadRomFromZip(String zipName) throws IOException {
    final String romName = selectRomInZip(listRomsInZip(zipName));
    if (romName != null) {
      final File extractedFile = extractRomFromZip(zipName, romName);
      if (extractedFile != null) {
        extractedFile.deleteOnExit();
      }
      nes.loadROM(extractedFile.getCanonicalPath());
    }
  }

  private List<String> listRomsInZip(String zipName) throws IOException {
    final ZipFile zipFile = new ZipFile(zipName);
    final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
    final List<String> romNames = new ArrayList<String>();
    while (zipEntries.hasMoreElements()) {
      final ZipEntry entry = zipEntries.nextElement();
      if (!entry.isDirectory() && (entry.getName().endsWith(".nes")
          || entry.getName().endsWith(".fds")
          || entry.getName().endsWith(".nsf"))) {
        romNames.add(entry.getName());
      }
    }
    zipFile.close();
    if (romNames.isEmpty()) {
      throw new IOException("No NES games found in ZIP file.");
    }
    return romNames;
  }

  private String selectRomInZip(List<String> romNames) {
    if (romNames.size() > 1) {
      throw new IllegalStateException("multi rom zips not supported");
    } else if (romNames.size() == 1) {
      return romNames.get(0);
    }
    return null;
  }

  private File extractRomFromZip(String zipName, String romName) throws IOException {
    final ZipInputStream zipStream = new ZipInputStream(new FileInputStream(zipName));
    ZipEntry entry;
    do {
      entry = zipStream.getNextEntry();
    } while ((entry != null) && (!entry.getName().equals(romName)));
    if (entry == null) {
      zipStream.close();
      throw new IOException("Cannot find file " + romName + " inside archive " + zipName);
    }
        //name temp. extracted file after parent zip and file inside

    //note: here's the bug, when it saves the temp file if it's in a folder 
    //in the zip it's trying to put it in the same folder outside the zip
    final File outputFile = new File(new File(zipName).getParent()
        + File.separator + FileUtils.stripExtension(new File(zipName).getName())
        + " - " + romName);
    if (outputFile.exists()) {
      if (!outputFile.delete()) {
        this.messageBox("Cannot extract file. File " + outputFile.getCanonicalPath() + " already exists.");
        zipStream.close();
        return null;
      }
    }
    final byte[] buf = new byte[4096];
    final FileOutputStream fos = new FileOutputStream(outputFile);
    int numBytes;
    while ((numBytes = zipStream.read(buf, 0, buf.length)) != -1) {
      fos.write(buf, 0, numBytes);
    }
    zipStream.close();
    fos.close();
    return outputFile;
  }

}

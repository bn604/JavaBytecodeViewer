package core;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Main
        extends Application {

    public static void main(final String[] args) {
        
        launch(args);
        
    }
    
    @Override
    public void start(final Stage primaryStage)
            throws IOException {
        
        binaryViewer = FXMLLoader.load(Path.of("res/fxml/binary-viewer.fxml").toUri().toURL());

        StackPane.setMargin(binaryViewer, new Insets(7.0));
        
        final var fxmlLoader = new FXMLLoader(Path.of("res/fxml/java-viewer.fxml").toUri().toURL());
        
        fxmlLoader.setController(this);
        
        scene = new Scene(new Group());
        
        scene.setRoot(fxmlLoader.load());
        
        scene.getStylesheets().add(new File("res/style/style.css").toURI().toURL().toExternalForm());
        
        primaryStage.setScene(scene);
        
        primaryStage.setTitle("Bytecode Viewer");
        
        primaryStage.centerOnScreen();
        
        primaryStage.show();
        
    }
    
    private Scene scene;
    
    @FXML
    private TextArea binaryViewer;
    
    @FXML
    private Button compileButton;
    
    @FXML
    private TextArea javaEditor;
    
    @FXML
    private TextArea bytecodeViewer;
    
    private boolean keyArmed = false;
    
    @FXML
    private CheckBox verboseCheckBox;
    
    @FXML
    private ProgressIndicator compileProgress;
    
    @FXML
    private StackPane disassemblyArea;
    
    @FXML
    private ToggleButton binaryButton;
    
    @FXML
    private Slider javaEditorFontSlider;
    
    @FXML
    private Slider bytecodeViewerFontSlider;
    
    @FXML
    private Tooltip compileButtonTooltip;
    
    @FXML
    private Tooltip verboseTooltip;
    
    private ExecutorService compiler;
    
    private enum CompilationStatus {
        
        READY,
        
        RUNNING
        
    }
    
    private final ObjectProperty<CompilationStatus> compilationStatusProperty = new SimpleObjectProperty<>(CompilationStatus.READY);
    
    private boolean inputDirty = true;
    
    private static final String DEFAULT_CODE = """
                public final class HelloWorld {
                    
                    public static void main(final String[] args) {
                        
                        System.out.println("Hello, world!");
                        
                    }
                    
                }""";
    
    @FXML
    private void initialize() {
        
        final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
            
            final var thread = new Thread(runnable);
            
            thread.setDaemon(true);
            
            return thread;
        });
        
        executorService.submit(() -> {

            String code = DEFAULT_CODE;

            try {

                code = Files.readString(Path.of("res/save/save.txt"));
                
            } catch (final IOException ignored) { }
            
            final String finalCode = code;
            
            Platform.runLater(() -> {
                
                javaEditor.setText(finalCode);
                
                javaEditor.setDisable(false);
                
            });
            
        });
        
        executorService.shutdown();
        
        Platform.runLater(() -> compileButton.requestFocus());
        
        javaEditor.setDisable(true);
        
        binaryButton.setText("ABCD");
        
        binaryButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            
            if (newValue) {
                
                disassemblyArea.getChildren().set(0, binaryViewer);
                
                binaryButton.setText("0xFF");
                
            } else {

                disassemblyArea.getChildren().set(0, bytecodeViewer);

                binaryButton.setText("ABCD");
                
            }
            
        });
        
        javaEditor.setStyle("-fx-font-size: " + (javaEditorFontSlider.valueProperty().doubleValue() * 80 + 10) + ";");
        
        javaEditorFontSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            
            javaEditor.setStyle("-fx-font-size: " + (newValue.doubleValue() * 80 + 10) + ";");
            
        });

        bytecodeViewer.setStyle("-fx-font-size: " + (bytecodeViewerFontSlider.valueProperty().doubleValue() * 80 + 10) + ";");
        
        bytecodeViewerFontSlider.valueProperty().addListener((observable, oldValue, newValue) -> {

            bytecodeViewer.setStyle("-fx-font-size: " + (newValue.doubleValue() * 80 + 10) + ";");

        });

        compileButtonTooltip.setShowDelay(Duration.millis(275));
        
        compileButtonTooltip.setHideDelay(Duration.millis(20));
        
        javaEditor.setTextFormatter(new TextFormatter<>(new CodeEditor()));
        
        javaEditor.textProperty().addListener(observable -> inputDirty = true);
        
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            
            if (keyEvent.isControlDown()
                    && (keyEvent.getCode() == KeyCode.A)
                    && (compilationStatusProperty.get() == CompilationStatus.READY)) {
                
                keyArmed = true;
                
                compileButton.arm();
                
                keyEvent.consume();
                
            }
            
        });
        
        verboseCheckBox.selectedProperty().addListener(observable -> inputDirty = true);
        
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_ANY), () -> {
            
            verboseCheckBox.setSelected(!verboseCheckBox.isSelected());
            
        });

        verboseTooltip.setShowDelay(Duration.millis(275));

        verboseTooltip.setHideDelay(Duration.millis(20));
        
        compiler = Executors.newSingleThreadExecutor();
        
        final Pattern PUBLIC_CLASS_PATTERN = Pattern.compile("public[ ]+((final)?)[ ]+class[ ]+([^{]+)");
        
        final Runnable startCompilation = () -> {

            if (inputDirty
                    && (compilationStatusProperty.get() == CompilationStatus.READY)) {
                
                compilationStatusProperty.set(CompilationStatus.RUNNING);
                
                final boolean verbose = verboseCheckBox.isSelected();
                
                compiler.submit(() -> {
                    
                    String compilationResult = "";
                    
                    String binaryResult = "";
                    
                    try {

                        final String javaCode = javaEditor.getText();
                        
                        final Matcher matcher = PUBLIC_CLASS_PATTERN.matcher(javaCode);
                        
                        final String publicClassName;
                        
                        if (matcher.find()) {

                            publicClassName = matcher.group(3).trim();
                            
                        } else {
                            
                            throw new RuntimeException("could not determine name of main class");
                            
                        }

                        final DisassemblyResult disassemblyResult = compile(verbose, publicClassName, javaCode);
                        
                        if (disassemblyResult.errorMessage != null) {
                            
                            compilationResult = disassemblyResult.errorMessage;
                            
                        } else {

                            compilationResult = disassemblyResult.disassembly;
                            
                            binaryResult = disassemblyResult.binary;
                            
                            inputDirty = false;
                            
                        }
                        
                    } finally {
                        
                        final String bytecode = compilationResult;
                        
                        final String binary = binaryResult;
                        
                        Platform.runLater(() -> {

                            bytecodeViewer.setText(bytecode);
                            
                            binaryViewer.setText(binary);

                            compilationStatusProperty.set(CompilationStatus.READY);

                        });
                        
                    }
                    
                });
                
            }
            
        };
        
        compileButton.setOnAction(actionEvent -> startCompilation.run());
        
        scene.addEventFilter(KeyEvent.KEY_RELEASED, keyEvent -> {

            if (keyArmed
                    && (keyEvent.getCode() == KeyCode.A)) {

                startCompilation.run();
                
                compileButton.disarm();
                
                keyArmed = false;

                keyEvent.consume();

            }

        });
        
        final var progressRotation = new AnimationTimer() {
            
            @Override
            public void handle(final long now) {
                
                compileProgress.setRotate(compileProgress.getRotate() + 1.2);
                
            }
            
        };

        compileProgress.visibleProperty().bind(compilationStatusProperty.isEqualTo(CompilationStatus.RUNNING));
        
        compilationStatusProperty.addListener((observable, oldCompilationStatus, newCompilationStatus) -> {
            
            if (newCompilationStatus == CompilationStatus.RUNNING) {

                progressRotation.start();
                
            } else if (newCompilationStatus == CompilationStatus.READY) {
                
                progressRotation.stop();
                
            }
            
        });
        
    }
    
    private static final Path TEMPORARY_FOLDER = Path.of("res/temp/");
    
    private record DisassemblyResult(String errorMessage, String disassembly, String binary) { }
    
    private static final DisassemblyResult INTERRUPTED = new DisassemblyResult("interrupted", "", "");
    
    private static DisassemblyResult compile(final boolean verbose, final String filename, final String javaCode) {
        
        final var errorFile = TEMPORARY_FOLDER.resolve("error.txt").toFile();
        
        final String javaFile = filename + ".java";
        
        final String classFile = filename + ".class";

        final String asmFile = "res/temp/" + filename + ".asm";
        
        try {
            
            Files.writeString(TEMPORARY_FOLDER.resolve(javaFile), javaCode, StandardOpenOption.WRITE, StandardOpenOption.CREATE);

            if (Thread.interrupted()) {

                return INTERRUPTED;

            }
            
            final int compilationResult = new ProcessBuilder()
                    .directory(TEMPORARY_FOLDER.toFile())
                    .command(
                            "javac",
                            javaFile
                    )
                    .redirectError(errorFile)
                    .start()
                    .waitFor();
            
            if (compilationResult != 0) {
                
                return new DisassemblyResult(Files.readString(errorFile.toPath()), "", "");
                
            }
            
            if (Thread.interrupted()) {
                
                return INTERRUPTED;
                
            }

            final byte[] binary = Files.readAllBytes(TEMPORARY_FOLDER.resolve(classFile));
            
            if (Thread.interrupted()) {

                return INTERRUPTED;

            }
            
            final class ByteIterator {
                
                private final byte[] bytes;
                
                private int index = 0;
                
                public ByteIterator(final byte[] bytes) {
                    
                    this.bytes = bytes;
                    
                }

                int remaining() {

                    return (bytes.length - index);
                }
                
                private String nextHex() {
                    
                    return Integer.toHexString(Byte.toUnsignedInt(bytes[index++]));
                }
                
            }
            
            final var byteIterator = new ByteIterator(binary);
            
            final var binaryString = new StringBuilder();
            
            while (byteIterator.remaining() != 0) {
                
                final int length = Math.min(byteIterator.remaining(), 15);
                
                for (int i = 0; i < length; i++) {
                    
                    final String hex = byteIterator.nextHex().toUpperCase();
                    
                    binaryString.append("0x");
                    
                    if (hex.length() == 1) {
                        
                        binaryString.append("0");
                        
                    }
                    
                    binaryString
                            .append(hex)
                            .append(" ");
                    
                }
                
                binaryString.append("\n");
                
            }

            if (Thread.interrupted()) {

                return INTERRUPTED;

            }
            
            final List<String> disassembleCommand = new ArrayList<>(List.of("javap", "-c"));
            
            if (verbose) {
                
                disassembleCommand.add("-v");
                
            }
            
            disassembleCommand.add(classFile);
            
            final int disassemblyResult = new ProcessBuilder()
                    .directory(TEMPORARY_FOLDER.toFile())
                    .command(disassembleCommand)
                    .redirectError(errorFile)
                    .redirectOutput(new File(asmFile))
                    .start()
                    .waitFor();

            if (Thread.interrupted()) {

                return INTERRUPTED;

            }
            
            if (disassemblyResult != 0) {

                return new DisassemblyResult(Files.readString(errorFile.toPath()), "", "");

            }
            
            return new DisassemblyResult(null, Files.readString(Path.of(asmFile)), binaryString.toString());
            
        } catch (final IOException e) {
            
            throw new RuntimeException(e);
            
        } catch (final InterruptedException e) {

            return INTERRUPTED;
            
        } finally {
            
            errorFile.delete();
            TEMPORARY_FOLDER.resolve(javaFile).toFile().delete();
            TEMPORARY_FOLDER.resolve(classFile).toFile().delete();
            new File(asmFile).delete();
            
        }

    }
    
    @Override
    public void stop()
            throws IOException {
        
        Files.writeString(Path.of("res/save/save.txt"), javaEditor.getText(), StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        
        compiler.shutdownNow();
        
    }
    
}
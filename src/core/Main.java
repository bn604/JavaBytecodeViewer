package core;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class Main
        extends Application {

    public static void main(final String[] args) {
        
        launch(args);
        
    }
    
    @Override
    public void start(final Stage primaryStage)
            throws IOException {
        
        final var fxmlLoader = new FXMLLoader(Path.of("res/fxml/java-viewer.fxml").toUri().toURL());
        
        fxmlLoader.setController(this);
        
        scene = new Scene(new Group());
        
        scene.setRoot(fxmlLoader.load());
        
        primaryStage.setScene(scene);
        
        primaryStage.centerOnScreen();
        
        primaryStage.show();
        
    }
    
    private Scene scene;
    
    @FXML
    private Button compileButton;
    
    @FXML
    private TextArea javaEditor;
    
    @FXML
    private TextField filenameTextField;
    
    @FXML
    private TextArea bytecodeViewer;
    
    private boolean keyArmed = false;
    
    @FXML
    private ProgressIndicator compileProgress;
    
    private enum CompilationStatus {
        
        READY,
        
        RUNNING
        
    }
    
    private final ObjectProperty<CompilationStatus> compilationStatusProperty = new SimpleObjectProperty<>(CompilationStatus.READY);
    
    private boolean textDirty = true;
    
    @FXML
    private void initialize() {
        
        javaEditor.setText("""
                public final class HelloWorld {
                    
                    public static void main(final String[] args) {
                        
                        System.out.println("Hello, world!");
                        
                    }
                    
                }""");
        
        javaEditor.textProperty().addListener(observable -> textDirty = true);
        
        filenameTextField.setText("HelloWorld");
        
        compileProgress.visibleProperty().bind(compilationStatusProperty.isEqualTo(CompilationStatus.RUNNING));
        
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            
            if (keyEvent.isControlDown()
                    && (keyEvent.getCode() == KeyCode.A)
                    && (compilationStatusProperty.get() == CompilationStatus.READY)) {
                
                keyArmed = true;
                
                compileButton.arm();
                
                keyEvent.consume();
                
            }
            
        });

        final ExecutorService compiler = Executors.newSingleThreadExecutor(runnable -> {

            final var thread = new Thread(runnable);

            thread.setDaemon(true);

            return thread;
        });
        
        final Runnable startCompilation = () -> {

            if (textDirty
                    && (compilationStatusProperty.get() == CompilationStatus.READY)) {
                
                final String filename = filenameTextField.getText();

                if (filename.isEmpty()
                        || filename.isBlank()) {

                    return;

                }
                
                compilationStatusProperty.set(CompilationStatus.RUNNING);
                
                compiler.submit(() -> {
                    
                    String compilationResult = "";
                    
                    try {

                        compilationResult = compile(filename, javaEditor.getText());
                        
                        textDirty = false;
                        
                    } finally {
                        
                        final String bytecode = compilationResult;
                        
                        Platform.runLater(() -> {

                            bytecodeViewer.setText(bytecode);

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
                
                compileProgress.setRotate(compileProgress.getRotate() + 1);
                
            }
            
        };
        
        compilationStatusProperty.addListener((observable, oldCompilationStatus, newCompilationStatus) -> {
            
            if (newCompilationStatus == CompilationStatus.RUNNING) {

                progressRotation.start();
                
            } else if (newCompilationStatus == CompilationStatus.READY) {
                
                progressRotation.stop();
                
            }
            
        });
        
    }
    
    private static String compile(final String filename, final String javaCode) {
        
        try {
            
            final String javaFile = filename + ".java";
            
            Files.writeString(Path.of("res/temp/" + javaFile), javaCode, StandardOpenOption.WRITE, StandardOpenOption.CREATE);

            new ProcessBuilder()
                    .directory(new File("res/temp/"))
                    .command(
                            "javac",
                            javaFile
                    )
                    .start()
                    .waitFor();
            
            final String classFile = filename + ".class";
            
            final String asmFile = "res/temp/" + filename + ".asm";
            
            new ProcessBuilder()
                    .directory(new File("res/temp/"))
                    .command(
                            "javap",
                            "-v",
                            classFile
                    )
                    .redirectOutput(new File(asmFile))
                    .start()
                    .waitFor();
            
            final String assembly = Files.readString(Path.of(asmFile));
            
            final Consumer<String> deleteFile = deleteFilename -> {
                
                if (!new File(deleteFilename).delete()) {
                    
                    throw new RuntimeException("failed to delete file " + deleteFilename);
                    
                }
                
            };
            
            deleteFile.accept("res/temp/" + javaFile);
            deleteFile.accept("res/temp/" + classFile);
            deleteFile.accept(asmFile);
            
            return assembly;
            
        } catch (final IOException | InterruptedException e) {
            
            throw new RuntimeException(e);
            
        }

    }
    
}
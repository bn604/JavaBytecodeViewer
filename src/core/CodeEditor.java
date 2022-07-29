package core;

import javafx.scene.control.TextFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

final class CodeEditor
        implements UnaryOperator<TextFormatter.Change> {

    public CodeEditor() {
        
        //
        
    }
    
    private static final class Line {
        
        private int startTextIndex;
        
        private final StringBuilder lineString = new StringBuilder();

        public Line(final int startTextIndex) {
            
            this.startTextIndex = startTextIndex;
            
        }
        
    }
    
    private final List<Integer> lines = new ArrayList<>();
    
    @Override
    public TextFormatter.Change apply(final TextFormatter.Change change) {

        if (change.isContentChange()) {
            
            final String newText = change.getText();
            
//            int lastTextIndex = lines.get(lines.size() + 1);
            
            for (int i = 0; i < newText.length(); i++) {
                
                if (newText.charAt(i) == '\n') {
                    
                    //
                    
                }
                
            }
            
        }
        
        return change;
    }
    
}
package ctbrec.ui.controls;

import java.io.File;

import javafx.stage.DirectoryChooser;

public class DirectorySelectionBox extends AbstractFileSelectionBox {
    public DirectorySelectionBox(String dir) {
        super(dir);
    }

    @Override
    protected void choose() {
        DirectoryChooser chooser = new DirectoryChooser();
        File currentDir = new File(fileProperty().get());
        if (currentDir.exists() && currentDir.isDirectory()) {
            chooser.setInitialDirectory(currentDir);
        }
        File selectedDir = chooser.showDialog(null);
        if(selectedDir != null) {
            fileInput.setText(selectedDir.getAbsolutePath());
            setFile(selectedDir);
        }
    }

    @Override
    protected String validate(File file) {
        String msg = super.validate(file);
        if(msg != null) {
            return msg;
        } else if (!file.isDirectory()) {
            return "This is not a directory";
        } else {
            return null;
        }
    }
}

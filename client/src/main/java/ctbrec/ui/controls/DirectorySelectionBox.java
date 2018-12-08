package ctbrec.ui.controls;

import java.io.File;

public class DirectorySelectionBox extends AbstractFileSelectionBox {
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

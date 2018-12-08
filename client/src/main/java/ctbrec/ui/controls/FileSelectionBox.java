package ctbrec.ui.controls;

import java.io.File;

public class FileSelectionBox extends AbstractFileSelectionBox {
    @Override
    protected String validate(File file) {
        String msg = super.validate(file);
        if(msg != null) {
            return msg;
        } else if (!file.isFile()) {
            return "This is not a regular file";
        } else {
            return null;
        }
    }
}

package ctbrec.ui.controls;

import java.io.File;

public class ProgramSelectionBox extends FileSelectionBox {
    public ProgramSelectionBox() {
    }

    public ProgramSelectionBox(String initialValue) {
        super(initialValue);
    }

    @Override
    protected String validate(File file) {
        String msg = super.validate(file);
        if(msg != null) {
            return msg;
        } else if (!file.canExecute()) {
            return "This is not an executable application";
        } else {
            return null;
        }
    }
}

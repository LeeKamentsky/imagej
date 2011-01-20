/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package imagej.envisaje.paintui.actions;
import java.io.IOException;
import imagej.envisaje.api.actions.GenericContextSensitiveAction;
import imagej.envisaje.api.editor.IO;
import org.openide.ErrorManager;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

public final class SaveCanvasAction extends GenericContextSensitiveAction <IO> {

    public SaveCanvasAction() {
	super ("ACT_Save", IO.class);
	setIcon(
              Utilities.loadImage (
	      "imagej/envisaje/paintui/resources/save24.png")); //NOI18N
    }

    public SaveCanvasAction(Lookup lookup) {
        super (lookup);
    }

    public void performAction (IO io) {
	try {
	    io.save();
	} catch (IOException ioe) {
	    ErrorManager.getDefault().notify (ioe);
	}
    }
}

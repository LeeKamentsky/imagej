package implementation.swing;

import ij.IJ;
import ij.Menus;
import ij.Prefs;
import ij.Undo;
import ij.WindowManager;
import ij.gui.IjxToolbar;
import ijx.app.IjxApplication;
import ij.gui.ProgressBar;
import ijx.CentralLookup;
import ijx.IjxTopComponent;
import ijx.event.EventBus;
import ijx.gui.IjxProgressBar;
import ijx.event.StatusMessage;
import ijx.util.GraphicsUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuBar;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.ImageProducer;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;

/**
 *
 * @author GBH
 */
public class TopComponentSwing //extends Frame
        implements IjxTopComponent {
    private ij.gui.IjxToolbar toolbar;
    private StatusLineSwing statusLine;
    private JPanel statusBar;
    private IjxProgressBar progressBar;
    private boolean windowClosed;
    private IjxApplication ijApp;
    private JFrame frame;
    private final CentralLookup centralLookup = CentralLookup.getDefault();
    Dimension prefSize = new Dimension(IjxToolbar.SIZE * IjxToolbar.NUM_BUTTONS, IjxToolbar.SIZE);

    public TopComponentSwing(IjxApplication ijApp, String title) {
        frame = new JFrame(title);
        //super("ImageJ");
        this.ijApp = ijApp;
        frame.setLayout(new GridLayout(2, 1));

        frame.addWindowListener(this);
        frame.setFocusTraversalKeysEnabled(false);
    }

    public void setToolbar(Object toolbar) {
        ((JToolBar) toolbar).addKeyListener(ijApp);
        frame.add((JToolBar) toolbar);
    }

    public void addStatusBar() {
        // Status bar
        statusBar = new JPanel();
        statusBar.setLayout(new BorderLayout());
        statusBar.setForeground(Color.black);
        statusBar.setBackground(IJ.backgroundColor);
        statusLine = new StatusLineSwing();
        statusLine.setFont(SansSerif12);
        statusLine.addMouseListener(this);
        statusBar.add("Center", statusLine);
        progressBar = IJ.getFactory().newProgressBar(120, 20);
        ((JProgressBar) progressBar).addKeyListener(ijApp);
        ((JProgressBar) progressBar).addMouseListener(this);
        statusBar.add("East", (JProgressBar) progressBar);

        statusBar.setSize(prefSize);
        //statusBar.setSize(((JToolBar) toolbar).getPreferredSize());
        frame.add(statusBar);
    }

    public void finishAndShow() {
        Point loc = getPreferredLocation();
        Dimension tbSize = prefSize;
        int ijWidth = tbSize.width + 10;
        int ijHeight = 100;
        getFrame().setCursor(Cursor.getDefaultCursor()); // work-around for JDK 1.1.8 bug
        if (IJ.isWindows()) {
            try {
                setIcon();
            } catch (Exception e) {
            }
        }
        getFrame().setBounds(loc.x, loc.y, ijWidth, ijHeight); // needed for pack to work
        getFrame().setLocation(loc.x, loc.y);
        getFrame().pack();
        //getFrame().setResizable(!(IJ.isMacintosh() || IJ.isWindows())); // make resizable on Linux
        //if (IJ.isJava15()) {
        //	try {
        //		Method method = Frame.class.getMethod("setAlwaysOnTop", new Class[] {boolean.class});
        //		method.invoke(this, new Object[]{Boolean.TRUE});
        //	} catch(Exception e) {}
        //}
        getFrame().show();
    }

    @Override
    public void setMenuBar(Object menuBar) {
        ((JFrame) getFrame()).setJMenuBar((JMenuBar) menuBar);
    }

    public void setIcon() throws Exception {
        URL url = this.getClass().getResource("/microscope.gif");
        if (url == null) {
            return;
        }
        Image img = getFrame().createImage((ImageProducer) url.getContent());
        if (img != null) {
            getFrame().setIconImage(img);
        }
    }

    public Point getPreferredLocation() {
        if (!IJ.isJava14()) {
            return new Point(0, 0);
        }
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle maxBounds = ge.getMaximumWindowBounds();
        int ijX = Prefs.getInt(IJ_X, -99);
        int ijY = Prefs.getInt(IJ_Y, -99);
        if (ijX >= 0 && ijY > 0 && ijX < (maxBounds.x + maxBounds.width - 75)) {
            return new Point(ijX, ijY);
        }
        Dimension tbsize = prefSize;
        int ijWidth = tbsize.width + 10;
        double percent = maxBounds.width > 832 ? 0.8 : 0.9;
        ijX = (int) (percent * (maxBounds.width - ijWidth));
        if (ijX < 10) {
            ijX = 10;
        }
        return new Point(ijX, maxBounds.y);
    }

    public void showStatus(String s) {
        //statusLine.setText(s);
        EventBus.getDefault().publish(new StatusMessage(s));
    }

    public IjxProgressBar getProgressBar() {
        return progressBar;
    }

    public Component getStatusBar() {
        return statusBar;
    }

    public void mousePressed(MouseEvent e) {
        Undo.reset();
        IJ.showStatus("Memory: " + IJ.freeMemory());
        if (IJ.debugMode) {
            IJ.log("Windows: " + WindowManager.getWindowCount());
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        ijApp.doCommand("Quit");
        windowClosed = true;
    }

    public void windowActivated(WindowEvent e) {
        if (IJ.isMacintosh() && !ijApp.quitting()) {
            IJ.wait(10); // may be needed for Java 1.4 on OS X
            //setMenuBar(Menus.getMenuBar());
        }
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    @Override
    public boolean canClose() {
        return this.canClose();
    }

    @Override
    public boolean close() {
        return this.close();
    }

    @Override
    public boolean isClosed() {
        return windowClosed;
    }

    public void setDropTarget(Object object) {
        getFrame().setDropTarget((DropTarget) object);
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void setBackground(Color color) {
        getFrame().setBackground(color);
    }

    public String getTitle() {
        return getFrame().getTitle();
    }

    public void setTitle(String s) {
        getFrame().setTitle(s);
    }

    public ImageIcon getImageIcon() {
        return new ImageIcon(getFrame().getIconImage());
    }

    public boolean isVisible() {
        return getFrame().isVisible();
    }

    public void setVisible(boolean b) {
        getFrame().setVisible(b);
    }

    public Dimension getSize() {
        return getFrame().getSize();
    }

    public Point getLocation() {
        return getFrame().getLocation();
    }

    public Point getLocationOnScreen() {
        return getFrame().getLocationOnScreen();
    }

    public Rectangle getBounds() {
        return getFrame().getBounds();
    }

    public void setLocation(int x, int y) {
        getFrame().setLocation(x, y);
    }

    public void setLocation(Point p) {
        getFrame().setLocation(p);
    }

    public void toFront() {
        getFrame().toFront();
    }

    public void dispose() {
        getFrame().dispose();
    }

    /**
     * @return the frame
     */
    public JFrame getFrame() {
        return frame;
    }
}

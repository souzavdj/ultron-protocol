package jason.runtime;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * A quick implementation of a TextPane with default coloring for Jason.
 *
 * @author Felipe Meneguzzi
 */
public class MASConsoleColorGUI extends MASConsoleGUI {

    private Map<String, MASColorTextPane> agsTextArea = new HashMap<String, MASColorTextPane>();

    private Hashtable<String, Color> agsColours = new Hashtable<String, Color>();

    private MASColorTextPane output;

    private MASConsoleColorGUI(String title) {
        super(title);
    }

    /** for singleton pattern */
    public static MASConsoleGUI get() {
        if (masConsole == null) {
            masConsole = new MASConsoleColorGUI("MAS Console");
        }
        return masConsole;
    }

    @Override
    public void cleanConsole() {
        output.setText("");
    }

    @Override
    protected void initOutput() {
        output = new MASColorTextPane(Color.black);
        output.setEditable(false);
        ((DefaultCaret) output.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        if (isTabbed()) {
            tabPane.add(" all", new JScrollPane(output));
        } else {
            pcenter.add(BorderLayout.CENTER, new JScrollPane(output));
        }
    }

    @Override
    public void append(String agName, String s) {
        try {
            Color c = null;
            if (agName != null) {
                c = agsColours.get(agName);
                if (c == null) {
                    c = MASColorTextPane.getNextAvailableColor();
                    agsColours.put(agName, c);
                }
            }

            if (!frame.isVisible()) {
                frame.setVisible(true);
            }
            if (inPause) {
                waitNotPause();
            }
            if (isTabbed() && agName != null) {
                MASColorTextPane ta = agsTextArea.get(agName);
                if (ta == null) {
                    synchronized (this) {
                        ta = new MASColorTextPane(Color.black);
                        ta.setEditable(false);
                        agsTextArea.put(agName, ta);
                        int i = 0;
                        for (; i < tabPane.getTabCount(); i++) {
                            if (agName.toUpperCase().compareTo(tabPane.getTitleAt(i).toUpperCase()) < 0) {
                                break;
                            }
                        }
                        tabPane.add(new JScrollPane(ta), i);
                        tabPane.setTitleAt(i, agName);
                    }
                }
                if (ta != null) { // no new TA was created
                    // print out
                    int l = ta.getDocument().getLength();
                    if (l > 100000) {
                        ta.setText("");
                    }
                    ta.append(s);
                }
            }

            // print in output
            int l = output.getDocument().getLength();
            if (l > 60000) {
                cleanConsole();
                l = 0;
            }
            synchronized (this) {
                output.append(c, s);
                output.setCaretPosition(l);
            }
        } catch (Exception e) {
            close();
            System.out.println(e);
            e.printStackTrace();
        }
    }

}

class MASColorTextPane extends JTextPane {

    protected static final Color seq[] = {//Color.black,
            Color.blue, Color.red, Color.gray,
            //Color.cyan,
            Color.magenta,
            //Color.orange,
            //Color.pink,
            //Color.yellow,
            Color.green};

    protected static int change = 0;

    protected static int lastColor = 0;

    public synchronized static Color getNextAvailableColor() {
        if (change > 0) {
            seq[lastColor] = (change % 2 == 1) ? seq[lastColor].brighter() : seq[lastColor].darker();
        }
        Color c = seq[lastColor];
        lastColor = (lastColor + 1) % seq.length;
        if (lastColor == 0) {
            change++;
        }
        return c;
    }

    protected Color defaultColor;

    public MASColorTextPane(Color defaultColor) {
        this.defaultColor = defaultColor;
    }

    public void append(String s) {
        append(defaultColor, s);
    }

    public void append(Color c, String s) {
        if (c == null) {
            c = defaultColor;
        }
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet as = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
        try {
            getDocument().insertString(getDocument().getLength(), s, as);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}


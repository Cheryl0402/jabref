/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.external.push;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.*;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Mar 7, 2007
 * Time: 6:55:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class PushToVim extends AbstractPushToApplication implements PushToApplication {

    private final JTextField vimServer = new JTextField(30);
    
    private boolean couldNotConnect;
    
    @Override
    public String getApplicationName() {
        return "Vim";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("vim");
    }

    @Override
    public JPanel getSettingsPanel() {
        if (settings == null) {
            initSettingsPanel();
        }
        Path.setText(Globals.prefs.get(JabRefPreferences.VIM));
        vimServer.setText(Globals.prefs.get(JabRefPreferences.VIM_SERVER));
        return settings;
    }

    @Override
    public void storeSettings() {
        Globals.prefs.put(JabRefPreferences.VIM, Path.getText());
        Globals.prefs.put(JabRefPreferences.VIM_SERVER, vimServer.getText());
    }

    protected void initSettingsPanel() {
        FormBuilder builder = FormBuilder.create();
        builder.layout(new FormLayout("left:pref, 4dlu, fill:pref:grow, 4dlu, fill:pref", "p, 2dlu, p"));
        builder.add(Localization.lang("Path to %0", getApplicationName()) + ":").xy(1, 1);
        builder.add(Path).xy(3,1);
        BrowseAction action = BrowseAction.buildForFile(Path);
        JButton browse = new JButton(Localization.lang("Browse"));
        browse.addActionListener(action);
        builder.add(browse).xy(5,1);
        builder.add(Localization.lang("Vim Server Name") + ":").xy(1, 3);
        builder.add(vimServer).xy(3,3);
        settings = builder.build();
    }

    @Override
    public void pushEntries(BibtexDatabase database, BibtexEntry[] entries, String keys,
            MetaData metaData) {

        couldNotConnect = false;
        couldNotCall = false;
        try {
            // @formatter:off
            String[] com = new String[] {Globals.prefs.get(JabRefPreferences.VIM), "--servername", 
                    Globals.prefs.get(JabRefPreferences.VIM_SERVER), "--remote-send",
                    "<C-\\><C-N>a" + citeCommand +
                            "{" + keys + "}"};
            // @formatter:on


            final Process p = Runtime.getRuntime().exec(com);

            Runnable errorListener = new Runnable() {

                @Override
                public void run() {
                    InputStream out = p.getErrorStream();
                    int c;
                    StringBuilder sb = new StringBuilder();
                    try {
                        while ((c = out.read()) != -1) {
                            sb.append((char) c);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Error stream has been closed. See if there were any errors:
                    if (!sb.toString().trim().isEmpty()) {
                        System.out.println(sb);
                        couldNotConnect = true;
                    }
                }
            };
            JabRefExecutorService.INSTANCE.executeAndWait(errorListener);
        } catch (IOException excep) {
            couldNotCall = true;
        }

    }

    @Override
    public void operationCompleted(BasePanel panel) {
        if (couldNotConnect) {
            JOptionPane.showMessageDialog(
                    panel.frame(),
                    "<HTML>" +
                            Localization.lang("Could not connect to Vim server. Make sure that "
                                    + "Vim is running<BR>with correct server name.")
                            + "</HTML>",
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else if (couldNotCall) {
            JOptionPane.showMessageDialog(
                    panel.frame(),
                    Localization.lang("Could not run the 'vim' program."),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else {
            panel.output(Localization.lang("Pushed citations to %0",getApplicationName()));
        }
    }
}

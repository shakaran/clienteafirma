/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation; 
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.standalone.ui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyListener;
import java.net.URI;
import java.util.logging.Logger;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.batik.swing.JSVGCanvas;

import es.gob.afirma.signature.SignValidity;
import es.gob.afirma.standalone.LookAndFeelManager;
import es.gob.afirma.standalone.Messages;

final class SignResultPanel extends JPanel {

    private static final long serialVersionUID = -7982793036430571363L;

    private final JEditorPane descTextLabel = new JEditorPane();
    private final JLabel resultTextLabel = new JLabel();

    SignResultPanel(final SignValidity validity, final KeyListener extKeyListener) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createUI(validity, extKeyListener);
            }
        });
    }

    void createUI(final SignValidity validity, final KeyListener extKeyListener) {

        // Para que se detecten apropiadamente los hipervinculos hay que establecer
        // el tipo de contenido antes que el contenido
        this.descTextLabel.setContentType("text/html"); //$NON-NLS-1$
        
        final JSVGCanvas resultOperationIcon = new JSVGCanvas();
        resultOperationIcon.setFocusable(false);
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            String iconFilename;
            switch (validity.getValidity()) {
            case KO:
                iconFilename = "ko_icon.svg"; //$NON-NLS-1$
                break;
            case OK:
                iconFilename = "ok_icon.svg"; //$NON-NLS-1$
                break;
            case GENERATED:
                iconFilename = "ok_icon.svg"; //$NON-NLS-1$
                break;
            default:
                iconFilename = "unknown_icon.svg"; //$NON-NLS-1$
            }
            resultOperationIcon.setDocument(dbf.newDocumentBuilder()
               .parse(this.getClass()
                          .getResourceAsStream("/resources/" + iconFilename))); //$NON-NLS-1$
        }
        catch (final Exception e) {
            Logger.getLogger("es.gob.afirma").warning("No se ha podido cargar el icono de resultado o validez de firma, este no se mostrara: " + e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        String errorMessage;
        final String resultOperationIconTooltip;
        switch (validity.getValidity()) {
            case GENERATED:
                this.resultTextLabel.setText(Messages.getString("SignResultPanel.2")); //$NON-NLS-1$
                this.descTextLabel.setText(Messages.getString("SignResultPanel.3")); //$NON-NLS-1$
                resultOperationIconTooltip = Messages.getString("SignResultPanel.4"); //$NON-NLS-1$
                break;
            case OK:
                this.resultTextLabel.setText(Messages.getString("SignResultPanel.8")); //$NON-NLS-1$
                this.descTextLabel.setText(Messages.getString("SignResultPanel.9")); //$NON-NLS-1$
                resultOperationIconTooltip = Messages.getString("SignResultPanel.10"); //$NON-NLS-1$
                break;
            case KO:
                this.resultTextLabel.setText(Messages.getString("SignResultPanel.5")); //$NON-NLS-1$
                if (validity.getError() != null) {
                    switch (validity.getError()) {
                    case CORRUPTED_SIGN: errorMessage = Messages.getString("SignResultPanel.14"); break; //$NON-NLS-1$
                    case CERTIFICATE_EXPIRED: errorMessage = Messages.getString("SignResultPanel.16"); break; //$NON-NLS-1$
                    case CERTIFICATE_NOT_VALID_YET: errorMessage = Messages.getString("SignResultPanel.17"); break; //$NON-NLS-1$
                    case CERTIFICATE_PROBLEM: errorMessage = Messages.getString("SignResultPanel.18"); break; //$NON-NLS-1$
                    case NO_MATCH_DATA: errorMessage = Messages.getString("SignResultPanel.19"); break; //$NON-NLS-1$
                    case CRL_PROBLEM: errorMessage = Messages.getString("SignResultPanel.20"); break; //$NON-NLS-1$
                    case ALGORITHM_NOT_SUPPORTED: errorMessage = Messages.getString("SignResultPanel.22"); break; //$NON-NLS-1$
                    default:
                        errorMessage = Messages.getString("SignResultPanel.6"); //$NON-NLS-1$
                    }
                } 
                else {
                    errorMessage = Messages.getString("SignResultPanel.6"); //$NON-NLS-1$
                }
                this.descTextLabel.setText("<html><p>" + errorMessage + "</p></html>"); //$NON-NLS-1$ //$NON-NLS-2$
                resultOperationIconTooltip = Messages.getString("SignResultPanel.6"); //$NON-NLS-1$
                break;
            default:
                this.resultTextLabel.setText(Messages.getString("SignResultPanel.11")); //$NON-NLS-1$
                if (validity.getError() != null) {
                    switch (validity.getError()) {
                    case NO_DATA: errorMessage = Messages.getString("SignResultPanel.15"); break; //$NON-NLS-1$
                    default:
                        errorMessage = Messages.getString("SignResultPanel.12"); //$NON-NLS-1$
                    }
                } 
                else {
                    errorMessage = Messages.getString("SignResultPanel.12"); //$NON-NLS-1$
                }
                this.descTextLabel.setText("<html><p>" + errorMessage + "</p></html>"); //$NON-NLS-1$ //$NON-NLS-2$
                resultOperationIconTooltip = Messages.getString("SignResultPanel.13"); //$NON-NLS-1$
                break;
        }
        resultOperationIcon.setPreferredSize(new Dimension(120, 120));
        resultOperationIcon.setToolTipText(resultOperationIconTooltip);

        final EditorFocusManager editorFocusManager = new EditorFocusManager (this.descTextLabel, new EditorFocusManagerAction() {  
            @Override
            public void openHyperLink(final HyperlinkEvent he, int linkIndex) {
                try {
                    if (he.getURL() != null) {
                        Desktop.getDesktop().browse(he.getURL().toURI());
                    }
                    else {
                        Desktop.getDesktop().browse(new URI(Messages.getString("SignResultPanel.23." + linkIndex))); //$NON-NLS-1$
                    }
                }
                catch (final Exception e) {
                    UIUtils.showErrorMessage(
                        SignResultPanel.this,
                        Messages.getString("SignResultPanel.0") + he.getURL(), //$NON-NLS-1$
                        Messages.getString("SignResultPanel.1"), //$NON-NLS-1$
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });
        
        this.descTextLabel.addFocusListener(editorFocusManager);
        this.descTextLabel.addHyperlinkListener(editorFocusManager);
        this.descTextLabel.addKeyListener(editorFocusManager);
        if (extKeyListener != null) {
        	this.descTextLabel.addKeyListener(extKeyListener);
        }
        
        this.descTextLabel.setEditable(false);
        this.descTextLabel.setOpaque(false);
        
        this.resultTextLabel.setFont(this.getFont().deriveFont(Font.PLAIN, 26));
        this.resultTextLabel.setLabelFor(this.descTextLabel);

        // Establecemos la configuracion de color
        if (!LookAndFeelManager.HIGH_CONTRAST) {
            setBackground(LookAndFeelManager.WINDOW_COLOR);
            this.resultTextLabel.setForeground(new Color(3399));
        }
        
        this.setLayout(new GridBagLayout());
        
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.gridheight = 2;
        c.insets = new Insets(11, 11, 11, 5);
        this.add(resultOperationIcon, c);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridx = 1;
        c.gridheight = 1;
        c.insets = new Insets(11, 6, 0, 11);
        this.add(this.resultTextLabel, c);
        c.weighty = 1.0;
        c.gridy = 1;
        c.insets = new Insets(0, 6, 5, 11);
        this.add(this.descTextLabel, c);

    }

}

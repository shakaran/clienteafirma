/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.standalone;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import es.gob.afirma.signers.cades.AOCAdESSigner;
import es.gob.afirma.signers.cms.AOCMSSigner;
import es.gob.afirma.signers.pades.AOPDFSigner;
import es.gob.afirma.signers.xades.AOFacturaESigner;
import es.gob.afirma.signers.xades.AOXAdESSigner;

/**
 * M&eacute;todos de utilidad para el an&aacute;lisis de ficheros de datos.
 * @author Carlos Gamuci
 */
public final class DataAnalizerUtil {

    private DataAnalizerUtil() {
        // No permitimos la instanciacion
    }

    /**
     * Comprueba si los datos introducidos se corresponden a un fichero XML.
     * @param data Datos a analizar.
     * @return Devuelve {@code true} si los datos son XML.
     */
    public static boolean isXML(final byte[] data) {
        try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(data));
        }
        catch(final Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Comprueba si los datos introducidos se corresponden a una firma XML soportada.
     * @param data Datos a analizar.
     * @return Devuelve {@code true} si los datos son una firma XML soportada.
     */
    public static boolean isSignedXML(final byte[] data) {

        try {
            return new AOXAdESSigner().isSign(data);
        }
        catch(final Exception e) {
            return false;
        }
    }

    /**
     * Comprueba si los datos introducidos se corresponden a un fichero PDF.
     * @param data Datos a analizar.
     * @return Devuelve {@code true} si los datos son un PDF.
     */
    public static boolean isPDF(final byte[] data) {
        try {
            return new AOPDFSigner().isValidDataFile(data);
        }
        catch(final Exception e) {
            return false;
        }
    }

    /**
     * Comprueba si los datos introducidos se corresponden a un fichero PDF firmado.
     * @param data Datos a analizar.
     * @return Devuelve {@code true} si los datos son un PDF firmado.
     */
    public static boolean isSignedPDF(final byte[] data) {
        try {
            return new AOPDFSigner().isSign(data);
        }
        catch(final Exception e) {
            return false;
        }
    }

    /**
     * Comprueba si los datos introducidos se corresponden a una firma binaria soportada.
     * @param data Datos a analizar.
     * @return Devuelve {@code true} si los datos son una firma binaria soportada.
     */
    public static boolean isSignedBinary(final byte[] data) {

        try {
            return new AOCMSSigner().isSign(data) || new AOCAdESSigner().isSign(data);
        }
        catch(final Exception e) {
            return false;
        }
    }

    /** Indica si los datos son una factura electr&oacute;nica.
     * @param file Datos a comprobar
     * @return <code>true</code> si los datos son una <a href="http://www.facturae.es/">factura electr&oacute;nica</a>,
     *         <code>false</code> en caso contrario */
    public static boolean isFacturae(final byte[] file) {
        try {
            return new AOFacturaESigner().isValidDataFile(file);
        }
        catch(final Exception e) {
            return false;
        }
    }
}

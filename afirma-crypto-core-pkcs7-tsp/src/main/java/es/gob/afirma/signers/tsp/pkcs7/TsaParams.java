package es.gob.afirma.signers.tsp.pkcs7;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.signers.AOSignConstants;

/** Par&aacute;metros de configuraci&oacute;n de una Autoridad de Sellado de Tiempo.
 * @author Tomas Garc&iacute;a-Mer&aacute;s. */
public final class TsaParams {

	private static final String POLICY = "0.4.0.2023.1.1"; //$NON-NLS-1$

	private final boolean tsaRequireCert;
	private final String tsaPolicy;
	private final URI tsaURL;
	private final String tsaUsr;
	private final String tsaPwd;
	private final TsaRequestExtension[] extensions;
	private final String tsaHashAlgorithm;
	private final byte[] sslPkcs12File;
	private final String sslPkcs12FilePassword;

	/** Construye los par&aacute;metros de configuraci&oacute;n de una Autoridad de Sellado de Tiempo.
	 * En caso de ausencia o error en las propiedades de entrada lanza una <code>IllegalArgumentException</code>.
	 * @param extraParams Propiedades que contienen los par&aacute;metros de configuraci&oacute;n necesarios. */
	public TsaParams(final Properties extraParams) {
		if (extraParams == null) {
			throw new IllegalArgumentException(
				"La propiedades de configuracion de la TSA no pueden ser nulas" //$NON-NLS-1$
			);
		}
		final String tsa = extraParams.getProperty("tsaURL"); //$NON-NLS-1$
        if (tsa == null) {
        	throw new IllegalArgumentException(
				"La URL del servidor de sello de tiempo no puede ser nula" //$NON-NLS-1$
			);
        }
        try {
    		this.tsaURL = new URI(tsa);
    	}
    	catch(final Exception e) {
    		throw new IllegalArgumentException(
				"Se ha indicado una URL de TSA invalida (" + tsa + "): " + e, e //$NON-NLS-1$ //$NON-NLS-2$
			);
    	}
        this.tsaPolicy = extraParams.getProperty("tsaPolicy") != null ? //$NON-NLS-1$
    		extraParams.getProperty("tsaPolicy") : //$NON-NLS-1$
    			POLICY;
        this.tsaHashAlgorithm = extraParams.getProperty("tsaHashAlgorithm") != null ? //$NON-NLS-1$
        		AOSignConstants.getDigestAlgorithmName(extraParams.getProperty("tsaHashAlgorithm")) : //$NON-NLS-1$
        			"SHA-512"; //$NON-NLS-1$
        this.tsaRequireCert = !Boolean.FALSE.toString().equalsIgnoreCase(extraParams.getProperty("tsaRequireCert")); //$NON-NLS-1$
        this.tsaUsr = extraParams.getProperty("tsaUsr"); //$NON-NLS-1$
        this.tsaPwd = extraParams.getProperty("tsaPwd"); //$NON-NLS-1$

        // PKCS#12 / PFX para el SSL cliente
        final String p12FileName = extraParams.getProperty("tsaSslPkcs12File"); //$NON-NLS-1$
        if (p12FileName != null) {
        	final File p12File = new File(p12FileName);
        	if (!p12File.exists()) {
        		throw new IllegalArgumentException("El fichero PKCS#12 para el SSL de la TSA no existe: " + p12File); //$NON-NLS-1$
        	}
			try {
				final InputStream is = new FileInputStream(p12File);
				this.sslPkcs12File = AOUtil.getDataFromInputStream(is);
				is.close();
			}
			catch(final Exception e) {
				throw new IllegalArgumentException(
					"El fichero PKCS#12 (" + p12File + ") para el SSL de la TSA no ha podido leerse: " + e, e  //$NON-NLS-1$//$NON-NLS-2$
				);
			}
        }
        else {
        	this.sslPkcs12File = null;
        }
        this.sslPkcs12FilePassword = extraParams.getProperty("tsaSslPkcs12FilePassword", ""); //$NON-NLS-1$ //$NON-NLS-2$

        try {
	        this.extensions = extraParams.getProperty("tsaExtensionOid") != null && extraParams.getProperty("tsaExtensionValueBase64") != null ? //$NON-NLS-1$ //$NON-NLS-2$
				new TsaRequestExtension[] {
					new TsaRequestExtension(
						extraParams.getProperty("tsaExtensionOid"), //$NON-NLS-1$
						Boolean.getBoolean(extraParams.getProperty("tsaExtensionCritical", "false")), //$NON-NLS-1$ //$NON-NLS-2$
						Base64.decode(extraParams.getProperty("tsaExtensionValueBase64")) //$NON-NLS-1$
					)
				} : null;
        }
        catch(final IOException e) {
        	throw new IllegalArgumentException("Las extensiones del sello de tiempo no estan adecuadamente codificadas: " + e, e); //$NON-NLS-1$
        }
	}

	boolean doTsaRequireCert() {
		return this.tsaRequireCert;
	}

	String getTsaPolicy() {
		return this.tsaPolicy;
	}

	URI getTsaUrl() {
		return this.tsaURL;
	}

	String getTsaUsr() {
		return this.tsaUsr;
	}

	String getTsaPwd() {
		return this.tsaPwd;
	}

	TsaRequestExtension[] getExtensions() {
		return this.extensions;
	}

	/** Obtiene el algoritmo de huella digital a usar en el sellado de tiempo.
	 * @return Algoritmo de huella digital a usar en el sellado de tiempo. */
	public String getTsaHashAlgorithm() {
		return this.tsaHashAlgorithm;
	}

	/** Obtiene el fichero PKCS#12 que contiene el certificado SSL cliente que pedir&aacute; la TSA al
	 * establecer la coneci&oacute;s HTTPS.
	 * @return Fichero PKCS#12 que contiene el certificado SSL cliente para las conexiones HTTPS, o
	 *         <code>null</code> si no se ha establecido ninguno. */
	public byte[] getSslPkcs12File() {
		return this.sslPkcs12File;
	}

	/** Obtiene la contrase&ntilde;a del fichero PKCS#12 que contiene el certificado SSL cliente para las conexiones HTTPS.
	 * @return Contrase&ntilde;a del fichero PKCS#12 que contiene el certificado SSL cliente para las conexiones HTTPS o
	 *         cadena vac&iacute;a si no se ha establecido ninguna. */
	public String getSslPkcs12FilePassword() {
		return this.sslPkcs12FilePassword;
	}
}

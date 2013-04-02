package es.gob.afirma.triphase.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import es.gob.afirma.core.AOException;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.signers.AOSignConstants;

// http://localhost:8080/afirma-crypto-padestri-sample/SignatureService?doc=1&op=0&algo=SHA1withRSA&format=PAdES&cert=AAAA

/**
 * Servlet implementation class SignatureService
 */
@WebServlet("/SignatureService")
public final class SignatureService extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	//TODO: Cambiar por una referencia al gestor documental de verdad
	private static DocumentManager DOC_MANAGER;

	private static final String CONFIG_FILE = "config.properties"; //$NON-NLS-1$

	private static final String DOCUMENT_MANAGER_CLASS_PARAM = "document.manager"; //$NON-NLS-1$

	static {
		final InputStream configIs = SignatureService.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
		if (configIs == null) {
			throw new RuntimeException("No se encuentra el fichero de configuracion del servicio: " + CONFIG_FILE); //$NON-NLS-1$
		}

		final Properties prop = new Properties();
		try {
			prop.load(configIs);
			configIs.close();
		} catch (final Exception e) {
			throw new RuntimeException("Error en la carga del fichero de propiedades", e); //$NON-NLS-1$
		}

		if (!prop.containsKey(DOCUMENT_MANAGER_CLASS_PARAM)) {
			throw new IllegalArgumentException(
					"No se ha indicado el document manager (" + DOCUMENT_MANAGER_CLASS_PARAM + ") en el fichero de propiedades"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		Class docManagerClass;
		try {
			docManagerClass = Class.forName(prop.getProperty(DOCUMENT_MANAGER_CLASS_PARAM));
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException("La clase DocumentManager indicada no existe: " + prop.getProperty(DOCUMENT_MANAGER_CLASS_PARAM), e); //$NON-NLS-1$
		}

		try {
			final Constructor<DocumentManager> docManagerConstructor = docManagerClass.getConstructor(Properties.class);
			DOC_MANAGER = docManagerConstructor.newInstance(prop);
		} catch (final ReflectiveOperationException e) {
			try {
				DOC_MANAGER = (DocumentManager) docManagerClass.newInstance();
			} catch (final Exception e2) {
				throw new RuntimeException("No se ha podido inicializar el DocumentManager. Debe tener un constructor vacio o que reciba un Properties", e); //$NON-NLS-1$
			}
		}

	}

	private static final String URL_DEFAULT_CHARSET = "utf-8"; //$NON-NLS-1$


	private static final String PARAMETER_NAME_OPERATION = "op"; //$NON-NLS-1$
	private static final String PARAMETER_NAME_SUB_OPERATION = "cop"; //$NON-NLS-1$

	private static final String PARAMETER_VALUE_SUB_OPERATION_SIGN = "sign"; //$NON-NLS-1$
	private static final String PARAMETER_VALUE_SUB_OPERATION_COSIGN = "cosign"; //$NON-NLS-1$

	// Parametros que necesitamos para la prefirma
	private static final String PARAMETER_NAME_DOCID = "doc"; //$NON-NLS-1$
	private static final String PARAMETER_NAME_ALGORITHM = "algo"; //$NON-NLS-1$
	private static final String PARAMETER_NAME_FORMAT = "format"; //$NON-NLS-1$
	private static final String PARAMETER_NAME_CERT = "cert"; //$NON-NLS-1$
	private static final String PARAMETER_NAME_EXTRA_PARAM = "params"; //$NON-NLS-1$

	/** Par&aacute;metro con los datos. Si no aparece se recuperar&aacute;n a partir de {@link #PARAMETER_NAME_DOCID}. */
	private static final String PARAMETER_NAME_DATA_PARAM = "dat"; //$NON-NLS-1$

	private static final String OPERATION_PRESIGN = "pre"; //$NON-NLS-1$
	private static final String OPERATION_POSTSIGN = "post"; //$NON-NLS-1$

	/** Indicador de finalizaci&oacute;n correcta de proceso. */
	private static final String SUCCESS = "OK"; //$NON-NLS-1$

	/** Etiqueta que se envia en la respuesta para el nuevo identificador de documento (el almacenado una vez firmado). */
	private static final String NEW_DOCID_TAG = "NEWID"; //$NON-NLS-1$

	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		LOGGER.info("Se realiza una peticion de firma trifasica"); //$NON-NLS-1$

		final Map<String, String> parameters = new HashMap<String, String>();
		final String[] params = new String(AOUtil.getDataFromInputStream(request.getInputStream())).split("&"); //$NON-NLS-1$
		for (final String param : params) {
			if (param.indexOf('=') != -1) {
				parameters.put(param.substring(0, param.indexOf('=')), URLDecoder.decode(param.substring(param.indexOf('=') + 1), URL_DEFAULT_CHARSET));
			}
		}

		LOGGER.info("-------------- CLAVES:");
		for (final String key : parameters.keySet().toArray(new String[parameters.size()])) {
			LOGGER.info(key);
		}
		LOGGER.info("--------------");

		LOGGER.info("Operacion: " + parameters.get(PARAMETER_NAME_OPERATION));

		// Obtenemos el codigo de operacion
		final PrintWriter out = response.getWriter();
		final String operation = parameters.get(PARAMETER_NAME_OPERATION);
		if (operation == null) {
			out.print(ErrorManager.getErrorMessage(1));
			return;
		}

		// Obtenemos el codigo de operacion
		//final String subOperation = request.getParameter(PARAMETER_NAME_SUB_OPERATION);
		String subOperation = parameters.get(PARAMETER_NAME_SUB_OPERATION);
		if (subOperation == null) {
			//TODO: Descomentar cuando se publique la segunda version de la aplicacion Android para que sea
			// obligatorio indicar la operacion criptografica
			subOperation = "sign"; //$NON-NLS-1$
			//			out.print(ErrorManager.getErrorMessage(13));
			//			return;
		}

		// Comprobamos si nos pasan los datos en la peticion, si no es que debe haber un docId para sacarlos del gestor documental
		String docId = null;
		final byte[] docBytes;
		final String dataB64 = parameters.get(PARAMETER_NAME_DATA_PARAM);
		if (dataB64 != null) {
			docBytes = Base64.decode(dataB64, Base64.URL_SAFE);
		}
		else {
			docId = parameters.get(PARAMETER_NAME_DOCID);
			if (docId == null) {
				out.print(ErrorManager.getErrorMessage(2));
				return;
			}
			try {
				docBytes = DOC_MANAGER.getDocument(docId, null);
			} catch (final IOException e) {
				out.print(ErrorManager.getErrorMessage(14));
				return;
			}
		}

		// Obtenemos el algoritmo de firma
		final String algorithm = parameters.get(PARAMETER_NAME_ALGORITHM);
		if (algorithm == null) {
			out.print(ErrorManager.getErrorMessage(3));
			return;
		}

		// Obtenemos el formato de firma
		final String format = parameters.get(PARAMETER_NAME_FORMAT);
		LOGGER.info("Formato de firma seleccionado: " + format); //$NON-NLS-1$
		if (format == null) {
			out.print(ErrorManager.getErrorMessage(4));
			return;
		}

		// Obtenemos el certificado
		final String cert = parameters.get(PARAMETER_NAME_CERT);
		if (cert == null) {
			out.print(ErrorManager.getErrorMessage(5));
			return;
		}
		final X509Certificate signerCert;
		try {
			signerCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate( //$NON-NLS-1$
					new ByteArrayInputStream(Base64.decode(cert, Base64.URL_SAFE))
					);
		}
		catch(final Exception e) {
			LOGGER.severe("Error al decodificar el certificado: " + e);  //$NON-NLS-1$
			out.print(ErrorManager.getErrorMessage(7));
			return;
		}

		// Obtenemos los parametros adicionales para la firma
		final Properties extraParams = new Properties();
		try {
			if (parameters.containsKey(PARAMETER_NAME_EXTRA_PARAM)) {
				extraParams.load(
						new ByteArrayInputStream(
								Base64.decode(parameters.get(PARAMETER_NAME_EXTRA_PARAM).trim(), Base64.URL_SAFE)
								)
						);
				for ( final String key : extraParams.keySet().toArray(new String[extraParams.size()])) {
					LOGGER.info(key);
				}

			}
		}
		catch (final Exception e) {
			e.printStackTrace();
			out.print(ErrorManager.getErrorMessage(6));
			return;
		}

		// Instanciamos el preprocesador adecuado
		final TriPhasePreProcessor prep;
		if (AOSignConstants.SIGN_FORMAT_PADES.equalsIgnoreCase(format)) {
			prep = new PAdESTriPhasePreProcessor();
		}
		else if (AOSignConstants.SIGN_FORMAT_CADES.equalsIgnoreCase(format)) {
			prep = new CAdESTriPhasePreProcessor();
		}
		else if (AOSignConstants.SIGN_FORMAT_XADES.equalsIgnoreCase(format)) {
			prep = new XAdESTriPhasePreProcessor();
		}
		else {
			LOGGER.severe("Formato de firma no soportado: " + format); //$NON-NLS-1$
			out.print(ErrorManager.getErrorMessage(8));
			return;
		}

		if (OPERATION_PRESIGN.equals(operation)) {
			try {
				final byte[] preRes;
				if (PARAMETER_VALUE_SUB_OPERATION_COSIGN.equals(subOperation)) {
					preRes = prep.preProcessPreCoSign(docBytes, algorithm, signerCert, extraParams);
				}
				else if (PARAMETER_VALUE_SUB_OPERATION_SIGN.equals(subOperation)) {
					preRes = prep.preProcessPreSign(docBytes, algorithm, signerCert, extraParams);
				}
				else {
					throw new AOException("No se reconoce el codigo de sub-operacion: " + subOperation); //$NON-NLS-1$
				}
				out.print(
						Base64.encodeBytes(
								preRes,
								Base64.URL_SAFE
								)
						);
			}
			catch (final AOException e) {
				LOGGER.severe("Error en la prefirma: " + e); //$NON-NLS-1$
				e.printStackTrace();
				out.print(ErrorManager.getErrorMessage(9));
				return;
			}
		}
		else if (OPERATION_POSTSIGN.equals(operation)) {

			final byte[] signedDoc;
			try {
				if (PARAMETER_VALUE_SUB_OPERATION_COSIGN.equals(subOperation)) {
					signedDoc = prep.preProcessPostCoSign(docBytes, algorithm, signerCert, extraParams);
				}
				else if (PARAMETER_VALUE_SUB_OPERATION_SIGN.equals(subOperation)) {
					signedDoc = prep.preProcessPostSign(docBytes, algorithm, signerCert, extraParams);
				}
				else {
					throw new AOException("No se reconoce el codigo de sub-operacion: " + subOperation); //$NON-NLS-1$
				}
			}
			catch (final Exception e) {
				LOGGER.severe("Error en la postfirma: " + e); //$NON-NLS-1$
				out.print(ErrorManager.getErrorMessage(12));
				e.printStackTrace();
				return;
			}

			// Establecemos parametros adicionales queutilizar para guardar el documento
			final Properties config = new Properties();
			config.setProperty(PARAMETER_NAME_FORMAT, parameters.get(PARAMETER_NAME_FORMAT));

			// Devolvemos al servidor documental el documento firmado
			final String newDocId;
			try {
				newDocId = DOC_MANAGER.storeDocument(docId, signedDoc, config);
			}
			catch(final Exception e) {
				LOGGER.severe("Error en el almacen del documento: " + e); //$NON-NLS-1$
				out.print(ErrorManager.getErrorMessage(10));
				e.printStackTrace();
				return;
			}

			LOGGER.info("Firma OK"); //$NON-NLS-1$

			out.println(SUCCESS + " " + NEW_DOCID_TAG + "=" + newDocId); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			out.println(ErrorManager.getErrorMessage(11));
		}
	}
}

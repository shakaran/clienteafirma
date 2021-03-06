package es.gob.afirma.crypto.handwritten;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class AppletMessages {
	private static final String BUNDLE_NAME = "es.gob.afirma.crypto.handwritten.appletmessages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private AppletMessages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}

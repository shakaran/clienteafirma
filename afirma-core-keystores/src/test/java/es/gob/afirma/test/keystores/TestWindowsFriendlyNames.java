package es.gob.afirma.test.keystores;

import java.security.InvalidKeyException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import es.gob.afirma.core.misc.Platform;
import es.gob.afirma.keystores.main.common.AOKeyStore;
import es.gob.afirma.keystores.main.common.AOKeyStoreManager;
import es.gob.afirma.keystores.main.common.AOKeyStoreManagerFactory;
import es.gob.afirma.keystores.main.common.AOKeystoreAlternativeException;
import es.gob.afirma.keystores.main.common.KeyStoreUtilities;

/** Prueba la conversi&oacute;n de alias en nombres significativos en CAPI.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
public class TestWindowsFriendlyNames {
    
    /** Prueba la conversi&oacute;n de alias en nombres significativos en CAPI. 
     * @throws AOKeystoreAlternativeException 
     * @throws InvalidKeyException */
    @Test
    public void testWindowsFriendlyNames() throws InvalidKeyException, AOKeystoreAlternativeException {
        if (!Platform.OS.WINDOWS.equals(Platform.getOS())) {
            return;
        }
        Logger.getLogger("es.gob.afirma").setLevel(Level.WARNING); //$NON-NLS-1$
        AOKeyStoreManager ksm = AOKeyStoreManagerFactory.getAOKeyStoreManager(
               AOKeyStore.WINDOWS, 
               null, 
               "TEST",  //$NON-NLS-1$
               null, 
               null
        );
        for(final String al : ksm.getAliases()) {
            System.out.println(al);
        }
        System.out.println();
        final Map<String, String> aliases = KeyStoreUtilities.getAliasesByFriendlyName(
               ksm.getAliases(), 
               ksm.getKeyStores(), 
               true, // Check private keys
               true, // Check validity 
               true, // Show expired
               null  // filters
       );
       for (final String key : aliases.keySet()) {
           System.out.println(aliases.get(key));
       }
    }

}
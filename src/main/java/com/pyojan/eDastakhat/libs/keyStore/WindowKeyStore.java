package com.pyojan.eDastakhat.libs.keyStore;

import com.pyojan.eDastakhat.exceptions.KeyStoreInitializationException;
import com.pyojan.eDastakhat.exceptions.CertificateNotFoundException;
import com.pyojan.eDastakhat.exceptions.NotADigitalSignatureException;
import com.pyojan.eDastakhat.exceptions.PrivateKeyAccessException;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Enumeration;

public class WindowKeyStore extends CertificateValidator {

    private final KeyStore keyStore;

    @Getter private final String provider = "SunMSCAPI";
    @Getter private final BouncyCastleProvider cryptoProvider = new BouncyCastleProvider();

    @Setter private String serialHex;

    public WindowKeyStore() throws KeyStoreInitializationException {
        try {
            Security.addProvider(cryptoProvider);
            this.keyStore = KeyStore.getInstance("Windows-MY", provider);
            this.keyStore.load(null, null);
        } catch (KeyStoreException | NoSuchProviderException e) {
            throw new KeyStoreInitializationException("Failed to initialize KeyStore: " + e.getMessage(), e);
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new KeyStoreInitializationException("Failed to load KeyStore: " + e.getMessage(), e);
        }
    }

    public X509Certificate getCertificate() throws KeyStoreInitializationException, CertificateNotFoundException {
        try {
            String alias = findAliasByCertSerial(serialHex);
            Certificate cert = keyStore.getCertificate(alias);
            if (cert instanceof X509Certificate) {
                return (X509Certificate) cert;
            } else {
                throw new CertificateNotFoundException("Certificate not found or not X509");
            }
        } catch (KeyStoreException e) {
            throw new KeyStoreInitializationException("Failed to fetch certificate: " + e.getMessage(), e);
        }
    }

    public X509Certificate[] getCertificateChain() throws Exception {
        String alias = findAliasByCertSerial(serialHex);
        return Arrays.stream(keyStore.getCertificateChain(alias))
                .map(cert -> (X509Certificate) cert)
                .toArray(X509Certificate[]::new);
    }

    public PrivateKey getPrivateKey() throws KeyStoreInitializationException, CertificateNotFoundException, PrivateKeyAccessException {
        try {
            String alias = findAliasByCertSerial(serialHex);
            return (PrivateKey) keyStore.getKey(alias, null);
        } catch (UnrecoverableKeyException e) {
            throw new PrivateKeyAccessException("Invalid PIN or access denied to private key", e);
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            throw new KeyStoreInitializationException("Failed to access private key", e);
        }
    }

    private String findAliasByCertSerial(String serialHex) throws CertificateNotFoundException, KeyStoreInitializationException {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    String serial = ((X509Certificate) cert).getSerialNumber().toString(16);
                    String providedSerialHex = serialHex.startsWith("0") ? serialHex.substring(1) : serialHex;

                    if (serial.equalsIgnoreCase(providedSerialHex)) {
                        if(isExpired((X509Certificate) cert)) throw new CertificateExpiredException("Certificate with serial " + serialHex + " is expired");
                        if(!isDigitalSignature((X509Certificate) cert)) throw new NotADigitalSignatureException("Certificate with serial " + serialHex + " is not a digital signature");
                        return alias;
                    }
                }
            }
            throw new CertificateNotFoundException(String.format("Certificate with serial %s not found", serialHex));
        } catch (KeyStoreException e) {
            throw new KeyStoreInitializationException("Error searching for alias", e);
        } catch (CertificateExpiredException | NotADigitalSignatureException e) {
            throw new RuntimeException(e);
        }
    }
}

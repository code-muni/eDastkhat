package com.pyojan.eDastakhat.libs.keyStore;

import com.pyojan.eDastakhat.exceptions.CertificateNotFoundException;
import com.pyojan.eDastakhat.exceptions.KeyStoreInitializationException;
import com.pyojan.eDastakhat.exceptions.NotADigitalSignatureException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.sf.oval.constraint.NotNull;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;

@AllArgsConstructor @NoArgsConstructor
public class PKCS12KeyStore extends CertificateValidator {
    @Setter @NotNull private String pkcs12FilePath;
    @Setter @NotNull private String pkcs12Password;
    @Getter private final Provider provider = new BouncyCastleProvider();

    private KeyStore keyStore;

    public void loadKeyStore() {
        try {
            if (pkcs12FilePath == null || pkcs12Password == null) {
                throw new IllegalArgumentException("PKCS#12 file path or password not provided.");
            }

            Security.addProvider(provider);
            keyStore = KeyStore.getInstance("PKCS12", provider);
            keyStore.load(Files.newInputStream(Paths.get(pkcs12FilePath)), pkcs12Password.toCharArray());
        } catch (IOException e) {
            throw new KeyStoreInitializationException("Failed to read the PKCS#12 file: " + pkcs12FilePath, e);
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new KeyStoreInitializationException("Failed to initialize PKCS#12 KeyStore", e);
        }
    }

    public PrivateKey getPrivateKey() {
        try {
            String alias = getFirstAlias();
            return (PrivateKey) keyStore.getKey(alias, pkcs12Password.toCharArray());
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException("Unable to recover the private key from the PKCS#12 store", e);
        } catch (Exception e) {
            throw new KeyStoreInitializationException("Failed to retrieve private key", e);
        }
    }

    public PrivateKey getPrivateKey(String certSerialHex) throws NotADigitalSignatureException {
        try {
            String alias = findAliasBySerialNumber(certSerialHex);
            return (PrivateKey) keyStore.getKey(alias, pkcs12Password.toCharArray());
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException("Unable to recover the private key for certificate with serial " + certSerialHex, e);
        } catch (Exception e) {
            throw new NotADigitalSignatureException("Failed to retrieve private key for certificate: " + certSerialHex, e);
        }
    }

    public X509Certificate[] getCertificateChain() {
        try {
            String alias = getFirstAlias();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

            if (!isDigitalSignature(cert)) {
                throw new NotADigitalSignatureException("Default certificate with serial " + cert.getSerialNumber().toString(16) + " is not valid for digital signatures");
            }

            Certificate[] chain = keyStore.getCertificateChain(alias);
            return Arrays.copyOf(chain, chain.length, X509Certificate[].class);

        } catch (KeyStoreException | NotADigitalSignatureException e) {
            throw new KeyStoreInitializationException("Failed to retrieve certificate chain from KeyStore or certificate is not a digital signature", e);
        }
    }

    public X509Certificate[] getCertificateChain(String certSerialHex) {
        try {
            String alias = findAliasBySerialNumber(certSerialHex);
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

            if (!isDigitalSignature(cert)) {
                throw new NotADigitalSignatureException("Certificate with serial " + certSerialHex + " is not valid for digital signatures");
            }

            Certificate[] chain = keyStore.getCertificateChain(alias);
            return Arrays.copyOf(chain, chain.length, X509Certificate[].class);

        } catch (KeyStoreException | NotADigitalSignatureException e) {
            throw new KeyStoreInitializationException("Failed to retrieve certificate chain from KeyStore or certificate is not a digital signature", e);
        }
    }

    private String findAliasBySerialNumber(String certSerialHex) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) cert;
                    if (x509.getSerialNumber().toString(16).equalsIgnoreCase(certSerialHex)) {
                        return alias;
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new KeyStoreInitializationException("Error accessing aliases in KeyStore", e);
        }
        throw new CertificateNotFoundException("No certificate with serial " + certSerialHex + " found in PKCS#12 keystore.");
    }

    private String getFirstAlias() throws KeyStoreException {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias) && keyStore.getCertificate(alias) != null) {
                return alias;
            }
        }
        throw new KeyStoreInitializationException("No valid certificate entries found in PKCS#12 keystore.");
    }



    private X509Certificate getCertificate() {
        try {
            String alias = getFirstAlias();
            return (X509Certificate) keyStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            throw new KeyStoreInitializationException("Failed to retrieve certificate from keystore", e);
        }
    }
}

package com.pyojan.eDastakhat.libs.keyStore;

import com.pyojan.eDastakhat.exceptions.*;
import lombok.Setter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.security.pkcs11.SunPKCS11;
import sun.security.pkcs11.wrapper.CK_TOKEN_INFO;
import sun.security.pkcs11.wrapper.PKCS11;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.*;

public class PKCS11KeyStore extends CertificateValidator {

    private static final String PKCS11_TYPE = "PKCS11";

    private final BouncyCastleProvider bcProvider = new BouncyCastleProvider();
    private SunPKCS11 pkcs11Provider;
    private KeyStore keyStore;
    private char[] pin;
    private String pkcs11LibPath;
    @Setter private String tokenSerial;
    @Setter  private String certSerialHex;


    public Provider getProvider() {
        return pkcs11Provider;
    }

    public void init(String pkcs11LibPath, String pin) throws Exception {
        this.pkcs11LibPath = pkcs11LibPath;
        this.pin = pin.toCharArray();

        long slot = tokenSerial != null ? findSlotByTokenSerial() : 0; // if tokenSerial is null, use the first slot
        String config = String.format("name=PKCS11\nlibrary=%s\nslotListIndex=%d", pkcs11LibPath, slot);
        ByteArrayInputStream configStream = new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8));

        pkcs11Provider = new SunPKCS11(configStream);
        Security.addProvider(pkcs11Provider);
        Security.addProvider(bcProvider);

        try {
            keyStore = KeyStore.getInstance(PKCS11_TYPE, pkcs11Provider);
        } catch (KeyStoreException e) {
            throw new KeyStoreInitializationException("Failed to create KeyStore instance", e);
        }

        try {
            keyStore.load(null, this.pin);
        } catch (IOException e) {
            throw new InvalidPINException("PIN is incorrect", e);
        } catch (NoSuchAlgorithmException | CertificateException e) {
            throw new KeyStoreInitializationException("KeyStore loading failed due to algorithm or certificate issues", e);
        }
    }


    public PublicKey getPublicKey(String serialHex) throws Exception {
        String alias = findAliasByCertSerial(serialHex);
        return keyStore.getCertificate(alias).getPublicKey();
    }

    public PublicKey getPublicKey() throws Exception {
        return getPublicKey(certSerialHex);
    }

    public PrivateKey getPrivateKey(String serialHex) throws Exception {
        String alias = findAliasByCertSerial(serialHex);
        try {
            Key key = keyStore.getKey(alias, pin);
            if (!(key instanceof PrivateKey)) {
                throw new NotAPrivateKeyException("Key with serial " + serialHex + " is not a private key");
            }

            return (PrivateKey) key;
        } catch (UnrecoverableKeyException e) {
            throw new InvalidPINException("PIN is incorrect.", e);
        }
    }

    public PrivateKey getPrivateKey() throws Exception {
        return getPrivateKey(certSerialHex);
    }

    public X509Certificate[] getCertificateChain(String serialHex) throws Exception {
        String alias = findAliasByCertSerial(serialHex);
        return Arrays.stream(keyStore.getCertificateChain(alias))
                .map(cert -> (X509Certificate) cert)
                .toArray(X509Certificate[]::new);
    }

    public X509Certificate[] getCertificateChain() throws Exception {
        return getCertificateChain(certSerialHex);
    }


    public X509Certificate getCertificate(String serialHex) throws Exception {
        String alias = findAliasByCertSerial(serialHex);
        return (X509Certificate) keyStore.getCertificate(alias);
    }

    public X509Certificate getCertificate() throws Exception {
        return getCertificate(certSerialHex);
    }



    // Internal Helpers

    private long findSlotByTokenSerial() throws Exception {
        PKCS11 pkcs11 = PKCS11.getInstance(pkcs11LibPath, "C_GetFunctionList", null, false);
        long[] slots = pkcs11.C_GetSlotList(true);

        for (long slot : slots) {
            CK_TOKEN_INFO info = pkcs11.C_GetTokenInfo(slot);
            String serial = new String(info.serialNumber).trim();
            if (serial.equalsIgnoreCase(tokenSerial)) {
                return (slot-1); // because the slot index is 0-based
            }
        }

        throw new TokenNotFoundException("Token with serial " + tokenSerial + " not found");
    }

    private String findAliasByCertSerial(String serialHex) throws Exception {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            String certSerial = cert.getSerialNumber().toString(16);
            if (certSerial.equalsIgnoreCase(serialHex)) {
                if(isExpired(cert)) throw new CertificateExpiredException("Certificate with serial " + serialHex + " is expired");
                if(!isDigitalSignature(cert)) throw new NotADigitalSignatureException("Certificate with serial " + serialHex + " is not a digital signature");

                return alias;
            }
        }
        throw new CertificateNotFoundException("Certificate with serial " + serialHex + " not found");
    }
}

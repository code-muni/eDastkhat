package com.pyojan.eDastakhat.libs.keyStore;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;


@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificateValidator {

    private X509Certificate certificate;

    /**
     * Check if the certificate has expired.
     * @return true if the certificate has expired, false if it is still valid.
     */
    protected boolean isExpired() {
        return isExpired(certificate);
    }

    /**
     * Checks whether the given X509Certificate has expired.
     *
     * @param cert the X509Certificate to check for expiration
     * @return true if the certificate has expired, false if it is still valid
     * @throws IllegalArgumentException if the certificate is null
     */
    protected boolean isExpired(X509Certificate cert) {
        try {
            cert.checkValidity();
            return false;
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            return true;
        }
    }


    /**
     * Check if the given X509Certificate can be used for digital signatures.
     * @param cert the X509Certificate to check
     * @return true if the certificate can be used for digital signatures, false otherwise
     */
    protected boolean isDigitalSignature(X509Certificate cert) {
        boolean[] keyUsage = cert.getKeyUsage();
        // KeyUsage[0] is for digitalSignature
        return keyUsage != null && keyUsage[0];
    }

    /**
     * Checks whether the certificate can be used for digital signatures.
     * @return true if the certificate can be used for digital signatures, false otherwise
     */
    protected boolean isDigitalSignature() {
        return isDigitalSignature(certificate);
    }

    /**
     * Checks if the given X509Certificate can be used for encryption.
     *
     * @param cert the X509Certificate to check
     * @return true if the certificate can be used for key encipherment or data encipherment, false otherwise
     */
    protected boolean isEncryption(X509Certificate cert) {
        boolean[] keyUsage = cert.getKeyUsage();
        // KeyUsage[2] is for keyEncipherment, [3] is for dataEncipherment
        return keyUsage != null && (keyUsage[2] || keyUsage[3]);
    }

    /**
     * Checks if the certificate can be used for encryption.
     * @return true if the certificate can be used for key encipherment or data encipherment, false otherwise
     */
    protected boolean isEncryption() {
        return isEncryption(certificate);
    }
}



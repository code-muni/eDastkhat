package com.pyojan.eDastakhat.services.xml;

import org.w3c.dom.Document;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * A self-contained XML signing utility that handles all aspects of XML signing.
 */
public class XMLSigner {
    private final X509Certificate certificate;
    private final PrivateKey privateKey;

    /**
     * Constructs an XMLSigner with the specified certificate and private key.
     *
     * @param certificate The X509 certificate to be included in the signature
     * @param privateKey The private key used for signing
     */
    public XMLSigner(X509Certificate certificate, PrivateKey privateKey) {
        this.certificate = Objects.requireNonNull(certificate, "Certificate cannot be null");
        this.privateKey = Objects.requireNonNull(privateKey, "Private key cannot be null");
    }

    /**
     * Signs the XML content from a file path.
     *
     * @param xmlFilePath Path to the XML file to sign
     * @return The signed XML as a string
     * @throws XMLSigningException if signing fails
     */
    public String signXmlFromFile(String xmlFilePath) throws XMLSigningException {
        try {
            String xmlContent = new String(Files.readAllBytes(new File(xmlFilePath).toPath()),
                    StandardCharsets.UTF_8);
            return signXmlString(xmlContent);
        } catch (IOException e) {
            throw new XMLSigningException("Failed to read XML file", e);
        }
    }

    /**
     * Signs the XML content from a string.
     *
     * @param xmlString The XML content to sign
     * @return The signed XML as a string
     * @throws XMLSigningException if signing fails
     */
    public String signXmlString(String xmlString) throws XMLSigningException {
        try {
            String normalizedXml = normalizeXmlString(xmlString);
            Document doc = parseXmlString(normalizedXml);
            return signXmlDocument(doc);
        } catch (Exception e) {
            throw new XMLSigningException("Failed to sign XML string", e);
        }
    }

    // Private helper methods

    private String signXmlDocument(Document doc) throws XMLSigningException {
        try {
            XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");

            // Create signed info with enveloped transform
            SignedInfo signedInfo = createSignedInfo(signatureFactory);

            // Create key info with certificate
            KeyInfo keyInfo = createKeyInfo(signatureFactory);

            // Create and sign the signature
            DOMSignContext signContext = new DOMSignContext(privateKey, doc.getDocumentElement());
            XMLSignature signature = signatureFactory.newXMLSignature(signedInfo, keyInfo);
            signature.sign(signContext);

            // Convert document back to string
            return documentToString(doc);
        } catch (Exception e) {
            throw new XMLSigningException("Failed to sign XML document", e);
        }
    }

    private String normalizeXmlString(String xmlString) {
        // Remove formatting whitespace but preserve significant whitespace
        return xmlString.replaceAll(">\\s+<", "><").trim();
    }

    private Document parseXmlString(String xmlString) throws XMLSigningException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new XMLSigningException("Failed to parse XML string", e);
        }
    }

    private SignedInfo createSignedInfo(XMLSignatureFactory factory) throws XMLSignatureException, InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        Reference ref = factory.newReference("",
                factory.newDigestMethod(DigestMethod.SHA256, null),
                Collections.singletonList(
                        factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                null, null);

        return factory.newSignedInfo(
                factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                factory.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
                Collections.singletonList(ref));
    }

    private KeyInfo createKeyInfo(XMLSignatureFactory factory) {
        KeyInfoFactory kif = factory.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<>();
        x509Content.add(certificate.getSubjectX500Principal().getName());
        x509Content.add(certificate);
        X509Data xd = kif.newX509Data(x509Content);
        return kif.newKeyInfo(Collections.singletonList(xd));
    }

    private String documentToString(Document doc) throws TransformerException {
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Custom exception for XML signing errors.
     */
    public static class XMLSigningException extends Exception {
        public XMLSigningException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
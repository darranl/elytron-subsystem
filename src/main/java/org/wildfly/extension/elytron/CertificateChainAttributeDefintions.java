/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron;

import static org.wildfly.extension.elytron.KeyStoreDefinition.ISO_8601_FORMAT;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.security.sasl.util.HexConverter;

/**
 * Class to contain the attribute definitions for certificates and their chains.
 *
 * Also contains utility methods to convert from the {@link Certificate} to the {@link ModelNode} representation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class CertificateChainAttributeDefintions {

    private static final String SHA_1 = "SHA-1";

    private static final String SHA_256 = "SHA-256";

    private static final SimpleAttributeDefinition TYPE = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.TYPE, ModelType.STRING).build();

    private static final SimpleAttributeDefinition ALGORITHM = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.ALGORITHM, ModelType.STRING).build();

    private static final SimpleAttributeDefinition ENCODED = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.ENCODED, ModelType.STRING).build();

    private static final SimpleAttributeDefinition FORMAT = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.FORMAT, ModelType.STRING).build();

    private static final ObjectTypeAttributeDefinition PUBLIC_KEY = new ObjectTypeAttributeDefinition.Builder(ElytronDescriptionConstants.PUBLIC_KEY, ALGORITHM, FORMAT, ENCODED).build();

    private static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.VALUE, ModelType.STRING).build();

    private static final ObjectTypeAttributeDefinition FINGER_PRINT = new ObjectTypeAttributeDefinition.Builder(ElytronDescriptionConstants.FINGER_PRINT, ALGORITHM, VALUE).build();

    // Should this me a map so we don't need to specify the algorithm?
    private static final ObjectListAttributeDefinition FINGER_PRINTS = new ObjectListAttributeDefinition.Builder(ElytronDescriptionConstants.FINGER_PRINTS, FINGER_PRINT).build();

    /*
     * X509 Certificate Specific Attributes
     */

    private static final SimpleAttributeDefinition SUBJECT = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.SUBJECT, ModelType.STRING)
        .setAllowNull(true)
        .build();

    private static final SimpleAttributeDefinition ISSUER = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.ISSUER, ModelType.STRING)
        .setAllowNull(true)
        .build();

    private static final SimpleAttributeDefinition NOT_AFTER = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.NOT_AFTER, ModelType.STRING)
        .setAllowNull(true)
        .build();

    private static final SimpleAttributeDefinition NOT_BEFORE = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.NOT_BEFORE, ModelType.STRING)
        .setAllowNull(true)
        .build();

    private static final SimpleAttributeDefinition SERIAL_NUMBER = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.SERIAL_NUMBER, ModelType.STRING)
        .setAllowNull(true)
        .build();

    private static final SimpleAttributeDefinition SIGNATURE_ALGORITHM = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.SIGNATURE_ALGORITHM, ModelType.STRING)
        .setAllowNull(true)
        .build();

    private static final SimpleAttributeDefinition SIGNATURE = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.SIGNATURE, ModelType.STRING)
        .setAllowNull(true)
        .build();

    private static final SimpleAttributeDefinition VERSION = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.VERSION, ModelType.STRING)
        .setAllowNull(true)
        .build();

    // TODO - Consider adding some of the more detailed fields from X509Certificate

    static final ObjectTypeAttributeDefinition CERTIFICATE = new ObjectTypeAttributeDefinition.Builder(ElytronDescriptionConstants.CERTIFICATE, TYPE, PUBLIC_KEY, FINGER_PRINTS, ENCODED,
                                                                                                         SUBJECT, ISSUER, NOT_BEFORE, NOT_AFTER, SERIAL_NUMBER, SIGNATURE_ALGORITHM, SIGNATURE, VERSION)
        .setStorageRuntime()
        .build();

    static final ObjectListAttributeDefinition CERTIFICATE_CHAIN = new ObjectListAttributeDefinition.Builder(ElytronDescriptionConstants.CERTIFICATE_CHAIN, CERTIFICATE)
        .setStorageRuntime()
        .build();

    static void writeCertificate(final ModelNode certificateModel, final Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
        certificateModel.get(ElytronDescriptionConstants.TYPE).set(certificate.getType());

        PublicKey publicKey = certificate.getPublicKey();
        ModelNode publicKeyModel = new ModelNode();
        publicKeyModel.get(ElytronDescriptionConstants.ALGORITHM).set(publicKey.getAlgorithm());
        publicKeyModel.get(ElytronDescriptionConstants.FORMAT).set(publicKey.getFormat());
        publicKeyModel.get(ElytronDescriptionConstants.ENCODED).set(encodedHexString(publicKey.getEncoded()));
        certificateModel.get(ElytronDescriptionConstants.PUBLIC_KEY).set(publicKeyModel);

        ModelNode fingerPrintsModel = new ModelNode();
        byte[] encodedCertificate = certificate.getEncoded();

        ModelNode sha1 = new ModelNode();
        sha1.get(ElytronDescriptionConstants.ALGORITHM).set(SHA_1);
        sha1.get(ElytronDescriptionConstants.VALUE).set(encodedHexString(digest(SHA_1, encodedCertificate)));
        fingerPrintsModel.add(sha1);

        ModelNode sha256 = new ModelNode();
        sha256.get(ElytronDescriptionConstants.ALGORITHM).set(SHA_256);
        sha256.get(ElytronDescriptionConstants.VALUE).set(encodedHexString(digest(SHA_256, encodedCertificate)));
        fingerPrintsModel.add(sha256);

        certificateModel.get(ElytronDescriptionConstants.FINGER_PRINTS).set(fingerPrintsModel);

        certificateModel.get(ElytronDescriptionConstants.ENCODED).set(encodedHexString(encodedCertificate));

        if (certificate instanceof X509Certificate) {
            writeCertificate(certificateModel, (X509Certificate) certificate);
        }
    }

    private static void writeCertificate(final ModelNode certificateModel, final X509Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601_FORMAT);

        certificateModel.get(ElytronDescriptionConstants.SUBJECT).set(certificate.getSubjectX500Principal().getName());
        certificateModel.get(ElytronDescriptionConstants.ISSUER).set(certificate.getIssuerX500Principal().getName());
        certificateModel.get(ElytronDescriptionConstants.NOT_BEFORE).set(sdf.format(certificate.getNotBefore()));
        certificateModel.get(ElytronDescriptionConstants.NOT_AFTER).set(sdf.format(certificate.getNotAfter()));
        certificateModel.get(ElytronDescriptionConstants.SERIAL_NUMBER).set(delimit(certificate.getSerialNumber().toString(16).toCharArray()));
        certificateModel.get(ElytronDescriptionConstants.SIGNATURE_ALGORITHM).set(certificate.getSigAlgName());
        certificateModel.get(ElytronDescriptionConstants.SIGNATURE).set(encodedHexString(certificate.getSignature()));
        certificateModel.get(ElytronDescriptionConstants.VERSION).set("v" + certificate.getVersion());
    }

    /**
     * Populate the supplied response with the model representation of the certificate chain.
     *
     * @param result the response to populate.
     * @param certificateChain the certificate chain to add to the response.
     * @throws CertificateEncodingException
     * @throws NoSuchAlgorithmException
     */
    static void writeCertificateChain(final ModelNode result, final Certificate[] certificateChain) throws CertificateEncodingException, NoSuchAlgorithmException {
        for (Certificate current : certificateChain) {
            ModelNode certificate = new ModelNode();
            writeCertificate(certificate, current);
            result.add(certificate);
        }
    }

    private static byte[] digest(final String algorithm, final byte[] encoded) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        return digest.digest(encoded);
    }

    private static String encodedHexString(byte[] encoded) {
        return delimit(HexConverter.convertToHexString(encoded).toCharArray());
    }

    private static String delimit(final char[] chars) {
        StringBuilder sb = new StringBuilder();
        int offset = 1;
        if (chars.length % 2 != 0) {
            sb.append('0');
            offset++;
        }

        for (int i = 0; i < chars.length; i++) {
            sb.append(chars[i]);
            if (i + 1 < chars.length && (i + offset) % 2 == 0) {
                sb.append(':');
            }
        }
        return sb.toString();
    }

}

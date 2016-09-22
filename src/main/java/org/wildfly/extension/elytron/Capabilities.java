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

import java.security.KeyStore;
import java.security.Provider;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.security.sasl.SaslServerFactory;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.security.CredentialStoreClient;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.SecurityFactory;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.auth.server.ModifiableSecurityRealm;
import org.wildfly.security.auth.server.NameRewriter;
import org.wildfly.security.auth.server.PrincipalDecoder;
import org.wildfly.security.auth.server.RealmMapper;
import org.wildfly.security.auth.server.SaslAuthenticationFactory;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.authz.PermissionMapper;
import org.wildfly.security.authz.RoleDecoder;
import org.wildfly.security.authz.RoleMapper;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;


/**
 * The capabilities provided by and required by this subsystem.
 *
 * It is a deliberate decision that this class is not public, by using capability definitions it should be possible to
 * completely remove this subsystem and allow another to provide all the capabilities - allowing references to this class would
 * not allow complete removal.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class Capabilities {

    private static final String CAPABILITY_BASE = "org.wildfly.security.";

    static final String HTTP_AUTHENTICATION_FACTORY_CAPABILITY = CAPABILITY_BASE + "http-authentication-factory";
    static final String CREDENTIAL_STORE_CLIENT_CAPABILITY = CAPABILITY_BASE + "credential-store-client";

    static final RuntimeCapability<Void> CREDENTIAL_STORE_CLIENT_RUNTIME_CAPABILITY =  RuntimeCapability
            .Builder.of(CREDENTIAL_STORE_CLIENT_CAPABILITY, true, CredentialStoreClient.class)
            .build();

    static final String HTTP_SERVER_AUTHENTICATION_CAPABILITY = CAPABILITY_BASE + "http-server-authentication";

    static final RuntimeCapability<Void> HTTP_AUTHENTICATION_FACTORY_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(HTTP_AUTHENTICATION_FACTORY_CAPABILITY, true, HttpAuthenticationFactory.class)
            .build();

    static final String HTTP_SERVER_MECHANISM_FACTORY_CAPABILITY = CAPABILITY_BASE + "http-server-mechanism-factory";

    static final RuntimeCapability<Void> HTTP_SERVER_MECHANISM_FACTORY_RUNTIME_CAPABILITY =  RuntimeCapability
        .Builder.of(HTTP_SERVER_MECHANISM_FACTORY_CAPABILITY, true, HttpServerAuthenticationMechanismFactory.class)
        .build();

    static final String KEY_MANAGERS_CAPABILITY = CAPABILITY_BASE + "key-managers";

    static final RuntimeCapability<Void> KEY_MANAGERS_RUNTIME_CAPABILITY =  RuntimeCapability
            .Builder.of(KEY_MANAGERS_CAPABILITY, true, KeyManager[].class)
            .build();

    static final String KEY_STORE_CAPABILITY = CAPABILITY_BASE + "key-store";

    static final RuntimeCapability<Void> KEY_STORE_RUNTIME_CAPABILITY =  RuntimeCapability
        .Builder.of(KEY_STORE_CAPABILITY, true, KeyStore.class)
        .build();

    static final String NAME_REWRITER_CAPABILITY = CAPABILITY_BASE + "name-rewriter";

    static final RuntimeCapability<Void> NAME_REWRITER_RUNTIME_CAPABILITY =  RuntimeCapability
        .Builder.of(NAME_REWRITER_CAPABILITY, true, NameRewriter.class)
        .build();

    static final String PERMISSION_MAPPER_CAPABILITY = CAPABILITY_BASE + "permission-mapper";

    static final RuntimeCapability<Void> PERMISSION_MAPPER_RUNTIME_CAPABILITY =  RuntimeCapability
            .Builder.of(PERMISSION_MAPPER_CAPABILITY, true, PermissionMapper.class)
            .build();

    static final String PRINCIPAL_DECODER_CAPABILITY = CAPABILITY_BASE + "principal-decoder";

    static final RuntimeCapability<Void> PRINCIPAL_DECODER_RUNTIME_CAPABILITY =  RuntimeCapability
        .Builder.of(PRINCIPAL_DECODER_CAPABILITY, true, PrincipalDecoder.class)
        .build();

    static final String PROVIDERS_CAPABILITY = CAPABILITY_BASE + "providers";

    static final RuntimeCapability<Void> PROVIDERS_RUNTIME_CAPABILITY =  RuntimeCapability
        .Builder.of(PROVIDERS_CAPABILITY, true, Provider[].class)
        .build();

    static final String REALM_MAPPER_CAPABILITY = CAPABILITY_BASE + "realm-mapper";

    static final RuntimeCapability<Void> REALM_MAPPER_RUNTIME_CAPABILITY =  RuntimeCapability
            .Builder.of(REALM_MAPPER_CAPABILITY, true, RealmMapper.class)
            .build();

    static final String ROLE_DECODER_CAPABILITY = CAPABILITY_BASE + "role-decoder";

    static final RuntimeCapability<Void> ROLE_DECODER_RUNTIME_CAPABILITY =  RuntimeCapability
            .Builder.of(ROLE_DECODER_CAPABILITY, true, RoleDecoder.class)
            .build();

    static final String ROLE_MAPPER_CAPABILITY = CAPABILITY_BASE + "role-mapper";

    static final RuntimeCapability<Void> ROLE_MAPPER_RUNTIME_CAPABILITY =  RuntimeCapability
            .Builder.of(ROLE_MAPPER_CAPABILITY, true, RoleMapper.class)
            .build();

    static final String SASL_AUTHENTICATION_FACTORY_CAPABILITY = CAPABILITY_BASE + "sasl-authentication-factory";

    static final RuntimeCapability<Void> SASL_AUTHENTICATION_FACTORY_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(SASL_AUTHENTICATION_FACTORY_CAPABILITY, true, SaslAuthenticationFactory.class)
            .build();

    static final String SASL_SERVER_FACTORY_CAPABILITY = CAPABILITY_BASE + "sasl-server-factory";

    static final RuntimeCapability<Void> SASL_SERVER_FACTORY_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(SASL_SERVER_FACTORY_CAPABILITY, true, SaslServerFactory.class)
            .build();

    static final String SECURITY_DOMAIN_CAPABILITY = CAPABILITY_BASE + "security-domain";

    static final RuntimeCapability<Void> SECURITY_DOMAIN_RUNTIME_CAPABILITY = RuntimeCapability
        .Builder.of(SECURITY_DOMAIN_CAPABILITY, true, SecurityDomain.class)
        .build();

    static final String SECURITY_FACTORY_CAPABILITY_BASE = CAPABILITY_BASE + "security-factory.";

    static final String SECURITY_FACTORY_CREDENTIAL_CAPABILITY = SECURITY_FACTORY_CAPABILITY_BASE + "credential";

    static final RuntimeCapability<Void> SECURITY_FACTORY_CREDENTIAL_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(SECURITY_FACTORY_CREDENTIAL_CAPABILITY, true, SecurityFactory.class)
            .build();

    static final String MODIFIABLE_SECURITY_REALM_CAPABILITY = CAPABILITY_BASE + "modifiable-security-realm";

    static final RuntimeCapability<Void> MODIFIABLE_SECURITY_REALM_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(MODIFIABLE_SECURITY_REALM_CAPABILITY, true, ModifiableSecurityRealm.class)
            .build();

    static final String SECURITY_REALM_CAPABILITY = CAPABILITY_BASE + "security-realm";

    static final RuntimeCapability<Void> SECURITY_REALM_RUNTIME_CAPABILITY = RuntimeCapability
        .Builder.of(SECURITY_REALM_CAPABILITY, true, SecurityRealm.class)
        .build();

    static final String SSL_CONTEXT_CAPABILITY = CAPABILITY_BASE + "ssl-context";

    static final RuntimeCapability<Void> SSL_CONTEXT_RUNTIME_CAPABILITY = RuntimeCapability
        .Builder.of(SSL_CONTEXT_CAPABILITY, true, SSLContext.class)
        .build();

    static final String TRUST_MANAGERS_CAPABILITY = CAPABILITY_BASE + "trust-managers";

    static final RuntimeCapability<Void> TRUST_MANAGERS_RUNTIME_CAPABILITY =  RuntimeCapability
            .Builder.of(TRUST_MANAGERS_CAPABILITY, true, TrustManager[].class)
            .build();

    static final String DIR_CONTEXT_CAPABILITY = CAPABILITY_BASE + "dir-context";

    static final RuntimeCapability<Void> DIR_CONTEXT_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(DIR_CONTEXT_CAPABILITY, true, ExceptionSupplier.class) // ExceptionSupplier<DirContext, Exception>
            .build();

    /**
     * Requirements, capabilities from other subsystems.
     */

    /**
     * Required by the {@link JdbcRealmDefinition}.
     */
    static final String DATA_SOURCE_CAPABILITY_NAME = "org.wildfly.data-source";
}

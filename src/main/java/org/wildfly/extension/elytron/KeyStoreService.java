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

import static org.wildfly.extension.elytron._private.ElytronSubsystemMessages.ROOT_LOGGER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.services.path.PathEntry;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManager.Callback.Handle;
import org.jboss.as.controller.services.path.PathManager.Event;
import org.jboss.as.controller.services.path.PathManager.PathEventContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.keystore.AtomicLoadKeyStore;

/**
 * A {@link Service} responsible for a single {@link KeyStore} instance.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class KeyStoreService implements Service<KeyStore> {

    private final String provider;
    private final String type;
    private final char[] password;
    private final String path;
    private final String relativeTo;
    private final boolean required;
    private final boolean watch;

    private final InjectedValue<PathManager> pathManager = new InjectedValue<PathManager>();

    private File resolvedPath;
    private Handle callbackHandle;

    private long synched;
    private AtomicLoadKeyStore keyStore = null;

    private KeyStoreService(String provider, String type, char[] password, String relativeTo, String path, boolean required, boolean watch) {
        this.provider = provider;
        this.type = type;
        this.password = password != null ? password.clone() : null;
        this.relativeTo = relativeTo;
        this.path = path;
        this.required = required;
        this.watch = watch;
    }

    static KeyStoreService createFileLessKeyStoreService(String provider, String type, char[] password) {
        return new KeyStoreService(provider, type, password, null, null, false, false);
    }

    static KeyStoreService createFileBasedKeyStoreService(String provider, String type, char[] password, String relativeTo, String path, boolean required, boolean watch) {
        return new KeyStoreService(provider, type, password, relativeTo, path, required, watch);
    }

    /*
     * Service Lifecycle Related Methods
     */

    @Override
    public void start(StartContext startContext) throws StartException {
        try {
            AtomicLoadKeyStore keyStore = provider == null ? AtomicLoadKeyStore.newInstance(type) : AtomicLoadKeyStore.newInstance(type, provider);
            if (path != null) {
                resolveFileLocation();
            }

            synched = System.currentTimeMillis();
            load(keyStore);

            this.keyStore = keyStore;
        } catch (GeneralSecurityException | IOException e) {
            throw ROOT_LOGGER.unableToInitialiseKeyStore(e);
        }
    }

    private void resolveFileLocation() {
        if (relativeTo != null) {
            PathManager pathManager = this.pathManager.getValue();
            resolvedPath = new File(pathManager.resolveRelativePathEntry(path, relativeTo));
            callbackHandle = pathManager.registerCallback(relativeTo, new org.jboss.as.controller.services.path.PathManager.Callback() {

                @Override
                public void pathModelEvent(PathEventContext eventContext, String name) {
                    if (eventContext.isResourceServiceRestartAllowed() == false) {
                        eventContext.reloadRequired();
                    }
                }

                @Override
                public void pathEvent(Event event, PathEntry pathEntry) {
                    // Service dependencies should trigger a stop and start.
                }
            }, Event.REMOVED, Event.UPDATED);
        } else {
            resolvedPath = new File(path);
        }
    }

    private AtomicLoadKeyStore.LoadKey load(AtomicLoadKeyStore keyStore) throws GeneralSecurityException, IOException {
        try (InputStream is = resolvedPath != null ? new FileInputStream(resolvedPath) : null) {
            return keyStore.revertableload(is, password);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        keyStore = null;
        if (callbackHandle != null) {
            callbackHandle.remove();
        }
    }

    @Override
    public KeyStore getValue() throws IllegalStateException, IllegalArgumentException {
        return keyStore;
    }

    Injector<PathManager> getPathManagerInjector() {
        return pathManager;
    }

    /*
     * OperationStepHandler Access Methods
     */

    long timeSynched() {
        return synched;
    }

    LoadKey load() throws OperationFailedException {
        try {
            AtomicLoadKeyStore.LoadKey loadKey = load(keyStore);
            long originalSynced = synched;
            synched = System.currentTimeMillis();
            return new LoadKey(loadKey, originalSynced);
        } catch (GeneralSecurityException | IOException e) {
            throw ROOT_LOGGER.unableToCompleteOperation(e);
        }
    }

    void revertLoad(final LoadKey loadKey) {
        keyStore.revert(loadKey.loadKey);
        synched = loadKey.modifiedTime;
    }

    void save() throws OperationFailedException {
        if (resolvedPath == null) {
            throw ROOT_LOGGER.cantSaveWithoutFile();
        }
        try (FileOutputStream fos = new FileOutputStream(resolvedPath)) {
            keyStore.store(fos, password);
            synched = System.currentTimeMillis();
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw ROOT_LOGGER.unableToCompleteOperation(e);
        }
    }

    class LoadKey {
        private final AtomicLoadKeyStore.LoadKey loadKey;
        private final long modifiedTime;

        LoadKey(AtomicLoadKeyStore.LoadKey loadKey, long modifiedTime) {
            this.loadKey = loadKey;
            this.modifiedTime = modifiedTime;
        }
    }

}

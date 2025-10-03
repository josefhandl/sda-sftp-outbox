/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package se.nbis.lega.outbox.filesystem;

import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.root.RootedFileSystemProvider;
import org.apache.sshd.common.session.SessionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * SFTP filesystem factory that backs Mina with <code>RootedFileSystem</code>.
 */
@Slf4j
@Service
public class LocalFileSystemFactory implements FileSystemFactory {

    private String outboxFolder;

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem createFileSystem(SessionContext session) throws IOException {
        String username = session.getUsername();
        String root;
        if (outboxFolder.endsWith(File.separator)) {
            root = outboxFolder + username;
        } else {
            root = outboxFolder + File.separator + username;
        }
        File home = new File(root);
        home.mkdirs();
        FileSystem fileSystem = new RootedFileSystemProvider().newFileSystem(home.toPath(), Collections.emptyMap());
        log.info("Local file system initialized for user {}, path: {}", username, root);
        return fileSystem;
    }

    @Value("${outbox.local.directory}")
    public void setOutboxFolder(String outboxFolder) {
        this.outboxFolder = outboxFolder;
    }

    @Override
    public Path getUserHomeDir(SessionContext session) throws IOException {
        String username = session.getUsername();
        String root;
        if (outboxFolder.endsWith(File.separator)) {
            root = outboxFolder + username;
        } else {
            root = outboxFolder + File.separator + username;
        }
        return Paths.get(root);
    }
}

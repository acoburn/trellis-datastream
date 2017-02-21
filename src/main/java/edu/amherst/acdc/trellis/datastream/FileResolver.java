/*
 * Copyright Amherst College
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
package edu.amherst.acdc.trellis.datastream;

import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import edu.amherst.acdc.trellis.spi.DatastreamService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;
import org.slf4j.Logger;

/**
 * @author acoburn
 */
public class FileResolver implements DatastreamService.Resolver {

    private final File directory;

    private static final Logger LOGGER = getLogger(FileResolver.class);

    /**
     * Create a new file resolver
     * @param directory the base directory in which to store and retrieve files
     */
    public FileResolver(final String directory) {
        requireNonNull(directory, "Directory may not be null!");
        this.directory = new File(directory);
    }

    @Override
    public List<String> getUriSchemes() {
        return singletonList("file");
    }

    @Override
    public Optional<InputStream> getContent(final IRI identifier) {
        return getFileFromIdentifier(identifier).map(file -> {
            try {
                return new FileInputStream(file);
            } catch (final FileNotFoundException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    @Override
    public Boolean exists(final IRI identifier) {
        return getFileFromIdentifier(identifier).filter(File::isFile).isPresent();
    }

    // TODO -- support incoming digest comparisons
    @Override
    public void setContent(final IRI identifier, final InputStream stream, final Map<String, String> metadata) {
        requireNonNull(stream, "InputStream may not be null!");
        getFileFromIdentifier(identifier).map(File::toPath).ifPresent(path -> {
            LOGGER.debug("Setting datastream content for {}", identifier.getIRIString());
            try {
                copy(stream, path, REPLACE_EXISTING);
            } catch (final IOException ex) {
                LOGGER.error("Error while setting content: {}", ex.getMessage());
                throw new UncheckedIOException(ex);
            }
        });
    }

    private Optional<File> getFileFromIdentifier(final IRI identifier) {
        return ofNullable(identifier).map(IRI::getIRIString).map(URI::create).map(URI::getSchemeSpecificPart)
                .filter(Objects::nonNull).map(path -> new File(directory, path));
    }
}

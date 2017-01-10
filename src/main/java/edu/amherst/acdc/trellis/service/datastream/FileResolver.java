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
package edu.amherst.acdc.trellis.service.datastream;

import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import edu.amherst.acdc.trellis.api.RuntimeRepositoryException;
import edu.amherst.acdc.trellis.spi.DatastreamService;
import org.apache.commons.rdf.api.IRI;

/**
 * @author acoburn
 */
public class FileResolver implements DatastreamService.Resolver {

    private final File directory;

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
                throw new RuntimeRepositoryException("File not found for " + identifier.getIRIString() +
                        ": " + ex.getMessage());
            }
        });
    }

    @Override
    public Boolean exists(final IRI identifier) {
        return getFileFromIdentifier(identifier).filter(File::isFile).isPresent();
    }

    // TODO -- support incoming digest comparisons
    // Note: the contentType value isn't used here; that may need to be changed to a more
    // general metadata Map<String, String> structure
    @Override
    public void setContent(final IRI identifier, final InputStream stream, final String contentType) {
        requireNonNull(stream, "InputStream may not be null!");
        getFileFromIdentifier(identifier).map(File::toPath).ifPresent(path -> {
            try {
                copy(stream, path, REPLACE_EXISTING);
            } catch (final IOException ex) {
                throw new RuntimeRepositoryException("IO Error writing to " + identifier.getIRIString() + ": " +
                        ex.getMessage());
            }
        });
    }

    private Optional<File> getFileFromIdentifier(final IRI identifier) {
        return ofNullable(identifier).map(IRI::getIRIString).map(URI::create).map(URI::getPath)
                .filter(Objects::nonNull).map(File::new);
    }
}

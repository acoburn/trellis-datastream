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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.slf4j.LoggerFactory.getLogger;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha384Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha512Hex;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;
import edu.amherst.acdc.trellis.spi.DatastreamService;
import edu.amherst.acdc.trellis.api.Datastream;
import org.slf4j.Logger;

/**
 * @author acoburn
 */
public class DefaultDatastreamService implements DatastreamService {

    private static final Logger LOGGER = getLogger(DefaultDatastreamService.class);

    final private Map<String, DatastreamService.Resolver> resolvers = new HashMap<>();

    /**
     * Create a datastream service
     * @param handlers the datastream resolvers
     */
    public DefaultDatastreamService(final List<DatastreamService.Resolver> handlers) {
        handlers.forEach(handler -> {
            handler.getUriSchemes().forEach(scheme -> {
                resolvers.put(scheme, handler);
            });
        });
    }

    @Override
    public IRI generateIdentifier(final IRI identifier, final Datastream.StoragePartition partition) {
        // TODO
        return null;
    }

    @Override
    public Optional<DatastreamService.Resolver> getResolver(final IRI identifier) {
        return of(identifier).map(IRI::getIRIString).map(URI::create).map(URI::getScheme).map(resolvers::get)
            .filter(Objects::nonNull);
    }

    @Override
    public Optional<String> hexDigest(final String algorithm, final InputStream stream) {
        try {
            if (algorithm.toUpperCase().equals("MD5")) {
                return of(md5Hex(stream));
            } else if (algorithm.toUpperCase().equals("SHA-1")) {
                return of(sha1Hex(stream));
            } else if (algorithm.toUpperCase().equals("SHA-256")) {
                return of(sha256Hex(stream));
            } else if (algorithm.toUpperCase().equals("SHA-384")) {
                return of(sha384Hex(stream));
            } else if (algorithm.toUpperCase().equals("SHA-512")) {
                return of(sha512Hex(stream));
            }
        } catch (final IOException ex) {
            LOGGER.error("Error computing digest: {}", ex.getMessage());
        }
        return empty();
    }
}

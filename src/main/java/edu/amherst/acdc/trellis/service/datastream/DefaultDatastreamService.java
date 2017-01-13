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
import static java.util.Optional.ofNullable;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.codec.digest.DigestUtils;
import edu.amherst.acdc.trellis.spi.DatastreamService;
import edu.amherst.acdc.trellis.api.Datastream;
import org.slf4j.Logger;

/**
 * @author acoburn
 */
public class DefaultDatastreamService implements DatastreamService {

    private static final Logger LOGGER = getLogger(DefaultDatastreamService.class);

    final private Map<String, DatastreamService.Resolver> resolvers = new HashMap<>();

    @Override
    public void setResolvers(final List<DatastreamService.Resolver> resolvers) {
        this.resolvers.clear();
        resolvers.forEach(resolver -> {
            resolver.getUriSchemes().forEach(scheme -> {
                this.resolvers.put(scheme, resolver);
            });
        });
    }

    @Override
    public synchronized void bind(final DatastreamService.Resolver resolver) {
        resolver.getUriSchemes().forEach(scheme -> {
            resolvers.put(scheme, resolver);
        });
    }

    @Override
    public synchronized void unbind(final DatastreamService.Resolver resolver) {
        resolver.getUriSchemes().forEach(scheme -> {
            resolvers.remove(scheme, resolver);
        });
    }

    @Override
    public IRI generateIdentifier(final IRI identifier, final Datastream.StoragePartition partition) {
        // TODO -- what am I doing with this? Does it even belong here?
        return null;
    }

    @Override
    public Optional<DatastreamService.Resolver> getResolver(final IRI identifier) {
        return of(identifier).map(IRI::getIRIString).map(URI::create).map(URI::getScheme).map(resolvers::get)
            .filter(Objects::nonNull);
    }

    @Override
    public Optional<String> hexDigest(final String algorithm, final InputStream stream) {
        return ofNullable(algorithm).map(DigestUtils::getDigest).flatMap(digest(stream));
    }

    private Function<MessageDigest, Optional<String>> digest(final InputStream stream) {
        return algorithm -> {
            try {
                return of(encodeHexString(DigestUtils.updateDigest(algorithm, stream).digest()));
            } catch (final IOException ex) {
                LOGGER.error("Error computing digest: {}", ex.getMessage());
            }
            return empty();
        };
    }
}

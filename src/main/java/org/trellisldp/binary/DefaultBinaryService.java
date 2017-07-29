/*
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
package org.trellisldp.binary;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD2;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_1;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_384;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_512;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.rdf.api.IRI;
import org.slf4j.Logger;
import org.trellisldp.spi.BinaryService;
import org.trellisldp.spi.IdentifierService;
import org.trellisldp.spi.RuntimeRepositoryException;

/**
 * @author acoburn
 */
public class DefaultBinaryService implements BinaryService {

    private static final Logger LOGGER = getLogger(DefaultBinaryService.class);

    private static final Set<String> algorithms = asList(MD5, MD2, SHA_1, SHA_256, SHA_384, SHA_512).stream()
        .collect(toSet());

    private final Map<String, BinaryService.Resolver> resolvers = new HashMap<>();

    private final IdentifierService idService;
    private final Map<String, Map<String, String>> partitions;

    /**
     * Create a binary service
     * @param resolvers the resolves
     * @param idService the identifier service
     * @param partitions the identifier suppliers for each partition
     */
    public DefaultBinaryService(final List<BinaryService.Resolver> resolvers, final IdentifierService idService,
            final Map<String, Map<String, String>> partitions) {
        resolvers.forEach(resolver -> {
            resolver.getUriSchemes().forEach(scheme -> {
                this.resolvers.put(scheme, resolver);
            });
        });
        this.idService = idService;
        this.partitions = partitions;
    }

    @Override
    public Optional<BinaryService.Resolver> getResolver(final IRI identifier) {
        return of(identifier).map(IRI::getIRIString).map(URI::create).map(URI::getScheme).map(resolvers::get)
            .filter(Objects::nonNull);
    }

    @Override
    public Optional<String> hexDigest(final String algorithm, final InputStream stream) {
        return ofNullable(algorithm).map(DigestUtils::getDigest).flatMap(digest(stream));
    }

    @Override
    public Set<String> supportedAlgorithms() {
        return algorithms;
    }

    @Override
    public Supplier<String> getIdentifierSupplier(final String partition) {
        // TODO add partition-specific config details here
        if (partitions.containsKey(partition)) {
            return idService.getSupplier();
        }
        throw new RuntimeRepositoryException("Invalid partition: " + partition);
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
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

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Base64.getEncoder;
import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
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
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.rdf.api.IRI;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.RuntimeRepositoryException;

/**
 * @author acoburn
 */
public class DefaultBinaryService implements BinaryService {

    private static final String DEFAULT_LEVELS = "0";
    private static final String DEFAULT_LENGTH = "2";

    private static final String SHA = "SHA";

    private static final Logger LOGGER = getLogger(DefaultBinaryService.class);

    private static final Set<String> algorithms = asList(MD5, MD2, SHA, SHA_1, SHA_256, SHA_384, SHA_512).stream()
        .collect(toSet());

    private final Map<String, BinaryService.Resolver> resolvers = new HashMap<>();
    private final Map<String, IdentifierConfiguration> partitions = new HashMap<>();
    private final IdentifierService idService;

    /**
     * Create a binary service
     * @param idService the identifier service
     * @param partitions the identifier suppliers for each partition
     * @param resolvers the resolves
     */
    public DefaultBinaryService(final IdentifierService idService, final Map<String, Properties> partitions,
            final List<BinaryService.Resolver> resolvers) {
        this.idService = idService;
        resolvers.forEach(resolver ->
                resolver.getUriSchemes().forEach(scheme ->
                    this.resolvers.put(scheme, resolver)));
        partitions.forEach((k, v) -> {
            final String prefix = v.getProperty("prefix");
            if (isNull(prefix)) {
                throw new RuntimeRepositoryException("No prefix value defined for partition: " + k);
            }
            if (!this.resolvers.containsKey(prefix.split(":", 2)[0])) {
                throw new RuntimeRepositoryException("No binary resolver defined to handle prefix " +
                        prefix + " in partition " + k);
            }
            this.partitions.put(k, new IdentifierConfiguration(prefix,
                        parseInt(v.getProperty("levels", DEFAULT_LEVELS)),
                        parseInt(v.getProperty("length", DEFAULT_LENGTH))));
        });
    }

    @Override
    public Optional<BinaryService.Resolver> getResolver(final IRI identifier) {
        return of(identifier).map(IRI::getIRIString).map(URI::create).map(URI::getScheme).map(resolvers::get)
            .filter(Objects::nonNull);
    }

    @Override
    public Optional<BinaryService.Resolver> getResolverForPartition(final String partition) {
        return of(partition).filter(partitions::containsKey).map(partitions::get)
            .map(IdentifierConfiguration::getPrefix).map(prefix -> prefix.split(":", 2)[0])
            .filter(resolvers::containsKey).map(resolvers::get);
    }

    @Override
    public Optional<String> digest(final String algorithm, final InputStream stream) {
        if (SHA.equals(algorithm)) {
            return of(SHA_1).map(DigestUtils::getDigest).flatMap(digest(stream));
        }
        return ofNullable(algorithm).map(DigestUtils::getDigest).flatMap(digest(stream));
    }

    @Override
    public Set<String> supportedAlgorithms() {
        return algorithms;
    }

    @Override
    public Supplier<String> getIdentifierSupplier(final String partition) {
        if (partitions.containsKey(partition)) {
            final IdentifierConfiguration config = partitions.get(partition);
            return idService.getSupplier(config.getPrefix(), config.getHierarchy(), config.getLength());
        }
        throw new RuntimeRepositoryException("Invalid partition: " + partition);
    }

    private Function<MessageDigest, Optional<String>> digest(final InputStream stream) {
        return algorithm -> {
            try {
                final String digest = getEncoder().encodeToString(DigestUtils.updateDigest(algorithm, stream).digest());
                stream.close();
                return of(digest);
            } catch (final IOException ex) {
                LOGGER.error("Error computing digest: {}", ex.getMessage());
            }
            return empty();
        };
    }
}

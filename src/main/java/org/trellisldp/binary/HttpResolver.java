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

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_MULTIPLE_CHOICES;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.spi.BinaryService;
import org.trellisldp.spi.RuntimeRepositoryException;

/**
 * @author acoburn
 */
public class HttpResolver implements BinaryService.Resolver {

    private static final String UNSUPPORTED_MESSAGE = "HTTP Resolver does not support multipart uploads";

    private static final Logger LOGGER = getLogger(HttpResolver.class);

    private CloseableHttpClient httpClient = createPoolingHttpClient();

    /**
     * Create a pooling HTTP client
     * @return the http client
     *
     * <p>Note: The maximum connection count is 5 but can be overridden with a system property "http.maxConnections".
     * </p>
     */
    private static CloseableHttpClient createPoolingHttpClient() {
        return HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .useSystemProperties()
                .build();
    }

    /**
     * Create an HttpResolver using the default HTTP client
     */
    public HttpResolver() {
        this(createPoolingHttpClient());
    }

    /**
     * Create an HttpResolver with a provided client
     * @param client the client
     */
    public HttpResolver(final CloseableHttpClient client) {
        requireNonNull(client, "HTTP client may not be null!");
        httpClient = client;
    }

    @Override
    public List<String> getUriSchemes() {
        return unmodifiableList(new ArrayList<String>() { {
            add("http");
            add("https");
        }});
    }

    @Override
    public Boolean supportsMultipartUpload() {
        return false;
    }

    @Override
    public String initiateUpload(final String partition, final IRI identifier, final String mimeType) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public String uploadPart(final String identifier, final Integer partNumber, final InputStream content) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public Binary completeUpload(final String identifier, final Map<Integer, String> partDigests) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void abortUpload(final String identifier) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public Stream<Map.Entry<Integer, String>> listParts(final String identifier) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public Boolean exists(final String partition, final IRI identifier) {
        requireNonNull(identifier, "Identifier may not be null!");
        try {
            final HttpResponse res = httpClient.execute(new HttpHead(identifier.getIRIString()));
            return res.getStatusLine().getStatusCode() < SC_BAD_REQUEST;
        } catch (final IOException ex) {
            LOGGER.error("Error while checking for " + identifier.getIRIString() + ": " + ex.getMessage());
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public Optional<InputStream> getContent(final String partition, final IRI identifier) {
        requireNonNull(identifier,  "Identifier may not be null!");
        try {
            final HttpResponse res = httpClient.execute(new HttpGet(identifier.getIRIString()));
            final StatusLine status = res.getStatusLine();
            LOGGER.debug("HTTP GET Request to {} returned {} status: {}", identifier.getIRIString(),
                    status.getStatusCode(), status.getReasonPhrase());
            return ofNullable(res.getEntity().getContent());
        } catch (final IOException ex) {
            LOGGER.error("IO Error while fetching the content for " + identifier.getIRIString() +
                    ": " + ex.getMessage());
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void setContent(final String partition, final IRI identifier, final InputStream stream,
            final Map<String, String> metadata) {
        requireNonNull(identifier, "Identifier may not be null!");
        try {
            final HttpResponse res = httpClient.execute(new HttpPut(identifier.getIRIString()));
            final StatusLine status = res.getStatusLine();
            LOGGER.info("HTTP PUT Request to {} returned {} status: {}", identifier.getIRIString(),
                    status.getStatusCode(), status.getReasonPhrase());
            if (status.getStatusCode() >= SC_MULTIPLE_CHOICES) {
                throw new RuntimeRepositoryException("HTTP PUT request to " + identifier.getIRIString() +
                        " failed with a " + Integer.toString(status.getStatusCode()) + " " +
                        status.getReasonPhrase());
            }
        } catch (final IOException ex) {
            LOGGER.error("IO Error while setting the content for " + identifier.getIRIString() +
                    ": " + ex.getMessage());
            throw new UncheckedIOException(ex);
        }
    }
}

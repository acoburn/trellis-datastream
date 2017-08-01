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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.trellisldp.spi.RuntimeRepositoryException;

/**
 * @author acoburn
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpResolverTest {

    private final static RDF rdf = new SimpleRDF();

    private final static IRI resource = rdf.createIRI("http://acdc.amherst.edu/ontology/relationships.rdf");
    private final static IRI sslResource = rdf.createIRI("https://acdc.amherst.edu/ontology/relationships.rdf");
    private final static String partition = "partition";

    @Mock
    private CloseableHttpClient mockClient;

    @Mock
    private CloseableHttpResponse mockResponse;

    @Mock
    private StatusLine mockStatusLine;

    @Before
    public void setUp() throws IOException {
        when(mockClient.execute(any(HttpPut.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(201);
        when(mockStatusLine.getReasonPhrase()).thenReturn("CREATED");
    }

    @Test
    public void testExists() {

        final HttpResolver resolver = new HttpResolver();

        assertTrue(resolver.exists(partition, resource));
        assertFalse(resolver.exists(partition, rdf.createIRI("http://acdc.amherst.edu/ontology/foo.bar")));
    }

    @Test
    public void testGetContent() {
        final HttpResolver resolver = new HttpResolver();

        assertTrue(resolver.getContent(partition, resource).isPresent());
        assertTrue(resolver.getContent(partition, resource).map(this::uncheckedToString).get()
                .contains("owl:Ontology"));
    }

    @Test
    public void testGetSslContent() {
        final HttpResolver resolver = new HttpResolver();

        assertTrue(resolver.getContent(partition, sslResource).isPresent());
        assertTrue(resolver.getContent(partition, sslResource).map(this::uncheckedToString).get()
                .contains("owl:Ontology"));
    }

    @Test(expected = RuntimeRepositoryException.class)
    public void testSetContent() {
        final String contents = "A new resource";
        final HttpResolver resolver = new HttpResolver();

        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        resolver.setContent(partition, sslResource, inputStream);
    }

    @Test
    public void testMockedClient() throws IOException {
        final HttpResolver resolver = new HttpResolver(mockClient);
        final String contents = "A new resource";
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        resolver.setContent(partition, sslResource, inputStream);

        verify(mockClient).execute(any(HttpPut.class));
    }

    @Test
    public void testHttpSchemes() {
        final HttpResolver resolver = new HttpResolver();
        assertEquals(2L, resolver.getUriSchemes().size());
        assertTrue(resolver.getUriSchemes().contains("http"));
        assertTrue(resolver.getUriSchemes().contains("https"));
    }

    @Test(expected = UncheckedIOException.class)
    public void testExceptedPut() throws IOException {
        when(mockClient.execute(any(HttpPut.class))).thenThrow(new IOException("Expected Error"));
        final String contents = "A new resource";
        final HttpResolver resolver = new HttpResolver(mockClient);
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));

        resolver.setContent(partition, resource, inputStream);
    }

    @Test(expected = UncheckedIOException.class)
    public void testExceptedExists() throws IOException {
        when(mockClient.execute(any(HttpHead.class))).thenThrow(new IOException("Expected Error"));
        final HttpResolver resolver = new HttpResolver(mockClient);
        resolver.exists(partition, resource);
    }

    @Test(expected = UncheckedIOException.class)
    public void testExceptedGet() throws IOException {
        when(mockClient.execute(any(HttpGet.class))).thenThrow(new IOException("Expected Error"));
        final HttpResolver resolver = new HttpResolver(mockClient);
        resolver.getContent(partition, resource);
    }

    private String uncheckedToString(final InputStream is) {
        try {
            return IOUtils.toString(is, UTF_8);
        } catch (final IOException ex) {
            return null;
        }
    }
}

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
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;

import org.trellisldp.spi.BinaryService;
import org.trellisldp.spi.IdentifierService;
import org.trellisldp.spi.RuntimeRepositoryException;

/**
 * @author acoburn
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultBinaryServiceTest {

    private static final RDF rdf = new SimpleRDF();

    @Mock
    private InputStream mockInputStream;

    @Mock
    private IdentifierService mockIdService;

    @Mock
    private Supplier<String> mockSupplier;

    @Before
    public void setUp() throws IOException {
        when(mockIdService.getSupplier(anyString(), anyInt(), anyInt())).thenReturn(mockSupplier);
        when(mockInputStream.read(any(), anyInt(), anyInt())).thenThrow(new IOException("Expected Error"));
    }

    @Test
    public void testService() {
        final Properties props = new Properties();
        props.setProperty("prefix", "file:");
        final Map<String, Properties> config = new HashMap<>();
        config.put("repository", props);

        final BinaryService service = new DefaultBinaryService(mockIdService, config,
                asList(new FileResolver()));

        assertEquals(mockSupplier, service.getIdentifierSupplier("repository"));
    }

    @Test(expected = RuntimeRepositoryException.class)
    public void testServiceNoPrefix() {
        final Properties props = new Properties();
        final Map<String, Properties> config = new HashMap<>();
        config.put("repository", props);

        final BinaryService service = new DefaultBinaryService(mockIdService, config,
                asList(new FileResolver()));
    }

    @Test(expected = RuntimeRepositoryException.class)
    public void testServiceNoMatch() {
        final Properties props = new Properties();
        props.setProperty("prefix", "foo:");
        final Map<String, Properties> config = new HashMap<>();
        config.put("repository", props);

        final BinaryService service = new DefaultBinaryService(mockIdService, config,
                asList(new FileResolver()));
    }

    @Test(expected = RuntimeRepositoryException.class)
    public void testUnknownPartition() {
        final Properties props = new Properties();
        props.setProperty("prefix", "file:");
        final Map<String, Properties> config = new HashMap<>();
        config.put("repository", props);

        final BinaryService service = new DefaultBinaryService(mockIdService, config,
                asList(new FileResolver()));

        service.getIdentifierSupplier("nonexistent");
    }

    @Test
    public void testAlgorithms() {
        final Properties props = new Properties();
        props.setProperty("prefix", "file:");
        final Map<String, Properties> config = new HashMap<>();
        config.put("repository", props);

        final BinaryService service = new DefaultBinaryService(mockIdService, config,
                asList(new FileResolver()));

        assertTrue(service.supportedAlgorithms().contains("MD5"));
        assertTrue(service.supportedAlgorithms().contains("SHA-1"));
        assertTrue(service.supportedAlgorithms().contains("SHA-256"));
    }

    @Test
    public void testGetResolver() {
        final Properties props = new Properties();
        props.setProperty("prefix", "file:");
        final Map<String, Properties> config = new HashMap<>();
        config.put("repository", props);
        final BinaryService.Resolver resolver = new FileResolver();
        final IRI fileIRI = rdf.createIRI("file:a_file_resource");

        final BinaryService service = new DefaultBinaryService(mockIdService, config, asList(resolver));

        assertEquals(of(resolver), service.getResolver(fileIRI));
    }

    @Test
    public void testHexDigest() {
        final byte[] data = "Some data".getBytes(UTF_8);

        final Properties props = new Properties();
        props.setProperty("prefix", "file:");
        final Map<String, Properties> config = new HashMap<>();
        config.put("repository", props);

        final BinaryService service = new DefaultBinaryService(mockIdService, config,
                asList(new FileResolver()));
        assertEquals(of("5b82f8bf4df2bfb0e66ccaa7306fd024"), service.hexDigest("MD5", new ByteArrayInputStream(data)));
        assertEquals(of("8d72453f10079af3dfc7fcfc4109b1ed55e1839f"), service.hexDigest("SHA-1",
                    new ByteArrayInputStream(data)));
        assertEquals(empty(), service.hexDigest("MD5", mockInputStream));
    }

}

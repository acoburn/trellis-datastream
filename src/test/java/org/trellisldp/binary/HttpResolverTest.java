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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.Test;

/**
 * @author acoburn
 */
public class HttpResolverTest {

    private final static RDF rdf = new SimpleRDF();

    private final static IRI resource = rdf.createIRI("http://acdc.amherst.edu/ontology/relationships.rdf");

    @Test
    public void testExists() {

        final HttpResolver resolver = new HttpResolver();

        assertTrue(resolver.exists(resource));
        assertFalse(resolver.exists(rdf.createIRI("http://acdc.amherst.edu/ontology/foo.bar")));
    }

    @Test
    public void testGetContent() {
        final HttpResolver resolver = new HttpResolver();

        assertTrue(resolver.getContent(resource).isPresent());
        assertTrue(resolver.getContent(resource).map(this::uncheckedToString).get().contains("owl:Ontology"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetContent() {
        final String contents = "A new resource";
        final HttpResolver resolver = new HttpResolver();

        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        resolver.setContent(resource, inputStream);
    }

    @Test
    public void testHttpSchemes() {
        final HttpResolver resolver = new HttpResolver();
        assertEquals(2L, resolver.getUriSchemes().size());
        assertTrue(resolver.getUriSchemes().contains("http"));
        assertTrue(resolver.getUriSchemes().contains("https"));
    }

    private String uncheckedToString(final InputStream is) {
        try {
            return IOUtils.toString(is, UTF_8);
        } catch (final IOException ex) {
            return null;
        }
    }
}

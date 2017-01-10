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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.Test;

/**
 * @author acoburn
 */
public class FileResolverTest {

    private final static String testDoc = "/test.txt";

    private final static RDF rdf = new SimpleRDF();

    private final static IRI file = rdf.createIRI("file:test.txt");

    @Test
    public void testFileExists() {
        final File res = new File(FileResolver.class.getResource(testDoc).getPath());

        final FileResolver resolver = new FileResolver(res.getParent());
        assertTrue(resolver.exists(file));
        assertFalse(resolver.exists(rdf.createIRI("file:fake.txt")));
    }

    @Test
    public void testFileContent() {
        final File res = new File(FileResolver.class.getResource(testDoc).getPath());
        final FileResolver resolver = new FileResolver(res.getParent());
        assertTrue(resolver.getContent(file).isPresent());
        assertEquals("A test document.\n", resolver.getContent(file).map(this::uncheckedToString).get());
    }

    @Test
    public void testSetFileContent() {
        final String contents = "A new file";
        final File res = new File(FileResolver.class.getResource(testDoc).getPath());
        final FileResolver resolver = new FileResolver(res.getParent());
        final IRI fileIRI = rdf.createIRI("file:" + randomFilename());
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        resolver.setContent(fileIRI, inputStream, emptyMap());
        assertTrue(resolver.getContent(fileIRI).isPresent());
        assertEquals(contents, resolver.getContent(fileIRI).map(this::uncheckedToString).get());
    }

    @Test
    public void testFileSchemes() {
        final File res = new File(FileResolver.class.getResource(testDoc).getPath());
        final FileResolver resolver = new FileResolver(res.getParent());
        assertTrue(resolver.getUriSchemes().size() == 1);
        assertTrue(resolver.getUriSchemes().contains("file"));
    }

    private String uncheckedToString(final InputStream is) {
        try {
            return IOUtils.toString(is, UTF_8);
        } catch (final IOException ex) {
            return null;
        }
    }

    private static String randomFilename() {
        final SecureRandom random = new SecureRandom();
        final String filename = new BigInteger(50, random).toString(32);
        return filename + ".json";
    }
}

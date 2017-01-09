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

import java.io.InputStream;
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;
import edu.amherst.acdc.trellis.spi.DatastreamService;
import edu.amherst.acdc.trellis.api.Datastream;

/**
 * @author acoburn
 */
public class DefaultDatastreamService implements DatastreamService {


    /**
     * Create a datastream service
     */
    public DefaultDatastreamService() {

    }

    @Override
    public InputStream getContent(final IRI identifier) {
        return null;
    }

    @Override
    public Boolean exists(final IRI identifier) {
        return false;
    }

    @Override
    public void setContent(final IRI identifier, final InputStream stream, final String contentType) {

    }

    @Override
    public Optional<String> calculateDigest(final String algorithm) {
        return Optional.empty();
    }

    @Override
    public IRI generateIdentifier(final IRI identifier, final Datastream.StoragePartition partition) {
        return null;
    }
}

/*
 * Copyright (c) 2023 Robert Bosch Manufacturing Solutions GmbH
 *
 * See the AUTHORS file(s) distributed with this work for additional
 * information regarding authorship.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package examples;

// tag::imports[]
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openmanufacturing.sds.aspectmodel.generator.jsonschema.AspectModelJsonSchemaGenerator;
import io.openmanufacturing.sds.aspectmodel.resolver.AspectModelResolver;
import io.openmanufacturing.sds.metamodel.Aspect;
import io.openmanufacturing.sds.metamodel.loader.AspectModelLoader;
// end::imports[]
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class GenerateJsonSchema {
   @Test
   public void generate() throws IOException {
      final File modelFile = new File( "aspect-models/io.openmanufacturing.examples.movement/1.0.0/Movement.ttl" );

      // tag::generate[]
      // Aspect as created by the AspectModelLoader
      final Aspect aspect = // ...
            // end::generate[]
            // exclude the actual loading from the example to reduce noise
            AspectModelResolver.loadAndResolveModel( modelFile ).flatMap( AspectModelLoader::getSingleAspect ).get();
      // tag::generate[]

      final AspectModelJsonSchemaGenerator generator = new AspectModelJsonSchemaGenerator();
      final JsonNode jsonSchema = generator.apply( aspect, Locale.ENGLISH );

      // If needed, print or pretty print it into a string
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final ObjectMapper objectMapper = new ObjectMapper();

      objectMapper.writerWithDefaultPrettyPrinter().writeValue( out, jsonSchema );
      final String result = out.toString();
      // end::generate[]
   }
}

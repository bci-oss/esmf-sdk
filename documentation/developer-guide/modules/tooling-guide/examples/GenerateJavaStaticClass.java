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
import org.eclipse.esmf.aspectmodel.java.JavaCodeGenerationConfig;
import org.eclipse.esmf.aspectmodel.java.JavaCodeGenerationConfigBuilder;
import org.eclipse.esmf.aspectmodel.java.metamodel.StaticMetaModelJavaGenerator;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.metamodel.Aspect;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import java.io.File;
import org.apache.commons.io.output.NullOutputStream;
// end::imports[]
import org.junit.jupiter.api.Test;

public class GenerateJavaStaticClass {
   @Test
   public void generate() {
      final File modelFile = new File( "aspect-models/org.eclipse.esmf.examples.movement/1.0.0/Movement.ttl" );

      // tag::generate[]
      // Aspect as created by the AspectModelLoader
      final Aspect aspect = // ...
            // end::generate[]
            // exclude the actual loading from the example to reduce noise
            AspectModelResolver.loadAndResolveModel( modelFile ).flatMap( AspectModelLoader::getSingleAspect ).get();
      // tag::generate[]

      final JavaCodeGenerationConfig config = JavaCodeGenerationConfigBuilder.builder()
            .enableJacksonAnnotations( true )
            .packageName( "com.example.mycompany" ) // if left out, package is taken from Aspect's namespace
            .build();
      final StaticMetaModelJavaGenerator generator = new StaticMetaModelJavaGenerator( aspect, config );
      generator.generate( qualifiedName -> {
         // Create an output stream for the given qualified Java class name
         // end::generate[]
         return NullOutputStream.INSTANCE;
         // tag::generate[]
      } );
      // end::generate[]
   }
}

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

package org.eclipse.esmf.aspectmodel.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.esmf.test.TestAspect;
import org.eclipse.esmf.test.TestModel;
import org.eclipse.esmf.test.TestProperty;
import org.eclipse.esmf.test.TestResources;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PrettyPrinterTest {
   @ParameterizedTest
   @MethodSource( value = "testModels" )
   void testPrettyPrinter( final TestModel testModel ) {
      final AspectModel aspectModel = TestResources.load( testModel );
      final AspectModelFile originalFile = aspectModel.files().iterator().next();
      final String formattedModel = formatAspectModelFile( originalFile );
      final Model prettyPrintedModel = TurtleLoader.loadTurtle( formattedModel ).get();
      assertThat( RdfComparison.hash( originalFile.sourceModel() ).equals( RdfComparison.hash( prettyPrintedModel ) ) ).isTrue();
   }

   @Test
   void testPrintMultiLineDescription() {
      final AspectModel aspectModel = TestResources.load( TestAspect.ASPECT_WITH_MULTI_LINE_DESCRIPTION );
      final AspectModelFile originalFile = aspectModel.files().iterator().next();
      final String formattedModel = formatAspectModelFile( originalFile );
      assertThat( formattedModel ).contains( "\"\"\"" );
      final Model prettyPrintedModel = TurtleLoader.loadTurtle( formattedModel ).get();
      assertThat( RdfComparison.hash( originalFile.sourceModel() ).equals( RdfComparison.hash( prettyPrintedModel ) ) ).isTrue();
   }

   private static String formatAspectModelFile( final AspectModelFile originalFile ) {
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      final PrintWriter writer = new PrintWriter( buffer, false, StandardCharsets.UTF_8 );
      new PrettyPrinter( originalFile, writer ).print();
      writer.flush();
      return buffer.toString( StandardCharsets.UTF_8 );
   }

   static Stream<Arguments> testModels() {
      return Stream.concat( Arrays.stream( TestAspect.values() ), Arrays.stream( TestProperty.values() )
            ).filter( testModel ->
                  // contains blank nodes which are not referenced from an aspect and therefore not pretty-printed
                  testModel != TestAspect.MODEL_WITH_BLANK_AND_ADDITIONAL_NODES )
            .map( Arguments::arguments );
   }

   @Test
   void testPrintAspectWithAnyValueDeclarations() {
      final AspectModel aspectModel = TestResources.load( TestAspect.ASPECT_WITH_ANY_VALUE_DECLARATIONS );
      final AspectModelFile originalFile = aspectModel.files().iterator().next();
      final String formattedModel = formatAspectModelFile( originalFile );
      final Model prettyPrintedModel = TurtleLoader.loadTurtle( formattedModel ).get();
      assertThat( RdfComparison.hash( originalFile.sourceModel() ) ).isEqualTo( RdfComparison.hash( prettyPrintedModel ) );
   }

   @Test
   void testPrintPropertyWithValue() {
      final AspectModel aspectModel = TestResources.load( TestProperty.PROPERTY_WITH_VALUE );
      final AspectModelFile originalFile = aspectModel.files().iterator().next();
      final String formattedModel = formatAspectModelFile( originalFile );
      final Model prettyPrintedModel = TurtleLoader.loadTurtle( formattedModel ).get();
      assertThat( RdfComparison.hash( originalFile.sourceModel() ) ).isEqualTo( RdfComparison.hash( prettyPrintedModel ) );
   }
}

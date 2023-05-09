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

package org.eclipse.esmf.aspectmodel;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

public class GenerateJavaClassesTest extends AspectModelMojoTest {

   @Test
   public void testGenerateJavaClassesValidAspectModel() throws Exception {
      final File testPom = getTestFile( "src/test/resources/test-pom-valid-aspect-model-output-directory.xml" );
      final Mojo generateJavaClasses = lookupMojo( "generateJavaClasses", testPom );
      assertThatCode( generateJavaClasses::execute ).doesNotThrowAnyException();

      final String packagePath = "org/eclipse/esmf/test";
      assertGeneratedFileExists( "Aspect.java", packagePath );
      deleteGeneratedFile( "Aspect.java", packagePath );
   }

   @Test
   public void testGenerateJavaClassesMultipleAspectModels() throws Exception {
      final File testPom = getTestFile( "src/test/resources/generate-java-classes.xml" );
      final Mojo generateJavaClasses = lookupMojo( "generateJavaClasses", testPom );
      assertThatCode( generateJavaClasses::execute ).doesNotThrowAnyException();

      final String packagePath = "org/eclipse/esmf/test";
   }

   @Test
   public void testGenerateJavaClassesCustomPackageName() throws Exception {
      final File testPom = getTestFile( "src/test/resources/generate-java-classes-pom-custom-package-name.xml" );
      final Mojo generateJavaClasses = lookupMojo( "generateJavaClasses", testPom );
      assertThatCode( generateJavaClasses::execute ).doesNotThrowAnyException();

      final String packagePath = "example/com";
      assertGeneratedFileExists( "Aspect.java", packagePath );
      deleteGeneratedFile( "Aspect.java", packagePath );
   }

   @Test
   public void testGenerateJavaClassesInvalidTemplateLibFile() throws Exception {
      final File testPom = getTestFile( "src/test/resources/generate-java-classes-pom-invalid-template-lib-file.xml" );
      final Mojo generateJavaClasses = lookupMojo( "generateJavaClasses", testPom );
      assertThatCode( generateJavaClasses::execute )
            .isInstanceOf( MojoExecutionException.class )
            .hasMessage( "Missing configuration. Valid path to velocity template library file must be provided." );
   }

}

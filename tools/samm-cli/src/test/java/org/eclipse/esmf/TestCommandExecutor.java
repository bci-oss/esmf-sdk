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

package org.eclipse.esmf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.eclipse.esmf.aspectmodel.resolver.CommandExecutor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class TestCommandExecutor {
   @EnabledOnOs( OS.WINDOWS )
   @Test
   void testCmdScript() {
      final URL url = Thread.currentThread().getContextClassLoader().getResource( "executor_test.bat" );
      final String result = CommandExecutor.executeCommand( url.getPath() );
      assertThat( "Result" ).isEqualTo( result );
   }

   @Test
   void testJarExecution() throws URISyntaxException, IOException {
      final Path targetDirectory = Paths.get( getClass().getResource( "/" ).toURI() ).getParent();
      final Path testsJar = findTestsJar( targetDirectory );
      final String result = CommandExecutor.executeCommand( testsJar.toString() );
      assertThat( "Result" ).isEqualTo( result );
   }

   @Test
   void testJavaExecution() throws IOException, URISyntaxException {
      final Path targetDirectory = Paths.get( getClass().getResource( "/" ).toURI() ).getParent();
      final Path testsJar = findTestsJar( targetDirectory );
      final String java = ProcessHandle.current().info().command().orElse( "java" );
      final String result = CommandExecutor.executeCommand( java + " -jar " + testsJar );
      assertThat( "Result" ).isEqualTo( result );
   }

   @Test
   void resolve() throws URISyntaxException {
      DelegatingCommandResolver.main( new String[] { "urn:samm:org.eclipse.esmf.test:1.0.0#AspectWithEntity" } );
   }

   @EnabledOnOs( OS.LINUX )
   @Test
   void testShellScript() {
      final URL url = Thread.currentThread().getContextClassLoader().getResource( "executor_test.sh" );
      final String result = CommandExecutor.executeCommand( url.getPath() );
      assertThat( "Result" ).isEqualTo( result );
   }

   private Path findTestsJar( final Path searchDirectory ) throws IOException {
      try ( final Stream<Path> files = Files.walk( searchDirectory, 1 ) ) {
         return files
               .filter( f -> f.getFileName().toString().endsWith( "-tests.jar" ) )
               .findFirst()
               .get();
      }
   }
}

/*
 * Copyright (c) 2021 Robert Bosch Manufacturing Solutions GmbH
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

package io.openmanufacturing.sds.test.shared.compiler;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import io.openmanufacturing.sds.aspectmodel.java.QualifiedName;

public class JavaCompiler {
   /**
    * In order to ease debugging of unit tests that generate code, run the corresponding JVM with
    * -Dio.openmanfacturing.javacompiler.writesources=true
    * This will write the generated sources into files in src/test/java.
    */
   public static final String WRITE_SOURCES_PROPERTY = "io.openmanfacturing.javacompiler.writesources";

   private static class DiagnosticListener implements javax.tools.DiagnosticListener<FileObject> {
      @Override
      public void report( final Diagnostic<? extends FileObject> diagnostic ) {
         System.out.println( diagnostic );
      }
   }

   public static Map<QualifiedName, Class<?>> compile( final List<QualifiedName> loadOrder,
         final Map<QualifiedName, String> sources, final List<String> predefinedClasses ) {
      final javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      final InMemoryClassFileManager manager = new InMemoryClassFileManager(
            compiler.getStandardFileManager( null, null, null ) );

      final List<JavaFileObject> compilerInput = loadOrder.stream()
            .map( key -> new CompilerInput( key.toString(), sources.get( key ) ) )
            .collect( Collectors.toList() );

      if ( System.getProperty( WRITE_SOURCES_PROPERTY ) != null ) {
         final String filepath = System.getProperty( "user.dir" ) + "/src/test/java/io/openmanufacturing/test/";
         final File outputdir = new File( filepath );
         if ( !outputdir.exists() && !outputdir.mkdirs() ) {
            throw new RuntimeException( "Could not create sources output directory " + outputdir );
         }
         for ( final Map.Entry<QualifiedName, String> entry : sources.entrySet() ) {
            final String filename = entry.getKey().getClassName();
            final File out = new File( filepath + filename + ".java" );
            try {
               final FileOutputStream outputStream = new FileOutputStream( out );
               outputStream.write( entry.getValue().getBytes( StandardCharsets.UTF_8 ) );
               outputStream.flush();
               outputStream.close();
            } catch ( final IOException e ) {
               throw new RuntimeException( e );
            }
         }
      }

      final DiagnosticListener diagnosticListener = new DiagnosticListener() {
         @Override
         public void report( final Diagnostic<? extends FileObject> diagnostic ) {
            System.out.println( sources );
            fail( "Compilation failed: " + diagnostic );
         }
      };

      final List<String> compilerOptions = List.of( "-classpath", System.getProperty( "java.class.path" ) );
      compiler.getTask( null, manager, diagnosticListener, compilerOptions, null, compilerInput ).call();
      return loadOrder.stream().collect( Collectors.toMap( Function.identity(), qualifiedName ->
            defineAndLoad( qualifiedName, manager ) ) );
   }

   @SuppressWarnings( "unchecked" )
   private static <T> Class<T> defineAndLoad( final QualifiedName className, final InMemoryClassFileManager manager ) {
      try {
         return (Class<T>) new ClassLoader() {
            @Override
            protected Class<?> findClass( final String name ) {
               final byte[] classBytes = manager.getOutput( name ).getBytes();
               return defineClass( name, classBytes, 0, classBytes.length );
            }
         }.loadClass( className.toString() );
      } catch ( final ClassNotFoundException exception ) {
         throw new RuntimeException( exception );
      }
   }
}

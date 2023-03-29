/*
 * Copyright (c) 2022 Robert Bosch Manufacturing Solutions GmbH
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

package io.openmanufacturing.sds.aspect.to;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import io.openmanufacturing.sds.AbstractCommand;
import io.openmanufacturing.sds.ExternalResolverMixin;
import io.openmanufacturing.sds.aspect.AspectToCommand;
import io.openmanufacturing.sds.aspectmodel.java.JavaCodeGenerationConfig;
import io.openmanufacturing.sds.aspectmodel.java.JavaCodeGenerationConfigBuilder;
import io.openmanufacturing.sds.aspectmodel.java.JavaGenerator;
import io.openmanufacturing.sds.aspectmodel.java.metamodel.StaticMetaModelJavaGenerator;
import io.openmanufacturing.sds.aspectmodel.java.pojo.AspectModelJavaGenerator;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.openmanufacturing.sds.exception.CommandException;
import io.openmanufacturing.sds.metamodel.Aspect;
import io.openmanufacturing.sds.metamodel.AspectContext;
import picocli.CommandLine;

@CommandLine.Command( name = AspectToJavaCommand.COMMAND_NAME,
      description = "Generate Java domain classes for an Aspect Model",
      descriptionHeading = "%n@|bold Description|@:%n%n",
      parameterListHeading = "%n@|bold Parameters|@:%n",
      optionListHeading = "%n@|bold Options|@:%n",
      mixinStandardHelpOptions = true
)
public class AspectToJavaCommand extends AbstractCommand {

   public static final String COMMAND_NAME = "java";

   @CommandLine.Option( names = { "--no-jackson", "-nj" }, description = "Disable Jackson annotation generation in generated Java classes." )
   private boolean disableJacksonAnnotations = false;

   @CommandLine.Option( names = { "--template-library-file", "-tlf" },
         description = "The path and name of the Velocity template file containing the macro library." )
   private String templateLib = "";

   @CommandLine.Option( names = { "--package-name", "-pn" }, description = "Package to use for generated Java classes" )
   private String packageName = "";

   @CommandLine.Option( names = { "--execute-library-macros", "-elm" }, description = "Execute the macros provided in the Velocity macro library." )
   private boolean executeLibraryMacros = false;

   @CommandLine.Option( names = { "--output-directory", "-d" }, description = "Output directory to write files to" )
   private String outputPath = ".";

   @CommandLine.Option( names = { "--static", "-s" }, description = "Generate Java domain classes for a Static Meta Model" )
   private boolean generateStaticMetaModelJavaClasses = false;

   @CommandLine.ParentCommand
   private AspectToCommand parentCommand;

   @CommandLine.Mixin
   private ExternalResolverMixin customResolver;

   @Override
   public void run() {
      final AspectContext context = loadModelOrFail( parentCommand.parentCommand.getInput(), customResolver );
      final JavaGenerator javaGenerator = generateStaticMetaModelJavaClasses ? getStaticModelGenerator( context ) : getModelGenerator( context );
      javaGenerator.generate( artifact -> {
         final String path = artifact.getPackageName();
         final String fileName = artifact.getClassName();
         return getStreamForFile( path.replace( '.', File.separatorChar ), fileName + ".java", outputPath );
      } );
   }

   private JavaCodeGenerationConfig buildConfig( final Aspect aspect ) {
      final File templateLibFile = Path.of( templateLib ).toFile();
      final String pkgName = Optional.ofNullable( packageName )
            .flatMap( pkg -> pkg.isBlank() ? Optional.empty() : Optional.of( pkg ) )
            .or( () -> aspect.getAspectModelUrn().map( AspectModelUrn::getNamespace ) )
            .orElseThrow( () -> new CommandException( "Could not determine Aspect's namespace" ) );
      return JavaCodeGenerationConfigBuilder.builder()
            .executeLibraryMacros( executeLibraryMacros )
            .templateLibFile( templateLibFile )
            .enableJacksonAnnotations( !disableJacksonAnnotations )
            .packageName( pkgName )
            .build();
   }

   private JavaGenerator getStaticModelGenerator( final AspectContext context ) {
      return new StaticMetaModelJavaGenerator( context.aspect(), buildConfig( context.aspect() ) );
   }

   private JavaGenerator getModelGenerator( final AspectContext context ) {
      return new AspectModelJavaGenerator( context.aspect(), buildConfig( context.aspect() ) );
   }
}
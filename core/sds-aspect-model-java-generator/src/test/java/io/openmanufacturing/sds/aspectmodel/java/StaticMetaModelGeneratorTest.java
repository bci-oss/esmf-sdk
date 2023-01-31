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

package io.openmanufacturing.sds.aspectmodel.java;

import java.io.File;
import java.util.Collection;
import java.util.List;

import io.openmanufacturing.sds.aspectmetamodel.KnownVersion;
import io.openmanufacturing.sds.aspectmodel.java.metamodel.StaticMetaModelJavaGenerator;
import io.openmanufacturing.sds.aspectmodel.java.pojo.AspectModelJavaGenerator;
import io.openmanufacturing.sds.aspectmodel.resolver.services.VersionedModel;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.openmanufacturing.sds.metamodel.Aspect;
import io.openmanufacturing.sds.metamodel.loader.AspectModelLoader;
import io.openmanufacturing.sds.test.MetaModelVersions;
import io.openmanufacturing.sds.test.TestAspect;
import io.openmanufacturing.sds.test.TestResources;
import io.openmanufacturing.sds.test.TestSharedAspect;

abstract class StaticMetaModelGeneratorTest extends MetaModelVersions {
   Collection<JavaGenerator> getGenerators( final TestAspect testAspect, final KnownVersion version, final boolean executeLibraryMacros,
         final File templateLibFile ) {
      final VersionedModel model = TestResources.getModel( testAspect, version ).get();
      final Aspect aspect = AspectModelLoader.getSingleAspectUnchecked( model );
      final JavaCodeGenerationConfig config = JavaCodeGenerationConfigBuilder.builder()
            .enableJacksonAnnotations( false )
            .executeLibraryMacros( executeLibraryMacros )
            .templateLibFile( templateLibFile )
            .packageName( aspect.getAspectModelUrn().map( AspectModelUrn::getNamespace ).get() )
            .build();
      final JavaGenerator pojoGenerator = new AspectModelJavaGenerator( aspect, config );
      final JavaGenerator staticGenerator = new StaticMetaModelJavaGenerator( aspect, config );
      return List.of( pojoGenerator, staticGenerator );
   }

   Collection<JavaGenerator> getGenerators( final TestAspect testAspect, final KnownVersion version ) {
      final VersionedModel model = TestResources.getModel( testAspect, version ).get();
      final Aspect aspect = AspectModelLoader.getSingleAspectUnchecked( model );
      final JavaCodeGenerationConfig config = JavaCodeGenerationConfigBuilder.builder()
            .enableJacksonAnnotations( false )
            .executeLibraryMacros( false )
            .packageName( aspect.getAspectModelUrn().map( AspectModelUrn::getNamespace ).get() )
            .build();
      final JavaGenerator pojoGenerator = new AspectModelJavaGenerator( aspect, config );
      final JavaGenerator staticGenerator = new StaticMetaModelJavaGenerator( aspect, config );
      return List.of( pojoGenerator, staticGenerator );
   }

   Collection<JavaGenerator> getGenerators( final TestSharedAspect testAspect, final KnownVersion version ) {
      final VersionedModel model = TestResources.getModel( testAspect, version ).get();
      final Aspect aspect = AspectModelLoader.getSingleAspectUnchecked( model );
      final JavaCodeGenerationConfig config = JavaCodeGenerationConfigBuilder.builder()
            .enableJacksonAnnotations( false )
            .executeLibraryMacros( false )
            .packageName( aspect.getAspectModelUrn().map( AspectModelUrn::getNamespace ).get() )
            .build();
      final JavaGenerator pojoGenerator = new AspectModelJavaGenerator( aspect, config );
      final JavaGenerator staticGenerator = new StaticMetaModelJavaGenerator( aspect, config );
      return List.of( pojoGenerator, staticGenerator );
   }
}

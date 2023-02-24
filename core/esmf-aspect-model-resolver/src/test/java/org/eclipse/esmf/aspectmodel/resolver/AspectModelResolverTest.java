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

package org.eclipse.esmf.aspectmodel.resolver;

import static org.assertj.vavr.api.VavrAssertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.assertj.core.api.Assertions;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.vocabulary.SAMM;
import org.eclipse.esmf.samm.KnownVersion;
import org.eclipse.esmf.test.MetaModelVersions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.Streams;

import io.vavr.control.Try;

public class AspectModelResolverTest extends MetaModelVersions {
   private final AspectModelResolver resolver = new AspectModelResolver();

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testLoadDataModelExpectSuccess( final KnownVersion metaModelVersion ) throws URISyntaxException {
      final File aspectModelsRootDirectory = new File(
            AspectModelResolverTest.class.getClassLoader()
                  .getResource( metaModelVersion.toString().toLowerCase() )
                  .toURI()
                  .getPath() );

      final AspectModelUrn testUrn = AspectModelUrn.fromUrn( "urn:samm:org.eclipse.esmf.test:1.0.0#Test" );

      final ResolutionStrategy urnStrategy = new FileSystemStrategy( aspectModelsRootDirectory.toPath() );
      final Try<VersionedModel> result = resolver.resolveAspectModel( urnStrategy, testUrn );
      assertThat( result ).isSuccess();

      final Resource aspect = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.test:1.0.0#Test" );
      final Resource sammAspect = ResourceFactory.createResource(
            "urn:samm:org.eclipse.esmf.samm:meta-model:" + metaModelVersion.toVersionString() + "#Aspect" );
      org.assertj.core.api.Assertions
            .assertThat( result.get().getModel().listStatements( aspect, RDF.type, sammAspect ).nextOptional() )
            .isNotEmpty();
   }

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testLoadModelWithVersionEqualToUnsupportedMetaModelVersionExpectSuccess(
         final KnownVersion metaModelVersion ) throws URISyntaxException {
      final File aspectModelsRootDirectory = new File(
            AspectModelResolverTest.class.getClassLoader()
                  .getResource( metaModelVersion.toString().toLowerCase() )
                  .toURI()
                  .getPath() );

      final AspectModelUrn testUrn = AspectModelUrn.fromUrn( "urn:samm:org.eclipse.esmf.test:1.1.0#Test" );

      final ResolutionStrategy urnStrategy = new FileSystemStrategy( aspectModelsRootDirectory.toPath() );
      final Try<VersionedModel> result = resolver.resolveAspectModel( urnStrategy, testUrn );
      assertThat( result ).isSuccess();

      final Resource aspect = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.test:1.1.0#Test" );
      final Resource sammAspect = ResourceFactory.createResource(
            "urn:samm:org.eclipse.esmf.samm:meta-model:" + metaModelVersion.toVersionString() + "#Aspect" );
      org.assertj.core.api.Assertions
            .assertThat( result.get().getModel().listStatements( aspect, RDF.type, sammAspect ).nextOptional() )
            .isNotEmpty();
   }

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testResolveReferencedModelFromMemoryExpectSuccess( final KnownVersion metaModelVersion ) throws URISyntaxException {
      final File aspectModelsRootDirectory = new File(
            AspectModelResolverTest.class.getClassLoader().getResource( metaModelVersion.toString().toLowerCase() )
                  .toURI().getPath() );

      final ResolutionStrategy urnStrategy = new FileSystemStrategy( aspectModelsRootDirectory.toPath() );
      final AspectModelUrn inputUrn = AspectModelUrn
            .fromUrn( "urn:samm:org.eclipse.esmf.test:1.0.0#AnotherTest" );
      final Model model = TurtleLoader.loadTurtle(
            AspectModelResolverTest.class.getResourceAsStream(
                  "/" + metaModelVersion.toString().toLowerCase()
                        + "/org.eclipse.esmf.test/1.0.0/Test.ttl" ) ).get();
      final ResolutionStrategy inMemoryStrategy = anyUrn -> Try.success( model );
      final EitherStrategy inMemoryResolutionStrategy = new EitherStrategy( urnStrategy, inMemoryStrategy );

      final Try<VersionedModel> result = resolver.resolveAspectModel( inMemoryResolutionStrategy, inputUrn );
      assertThat( result ).isSuccess();

      final Resource aspect = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.test:1.0.0#AnotherTest" );
      final Resource sammAspect = ResourceFactory
            .createResource(
                  "urn:samm:org.eclipse.esmf.samm:meta-model:" + metaModelVersion.toVersionString()
                        + "#Aspect" );
      org.assertj.core.api.Assertions
            .assertThat( result.get().getModel().listStatements( aspect, RDF.type, sammAspect ).nextOptional() )
            .isNotEmpty();

      final Resource propertyFromReferencedAspect = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.test:1.0.0#foo" );
      final Resource sammProperty = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.samm:meta-model:" + metaModelVersion.toVersionString()
                  + "#Property" );
      org.assertj.core.api.Assertions
            .assertThat(
                  result.get().getModel().listStatements( propertyFromReferencedAspect, RDF.type, sammProperty )
                        .nextOptional() ).isNotEmpty();
   }

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testResolveReferencedModelExpectSuccess( final KnownVersion metaModelVersion ) throws URISyntaxException {
      final File aspectModelsRootDirectory = new File(
            AspectModelResolverTest.class.getClassLoader().getResource( metaModelVersion.toString().toLowerCase() )
                  .toURI().getPath() );

      final AspectModelUrn testUrn = AspectModelUrn
            .fromUrn( "urn:samm:org.eclipse.esmf.test:1.0.0#AnotherTest" );

      final ResolutionStrategy urnStrategy = new FileSystemStrategy( aspectModelsRootDirectory.toPath() );

      final Try<VersionedModel> result = resolver.resolveAspectModel( urnStrategy, testUrn );
      assertThat( result ).isSuccess();

      final Resource aspect = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.test:1.0.0#AnotherTest" );
      final Resource sammAspect = ResourceFactory
            .createResource(
                  "urn:samm:org.eclipse.esmf.samm:meta-model:" + metaModelVersion.toVersionString()
                        + "#Aspect" );
      org.assertj.core.api.Assertions
            .assertThat( result.get().getModel().listStatements( aspect, RDF.type, sammAspect ).nextOptional() )
            .isNotEmpty();

      final Resource propertyFromReferencedAspect = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.test:1.0.0#foo" );
      final Resource sammProperty = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.samm:meta-model:" + metaModelVersion.toVersionString()
                  + "#Property" );
      org.assertj.core.api.Assertions
            .assertThat(
                  result.get().getModel().listStatements( propertyFromReferencedAspect, RDF.type, sammProperty )
                        .nextOptional() ).isNotEmpty();
   }

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testResolutionMissingAspectExpectFailure( final KnownVersion metaModelVersion ) throws URISyntaxException {
      final File aspectModelsRootDirectory = new File(
            AspectModelResolverTest.class.getClassLoader()
                  .getResource( metaModelVersion.toString().toLowerCase() )
                  .toURI()
                  .getPath() );

      final AspectModelUrn testUrn = AspectModelUrn
            .fromUrn( "urn:samm:org.eclipse.esmf.test:1.0.0#AnotherFailingTest" );

      final ResolutionStrategy urnStrategy = new FileSystemStrategy( aspectModelsRootDirectory.toPath() );
      final Try<VersionedModel> result = resolver.resolveAspectModel( urnStrategy, testUrn );
      assertThat( result ).isFailure();
      assertThat( result ).failBecauseOf( ModelResolutionException.class );
   }

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testResolutionMissingModelElementExpectFailure( final KnownVersion metaModelVersion ) throws Throwable {
      final File aspectModelsRootDirectory = new File(
            AspectModelResolverTest.class.getClassLoader().getResource( metaModelVersion.toString().toLowerCase() )
                  .toURI().getPath() );

      final AspectModelUrn testUrn = AspectModelUrn
            .fromUrn( "urn:samm:org.eclipse.esmf.test:1.0.0#FailingTest" );

      final ResolutionStrategy urnStrategy = new FileSystemStrategy( aspectModelsRootDirectory.toPath() );
      final Try<VersionedModel> result = resolver.resolveAspectModel( urnStrategy, testUrn );
      assertThat( result ).isFailure();
      assertThat( result ).failBecauseOf( ModelResolutionException.class );
   }

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testResolutionReferencedCharacteristicExpectSuccess( final KnownVersion metaModelVersion ) throws URISyntaxException {
      final File aspectModelsRootDirectory = new File(
            AspectModelResolverTest.class.getClassLoader().getResource( metaModelVersion.toString().toLowerCase() )
                  .toURI().getPath() );

      final AspectModelUrn testUrn = AspectModelUrn
            .fromUrn( "urn:samm:org.eclipse.esmf.test:1.0.0#ReferenceCharacteristicTest" );

      final ResolutionStrategy urnStrategy = new FileSystemStrategy(
            aspectModelsRootDirectory.toPath() );

      final Try<VersionedModel> result = resolver.resolveAspectModel( urnStrategy, testUrn );
      assertThat( result ).isSuccess();

      final Resource aspect = ResourceFactory
            .createResource(
                  "urn:samm:org.eclipse.esmf.test:1.0.0#ReferenceCharacteristicTest" );
      final Resource sammAspect = ResourceFactory
            .createResource(
                  "urn:samm:org.eclipse.esmf.samm:meta-model:" + metaModelVersion.toVersionString()
                        + "#Aspect" );
      org.assertj.core.api.Assertions
            .assertThat( result.get().getModel().listStatements( aspect, RDF.type, sammAspect ).nextOptional() )
            .isNotEmpty();

      final Resource referencedCharacteristic = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.test:1.0.0#TestCharacteristic" );
      final Resource sammCharacteristic = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.samm:meta-model:" + metaModelVersion.toVersionString()
                  + "#Characteristic" );
      org.assertj.core.api.Assertions
            .assertThat(
                  result.get().getModel().listStatements( referencedCharacteristic, RDF.type, sammCharacteristic )
                        .nextOptional() ).isNotEmpty();
   }

   /**
    * This test checks that if the same shared resource (in this case: the shared TestCharacteristic) is
    * transitively imported on multiple paths through the dependency graph, it is still only added once to the
    * final merged model, so for example the Statement x:testCharacteristic samm:name "testCharacteristic" is
    * only present once in the model. Here, TransitiveReferenceTest references both the Test Characteristic
    * and a second Aspect model, ReferenceCharacteristicTest, which in turn also references the Test Characteristic.
    *
    * @throws Throwable if one of the resources is not found
    */
   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testTransitiveReferenceExpectSuccess( final KnownVersion metaModelVersion ) throws Throwable {
      final File aspectModelsRootDirectory = new File(
            AspectModelResolverTest.class.getClassLoader().getResource( metaModelVersion.toString().toLowerCase() )
                  .toURI().getPath() );

      final AspectModelUrn testUrn = AspectModelUrn
            .fromUrn( "urn:samm:org.eclipse.esmf.test:1.0.0#TransitiveReferenceTest" );

      final ResolutionStrategy urnStrategy = new FileSystemStrategy( aspectModelsRootDirectory.toPath() );
      final Try<VersionedModel> result = resolver.resolveAspectModel( urnStrategy, testUrn );
      assertThat( result ).isSuccess();

      final Model model = result.get().getModel();
      final Resource testCharacteristic = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.test:1.0.0#TestCharacteristic" );
      org.assertj.core.api.Assertions.assertThat(
                  Streams.stream( model.listStatements( testCharacteristic, RDF.type, (RDFNode) null ) ).count() )
            .isEqualTo( 1 );
   }

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testResolutionReferencedEntity( final KnownVersion metaModelVersion ) throws URISyntaxException {
      final File aspectModelsRootDirectory = new File(
            AspectModelResolverTest.class.getClassLoader().getResource( metaModelVersion.toString().toLowerCase() )
                  .toURI().getPath() );

      final AspectModelUrn testUrn = AspectModelUrn
            .fromUrn( "urn:samm:org.eclipse.esmf.test:1.0.0#ReferenceEntityTest" );

      final ResolutionStrategy urnStrategy = new FileSystemStrategy(
            aspectModelsRootDirectory.toPath() );

      final Try<VersionedModel> result = resolver.resolveAspectModel( urnStrategy, testUrn );
      assertThat( result ).isSuccess();

      final Resource aspect = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.test:1.0.0#ReferenceEntityTest" );
      final Resource sammAspect = ResourceFactory
            .createResource(
                  "urn:samm:org.eclipse.esmf.samm:meta-model:" + metaModelVersion.toVersionString()
                        + "#Aspect" );
      org.assertj.core.api.Assertions
            .assertThat( result.get().getModel().listStatements( aspect, RDF.type, sammAspect ).nextOptional() )
            .isNotEmpty();

      final Resource referencedEntity = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.test:1.0.0#TestEntity" );
      final Resource sammEntity = ResourceFactory
            .createResource( "urn:samm:org.eclipse.esmf.samm:meta-model:" + metaModelVersion.toVersionString() + "#Entity" );
      org.assertj.core.api.Assertions
            .assertThat( result.get().getModel().listStatements( referencedEntity, RDF.type, sammEntity ).nextOptional() ).isNotEmpty();
   }

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testAspectReferencingAnotherAspectExpectSuccess( final KnownVersion metaModelVersion ) throws URISyntaxException {
      final File aspectModelsRootDirectory = new File(
            AspectModelResolverTest.class.getClassLoader().getResource( metaModelVersion.toString().toLowerCase() )
                  .toURI().getPath() );

      final String aspectUrn = "urn:samm:org.eclipse.esmf.test:2.0.0#Test";
      final AspectModelUrn testUrn = AspectModelUrn.fromUrn( aspectUrn );

      final ResolutionStrategy urnStrategy = new FileSystemStrategy( aspectModelsRootDirectory.toPath() );
      final Try<VersionedModel> result = resolver.resolveAspectModel( urnStrategy, testUrn );
      assertThat( result ).isSuccess();

      final Model model = result.get().getModel();
      final SAMM samm = new SAMM( metaModelVersion );
      org.assertj.core.api.Assertions.assertThat(
            Streams.stream( model.listStatements( null, RDF.type, samm.Aspect() ) ).count() ).isEqualTo( 2 );
   }

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testClassPathResolution( final KnownVersion metaModelVersion ) {
      final String aspectUrn = "urn:samm:org.eclipse.esmf.test:1.0.0#Test";
      final AspectModelResolver resolver = new AspectModelResolver();
      final ClasspathStrategy strategy = new ClasspathStrategy( metaModelVersion.toString().toLowerCase() );
      final Try<VersionedModel> result = resolver
            .resolveAspectModel( strategy, AspectModelUrn.fromUrn( aspectUrn ) );
      Assertions.assertThat( result.isSuccess() ).isTrue();
   }

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testResolveAspectContainingRefinedProperty2( final KnownVersion metaModelVersion ) {
      final String aspectUrn = "urn:samm:org.eclipse.esmf.test:1.0.0#ReferenceCharacteristicTest";
      final AspectModelResolver resolver = new AspectModelResolver();
      final ClasspathStrategy strategy = new ClasspathStrategy( metaModelVersion.toString().toLowerCase() );
      final Try<VersionedModel> result = resolver
            .resolveAspectModel( strategy, AspectModelUrn.fromUrn( aspectUrn ) );
      Assertions.assertThat( result.isSuccess() ).describedAs( "Resolution of refined Property failed." ).isTrue();
   }

   @ParameterizedTest
   @MethodSource( value = "versionsStartingWith2_0_0" )
   public void testMergingModelsWithBlankNodeValues( final KnownVersion metaModelVersion ) {
      final String aspectUrn = "urn:samm:org.eclipse.esmf.test:1.0.0#SecondaryAspect";
      final AspectModelResolver resolver = new AspectModelResolver();
      final ClasspathStrategy strategy = new ClasspathStrategy( metaModelVersion.toString().toLowerCase() );
      final Try<VersionedModel> result = resolver
            .resolveAspectModel( strategy, AspectModelUrn.fromUrn( aspectUrn ) );
      Assertions.assertThat( result.isSuccess() ).isTrue();

      final Model model = result.get().getModel();
      final Resource primaryAspect = model.createResource( "urn:samm:org.eclipse.esmf.test:1.0.0#PrimaryAspect" );
      final SAMM samm = new SAMM( metaModelVersion );
      final List<Statement> propertiesAssertions = model.listStatements( primaryAspect, samm.properties(), (RDFNode) null ).toList();
      Assertions.assertThat( propertiesAssertions.size() ).isEqualTo( 1 );
   }

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   public void testMultiReferenceSameSource( final KnownVersion metaModelVersion ) throws URISyntaxException {
      final File aspectModelsRootDirectory = new File(
            AspectModelResolverTest.class.getClassLoader()
                  .getResource( metaModelVersion.toString().toLowerCase() )
                  .toURI()
                  .getPath() );

      final AspectModelUrn testUrn = AspectModelUrn.fromUrn( "urn:samm:org.eclipse.esmf.test:1.0.0#VehicleInstance" );

      final ResolutionStrategy urnStrategy = new FileSystemStrategy( aspectModelsRootDirectory.toPath() );
      final Try<VersionedModel> result = resolver.resolveAspectModel( urnStrategy, testUrn );
      assertThat( result ).isSuccess();

      // make sure the source file for the definitions of ModelYear and ModelCode (ModelDef.ttl) is only loaded once
      final Model model = result.get().getModel();
      final Resource entity = model.createResource( "urn:samm:org.eclipse.esmf.test:1.0.0#SomeOtherNonRelatedEntity" );
      final SAMM samm = new SAMM( metaModelVersion );
      final List<Statement> properties = model.listStatements( entity, samm.properties(), (RDFNode) null ).toList();
      Assertions.assertThat( properties.size() ).isEqualTo( 1 );
   }
}

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

package org.eclipse.esmf.metamodel.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.InvalidModelException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.InvalidNamespaceException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.InvalidRootElementCountException;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.versionupdate.MigratorService;
import org.eclipse.esmf.aspectmodel.vocabulary.SammNs;
import org.eclipse.esmf.metamodel.Aspect;
import org.eclipse.esmf.metamodel.ModelElement;
import org.eclipse.esmf.metamodel.ModelNamespace;
import org.eclipse.esmf.metamodel.impl.DefaultModelNamespace;
import org.eclipse.esmf.samm.KnownVersion;

import com.google.common.collect.ImmutableSet;
import io.vavr.control.Try;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides functionality to load an Aspect Model from a {@link VersionedModel} and use the correct SAMM resources to
 * instantiate it. To load a regular Aspect Model, use {@link #getElements(VersionedModel)} or
 * {@link #getElementsUnchecked(VersionedModel)}.
 * To load elements from an RDF model that might contain elements from multiple namespaces, use {@link #getNamespaces(VersionedModel)}.
 * Instances of {@code VersionedModel} are gained through an {@link AspectModelResolver}.
 */
public class AspectModelLoader {
   private static final Logger LOG = LoggerFactory.getLogger( AspectModelLoader.class );

   private static final Set<KnownVersion> SUPPORTED_VERSIONS = ImmutableSet.of(
         KnownVersion.SAMM_1_0_0,
         KnownVersion.SAMM_2_0_0,
         KnownVersion.SAMM_2_1_0
   );

   private static final MigratorService MIGRATOR_SERVICE = new MigratorService();

   private AspectModelLoader() {
   }

   private static void validateNamespaceOfCustomUnits( final Model rawModel ) {
      final List<String> customUnitsWithSammNamespace = new ArrayList<>();
      rawModel.listStatements( null, RDF.type, SammNs.SAMM.Unit() )
            .mapWith( Statement::getSubject )
            .filterKeep( subject -> subject.getNameSpace().equals( SammNs.SAMM.getNamespace() ) )
            .mapWith( Resource::getLocalName )
            .forEach( customUnitsWithSammNamespace::add );

      if ( !customUnitsWithSammNamespace.isEmpty() ) {
         throw new InvalidNamespaceException(
               String.format( "Aspect model contains unit(s) %s not specified in the unit catalog but referred with samm namespace",
                     customUnitsWithSammNamespace ) );
      }
   }

   /**
    * Loads elements from an RDF model that possibly contains multiple namespaces, and organize the result into a
    * collection of {@link ModelNamespace}. Use this method only when you expect the RDF model to contain more than
    * one namespace (which is not the case when aspect models contain the usual element definitions with one namespace per file),
    * otherwise use {@link #getElements(VersionedModel)}.
    *
    * @param versionedModel The RDF model representation of the Aspect model
    * @return the list of namespaces
    */
   public static Try<List<ModelNamespace>> getNamespaces( final VersionedModel versionedModel ) {
      return getElements( versionedModel ).map( elements ->
            elements.stream()
                  .filter( element -> element.is( ModelElement.class ) && !element.isAnonymous() )
                  .collect( Collectors.groupingBy( namedElement -> {
                     final String urn = namedElement.as( ModelElement.class ).urn().toString();
                     return urn.substring( 0, urn.indexOf( "#" ) );
                  } ) )
                  .entrySet()
                  .stream()
                  .map( entry -> DefaultModelNamespace.from( entry.getKey(), entry.getValue(), Optional.empty() ) )
                  .toList()
      );
   }

   /**
    * Creates Java instances for model element classes from the RDF input model
    *
    * @param versionedModel The RDF model representation of the Aspect model
    * @return the list of loaded model elements on success
    */
   public static Try<List<ModelElement>> getElements( final VersionedModel versionedModel ) {
      final Optional<KnownVersion> metaModelVersion = KnownVersion.fromVersionString( versionedModel.getMetaModelVersion().toString() );
      if ( metaModelVersion.isEmpty() || !SUPPORTED_VERSIONS.contains( metaModelVersion.get() ) ) {
         return Try.failure( new UnsupportedVersionException( versionedModel.getMetaModelVersion() ) );
      }

      final Try<VersionedModel> updatedModel = metaModelVersion.get().isOlderThan( KnownVersion.getLatest() )
            ? MIGRATOR_SERVICE.updateMetaModelVersion( versionedModel )
            : Try.success( versionedModel );
      if ( updatedModel.isFailure() ) {
         return Try.failure( updatedModel.getCause() );
      }

      try {
         validateNamespaceOfCustomUnits( versionedModel.getRawModel() );
      } catch ( final InvalidNamespaceException exception ) {
         return Try.failure( exception );
      }

      try {
         final VersionedModel model = updatedModel.get();
         final ModelElementFactory modelElementFactory = new ModelElementFactory( model.getModel(), Map.of() );
         // List element definitions (... rdf:type ...) from the raw model (i.e. the actual aspect model to load)
         // but then load them from the resolved model, because it contains all necessary context (e.g. unit definitions)
         return Try.success( model.getRawModel().listStatements( null, RDF.type, (RDFNode) null ).toList().stream()
               .map( Statement::getSubject )
               .filter( RDFNode::isURIResource )
               .map( resource -> model.getModel().createResource( resource.getURI() ) )
               .map( resource -> modelElementFactory.create( ModelElement.class, resource ) )
               .toList() );
      } catch ( final RuntimeException exception ) {
         return Try.failure( new InvalidModelException( "Could not load Aspect model, please make sure the model is valid", exception ) );
      }
   }

   /**
    * Does the same as {@link #getElements(VersionedModel)} but throws an exception in case of failures.
    *
    * @param versionedModel The RDF model representation of the Aspect model
    * @return the list of model elements
    * @throws AspectLoadingException when elements can not be loaded
    */
   public static List<ModelElement> getElementsUnchecked( final VersionedModel versionedModel ) {
      return getElements( versionedModel ).getOrElseThrow( cause -> {
         LOG.error( "Could not load elements", cause );
         throw new AspectLoadingException( cause );
      } );
   }

   /**
    * Convenience method that does the same as {@link #getElements(VersionedModel)} except it will return only the aspects contained in the
    * model.
    *
    * @param versionedModel The RDF model representation of the Aspect model
    * @return the list of model aspects
    */
   public static Try<List<Aspect>> getAspects( final VersionedModel versionedModel ) {
      return getElements( versionedModel ).map( elements ->
            elements.stream().filter( element -> element.is( Aspect.class ) )
                  .map( element -> element.as( Aspect.class ) )
                  .toList() );
   }

   /**
    * Does the same as {@link #getAspects(VersionedModel)} but throws an exception in case of failures.
    *
    * @param versionedModel The RDF model representation of the Aspect model
    * @return the list of model aspects
    * @throws AspectLoadingException when elements can not be loaded
    */
   public static List<Aspect> getAspectsUnchecked( final VersionedModel versionedModel ) {
      return getAspects( versionedModel ).getOrElseThrow( cause -> {
         LOG.error( "Could not load aspects", cause );
         throw new AspectLoadingException( cause );
      } );
   }

   /**
    * Convenience method to load the single Aspect from a model, when the model contains exactly one Aspect.
    * <b>Caution:</b> The method handles this special case. Aspect Models are allowed to contain any number of Aspects (including zero),
    * so for the general case you should use {@link #getElements(VersionedModel)} instead.
    *
    * @param versionedModel The RDF model representation of the Aspect model
    * @return the single Aspect contained in the model
    */
   public static Try<Aspect> getSingleAspect( final VersionedModel versionedModel ) {
      return getSingleAspect( versionedModel, aspect -> true );
   }

   /**
    * Convenience method to load the single Aspect from a model, when the model contains exactly one Aspect. Does the same as
    * {@link #getSingleAspect(VersionedModel)} but throws an exception on failure.
    * <b>Caution:</b> The method handles this special case. Aspect Models are allowed to contain any number of Aspects (including zero),
    * so for the general case you should use {@link #getElementsUnchecked(VersionedModel)} instead.
    *
    * @param versionedModel The RDF model representation of the Aspect model
    * @return the single Aspect contained in the model
    */
   public static Aspect getSingleAspectUnchecked( final VersionedModel versionedModel ) {
      return getSingleAspect( versionedModel ).getOrElseThrow( cause -> {
         LOG.error( "Could not load aspect", cause );
         throw new AspectLoadingException( cause );
      } );
   }

   /**
    * Similar to {@link #getSingleAspect(VersionedModel)}, except that a predicate can be provided to select which of potentially
    * multiple aspects should be selected
    *
    * @param versionedModel the RDF model reprensentation of the Aspect model
    * @param selector the predicate to select an Aspect
    * @return the selected Aspect, or a failure if 0 or more than 1 matching Aspects were found
    */
   public static Try<Aspect> getSingleAspect( final VersionedModel versionedModel, final Predicate<Aspect> selector ) {
      return getAspects( versionedModel ).flatMap( allAspects -> {
         final List<Aspect> aspects = allAspects.stream().filter( selector ).toList();
         return switch ( aspects.size() ) {
            case 1 -> Try.success( aspects.iterator().next() );
            case 0 -> Try.failure( new InvalidRootElementCountException( "No Aspects were found in the model" ) );
            default -> Try.failure( new AspectLoadingException( "Multiple Aspects were found in the resolved model" ) );
         };
      } );
   }

   /**
    * Convenience method to create an {@link Aspect} directly from a model file. This method makes the following assumptions:
    * <ul>
    *    <li>The model file is located in a directory structure as required by the {@link FileSystemStrategy}</li>
    *    <li>The closure of the loaded model contains exactly one Aspect</li>
    *    <li>The Aspect has the same name as the file's basename</li>
    * </ul>
    * The method is intended for use in tests and comparable use cases, not as a general replacement for loading Aspect Models, since it
    * does not
    * handle model files with less or more than one Aspect.
    *
    * @param input the model file
    * @return the loaded Aspect Context
    */
   public static Try<Aspect> getAspectContext( final File input ) {
      return AspectModelResolver.loadAndResolveModel( input ).flatMap( versionedModel ->
            getSingleAspect( versionedModel, aspect -> input.getName().equals( aspect.getName() + ".ttl" ) )
      );
   }
}

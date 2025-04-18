/*
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH
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

package org.eclipse.esmf.aspectmodel.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.esmf.aspectmodel.AspectLoadingException;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.RdfUtil;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelFileLoader;
import org.eclipse.esmf.aspectmodel.resolver.EitherStrategy;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.resolver.ModelSource;
import org.eclipse.esmf.aspectmodel.resolver.NamespacePackage;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategy;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategySupport;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.resolver.fs.FlatModelsRoot;
import org.eclipse.esmf.aspectmodel.resolver.modelfile.DefaultAspectModelFile;
import org.eclipse.esmf.aspectmodel.resolver.modelfile.MetaModelFile;
import org.eclipse.esmf.aspectmodel.resolver.modelfile.RawAspectModelFile;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.urn.ElementType;
import org.eclipse.esmf.aspectmodel.urn.UrnSyntaxException;
import org.eclipse.esmf.aspectmodel.versionupdate.MetaModelVersionMigrator;
import org.eclipse.esmf.metamodel.Aspect;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.esmf.metamodel.ModelElement;
import org.eclipse.esmf.metamodel.Namespace;
import org.eclipse.esmf.metamodel.impl.DefaultAspectModel;
import org.eclipse.esmf.metamodel.impl.DefaultNamespace;
import org.eclipse.esmf.metamodel.vocabulary.SammNs;

import com.google.common.collect.Streams;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The core class to load an {@link AspectModel}. The AspectModelLoader is also a {@link ModelSource} and allows to list the
 * contents of its configured {@link ResolutionStrategy}s.
 */
public class AspectModelLoader implements ModelSource, ResolutionStrategySupport {
   private static final Logger LOG = LoggerFactory.getLogger( AspectModelLoader.class );

   public static final Supplier<ResolutionStrategy> DEFAULT_STRATEGY = () -> {
      final Path currentDirectory = Path.of( System.getProperty( "user.dir" ) );
      return new FileSystemStrategy( new FlatModelsRoot( currentDirectory ) );
   };

   private final ResolutionStrategy resolutionStrategy;

   /**
    * Default constructor. When encountering model elements not defined in the current file, this will use the default strategy of looking
    * for definitions of model elements in .ttl files in the current working directory.
    */
   public AspectModelLoader() {
      this( List.of() );
   }

   /**
    * Constructor that takes a single {@link ResolutionStrategy} to control where the loading process should look for definitions
    * of model elements not defined in the current file.
    *
    * @param resolutionStrategy the strategy
    */
   public AspectModelLoader( final ResolutionStrategy resolutionStrategy ) {
      this( List.of( resolutionStrategy ) );
   }

   /**
    * Constructor that takes multiple {@link ResolutionStrategy}s to control where the loading process should look for definitions
    * of model elements not defined in the current file. The strategies are tried one after another until an element definition is found.
    *
    * @param resolutionStrategies the strategies
    */
   public AspectModelLoader( final List<ResolutionStrategy> resolutionStrategies ) {
      TurtleLoader.init();
      if ( resolutionStrategies.size() == 1 ) {
         resolutionStrategy = resolutionStrategies.get( 0 );
      } else if ( resolutionStrategies.isEmpty() ) {
         resolutionStrategy = DEFAULT_STRATEGY.get();
      } else {
         resolutionStrategy = new EitherStrategy( resolutionStrategies );
      }
   }

   /**
    * Load an Aspect Model from a given file
    *
    * @param file the file
    * @return the Aspect Model
    */
   public AspectModel load( final File file ) {
      return load( List.of( file ) );
   }

   /**
    * Load a set of files into a single Aspect Model
    *
    * @param files the files
    * @return the Aspect Model
    */
   public AspectModel load( final Collection<File> files ) {
      final List<AspectModelFile> migratedFiles = files.stream()
            .map( AspectModelFileLoader::load )
            .map( this::migrate )
            .toList();
      final LoaderContext loaderContext = new LoaderContext();
      resolve( migratedFiles, loaderContext );
      return loadAspectModelFiles( loaderContext.loadedFiles() );
   }

   /**
    * Load an Aspect Model by transitively resolving a given input URN
    *
    * @param urn the Aspect Model URN
    * @return the Aspect Model
    */
   public AspectModel load( final AspectModelUrn urn ) {
      return loadUrns( List.of( urn ) );
   }

   /**
    * Loads all contents of a complete namespace into an Aspect Model
    *
    * @param urn the namespace URN
    * @return the Aspect Model
    */
   public AspectModel loadNamespace( final AspectModelUrn urn ) {
      if ( !urn.getName().isEmpty() ) {
         throw new AspectLoadingException( "URN does not denote a namespace" );
      }
      return loadAspectModelFiles( loadContentsForNamespace( urn ).toList() );
   }

   /**
    * Load an Aspect Model by transitively resolving a set of given input URNs
    *
    * @param urns the Aspect Model URNs
    * @return the Aspect Model
    */
   public AspectModel loadUrns( final Collection<AspectModelUrn> urns ) {
      final LoaderContext loaderContext = new LoaderContext();
      for ( final AspectModelUrn inputUrn : urns ) {
         loaderContext.unresolvedUrns().add( inputUrn.toString() );
      }
      resolve( List.of(), loaderContext );
      return loadAspectModelFiles( loaderContext.loadedFiles() );
   }

   /**
    * Load an Aspect Model (.ttl) from an input stream and optionally set the source location for this input. For loading
    * an Aspect Model Namespace Package (.zip), use {@link #loadNamespacePackage(InputStream, URI)} instead.
    *
    * @param inputStream the input stream
    * @param sourceLocation the source location for the model
    * @return the Aspect Model
    */
   public AspectModel load( final InputStream inputStream, final Optional<URI> sourceLocation ) {
      final AspectModelFile rawFile = AspectModelFileLoader.load( inputStream, sourceLocation );
      final AspectModelFile migratedModel = migrate( rawFile );
      final LoaderContext loaderContext = new LoaderContext();
      resolve( List.of( migratedModel ), loaderContext );
      return loadAspectModelFiles( loaderContext.loadedFiles() );
   }

   /**
    * Load an Aspect Model from an input stream
    *
    * @param inputStream the input stream
    * @return the Aspect Model
    */
   public AspectModel load( final InputStream inputStream ) {
      return load( inputStream, Optional.empty() );
   }

   /**
    * Load a Namespace Package (Archive) from a File
    *
    * @param namespacePackage the archive file
    * @return the Aspect Model
    */
   public AspectModel loadNamespacePackage( final File namespacePackage ) {
      if ( !namespacePackage.exists() || !namespacePackage.isFile() ) {
         throw new AspectLoadingException( "The specified file does not exist or is not a file." );
      }

      try ( final InputStream inputStream = new FileInputStream( namespacePackage ) ) {
         return loadNamespacePackage( inputStream, namespacePackage.toURI() );
      } catch ( final IOException exception ) {
         LOG.error( "Error reading the file: {}", namespacePackage.getAbsolutePath(), exception );
         throw new AspectLoadingException( "Error reading the file: " + namespacePackage.getAbsolutePath(), exception );
      }
   }

   /**
    * Load a namespace package from binary with a given location. The location is not resolved or loaded from, but is only attached
    * to the files loaded from the input stream to indicate their original source, e.g., file system location or URL.
    *
    * @param location the source location
    * @param binaryContent the ZIP content
    * @return the loaded and resolved Aspect Model
    */
   public AspectModel loadNamespacePackage( final byte[] binaryContent, final URI location ) {
      final List<AspectModelFile> packageFiles = new NamespacePackage( binaryContent, location ).loadContents().toList();
      final LoaderContext loaderContext = new LoaderContext();
      resolve( packageFiles, loaderContext );
      return loadAspectModelFiles( loaderContext.loadedFiles() );
   }

   /**
    * Load a namespace package from an input stream with a given location. The location is not resolved or loaded from, but is only attached
    * to the files loaded from the input stream to indicate their original source, i.e., a file system location or URL of the
    * namespace package. For example, if the namespace package is located at {@code file:/some/path/package.zip} or
    * {@code https://example.com/package.zip}, the files in the package will have a location URI such as
    * {@code jar:file:/some/path/package.zip!/com.example.namespace/1.0.0/AspectModel.ttl} or
    * {@code jar:https://example.com/package.zip!/com.example.namespace/1.0.0/AspectModel.ttl}, respectively, as described in
    * the JavaDoc for {@link java.net.JarURLConnection}.
    *
    * @param location the source location
    * @param inputStream the input stream to load the ZIP content from
    * @return the loaded and resolved Aspect Model
    */
   public AspectModel loadNamespacePackage( final InputStream inputStream, final URI location ) {
      final List<AspectModelFile> packageFiles = new NamespacePackage( inputStream, location ).loadContents().toList();
      final LoaderContext loaderContext = new LoaderContext();
      resolve( packageFiles, loaderContext );
      return loadAspectModelFiles( loaderContext.loadedFiles() );
   }

   /**
    * Load a Namespace Package (Archive) from an InputStream
    *
    * @param inputStream the input stream
    * @return the Aspect Model
    * @deprecated Use {@link #loadNamespacePackage(InputStream, URI)} instead
    */
   @Deprecated
   public AspectModel loadNamespacePackage( final InputStream inputStream ) {
      return loadNamespacePackage( inputStream, URI.create( "file:unknown" ) );
   }

   private AspectModelFile migrate( final AspectModelFile file ) {
      return MetaModelVersionMigrator.INSTANCE.apply( file );
   }

   private record LoaderContext(
         Set<String> resolvedUrns,
         Set<AspectModelFile> loadedFiles,
         Deque<String> unresolvedUrns,
         Deque<AspectModelFile> unresolvedFiles
   ) {
      private LoaderContext() {
         this( new HashSet<>(), new HashSet<>(), new ArrayDeque<>(), new ArrayDeque<>() );
      }
   }

   /**
    * Adapter that enables the resolver to handle URNs with the legacy "urn:bamm:" prefix.
    *
    * @param urn the URN to clean up
    * @return the original URN (if using valid urn:samm: scheme) or the the cleaned up URN
    */
   private String replaceLegacyBammUrn( final String urn ) {
      if ( urn.startsWith( "urn:bamm:" ) ) {
         return urn.replace( "urn:bamm:", "urn:samm:" );
      }
      return urn;
   }

   private boolean containsType( final Model model, final String urn ) {
      if ( model.contains( model.createResource( urn ), RDF.type, (RDFNode) null ) ) {
         return true;
      } else if ( urn.startsWith( "urn:samm:" ) ) {
         // when deriving a URN from file (via "fileToUrn" method - mainly in samm-cli scenarios),
         // we assume new "samm" format, but could actually have been the old "bamm"
         return model.contains( model.createResource( toLegacyBammUrn( urn ) ), RDF.type, (RDFNode) null );
      }
      return false;
   }

   private String toLegacyBammUrn( final String urn ) {
      if ( urn.startsWith( "urn:samm:" ) ) {
         return urn.replace( "urn:samm:", "urn:bamm:" );
      }
      return urn;
   }

   private Optional<AspectModelFile> applyResolutionStrategy( final String urn ) {
      if ( urn.startsWith( RDF.getURI() ) || urn.startsWith( XSD.getURI() ) ) {
         return Optional.empty();
      }

      try {
         final AspectModelUrn aspectModelUrn = AspectModelUrn.fromUrn( replaceLegacyBammUrn( urn ) );
         if ( aspectModelUrn.getElementType() != ElementType.NONE ) {
            return Optional.empty();
         }
         final AspectModelFile resolutionResult = resolutionStrategy.apply( aspectModelUrn, this );
         if ( !containsType( resolutionResult.sourceModel(), urn ) ) {
            throw new AspectLoadingException(
                  "Resolution strategy returned a model which does not contain element definition for " + urn );
         }
         return Optional.of( resolutionResult );
      } catch ( final UrnSyntaxException exception ) {
         // This happens if the URN to load is no actual URN.
         // If it's no valid Aspect Model URN but some other URI (e.g., a samm:see value), there is nothing
         // to resolve, so we return just an empty model.
         return Optional.empty();
      }
   }

   private void urnsFromModelNeedResolution( final AspectModelFile modelFile, final LoaderContext context ) {
      Streams.stream( modelFile.sourceModel().listStatements( null, RDF.type, (RDFNode) null ) )
            .map( Statement::getSubject )
            .filter( Resource::isURIResource )
            .map( Resource::getURI )
            .filter( uri -> uri.startsWith( "urn:samm:" ) )
            .forEach( urn -> context.resolvedUrns().add( urn ) );

      RdfUtil.getAllUrnsInModel( modelFile.sourceModel() ).stream()
            .map( AspectModelUrn::toString )
            .filter( urn -> !context.resolvedUrns().contains( urn ) )
            .filter( urn -> !urn.startsWith( XSD.NS ) )
            .filter( urn -> !urn.startsWith( RDF.uri ) )
            .filter( urn -> !urn.startsWith( SammNs.SAMM.getNamespace() ) )
            .filter( urn -> !urn.startsWith( SammNs.SAMMC.getNamespace() ) )
            .filter( urn -> !urn.startsWith( SammNs.SAMME.getNamespace() ) )
            .filter( urn -> !urn.startsWith( SammNs.UNIT.getNamespace() ) )
            .forEach( urn -> context.unresolvedUrns().add( urn ) );
   }

   private void markModelFileAsLoaded( final AspectModelFile modelFile, final LoaderContext context ) {
      if ( !context.loadedFiles().contains( modelFile ) ) {
         context.loadedFiles().add( modelFile );
         urnsFromModelNeedResolution( modelFile, context );
      }
   }

   private void resolve( final List<AspectModelFile> inputFiles, final LoaderContext context ) {
      for ( final AspectModelFile aspectModelFile : inputFiles ) {
         context.unresolvedFiles().push( aspectModelFile );
      }

      final List<ModelResolutionException.LoadingFailure> loadingFailures = new ArrayList<>();
      while ( !context.unresolvedFiles().isEmpty() || !context.unresolvedUrns().isEmpty() ) {
         if ( !context.unresolvedFiles().isEmpty() ) {
            final AspectModelFile modelFile = context.unresolvedFiles().pop();
            if ( context.loadedFiles().contains( modelFile ) ) {
               continue;
            }
            markModelFileAsLoaded( modelFile, context );
         }

         while ( !context.unresolvedUrns().isEmpty() ) {
            try {
               applyResolutionStrategy( context.unresolvedUrns().pop() )
                     .map( this::migrate )
                     .ifPresent( resolvedFile -> markModelFileAsLoaded( resolvedFile, context ) );
            } catch ( final ModelResolutionException exception ) {
               // If one element can not be resolved, collect its cause and continue, so that
               // we can create a comprehensive overview of all elements that can not be resolved
               if ( exception.getCheckedLocations().isEmpty() ) {
                  throw exception;
               } else {
                  loadingFailures.addAll( exception.getCheckedLocations() );
               }
            }
         }
      }

      if ( !loadingFailures.isEmpty() ) {
         throw new ModelResolutionException( loadingFailures );
      }
   }

   /**
    * Checks if a given model contains the definition of a model element.
    *
    * @param aspectModelFile the model file
    * @param urn the URN of the model element
    * @return true if the model contains the definition of the model element
    */
   @Override
   public boolean containsDefinition( final AspectModelFile aspectModelFile, final AspectModelUrn urn ) {
      final Model model = aspectModelFile.sourceModel();
      if ( model.getNsPrefixMap().values().stream().anyMatch( prefixUri -> prefixUri.startsWith( "urn:bamm:" ) ) ) {
         final boolean result = model.contains( model.createResource( urn.toString().replace( "urn:samm:", "urn:bamm:" ) ), RDF.type,
               (RDFNode) null );
         LOG.debug( "Checking if model contains {}: {}", urn, result );
         return result;
      }
      final boolean result = model.contains( model.createResource( urn.toString() ), RDF.type, (RDFNode) null );
      LOG.debug( "Checking if model contains {}: {}", urn, result );
      return result;
   }

   /**
    * Creates a new empty Aspect Model.
    *
    * @return A new empty Aspect Model
    */
   public AspectModel emptyModel() {
      return new DefaultAspectModel( new ArrayList<>(), ModelFactory.createDefaultModel(), new ArrayList<>() );
   }

   /**
    * Creates a new Aspect Model from a collection of {@link AspectModelFile}s. The AspectModelFiles can be {@link RawAspectModelFile}
    * (i.e., not contain {@link ModelElement} instances yet); this method takes care of instantiating the model elements.
    *
    * @param inputFiles the list of input files
    * @return the Aspect Model
    */
   public AspectModel loadAspectModelFiles( final Collection<AspectModelFile> inputFiles ) {
      final Collection<AspectModelFile> migratedInputs = inputFiles.stream()
            .map( this::migrate )
            .toList();
      final Model mergedModel = ModelFactory.createDefaultModel();
      for ( final AspectModelFile file : migratedInputs ) {
         RdfUtil.mergeModel( mergedModel, file.sourceModel() );
      }
      mergedModel.add( MetaModelFile.metaModelDefinitions() );

      final List<ModelElement> elements = new ArrayList<>();
      final List<AspectModelFile> files = new ArrayList<>();
      for ( final AspectModelFile file : migratedInputs ) {
         final DefaultAspectModelFile aspectModelFile = new DefaultAspectModelFile( file.sourceModel(), file.headerComment(),
               file.sourceLocation() );
         files.add( aspectModelFile );
         final Model model = file.sourceModel();
         final ModelElementFactory modelElementFactory = new ModelElementFactory( mergedModel, Map.of(), element -> aspectModelFile );
         final List<ModelElement> fileElements = model.listStatements( null, RDF.type, (RDFNode) null ).toList().stream()
               .filter( statement -> !statement.getObject().isURIResource() || !statement.getResource().equals( SammNs.SAMM.Namespace() ) )
               .map( Statement::getSubject )
               .filter( RDFNode::isURIResource )
               .map( resource -> mergedModel.createResource( resource.getURI() ) )
               .map( resource -> modelElementFactory.create( ModelElement.class, resource ) )
               .toList();
         aspectModelFile.setElements( fileElements );
         elements.addAll( fileElements );
      }

      setNamespaces( files, elements );
      elements.stream()
            .filter( modelElement -> modelElement.is( Aspect.class ) )
            .findFirst()
            .ifPresent( aspect -> mergedModel.setNsPrefix( "", aspect.urn().getUrnPrefix() ) );
      for ( final AspectModelFile file : files ) {
         if ( file.aspects().size() > 1 ) {
            throw new AspectLoadingException(
                  "Aspect model file " + file.sourceLocation().map( location -> location + " " ).orElse( "" ) + "contains " + file.aspects()
                        .size() + " aspects, but may only contain one." );
         }
      }
      return new DefaultAspectModel( files, mergedModel, elements );
   }

   /**
    * Sets up the namespace references in the collection of newly created AspectModelFiles
    *
    * @param files the files
    * @param elements the collection of all model elements across all files
    */
   private void setNamespaces( final Collection<AspectModelFile> files, final Collection<ModelElement> elements ) {
      final Map<String, List<ModelElement>> elementsGroupedByNamespaceUrn = elements.stream()
            .filter( element -> !element.isAnonymous() )
            .collect( Collectors.groupingBy( element -> element.urn().getNamespaceIdentifier() ) );
      for ( final AspectModelFile file : files ) {
         final Optional<String> optionalNamespaceUrn =
               Optional.ofNullable( file.sourceModel().getNsPrefixURI( "" ) )
                     .map( urnPrefix -> urnPrefix.split( "#" )[0] )
                     .or( () -> file.elements().stream()
                           .filter( element -> !element.isAnonymous() )
                           .map( element -> element.urn().getNamespaceIdentifier() )
                           .findAny() );
         if ( optionalNamespaceUrn.isEmpty() ) {
            continue;
         }

         final String namespaceUrn = optionalNamespaceUrn.get();
         MetaModelBaseAttributes namespaceDefinition = null;
         AspectModelFile fileContainingNamespaceDefinition = null;
         final List<ModelElement> elementsForUrn = elementsGroupedByNamespaceUrn.get( namespaceUrn );
         if ( elementsForUrn != null ) {
            for ( final ModelElement element : elementsForUrn ) {
               final AspectModelFile elementFile = element.getSourceFile();
               if ( elementFile.sourceModel().contains( null, RDF.type, SammNs.SAMM.Namespace() ) ) {
                  final Model model = elementFile.sourceModel();
                  final ModelElementFactory modelElementFactory = new ModelElementFactory( model, Map.of(), r -> null );
                  final Resource namespaceResource = model.listStatements( null, RDF.type, SammNs.SAMM.Namespace() )
                        .mapWith( Statement::getSubject )
                        .toList().getFirst();
                  namespaceDefinition = modelElementFactory.createBaseAttributes( namespaceResource );
                  fileContainingNamespaceDefinition = elementFile;
                  break;
               }
            }
         }

         final Namespace namespace = new DefaultNamespace( namespaceUrn, elementsGroupedByNamespaceUrn.get( namespaceUrn ),
               Optional.ofNullable( fileContainingNamespaceDefinition ), Optional.ofNullable( namespaceDefinition ) );
         ( (DefaultAspectModelFile) file ).setNamespace( namespace );
      }
   }

   /**
    * Creates a new Aspect Model that contains the closure of two input Aspect Models
    *
    * @param aspectModel1 the first input Aspect Model
    * @param aspectModel2 the second input Aspect Model
    * @return the merged Aspect Model
    */
   public AspectModel merge( final AspectModel aspectModel1, final AspectModel aspectModel2 ) {
      final List<AspectModelFile> files = new ArrayList<>( aspectModel1.files() );
      final Set<URI> locations = aspectModel1.files().stream()
            .flatMap( f -> f.sourceLocation().stream() )
            .collect( Collectors.toSet() );
      for ( final AspectModelFile file : aspectModel2.files() ) {
         file.sourceLocation().filter( uri -> !locations.contains( uri ) ).ifPresent( uri -> files.add( file ) );
      }
      return loadAspectModelFiles( files );
   }

   /**
    * Lists the URIs of all Aspect Model files for all configured resolution strategies
    *
    * @return the URIs
    */
   @Override
   public Stream<URI> listContents() {
      return resolutionStrategy.listContents();
   }

   /**
    * Lists the URIs of all Aspect Model files for a given namespace for all configured resolution strategies
    *
    * @param namespace the namespace
    * @return the URIs
    */
   @Override
   public Stream<URI> listContentsForNamespace( final AspectModelUrn namespace ) {
      return resolutionStrategy.listContentsForNamespace( namespace );
   }

   /**
    * Returns all loadable Aspect Model files from all configured resolution strategies
    *
    * @return the Aspect Model files
    */
   @Override
   public Stream<AspectModelFile> loadContents() {
      return resolutionStrategy.loadContents();
   }

   /**
    * Returns all loadable Aspect Model files for a given namespace from all configured resolution strategies
    *
    * @param namespace the namespace
    * @return the Aspect Model files
    */
   @Override
   public Stream<AspectModelFile> loadContentsForNamespace( final AspectModelUrn namespace ) {
      return resolutionStrategy.loadContentsForNamespace( namespace );
   }
}

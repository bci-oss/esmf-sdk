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

package io.openmanufacturing.sds.metamodel.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

import com.google.common.collect.ImmutableList;

import io.openmanufacturing.sds.aspectmetamodel.KnownVersion;
import io.openmanufacturing.sds.aspectmodel.resolver.services.ExtendedXsdDataType;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.openmanufacturing.sds.aspectmodel.vocabulary.BAMM;
import io.openmanufacturing.sds.aspectmodel.vocabulary.BAMMC;
import io.openmanufacturing.sds.aspectmodel.vocabulary.UNIT;
import io.openmanufacturing.sds.metamodel.AbstractEntity;
import io.openmanufacturing.sds.metamodel.Base;
import io.openmanufacturing.sds.metamodel.Characteristic;
import io.openmanufacturing.sds.metamodel.Collection;
import io.openmanufacturing.sds.metamodel.CollectionValue;
import io.openmanufacturing.sds.metamodel.Entity;
import io.openmanufacturing.sds.metamodel.EntityInstance;
import io.openmanufacturing.sds.metamodel.Property;
import io.openmanufacturing.sds.metamodel.QuantityKinds;
import io.openmanufacturing.sds.metamodel.Scalar;
import io.openmanufacturing.sds.metamodel.ScalarValue;
import io.openmanufacturing.sds.metamodel.Type;
import io.openmanufacturing.sds.metamodel.Unit;
import io.openmanufacturing.sds.metamodel.Units;
import io.openmanufacturing.sds.metamodel.Value;
import io.openmanufacturing.sds.metamodel.datatypes.LangString;
import io.openmanufacturing.sds.metamodel.impl.DefaultCollectionValue;
import io.openmanufacturing.sds.metamodel.impl.DefaultEntityInstance;
import io.openmanufacturing.sds.metamodel.impl.DefaultScalar;
import io.openmanufacturing.sds.metamodel.impl.DefaultScalarValue;
import io.openmanufacturing.sds.metamodel.impl.DefaultUnit;

public abstract class Instantiator<T extends Base> implements Function<Resource, T> {
   protected final ModelElementFactory modelElementFactory;
   protected Class<T> targetClass;
   protected BAMM bamm;
   protected BAMMC bammc;
   protected UNIT unit;
   protected Model model;
   protected KnownVersion metaModelVersion;

   public Instantiator( final ModelElementFactory modelElementFactory, final Class<T> targetClass ) {
      this.modelElementFactory = modelElementFactory;
      this.targetClass = targetClass;
      bamm = modelElementFactory.getBamm();
      bammc = modelElementFactory.getBammc();
      unit = modelElementFactory.getUnit();
      model = modelElementFactory.getModel();
      metaModelVersion = modelElementFactory.getMetaModelVersion();
   }

   protected MetaModelBaseAttributes buildBaseAttributes( final Resource resource ) {
      return MetaModelBaseAttributes.fromMetaModelElement( metaModelVersion, resource, model, bamm );
   }

   protected Optional<Statement> optionalPropertyValue( final Resource subject, final org.apache.jena.rdf.model.Property type ) {
      return modelElementFactory.propertyValue( subject, type );
   }

   protected Statement propertyValue( final Resource subject, final org.apache.jena.rdf.model.Property type ) {
      return optionalPropertyValue( subject, type ).orElseThrow( () -> new AspectLoadingException( "Missing Property " + type + " on " + subject ) );
   }

   /**
    * Extracts all {@link BAMM#Property()} model elements which are referenced in the given Property List, e.g.
    * {@link BAMM#properties()}, and creates {@link Property} instances for these model elements.
    *
    * @param elementWithProperties the {@link Resource} which has the propertyRdfClass list for which the model
    *       elements are extracted
    * @param rootProperty the {@link Property} defining the property list
    * @return a {@link List} containing the {@link Property} instances
    */
   protected List<Property> getPropertiesModels( final Resource elementWithProperties,
         final org.apache.jena.rdf.model.Property rootProperty ) {
      return getResourcesFromList( elementWithProperties, rootProperty )
            .map( propertyResource -> modelElementFactory.create( Property.class, propertyResource ) )
            .collect( Collectors.toList() );
   }

   protected Stream<RDFNode> getNodesFromList( final Resource element, final org.apache.jena.rdf.model.Property property ) {
      return Optional.ofNullable( element.getProperty( property ) ).stream()
            .flatMap( p -> p.getObject().as( RDFList.class ).asJavaList().stream() );
   }

   protected Stream<Resource> getResourcesFromList( final Resource element, final org.apache.jena.rdf.model.Property property ) {
      return getNodesFromList( element, property ).map( RDFNode::asResource );
   }

   protected Type getType( final Resource characteristicResource ) {
      final Statement dataType = getDataType( characteristicResource );
      final Resource dataTypeResource = dataType.getObject().asResource();

      final Optional<Statement> entityStatement = optionalPropertyValue( dataTypeResource, RDF.type );

      if ( entityStatement.isPresent() && bamm.Entity().equals( entityStatement.get().getObject().asResource() ) ) {
         return modelElementFactory.create( Entity.class, entityStatement.get().getSubject() );
      }

      if ( entityStatement.isPresent() && bamm.AbstractEntity().equals( entityStatement.get().getObject().asResource() ) ) {
         return modelElementFactory.create( AbstractEntity.class, entityStatement.get().getSubject() );
      }

      return new DefaultScalar( dataTypeResource.getURI(), metaModelVersion );
   }

   /**
    * Recursively search the datatype in a Characteristic chain, baseCharacteristics first
    *
    * @param resource The Characteristic resource
    * @return The statement describing the datatype
    */
   private Statement getDataType( final Resource resource ) {
      return Optional.ofNullable( resource.getPropertyResourceValue( bamm.baseCharacteristic() ) )
            .map( this::getDataType )
            .orElseGet( () -> resource.getProperty( bamm.dataType() ) );
   }

   protected Optional<Unit> findOrCreateUnit( final Resource unitResource ) {
      if ( unit.getNamespace().equals( unitResource.getNameSpace() ) ) {
         return Units.fromName( unitResource.getProperty( bamm.name() ).getString(), metaModelVersion );
      }

      return Optional.of( new DefaultUnit(
            MetaModelBaseAttributes.fromMetaModelElement( metaModelVersion, unitResource, model, bamm ),
            optionalPropertyValue( unitResource, bamm.symbol() ).map( Statement::getString ),
            optionalPropertyValue( unitResource, bamm.commonCode() ).map( Statement::getString ),
            optionalPropertyValue( unitResource, bamm.referenceUnit() ).map( Statement::getResource ).map( Resource::getLocalName ),
            optionalPropertyValue( unitResource, bamm.conversionFactor() ).map( Statement::getString ),
            ImmutableList.copyOf( model.listStatements( unitResource, bamm.quantityKind(), (RDFNode) null ) ).stream()
                  .flatMap( quantityKindStatement -> QuantityKinds.fromName( quantityKindStatement.getObject().asResource().getLocalName() ).stream() )
                  .collect( Collectors.toSet() ) ) );
   }

   protected Optional<Characteristic> getElementCharacteristic( final Resource collection ) {
      return optionalPropertyValue( collection, bammc.elementCharacteristic() )
            .map( Statement::getResource )
            .map( elementCharacteristicResource ->
                  modelElementFactory.create( Characteristic.class, elementCharacteristicResource ) );
   }

   protected boolean isTypeOf( final Resource element, final Resource type ) {
      return model.contains( element, RDF.type, type );
   }

   protected boolean isTypeOfOrSubtypeOf( final Resource element, final Resource type ) {
      //      Resource typeToCheck = type;
      //      do {
      //         if ( isTypeOf( element, typeToCheck ) ) {
      //            return true;
      //         }
      //         typeToCheck = optionalPropertyValue( type, RDFS.subClassOf ).map( Statement::getResource ).orElse( null );
      //      } while ( typeToCheck != null );
      //      return false;
      return isTypeOf( element, type );
   }

   /**
    * Creates a {@link Value} from a given constant value in the RDF model. This can be either a scalar, a collection or an Entity.
    * What is constructed can depend on the type of RDF node, but also on the Characteristic of the Property this value is used for.
    *
    * @param node the RDF node that represents the value
    * @param characteristicResource the resources that represents the Characteristic that describes the value. This can be empty for the values of collections
    *                               that have no bamm-c:elementCharacterisic set
    * @param type the type that describes the value
    * @return a value instance
    */
   protected Value buildValue( final RDFNode node, final Optional<Resource> characteristicResource, final Type type ) {
      // Literals
      if ( node.isLiteral() ) {
         return buildScalarValue( node.asLiteral() );
      }

      // Collections
      if ( characteristicResource.isPresent() ) {
         final Resource characteristic = characteristicResource.get();
         final Optional<Resource> elementCharacteristic = optionalPropertyValue( characteristic, bammc.elementCharacteristic() ).map( Statement::getResource );
         Collection.CollectionType collectionType = null;
         if ( isTypeOfOrSubtypeOf( characteristic, bammc.Set() ) ) {
            collectionType = Collection.CollectionType.SET;
         }
         if ( isTypeOfOrSubtypeOf( characteristic, bammc.SortedSet() ) ) {
            collectionType = Collection.CollectionType.SORTEDSET;
         }
         if ( isTypeOfOrSubtypeOf( characteristic, bammc.List() ) ) {
            collectionType = Collection.CollectionType.LIST;
         }
         if ( isTypeOfOrSubtypeOf( characteristic, bammc.Collection() ) ) {
            collectionType = Collection.CollectionType.COLLECTION;
         }
         if ( collectionType != null ) {
            return buildCollectionValue( node.as( RDFList.class ), collectionType, elementCharacteristic, type );
         }
      }

      // This could happen if an entity instance should be constructed for an AbstractEntity type
      if ( !type.is( Entity.class ) ) {
         throw new AspectLoadingException( "Expected type of value " + node + " to be bamm:Entity, but it is not" );
      }

      // Entities
      final Resource resource = node.asResource();
      return buildEntityInstance( resource, (Entity) type );
   }

   private ScalarValue buildScalarValue( final Literal literal ) {
      // rdf:langString needs special handling here:
      // 1. A custom parser for rdf:langString values can not be registered with Jena, because it would only receive from Jena during parsing
      //    the lexical representation of the value without the language tag (this is handled specially in Jena).
      // 2. This means that a Literal we receive here which has a type URI of rdf:langString will be of type org.apache.jena.datatypes.xsd.impl.RDFLangString
      //    but _not_ io.openmanufacturing.sds.metamodel.datatypes.LangString as we would like to.
      // 3. So we construct an instance of LangString here from the RDFLangString.
      if ( literal.getDatatypeURI().equals( RDF.langString.getURI() ) ) {
         final LangString langString = new LangString( literal.getString(), Locale.forLanguageTag( literal.getLanguage() ) );
         final Scalar type = new DefaultScalar( RDF.langString.getURI(), metaModelVersion );
         return new DefaultScalarValue( langString, type );
      }

      return ExtendedXsdDataType.supportedXsdTypes.stream()
            .filter( type -> type.getURI().equals( literal.getDatatypeURI() ) )
            .map( type -> type.parse( literal.getLexicalForm() ) )
            .map( value -> new DefaultScalarValue( value, new DefaultScalar( literal.getDatatypeURI(), metaModelVersion ) ) )
            .findAny()
            .orElseThrow( () -> new AspectLoadingException( "Literal can not be parsed: " + literal ) );
   }

   private CollectionValue buildCollectionValue( final RDFList list, final Collection.CollectionType collectionType,
         final Optional<Resource> elementCharacteristic, final Type elementType ) {
      final java.util.Collection<Value> values = createEmptyCollectionForType( collectionType );
      list.asJavaList().forEach( element -> values.add( buildValue( element, elementCharacteristic, elementType ) ) );
      return new DefaultCollectionValue( values, collectionType, elementType );
   }

   private EntityInstance buildEntityInstance( final Resource entityInstance, final Entity type ) {
      final Map<Property, Value> assertions = new HashMap<>();
      type.getAllProperties().forEach( property -> {
         final AspectModelUrn propertyUrn = property.getAspectModelUrn().orElseThrow( () ->
               new AspectLoadingException( "Invalid Property without a URN found" ) );
         final org.apache.jena.rdf.model.Property rdfProperty = ResourceFactory.createProperty( propertyUrn.getUrn().toASCIIString() );
         final Statement statement = entityInstance.getProperty( rdfProperty );
         if ( statement == null ) {
            if ( property.isOptional() ) {
               return;
            }
            throw new AspectLoadingException( "Mandatory Property " + property + " not found in Entity instance " + entityInstance );
         }
         final RDFNode rdfValue = entityInstance.getProperty( rdfProperty ).getObject();
         final Type propertyType = property.getDataType().orElseThrow( () -> new AspectLoadingException( "Invalid Property without a dataType found" ) );
         final Resource characteristic = propertyValue( rdfProperty, bamm.characteristic() ).getResource();
         final Value value = buildValue( rdfValue, Optional.of( characteristic ), propertyType );
         assertions.put( property, value );
      } );
      final MetaModelBaseAttributes baseAttributes = buildBaseAttributes( entityInstance );
      return new DefaultEntityInstance( baseAttributes, assertions, type );
   }

   private java.util.Collection<Value> createEmptyCollectionForType( final Collection.CollectionType collectionType ) {
      if ( collectionType == Collection.CollectionType.SORTEDSET ) {
         return new LinkedHashSet<>();
      }
      if ( collectionType == Collection.CollectionType.SET ) {
         return new HashSet<>();
      }
      return new ArrayList<>();
   }
}

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

package io.openmanufacturing.sds.aspectmodel.java;

import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

import io.openmanufacturing.sds.aspectmodel.java.exception.CodeGenerationException;
import io.openmanufacturing.sds.aspectmodel.resolver.services.DataType;
import io.openmanufacturing.sds.metamodel.ModelElement;
import io.openmanufacturing.sds.metamodel.CollectionValue;
import io.openmanufacturing.sds.metamodel.Entity;
import io.openmanufacturing.sds.metamodel.EntityInstance;
import io.openmanufacturing.sds.metamodel.Scalar;
import io.openmanufacturing.sds.metamodel.ScalarValue;
import io.openmanufacturing.sds.metamodel.Value;
import io.openmanufacturing.sds.metamodel.datatypes.LangString;
import io.openmanufacturing.sds.metamodel.visitor.AspectVisitor;

/**
 * Builds an initializer expression for a {@link Value}. For example:
 * <ul>
 *    <li>If the value is (int) 3, it will return "3"</li>
 *    <li>If the value is (String) "hi", it will return "\"hi\""</li>
 *    <li>If the value is (LangString) "hi"@en, it will return "new LangString(\"hi\", Locale.forLanguageTag(\"en\"))"</li>
 *    <li>If the value is a collection, it will return the corresponding collection, e.g. "new ArrayList<>(){{ add(1); add(2); add(3); }}"</></li>
 *    <li>If the value is an Entity, it will return the corresponding constructor call, e.g. "new MyEntity(\"foo\", 2, 3)"</li>
 * </ul>
 */
public class ValueExpressionVisitor implements AspectVisitor<String, ValueExpressionVisitor.Context> {
   private final ValueInitializer valueInitializer = new ValueInitializer();

   @lombok.Value
   public static class Context {
      JavaCodeGenerationConfig codeGenerationConfig;
      boolean isOptional;
   }

   @Override
   public String visitBase( final ModelElement modelElement, final Context context ) {
      throw new UnsupportedOperationException( "Can't create value expression for model element: " + modelElement );
   }

   @Override
   public String visitScalarValue( final ScalarValue value, final Context context ) {
      final String valueExpression = generateValueExpression( value, context );
      if ( context.isOptional() ) {
         return "Optional.of(" + valueExpression + ")";
      }
      return valueExpression;
   }

   private String generateValueExpression( final ScalarValue value, final Context context ) {
      final String typeUri = value.getType().as( Scalar.class ).getUrn();
      if ( typeUri.equals( RDF.langString.getURI() ) ) {
         context.getCodeGenerationConfig().getImportTracker().importExplicit( LangString.class );
         context.getCodeGenerationConfig().getImportTracker().importExplicit( Locale.class );
         final LangString langStringValue = (LangString) value.as( ScalarValue.class ).getValue();
         return String.format( "new LangString(\"%s\", Locale.forLanguageTag(\"%s\"))", StringEscapeUtils.escapeJava( langStringValue.getValue() ),
               langStringValue.getLanguageTag().toLanguageTag() );
      }

      final Resource typeResource = ResourceFactory.createResource( typeUri );
      final Class<?> javaType = DataType.getJavaTypeForMetaModelType( typeResource, value.getMetaModelVersion() );
      context.getCodeGenerationConfig().getImportTracker().importExplicit( javaType );
      return valueInitializer.apply( typeResource, javaType, "\"" + StringEscapeUtils.escapeJava( value.getValue().toString() ) + "\"",
            value.getMetaModelVersion() );
   }

   @Override
   public String visitCollectionValue( final CollectionValue collection, final Context context ) {
      final Class<?> collectionClass = collection.getValues().getClass();
      context.getCodeGenerationConfig().getImportTracker().importExplicit( collectionClass );
      final StringBuilder result = new StringBuilder();
      result.append( "new " );
      result.append( collectionClass.getSimpleName() );
      result.append( "<>() {{" );
      collection.getValues().forEach( value -> {
         result.append( "add(" );
         result.append( value.accept( this, context ) );
         result.append( ");" );
      } );
      result.append( "}}" );
      return result.toString();
   }

   @Override
   public String visitEntityInstance( final EntityInstance instance, final Context context ) {
      final Entity entity = instance.getType().as( Entity.class );
      return "new " + entity.getName() + entity.getProperties().stream().sequential().map( property -> {
         final Value value = instance.getAssertions().get( property );
         if ( value == null ) {
            if ( property.isOptional() ) {
               return "null";
            }
            throw new CodeGenerationException( "EntityInstance " + instance.getName() + " is missing value for Property " + property.getName() );
         }
         final Context newContext = new Context( context.codeGenerationConfig, property.isOptional() );
         return value.accept( this, newContext );
      } ).collect( Collectors.joining( ",", "(", ")" ) );
   }
}

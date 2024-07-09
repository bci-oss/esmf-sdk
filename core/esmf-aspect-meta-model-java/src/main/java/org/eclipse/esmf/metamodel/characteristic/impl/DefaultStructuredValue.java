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

package org.eclipse.esmf.metamodel.characteristic.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import org.eclipse.esmf.aspectmodel.loader.MetaModelBaseAttributes;
import org.eclipse.esmf.aspectmodel.visitor.AspectVisitor;
import org.eclipse.esmf.metamodel.Type;
import org.eclipse.esmf.metamodel.characteristic.StructuredValue;
import org.eclipse.esmf.metamodel.impl.DefaultCharacteristic;

public class DefaultStructuredValue extends DefaultCharacteristic implements StructuredValue {
   private final String deconstructionRule;
   private final List<Object> elements;

   public DefaultStructuredValue( final MetaModelBaseAttributes metaModelBaseAttributes,
         final Type dataType, final String deconstructionRule, final List<Object> elements ) {
      super( metaModelBaseAttributes, Optional.of( dataType ) );
      this.deconstructionRule = deconstructionRule;
      this.elements = elements;
   }

   @Override
   public String getDeconstructionRule() {
      return deconstructionRule;
   }

   @Override
   public List<Object> getElements() {
      return elements;
   }

   /**
    * Accepts an Aspect visitor
    *
    * @param visitor The visitor to accept
    * @param <T> The result type of the traversal operation
    * @param <C> The context of the visitor traversal
    */
   @Override
   public <T, C> T accept( final AspectVisitor<T, C> visitor, final C context ) {
      return visitor.visitStructuredValue( this, context );
   }

   @Override
   public String toString() {
      return new StringJoiner( ", ", DefaultStructuredValue.class.getSimpleName() + "[", "]" )
            .add( "deconstructionRule='" + deconstructionRule + "'" )
            .add( "elements=" + elements )
            .toString();
   }

   @Override
   public boolean equals( final Object o ) {
      if ( this == o ) {
         return true;
      }
      if ( o == null || getClass() != o.getClass() ) {
         return false;
      }
      if ( !super.equals( o ) ) {
         return false;
      }
      final DefaultStructuredValue that = (DefaultStructuredValue) o;
      return Objects.equals( deconstructionRule, that.deconstructionRule )
            && Objects.equals( elements, that.elements );
   }

   @Override
   public int hashCode() {
      return Objects.hash( super.hashCode(), deconstructionRule, elements );
   }
}

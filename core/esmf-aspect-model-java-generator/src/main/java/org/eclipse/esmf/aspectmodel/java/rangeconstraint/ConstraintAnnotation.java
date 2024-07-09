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

package org.eclipse.esmf.aspectmodel.java.rangeconstraint;

import org.eclipse.esmf.metamodel.BoundDefinition;

public abstract class ConstraintAnnotation implements AnnotationExpression {
   protected Class<?> targetAnnotation;

   public String buildConstraintAnnotation( final Class<?> beanAnnotation, final String expression ) {
      return "@" + beanAnnotation.getSimpleName() + "(" + expression + ")\n";
   }

   @Override
   public String apply( final Object value, final BoundDefinition boundDefinition ) {
      return buildConstraintAnnotation( targetAnnotation,
            new AnnotationFieldBuilder().setValueWithQuotesForAnnotation( value ).setBoundDefinition( boundDefinition )
                                        .build() );
   }
}

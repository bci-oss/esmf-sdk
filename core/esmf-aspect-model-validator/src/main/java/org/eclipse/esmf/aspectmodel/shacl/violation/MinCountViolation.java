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

package org.eclipse.esmf.aspectmodel.shacl.violation;

import org.eclipse.esmf.aspectmodel.shacl.constraint.MinCountConstraint;

/**
 * Violation of a {@link MinCountConstraint}
 *
 * @param context the evaluation context
 * @param allowed the allowed value
 * @param actual the encountered value
 */
public record MinCountViolation( EvaluationContext context, int allowed, int actual ) implements Violation {
   public static final String ERROR_CODE = "ERR_MIN_COUNT";

   @Override
   public String errorCode() {
      return ERROR_CODE;
   }

   @Override
   public String violationSpecificMessage() {
      return allowed == 1 ?
            String.format( "Mandatory property %s is missing on %s.", propertyName(), elementName() ) :
            String.format( "Property %s must be present on %s at least %d time%s, but is present only %d time%s.",
                  propertyName(), elementName(), allowed, allowed > 1 ? "s" : "", actual, actual > 1 ? "s" : "" );
   }

   @Override
   public <T> T accept( final Visitor<T> visitor ) {
      return visitor.visitMinCountViolation( this );
   }
}

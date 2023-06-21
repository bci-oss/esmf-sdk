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

package org.eclipse.esmf.aspectmodel.shacl.constraint;

import java.util.List;

import org.apache.jena.rdf.model.RDFNode;

import org.eclipse.esmf.aspectmodel.shacl.Shape;
import org.eclipse.esmf.aspectmodel.shacl.violation.EvaluationContext;
import org.eclipse.esmf.aspectmodel.shacl.violation.LanguageFromListViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;

/**
 * Implements <a href="https://www.w3.org/TR/shacl/#LanguageInConstraintComponent">sh:languageIn</a>
 *
 * @param allowedLanguages the list of allowed language tags
 */
public record AllowedLanguagesConstraint( List<String> allowedLanguages ) implements Constraint {
   @Override
   public List<Violation> apply( final RDFNode rdfNode, final EvaluationContext context ) {
      final List<Violation> nodeKindViolations = new NodeKindConstraint( Shape.NodeKind.Literal ).apply( rdfNode, context );
      if ( !nodeKindViolations.isEmpty() ) {
         return nodeKindViolations;
      }

      final String language = rdfNode.asLiteral().getLanguage();
      return allowedLanguages.contains( language ) ?
            List.of() :
            List.of( new LanguageFromListViolation( context, allowedLanguages, language ) );
   }

   @Override
   public String name() {
      return "sh:languageIn";
   }

   @Override
   public <T> T accept( final Visitor<T> visitor ) {
      return visitor.visitAllowedLanguagesConstraint( this );
   }
}

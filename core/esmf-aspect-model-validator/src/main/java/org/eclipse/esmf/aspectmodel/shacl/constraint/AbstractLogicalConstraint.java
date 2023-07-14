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
import java.util.Optional;

import org.eclipse.esmf.aspectmodel.shacl.Shape;
import org.eclipse.esmf.aspectmodel.shacl.violation.EvaluationContext;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;

import org.apache.jena.rdf.model.RDFNode;

/**
 * Abstract base class for {@link AndConstraint}, {@link OrConstraint} and {@link XoneConstraint}.
 */
public abstract class AbstractLogicalConstraint implements Constraint {
   protected final List<Shape> shapes;

   protected AbstractLogicalConstraint( final List<Shape> shapes ) {
      this.shapes = shapes;
   }

   protected List<List<Violation>> violationsPerShape( final RDFNode rdfNode, final EvaluationContext context ) {
      return shapes.stream().map( shape -> shape.accept( new ViolationsForShape( rdfNode, context ) ) ).toList();
   }

   protected long numberOfEmptyViolationLists( final List<List<Violation>> violationsPerConstraint ) {
      return violationsPerConstraint.stream().filter( List::isEmpty ).count();
   }

   public List<Shape> shapes() {
      return shapes;
   }

   protected static class ViolationsForShape implements Shape.Visitor<List<Violation>> {
      private final RDFNode rdfNode;
      private final EvaluationContext context;

      protected ViolationsForShape( final RDFNode rdfNode, final EvaluationContext context ) {
         this.rdfNode = rdfNode;
         this.context = context;
      }

      @Override
      public List<Violation> visitNodeShape( final Shape.Node shape ) {
         final EvaluationContext newContext = new EvaluationContext( context.element(), context.shape(),
               context.propertyShape(), context.property(), Optional.of( context ), context.offendingStatements(), context.validator(),
               context.resolvedModel() );
         return shape.attributes().constraints().stream().flatMap( constraint ->
                     constraint.apply( rdfNode, newContext ).stream() )
               .toList();
      }

      @Override
      public List<Violation> visitPropertyShape( final Shape.Property propertyShape ) {
         return context.validator().validateShapeForElement( context.element(), (Shape.Node) context.shape(), propertyShape,
               context.resolvedModel(), Optional.of( context ) );
      }
   }
}

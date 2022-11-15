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

package io.openmanufacturing.sds.aspectmodel.shacl.constraint.js;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * The wrapper that wraps a {@link Graph} in the JavaScript context.
 * Implements <a href="https://www.w3.org/TR/shacl-js/#js-api-graphs">SHACL JS Graph</a>.
 */
public class JsGraph {
   private final Graph graph;

   public JsGraph( final Graph graph ) {
      this.graph = graph;
   }

   public Graph getGraph() {
      return graph;
   }

   @SuppressWarnings( "unused" ) // The find function is called from JavaScript contexts
   public JSTripleIterator find( final Object subject, final Object predicate, final Object object ) {
      return new JSTripleIterator( getGraph().find( getNode( subject ), getNode( predicate ), getNode( object ) ) );
   }

   private Node getNode( final Object obj ) {
      if ( obj == null ) {
         return null;
      } else if ( obj instanceof JsTerm ) {
         return ((JsTerm) obj).getNode();
      } else {
         throw new IllegalArgumentException( "Unsupported term type " + obj );
      }
   }

   public static class JSTripleIterator {
      private final ExtendedIterator<Triple> it;

      JSTripleIterator( final ExtendedIterator<Triple> it ) {
         this.it = it;
      }

      public void close() {
         it.close();
      }

      public JsTriple next() {
         if ( it.hasNext() ) {
            return JsFactory.asJsTriple( it.next() );
         } else {
            close();
            return null;
         }
      }
   }
}

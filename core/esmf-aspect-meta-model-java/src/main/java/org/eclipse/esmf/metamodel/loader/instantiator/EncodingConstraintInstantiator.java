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

package org.eclipse.esmf.metamodel.loader.instantiator;

import java.nio.charset.Charset;

import org.eclipse.esmf.aspectmodel.vocabulary.SammNs;
import org.eclipse.esmf.constraint.EncodingConstraint;
import org.eclipse.esmf.constraint.impl.DefaultEncodingConstraint;
import org.eclipse.esmf.metamodel.loader.Instantiator;
import org.eclipse.esmf.metamodel.loader.MetaModelBaseAttributes;
import org.eclipse.esmf.metamodel.loader.ModelElementFactory;

import org.apache.jena.rdf.model.Resource;

public class EncodingConstraintInstantiator extends Instantiator<EncodingConstraint> {
   public EncodingConstraintInstantiator( final ModelElementFactory modelElementFactory ) {
      super( modelElementFactory, EncodingConstraint.class );
   }

   @Override
   public EncodingConstraint apply( final Resource encodingConstraint ) {
      final MetaModelBaseAttributes metaModelBaseAttributes = buildBaseAttributes( encodingConstraint );
      final String value = encodingConstraint.getProperty( SammNs.SAMM.value() ).getObject().toString();
      final String encoding = value.substring( value.indexOf( '#' ) + 1 );
      return new DefaultEncodingConstraint( metaModelBaseAttributes, Charset.forName( encoding ) );
   }
}

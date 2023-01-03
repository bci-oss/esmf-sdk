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

package io.openmanufacturing.sds.metamodel.loader.instantiator;

import java.util.Optional;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import io.openmanufacturing.sds.characteristic.Quantifiable;
import io.openmanufacturing.sds.metamodel.Type;
import io.openmanufacturing.sds.metamodel.Unit;
import io.openmanufacturing.sds.characteristic.impl.DefaultQuantifiable;
import io.openmanufacturing.sds.metamodel.loader.Instantiator;
import io.openmanufacturing.sds.metamodel.loader.MetaModelBaseAttributes;
import io.openmanufacturing.sds.metamodel.loader.ModelElementFactory;

public class QuantifiableInstantiator extends Instantiator<Quantifiable> {
   public QuantifiableInstantiator( final ModelElementFactory modelElementFactory ) {
      super( modelElementFactory, Quantifiable.class );
   }

   @Override
   public Quantifiable apply( final Resource quantifiable ) {
      final MetaModelBaseAttributes metaModelBaseAttributes = buildBaseAttributes( quantifiable );
      final Type type = getType( quantifiable );
      final Optional<Unit> unit = optionalAttributeValue( quantifiable, bammc.unit() )
            .map( Statement::getResource )
            .map( modelElementFactory::findOrCreateUnit );
      return new DefaultQuantifiable( metaModelBaseAttributes, type, unit );
   }
}

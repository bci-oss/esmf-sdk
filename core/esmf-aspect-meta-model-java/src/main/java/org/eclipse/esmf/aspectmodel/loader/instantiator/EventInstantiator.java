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

package org.eclipse.esmf.aspectmodel.loader.instantiator;

import java.util.List;

import org.eclipse.esmf.metamodel.vocabulary.SammNs;
import org.eclipse.esmf.metamodel.Event;
import org.eclipse.esmf.metamodel.Property;
import org.eclipse.esmf.metamodel.impl.DefaultEvent;
import org.eclipse.esmf.aspectmodel.loader.Instantiator;
import org.eclipse.esmf.aspectmodel.loader.MetaModelBaseAttributes;
import org.eclipse.esmf.aspectmodel.loader.ModelElementFactory;

import org.apache.jena.rdf.model.Resource;

public class EventInstantiator extends Instantiator<Event> {
   public EventInstantiator( final ModelElementFactory modelElementFactory ) {
      super( modelElementFactory, Event.class );
   }

   @Override
   public Event apply( final Resource event ) {
      final MetaModelBaseAttributes metaModelBaseAttributes = buildBaseAttributes( event );
      final List<Property> events = getPropertiesModels( event, SammNs.SAMM.parameters() );
      return new DefaultEvent( metaModelBaseAttributes, events );
   }
}

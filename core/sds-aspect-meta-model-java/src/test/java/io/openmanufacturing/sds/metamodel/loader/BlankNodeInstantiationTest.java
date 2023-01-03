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

package io.openmanufacturing.sds.metamodel.loader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.openmanufacturing.sds.aspectmetamodel.KnownVersion;
import io.openmanufacturing.sds.characteristic.List;
import io.openmanufacturing.sds.metamodel.Aspect;
import io.openmanufacturing.sds.metamodel.Characteristic;
import io.openmanufacturing.sds.test.TestAspect;

public class BlankNodeInstantiationTest extends MetaModelInstantiatorTest {

   @ParameterizedTest
   @MethodSource( value = "allVersions" )
   void testBlankNodeInstantiationAndSyntheticNameCalculation( final KnownVersion metaModelVersion ) {
      final Aspect aspect = loadAspect( TestAspect.MODEL_WITH_BLANK_AND_ADDITIONAL_NODES, metaModelVersion );
      assertThat( aspect.getProperties() ).hasSize( 1 );
      final List list = (List) aspect.getProperties().get( 0 ).getCharacteristic().get();
      final Characteristic characteristic = list.getElementCharacteristic().get();
      assertThat( characteristic.getName() ).isEqualTo( "NumberListTrait" );
   }
}

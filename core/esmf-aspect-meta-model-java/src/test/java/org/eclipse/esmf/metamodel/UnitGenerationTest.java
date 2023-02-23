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
package org.eclipse.esmf.metamodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class UnitGenerationTest {
   @Test
   public void testUnitFromName() {
      final Optional<Unit> optionalUnit = Units.fromName( "degreeCelsius" );
      final Unit unit = optionalUnit.get();
      assertThat( unit.getPreferredName( Locale.ENGLISH ) ).isEqualTo( "degree Celsius" );

      assertThat( unit.getReferenceUnit() ).get().isEqualTo( "kelvin" );
      assertThat( unit.getConversionFactor().get() ).isEqualTo( "1 × K" );
   }

   @Test
   public void testUnitWithoutReferenceUnit() {
      final Unit ampere = Units.fromName( "ampere" ).get();
      assertThat( ampere.getReferenceUnit() ).isNotPresent();
   }

   @Test
   public void testUnitFromcode() {
      final Optional<Unit> unit = Units.fromCode( "A97" );
      assertThat( unit.get() ).isEqualTo( Units.fromName( "hectopascal" ).get() );
   }

   @Test
   public void testUnitFromSymbol() {
      // Unit with unique symbol
      assertThat( Units.fromSymbol( "kg" ) ).containsExactly( Units.fromName( "kilogram" ).get() );

      // Mutiple units with the same symbol
      assertThat( Units.fromSymbol( "rad" ).size() ).isGreaterThan( 1 );
   }

   @Test
   public void testUnitsWithQuantityKind() {
      final Set<Unit> units = Units.unitsWithQuantityKind( QuantityKinds.DISTANCE );
      Assertions.assertThat( units ).containsAnyOf( Units.fromName( "nanometre" ).get(), Units.fromName( "metre" ).get(),
            Units.fromName( "kilometre" ).get(), Units.fromName( "foot" ).get(), Units.fromName( "lightYear" ).get() );
      Assertions.assertThat( units ).doesNotContain( Units.fromName( "degreeCelsius" ).get(), Units.fromName( "litre" ).get(),
            Units.fromName( "electronvoltPerMetre" ).get() );
   }

   @Test
   public void testGeneratedQuantityKinds() {
      assertThat( QuantityKinds.values().length ).isGreaterThanOrEqualTo( 80 );
   }

   @Test
   public void testQuantityKindFromName() {
      final Optional<QuantityKind> optionalQuantityKind = QuantityKinds.fromName( "distance" );
      assertThat( optionalQuantityKind ).get().isEqualTo( QuantityKinds.DISTANCE );
      final QuantityKind quantityKind = optionalQuantityKind.get();
      assertThat( quantityKind.getName() ).isEqualTo( "distance" );
      assertThat( quantityKind.getLabel() ).isEqualTo( "distance" );
   }

   @Test
   public void testJavaScriptUnits() {
      final InputStream js = UnitGenerationTest.class.getClassLoader().getResourceAsStream( "samm-units.js" );
      final String sammUnits = new Scanner( js, "UTF-8" ).useDelimiter( "\\A" ).next();

      final Context context = Context.enter();
      final Scriptable scope = context.initStandardObjects();

      // Evaluate .js
      context.evaluateString( scope, sammUnits, "<cmd>", 1, null );

      final ScriptableObject samm = (ScriptableObject) scope.get( "samm", scope );
      assertThat( samm ).isNotNull();
      assertThat( samm ).isNotEqualTo( Scriptable.NOT_FOUND );
      assertThat( Context.toString( samm ) ).isEqualTo( "[object Object]" );

      final ScriptableObject units = (ScriptableObject) samm.get( "units" );
      assertThat( units ).isNotNull();
      assertThat( units ).isNotEqualTo( Scriptable.NOT_FOUND );
      assertThat( Context.toString( units ) ).isEqualTo( "[object Object]" );
      assertThat( units.size() ).isGreaterThanOrEqualTo( 800 );

      final ScriptableObject degreeCelsius = (ScriptableObject) units.get( "degreeCelsius" );
      assertThat( degreeCelsius ).isNotNull();
      assertThat( degreeCelsius ).isNotEqualTo( Scriptable.NOT_FOUND );
      assertThat( Context.toString( degreeCelsius ) ).isEqualTo( "[object Object]" );
      assertThat( Context.toString( degreeCelsius.get( "name" ) ) ).isEqualTo( "degreeCelsius" );
      assertThat( Context.toString( degreeCelsius.get( "label" ) ) ).isEqualTo( "degree Celsius" );

      final ScriptableObject quantityKinds = (ScriptableObject) samm.get( "quantityKinds" );
      assertThat( quantityKinds ).isNotNull();
      assertThat( quantityKinds ).isNotEqualTo( Scriptable.NOT_FOUND );

      assertThat( quantityKinds.size() ).isGreaterThanOrEqualTo( 80 );
   }
}

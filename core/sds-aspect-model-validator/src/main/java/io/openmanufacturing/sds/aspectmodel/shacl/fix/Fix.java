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

package io.openmanufacturing.sds.aspectmodel.shacl.fix;

import io.openmanufacturing.sds.aspectmodel.shacl.violation.EvaluationContext;

public interface Fix {
   EvaluationContext context();

   String description();

   <T> T accept( Visitor<T> visitor );

   interface Visitor<T> {
      T visit( Fix fix );

      T visitReplaceValue( ReplaceValue replaceValue );
   }
}


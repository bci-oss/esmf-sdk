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

package org.eclipse.esmf.constraint;

import org.eclipse.esmf.metamodel.Constraint;

/**
 * Defines the scale as well as the number of integral digits for a fixed point number.
 *
 * @since SAMM 1.0.0
 */
public interface FixedPointConstraint extends Constraint {

   Integer getScale();

   Integer getInteger();
}

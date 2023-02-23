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

/**
 * A quantity kind, e.g. length or temperature.
 *
 * @since SAMM 1.0.0
 */
public interface QuantityKind extends NamedElement {
   /**
    * Returns the quantity kind's human-readable name
    */
   String getLabel();
}

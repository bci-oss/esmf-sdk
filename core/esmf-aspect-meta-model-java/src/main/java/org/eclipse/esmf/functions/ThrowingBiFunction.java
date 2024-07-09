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

package org.eclipse.esmf.functions;

/**
 * A BiFunction similar to {@link java.util.function.BiFunction} except a {@link Throwable} can be thrown
 *
 * @param <T> the type of the first input to the function
 * @param <U> the type of the second input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of Throwable that is thrown
 */
@FunctionalInterface
public interface ThrowingBiFunction<T, U, R, E extends Throwable> {
   R apply( T t, U u ) throws E;
}

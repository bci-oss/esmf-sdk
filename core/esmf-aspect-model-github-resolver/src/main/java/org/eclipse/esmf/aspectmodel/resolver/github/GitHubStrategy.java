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

package org.eclipse.esmf.aspectmodel.resolver.github;

import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.resolver.GithubRepository;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategy;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategySupport;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resolution strategy to retrieve files from a remote GitHub repository
 */
public class GitHubStrategy extends GitHubModelSource implements ResolutionStrategy {
   private static final Logger LOG = LoggerFactory.getLogger( GitHubStrategy.class );

   /**
    * Constructor.
    *
    * @param sourceConfig the configuration for the model source
    */
   public GitHubStrategy( final GithubModelSourceConfig sourceConfig ) {
      super( sourceConfig );
   }

   /**
    * Convenience constructor. Proxy settings are automatically detected, no authentication is used.
    *
    * @param repository the GitHub repository
    * @param directory the relative directory inside the repository
    */
   public GitHubStrategy( final GithubRepository repository, final String directory ) {
      super( repository, directory );
   }

   @Override
   public AspectModelFile apply( final AspectModelUrn aspectModelUrn, final ResolutionStrategySupport resolutionStrategySupport )
         throws ModelResolutionException {
      return loadContentsForNamespace( aspectModelUrn )
            .peek( aspectModelFile -> {
               LOG.debug( "Found aspect model file at {} ", aspectModelFile.sourceLocation() );
            } )
            .filter( file -> resolutionStrategySupport.containsDefinition( file, aspectModelUrn ) )
            .findFirst()
            .orElseThrow( () -> new ModelResolutionException(
                  "No model file containing %s could be found in GitHub repository: %s/%s in branch/tag %s".formatted(
                        aspectModelUrn,
                        config.repository().owner(),
                        config.repository().repository(),
                        config.repository().branchOrTag().name() ) ) );
   }
}

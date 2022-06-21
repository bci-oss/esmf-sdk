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
package io.openmanufacturing.sds;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.fusesource.jansi.AnsiConsole;

import io.openmanufacturing.sds.aspect.AspectCommand;
import picocli.CommandLine;

@CommandLine.Command( name = BammCli.COMMAND_NAME,
      description = "Command line tool for working with Aspect Models",
      subcommands = { CommandLine.HelpCommand.class },
      headerHeading = "@|bold Usage|@:%n%n",
      descriptionHeading = "%n@|bold Description|@:%n%n",
      parameterListHeading = "%n@|bold Parameters|@:%n",
      optionListHeading = "%n@|bold Options|@:%n",
      footer = "%nRun @|bold " + BammCli.COMMAND_NAME + " help <command>|@ to display its help."
            + "%nDocumentation: https://openmanufacturingplatform.github.io/sds-documentation/"
)
@SuppressWarnings( "squid:S1147" ) // System.exit is really required here, this is a CLI tool
public class BammCli extends AbstractCommand {

   public static final String COMMAND_NAME = "bamm";

   private final CommandLine commandLine = new CommandLine( this );

   @CommandLine.Option( names = { "--version" }, description = "Show current version" )
   private boolean version;

   public void run( final String... argv ) throws Exception {
      main( argv );
   }

   public static void main( final String[] argv ) throws Exception {
      AnsiConsole.systemInstall();

      final BammCli command = new BammCli();
      final CommandLine commandLine = command.commandLine
            .addSubcommand( new AspectCommand() )
            .setCaseInsensitiveEnumValuesAllowed( true );

      final int exitCode = commandLine.execute( argv );
      AnsiConsole.systemUninstall();
      System.exit( exitCode );
   }

   protected String format( final String string ) {
      return commandLine.getColorScheme().ansi().string( string );
   }

   private Properties loadProperties( final String filename ) {
      final Properties properties = new Properties();
      final InputStream propertiesResource = BammCli.class.getClassLoader().getResourceAsStream( filename );
      try {
         properties.load( propertiesResource );
      } catch ( final IOException exception ) {
         throw new RuntimeException( "Failed to load Properties: " + filename );
      }
      return properties;
   }

   @Override
   public void run() {
      if ( version ) {
         final Properties applicationProperties = loadProperties( "application.properties" );
         final Properties gitProperties = loadProperties( "git.properties" );
         System.out.printf( "bamm-cli - %s%nVersion: %s%nBuild date: %s%nGit commit: %s%n",
               applicationProperties.get( "application.name" ),
               applicationProperties.get( "version" ),
               applicationProperties.get( "build.date" ),
               gitProperties.get( "git.commit.id" ) );
         System.exit( 0 );
      }
      System.out.println( commandLine.getHelp().fullSynopsis() );
      System.out.println( format( "Run @|bold " + commandLine.getCommandName() + " help|@ for help." ) );
   }
}

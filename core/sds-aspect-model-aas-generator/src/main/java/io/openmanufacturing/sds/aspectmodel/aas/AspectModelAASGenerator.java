/*
 * Copyright (c) 2021, 2022 Robert Bosch Manufacturing Solutions GmbH
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
package io.openmanufacturing.sds.aspectmodel.aas;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;

import io.adminshell.aas.v3.dataformat.SerializationException;
import io.adminshell.aas.v3.dataformat.aasx.AASXSerializer;
import io.adminshell.aas.v3.dataformat.xml.XmlSerializer;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import io.openmanufacturing.sds.metamodel.Aspect;

/** Generator that generates an AASX file containing an AAS submodel for a given Aspect model */
public class AspectModelAASGenerator {

   /**
    * Generates an AASX archive file for a given Aspect and writes it to a given OutputStream provided by <code>nameMapper<code/>
    *
    * @param aspect the Aspect for which an AASX archive shall be generated
    * @param nameMapper a Name Mapper implementation, which provides an OutputStream for a given filename
    * @throws IOException in case the generation can not properly be executed
    */
   public void generateAASXFile( final Aspect aspect, final Function<String, OutputStream> nameMapper )
         throws IOException {
      try ( final OutputStream output = nameMapper.apply( aspect.getName() ) ) {
         output.write( generateAasxOutput( aspect ).toByteArray() );
      }
   }

   /**
    * Generates an AAS XML archive file for a given Aspect and writes it to a given OutputStream provided by <code>nameMapper<code/>
    *
    * @param aspect the Aspect for which an AASX archive shall be generated
    * @param nameMapper a Name Mapper implementation, which provides an OutputStream for a given filename
    * @throws IOException in case the generation can not properly be executed
    */
   public void generateAasXmlFile(
         final Aspect aspect, final Function<String, OutputStream> nameMapper ) throws IOException {
      try ( final OutputStream output = nameMapper.apply( aspect.getName() ) ) {
         output.write( generateXmlOutput( aspect ).toByteArray() );
      }
   }

   protected ByteArrayOutputStream generateAasxOutput( Aspect aspect ) throws IOException {
      final AspectModelAASVisitor visitor = new AspectModelAASVisitor();
      AssetAdministrationShellEnvironment environment = visitor.visitAspect( aspect, null );

      try ( ByteArrayOutputStream out = new ByteArrayOutputStream() ) {
         AASXSerializer serializer = new AASXSerializer();
         serializer.write( environment, null, out );
         return out;
      } catch ( SerializationException e ) {
         throw new IOException( e );
      }
   }

   protected ByteArrayOutputStream generateXmlOutput( Aspect aspect ) throws IOException {
      final AspectModelAASVisitor visitor = new AspectModelAASVisitor();
      AssetAdministrationShellEnvironment environment = visitor.visitAspect( aspect, null );

      try ( ByteArrayOutputStream out = new ByteArrayOutputStream() ) {
         XmlSerializer serializer = new XmlSerializer();
         serializer.write( out, environment );
         return out;
      } catch ( SerializationException e ) {
         throw new IOException( e );
      }
   }
}

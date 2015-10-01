/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.openflowjava.protocol.impl.deserialization;

import org.opendaylight.openflowjava.protocol.api.extensibility.DeserializerRegistry;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.openflowjava.protocol.impl.deserialization.factories.FlowModInputMessageFactory;
import org.opendaylight.openflowjava.protocol.impl.deserialization.factories.GetFeaturesInputMessageFactory;
import org.opendaylight.openflowjava.protocol.impl.deserialization.factories.PacketOutInputMessageFactory;
import org.opendaylight.openflowjava.protocol.impl.util.SimpleDeserializerRegistryHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.FlowModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PacketOutInput;

public final class NetIdeMessageDeserializerInitializer {

    private NetIdeMessageDeserializerInitializer() {
        throw new UnsupportedOperationException("Utility class shouldn't be instantiated");
    }

    public static void registerMessageDeserializers(DeserializerRegistry registry) {
        // register OF v1.3 NEW message deserializers
        SimpleDeserializerRegistryHelper helper;
        helper = new SimpleDeserializerRegistryHelper(EncodeConstants.OF13_VERSION_ID, registry);
        helper.registerDeserializer(14, null, FlowModInput.class, new FlowModInputMessageFactory());
        helper.registerDeserializer(5, null, GetFeaturesInput.class, new GetFeaturesInputMessageFactory());
        helper.registerDeserializer(13, null, PacketOutInput.class, new PacketOutInputMessageFactory());
    }
}

/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import com.google.common.util.concurrent.Futures;
import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.netide.netiplib.HelloMessage;
import org.opendaylight.netide.netiplib.Protocol;
import org.opendaylight.netide.netiplib.ProtocolVersions;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.hello.Elements;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class ShimSwitchConnectionHandlerImplTest {

    ShimSwitchConnectionHandlerImpl connectionHandler;

    @Mock
    ZeroMQBaseConnector coreConnector;

    @Mock
    InetAddress address;

    @Mock
    ShimRelay shimRelay;

    @Mock
    ConnectionAdaptersRegistry registry;

    @Mock
    ConnectionAdapter connectionAdapter;

    @Mock
    ByteBuf msg;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        connectionHandler = Mockito.spy(new ShimSwitchConnectionHandlerImpl(coreConnector));
        Mockito.stub(connectionHandler.createShimRelay()).toReturn(shimRelay);
        Mockito.stub(connectionHandler.createConnectionAdaptersRegistry()).toReturn(registry);
        connectionHandler.init();
    }

    @Test
    public void testAccept() {
        Assert.assertEquals(true, connectionHandler.accept(address));
    }

    @Test
    public void testOnSwitchConnected() {
        Mockito.doNothing().when(connectionHandler).handshake(connectionAdapter);
        connectionHandler.onSwitchConnected(connectionAdapter);
        Mockito.verify(registry).registerConnectionAdapter(connectionAdapter, null);
        Mockito.verify(connectionAdapter).setMessageListener(Matchers.any(ShimMessageListener.class));
        Mockito.verify(connectionAdapter).setSystemListener(Matchers.any(ShimMessageListener.class));
        Mockito.verify(connectionAdapter).setConnectionReadyListener(Matchers.any(ShimMessageListener.class));
        Mockito.verify(connectionHandler).handshake(connectionAdapter);
    }

    @Test
    public void testHandshake() {
        Mockito.stub(connectionHandler.getMaxOFSupportedProtocol()).toReturn(EncodeConstants.OF13_VERSION_ID);
        connectionHandler.handshake(connectionAdapter);
        HelloInputBuilder builder = new HelloInputBuilder();
        builder.setVersion((short) EncodeConstants.OF13_VERSION_ID);
        builder.setXid(ShimSwitchConnectionHandlerImpl.DEFAULT_XID);
        List<Elements> elements = new ArrayList<Elements>();
        builder.setElements(elements);
        Mockito.verify(connectionAdapter).hello(builder.build());
    }

    @Test
    public void testOnSwitchHelloMessage1() {
        Mockito.stub(connectionHandler.getMaxOFSupportedProtocol()).toReturn(EncodeConstants.OF13_VERSION_ID);
        connectionHandler.onSwitchHelloMessage(0L, (short) EncodeConstants.OF13_VERSION_ID);
        Assert.assertNull(connectionHandler.getSupportedProtocol());
    }

    @Test
    public void testOnSwitchHelloMessage2() {
        Mockito.stub(connectionHandler.getMaxOFSupportedProtocol()).toReturn(EncodeConstants.OF13_VERSION_ID);
        connectionHandler.onSwitchHelloMessage(1L, (short) EncodeConstants.OF13_VERSION_ID);
        Assert.assertEquals(EncodeConstants.OF13_VERSION_ID,
                connectionHandler.getSupportedProtocol().getValue1().getValue());
    }

    @Test
    public void testOnSwitchHelloMessage3() {
        Mockito.stub(connectionHandler.getMaxOFSupportedProtocol()).toReturn(EncodeConstants.OF13_VERSION_ID);
        connectionHandler.onSwitchHelloMessage(1L, (short) EncodeConstants.OF10_VERSION_ID);
        Assert.assertEquals(EncodeConstants.OF10_VERSION_ID,
                connectionHandler.getSupportedProtocol().getValue1().getValue());
    }

    @Test
    public void testOnSwitchHelloMessage4() {
        Mockito.stub(connectionHandler.getMaxOFSupportedProtocol()).toReturn(EncodeConstants.OF10_VERSION_ID);
        connectionHandler.onSwitchHelloMessage(1L, (short) EncodeConstants.OF13_VERSION_ID);
        Assert.assertEquals(EncodeConstants.OF10_VERSION_ID,
                connectionHandler.getSupportedProtocol().getValue1().getValue());
    }

    @Test
    public void testOnOpenFlowCoreMessage() {
        Mockito.doReturn(connectionAdapter).when(registry).getConnectionAdapter(1L);
        Mockito.doReturn((short) EncodeConstants.OF13_VERSION_ID).when(msg).readUnsignedByte();
        connectionHandler.onOpenFlowCoreMessage(1L, msg, 0);
        Mockito.verify(shimRelay).sendToSwitch(connectionAdapter, msg, EncodeConstants.OF13_VERSION_ID, coreConnector,
                1L, 0);
    }

    @Test
    public void testOnHelloCoreMessage() {
        Mockito.doNothing().when(connectionHandler).sendGetFeaturesToSwitch((short) EncodeConstants.OF13_VERSION_ID,
                ShimSwitchConnectionHandlerImpl.DEFAULT_XID, connectionAdapter, 0);
        Pair<Protocol, ProtocolVersions> supportedProtocol = new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW,
                ProtocolVersions.parse(Protocol.OPENFLOW, EncodeConstants.OF13_VERSION_ID));
        Mockito.stub(connectionHandler.getSupportedProtocol()).toReturn(supportedProtocol);
        Set<ConnectionAdapter> connections = new HashSet<ConnectionAdapter>();
        connections.add(connectionAdapter);
        Mockito.stub(registry.getConnectionAdapters()).toReturn(connections);
        List<Pair<Protocol, ProtocolVersions>> requestedProtocols = new ArrayList<>();
        requestedProtocols.add(supportedProtocol);
        HelloMessage msg = new HelloMessage();
        msg.getSupportedProtocols().add(supportedProtocol);
        msg.getHeader().setPayloadLength((short) 2);
        msg.getHeader().setModuleId(0);
        connectionHandler.onHelloCoreMessage(requestedProtocols, 0);
        Mockito.verify(coreConnector).SendData(msg.toByteRepresentation());
        Mockito.verify(connectionHandler).sendGetFeaturesToSwitch((short) EncodeConstants.OF13_VERSION_ID,
                ShimSwitchConnectionHandlerImpl.DEFAULT_XID, connectionAdapter, 0);
    }

    @Test
    public void testSendGetFeaturesToSwitch() {
        GetFeaturesOutput messageReply = new GetFeaturesOutputBuilder()
                .setVersion((short) EncodeConstants.OF13_VERSION_ID).build();

        Future<RpcResult<GetFeaturesOutput>> reply = Futures
                .immediateFuture(RpcResultBuilder.success(messageReply).build());

        Mockito.doNothing().when(connectionHandler).sendGetFeaturesOuputToCore(Matchers.any(reply.getClass()),
                Matchers.eq((short) EncodeConstants.OF13_VERSION_ID), Matchers.eq(0), Matchers.eq(connectionAdapter));

        connectionHandler.sendGetFeaturesToSwitch((short) EncodeConstants.OF13_VERSION_ID,
                ShimSwitchConnectionHandlerImpl.DEFAULT_XID, connectionAdapter, 0);
        Mockito.verify(connectionHandler).sendGetFeaturesOuputToCore(Matchers.any(reply.getClass()),
                Matchers.eq((short) EncodeConstants.OF13_VERSION_ID), Matchers.eq(0), Matchers.eq(connectionAdapter));

    }

    @Test
    public void testSendGetFeaturesOuputToCore() {
        GetFeaturesOutput messageReply = new GetFeaturesOutputBuilder().setXid(1L)
                .setVersion((short) EncodeConstants.OF13_VERSION_ID).setDatapathId(new BigInteger("1")).build();
        Future<RpcResult<GetFeaturesOutput>> reply = Futures
                .immediateFuture(RpcResultBuilder.success(messageReply).build());

        connectionHandler.sendGetFeaturesOuputToCore(reply, (short) EncodeConstants.OF13_VERSION_ID, 0,
                connectionAdapter);

        Mockito.verify(registry).registerConnectionAdapter(connectionAdapter, new BigInteger("1"));
        Mockito.verify(shimRelay).sendOpenFlowMessageToCore(coreConnector, messageReply,
                EncodeConstants.OF13_VERSION_ID, 1L, 1L, 0);
    }

}
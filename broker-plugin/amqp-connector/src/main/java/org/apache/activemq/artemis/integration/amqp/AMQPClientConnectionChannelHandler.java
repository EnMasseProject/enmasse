/*
 * Copyright 2021, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package org.apache.activemq.artemis.integration.amqp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import org.apache.activemq.artemis.protocol.amqp.proton.handler.ProtonHandler;
import org.apache.activemq.artemis.spi.core.remoting.ClientConnectionLifeCycleListener;

import java.util.concurrent.Executor;


/**
 * Common handler implementation for client and server side handler.
 */
public class AMQPClientConnectionChannelHandler extends ChannelDuplexHandler {

    private final ChannelGroup group;

    private final ProtonHandler handler;

    private final ClientConnectionLifeCycleListener listener;

    private final Executor listenerExecutor;

    private boolean active = true;

    public AMQPClientConnectionChannelHandler(final ChannelGroup group, final ProtonHandler handler, ClientConnectionLifeCycleListener listener, Executor executor) {
        this.group = group;
        this.handler = handler;
        this.listener = listener;
        this.listenerExecutor = executor;
    }

    protected static Object channelId(Channel channel) {
        return channel.id();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        group.add(ctx.channel());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        synchronized (this) {
            if (active) {
                listenerExecutor.execute(() -> listener.connectionDestroyed(channelId(ctx.channel())));
                super.channelInactive(ctx);
                active = false;
            }
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        ByteBuf buffer = (ByteBuf) msg;

        try {
            handler.inputBuffer(buffer);
        } finally {
            buffer.release();
        }
    }
}

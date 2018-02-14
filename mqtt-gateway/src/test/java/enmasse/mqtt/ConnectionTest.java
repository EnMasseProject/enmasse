/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to connection
 */
@RunWith(VertxUnitRunner.class)
public class ConnectionTest extends MockMqttGatewayTestBase {

    private static final String MQTT_TOPIC = "mytopic";
    private static final String MQTT_MESSAGE = "Hello MQTT on EnMasse";
    private static final String MQTT_WILL_TOPIC = "will";
    private static final String MQTT_WILL_MESSAGE = "Will on EnMasse";
    private static final String CLIENT_ID = "my_client_id";
    private static final String CLIENT_ID_WITH_SESSION = "my_client_id_with_session";

    @Before
    public void before(TestContext context) {
        super.setup(context, false);
    }

    @After
    public void after(TestContext context) {
        super.tearDown(context);
    }

    @Test
    public void connectionCleanSession(TestContext context) {

        this.connect(context, true, CLIENT_ID, true);
    }

    @Test
    public void connectionNotCleanSession(TestContext context) {

        this.connect(context, false, CLIENT_ID_WITH_SESSION, true);
    }

    @Test
    public void connectionNoWill(TestContext context) {

        this.connect(context, true, CLIENT_ID, false);
    }

    @Test
    public void noConnectMessage(TestContext context) throws Exception {

        EventLoopGroup group = new NioEventLoopGroup();
        try {

            Bootstrap bootstrap = new Bootstrap();
            bootstrap
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {

                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("mqttEncoder", MqttEncoder.INSTANCE);
                        }
                    });

            // Start the client.
            ChannelFuture f = bootstrap.connect(MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT).sync();

            f.channel().writeAndFlush(createPublishMessage());

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();

            context.assertTrue(true);

        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }

    private void connect(TestContext context, boolean isCleanSession, String clientId, boolean isWill) {

        Async async = context.async();

        try {

            MemoryPersistence persistence = new MemoryPersistence();

            MqttConnectOptions options = new MqttConnectOptions();
            if (isWill) {
                options.setWill(new MqttTopic(MQTT_WILL_TOPIC, null), MQTT_WILL_MESSAGE.getBytes(), 1, false);
            }
            options.setCleanSession(isCleanSession);

            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), clientId, persistence);
            client.connect(options);

            context.assertTrue(true);

            async.complete();

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }

    private final ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);

    private MqttPublishMessage createPublishMessage() {

        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, true, 0);

        MqttPublishVariableHeader mqttPublishVariableHeader = new MqttPublishVariableHeader(MQTT_TOPIC, 1);

        ByteBuf payload =  ALLOCATOR.buffer();
        payload.writeBytes(MQTT_MESSAGE.getBytes(CharsetUtil.UTF_8));

        return new MqttPublishMessage(mqttFixedHeader, mqttPublishVariableHeader, payload);
    }
}

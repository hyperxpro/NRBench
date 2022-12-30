package com.aayushatharva.nrbench;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.UnixChannelOption;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.incubator.channel.uring.IOUringDatagramChannel;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;

public final class Main {

    public static final String DATA_FILE = System.getProperty("data.file");
    private static String ip;
    private static int port;
    private static EventLoopGroup parentGroup;
    private static EventLoopGroup childGroup;
    private static String transport;
    private static int parentThreads;
    private static int childThreads;
    private static String type;
    private static Class<? extends ServerSocketChannel> serverSocketChannel;
    private static Class<? extends DatagramChannel> datagramChannel;
    private static boolean isNative;

    public static void main(String[] args) throws Exception {
        final SelfSignedCertificate ssc = new SelfSignedCertificate("localhost", "EC", 256);

        try {
            transport = args[0];
            parentThreads = Integer.parseInt(args[1]);
            childThreads = Integer.parseInt(args[2]);
            type = args[3];
            ip = args[4];
            port = Integer.parseInt(args[5]);
        } catch (Exception ex) {
            System.out.println("Usages Argument: <TRANSPORT (Nio, Epoll, IoUring)>" +
                    " <PARENT THREADS> <CHILD THREADS>" +
                    " <TYPE (Tcp, Tcp-JdkSsl, Tcp-OpenSsl, Udp, Http11, Http11-JdkSsl, Http11-OpenSsl, Http2-JdkSsl, Http2-OpenSsl)> " +
                    " <IP> " +
                    " <PORT>");
            System.out.println("Optional System Property for loading HTTP response data from file: -Ddata.file=<PATH TO FILE>");
            return;
        }

        System.out.println("Transport: " + transport);
        System.out.println("ParentThreads: " + parentThreads);
        System.out.println("ChildThreads: " + childThreads);
        System.out.println("Type: " + type);
        System.out.println("IP: " + ip);
        System.out.println("Port: " + port);
        System.out.println("Data File: " + DATA_FILE);

        if (transport.equalsIgnoreCase("nio")) {
            parentGroup = new NioEventLoopGroup(parentThreads);
            childGroup = new NioEventLoopGroup(childThreads);

            serverSocketChannel = NioServerSocketChannel.class;
            datagramChannel = NioDatagramChannel.class;
        } else if (transport.equalsIgnoreCase("epoll")) {
            parentGroup = new EpollEventLoopGroup(parentThreads);
            childGroup = new EpollEventLoopGroup(childThreads);

            serverSocketChannel = EpollServerSocketChannel.class;
            datagramChannel = EpollDatagramChannel.class;
            isNative = true;
        } else if (transport.equalsIgnoreCase("iouring")) {
            parentGroup = new IOUringEventLoopGroup(parentThreads);
            childGroup = new IOUringEventLoopGroup(childThreads);

            serverSocketChannel = IOUringServerSocketChannel.class;
            datagramChannel = IOUringDatagramChannel.class;
            isNative = true;
        } else {
            throw new IllegalArgumentException("Unknown Transport: " + transport);
        }

        System.out.println("ParentGroup: " + parentGroup);
        System.out.println("ChildGroup: " + childGroup);
        System.out.println("ServerSocketChannel: " + serverSocketChannel);

        if (type.equalsIgnoreCase("tcp")) {
            System.out.println("Using TCP Echo");
            tcpEcho(null);
        } else if (type.equalsIgnoreCase("tcp-jdkssl")) {
            System.out.println("Using TCP Echo JDK SSL");
            SslContext sslContext = SslContextBuilder.forServer(ssc.key(), ssc.cert())
                    .sslProvider(SslProvider.JDK)
                    .protocols("TLSv1.3")
                    .build();

            tcpEcho(sslContext);
        } else if (type.equalsIgnoreCase("tcp-openssl")) {
            System.out.println("Using TCP Echo OpenSSL");
            SslContext sslContext = SslContextBuilder.forServer(ssc.key(), ssc.cert())
                    .sslProvider(SslProvider.OPENSSL)
                    .protocols("TLSv1.3")
                    .build();

            tcpEcho(sslContext);
        } else if (type.equalsIgnoreCase("udp")) {
            System.out.println("Using UDP Echo");
            udpEcho();
        } else if (type.equalsIgnoreCase("http11")) {
            System.out.println("Using HTTP/1.1");
            http11(null);
        } else if (type.equalsIgnoreCase("http11-jdkssl")) {
            System.out.println("Using HTTP/1.1 JDK SSL");
            SslContext sslContext = SslContextBuilder.forServer(ssc.key(), ssc.cert())
                    .sslProvider(SslProvider.JDK)
                    .protocols("TLSv1.3")
                    .build();

            http11(sslContext);
        } else if (type.equalsIgnoreCase("http11-openssl")) {
            System.out.println("Using HTTP/1.1 OpenSSL");
            SslContext sslContext = SslContextBuilder.forServer(ssc.key(), ssc.cert())
                    .sslProvider(SslProvider.OPENSSL)
                    .protocols("TLSv1.3")
                    .build();

            http11(sslContext);
        } else if (type.equalsIgnoreCase("http2-jdkssl")) {
            System.out.println("Using HTTP/2 JDK SSL");
            SslContext sslContext = SslContextBuilder.forServer(ssc.key(), ssc.cert())
                    .sslProvider(SslProvider.JDK)
                    .protocols("TLSv1.3")
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2,
                            ApplicationProtocolNames.HTTP_1_1
                    ))
                    .build();

            http2(sslContext);
        } else if (type.equalsIgnoreCase("http2-openssl")) {
            System.out.println("Using HTTP/2 OpenSSL");
            SslContext sslContext = SslContextBuilder.forServer(ssc.key(), ssc.cert())
                    .sslProvider(SslProvider.OPENSSL)
                    .protocols("TLSv1.3")
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2,
                            ApplicationProtocolNames.HTTP_1_1
                    ))
                    .build();

            http2(sslContext);
        } else {
            throw new IllegalArgumentException("Unsupported Type: " + type);
        }
    }

    private static void http11(SslContext sslContext) {
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(parentGroup, childGroup)
                .channel(serverSocketChannel)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (sslContext != null) {
                            ch.pipeline().addFirst(sslContext.newHandler(ch.alloc()));
                        }
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(1024 * 10));
                        ch.pipeline().addLast(new ChunkedWriteHandler());
                        ch.pipeline().addLast(new Http11Handler());
                    }
                });

        serverBootstrap.bind(ip, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("Successfully started HTTP/1.1 server");
            } else {
                System.out.println("Failed to start HTTP/1.1 server: " + future.cause().getCause().getMessage());
            }
        });
    }

    private static void http2(SslContext sslContext) {
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(parentGroup, childGroup)
                .channel(serverSocketChannel)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addFirst(sslContext.newHandler(ch.alloc()));
                        ch.pipeline().addLast(new Http2Handler.AlpnHandler());
                    }
                });

        serverBootstrap.bind(ip, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("Successfully started HTTP/2 server");
            } else {
                System.out.println("Failed to start HTTP/2 server: " + future.cause().getCause().getMessage());
            }
        });
    }

    private static void tcpEcho(SslContext sslContext) {
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(parentGroup, childGroup)
                .channel(serverSocketChannel)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (sslContext != null) {
                            ch.pipeline().addFirst(sslContext.newHandler(ch.alloc()));
                        }
                        ch.pipeline().addLast(new TcpEchoHandler());
                    }
                });

        serverBootstrap.bind(ip, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("Successfully started TCP Echo server");
            } else {
                System.out.println("Failed to start TCP Echo server: " + future.cause().getCause().getMessage());
            }
        });
    }

    private static void udpEcho() {
        Bootstrap bootstrap = new Bootstrap()
                .group(childGroup)
                .channel(datagramChannel)
                .option(UnixChannelOption.SO_REUSEPORT, true)
                .handler(new UdpEchoHandler());

        int bindRounds = 1;
        if (isNative) {
            bindRounds = childThreads;
        }

        for (int i = 0; i < bindRounds; i++) {
            bootstrap.bind(ip, port).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    System.out.println("Successfully started UDP Echo server");
                } else {
                    System.out.println("Failed to start UDP Echo server: " + future.cause().getCause().getMessage());
                }
            });
        }
    }
}

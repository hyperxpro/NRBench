package com.aayushatharva.nrbench;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

public final class Http2Handler extends SimpleChannelInboundHandler<Object> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http2HeadersFrame headersFrame) {
            Http2Headers headers = new DefaultHttp2Headers();
            headers.status("200");
            headers.add(HttpHeaderNames.SERVER, "Netty 4.1 HTTP/2 Server");
            headers.add(HttpHeaderNames.CONTENT_LENGTH, "12");
            headers.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);

            ByteBuf content = ctx.channel().alloc().buffer(12);
            ByteBufUtil.writeAscii(content, "Hello World!");

            Http2HeadersFrame http2HeadersFrame = new DefaultHttp2HeadersFrame(headers);
            http2HeadersFrame.stream(headersFrame.stream());

            Http2DataFrame http2DataFrame = new DefaultHttp2DataFrame(content, true);
            http2DataFrame.stream(headersFrame.stream());

            ctx.write(http2HeadersFrame);
            ctx.writeAndFlush(http2DataFrame);
        } else {
            // Ignore other frames such as Settings or Window update.
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Swallow
    }

    public static final class AlpnHandler extends ApplicationProtocolNegotiationHandler {

        AlpnHandler() {
            super(ApplicationProtocolNames.HTTP_1_1);
        }

        @Override
        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
            if (protocol.equalsIgnoreCase(ApplicationProtocolNames.HTTP_2)) {
                ctx.pipeline().addLast(Http2FrameCodecBuilder.forServer().build());
                ctx.pipeline().addLast(new Http2Handler());
            } else if (protocol.equalsIgnoreCase(ApplicationProtocolNames.HTTP_1_1)) {
                ctx.pipeline().addLast(new HttpServerCodec());
                ctx.pipeline().addLast(new HttpObjectAggregator(1024 * 10));
                ctx.pipeline().addLast(new Http11Handler());
            } else {
                throw new IllegalArgumentException("Unsupported ALPN Protocol: " + protocol);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // Swallow
        }
    }
}
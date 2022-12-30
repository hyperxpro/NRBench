package com.aayushatharva.nrbench;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.StandardCharsets;

import static com.aayushatharva.nrbench.Main.DATA_FILE;

public final class Http11Handler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        String data;
        if (DATA_FILE != null) {
            data = StandardCharsets.UTF_8.decode(MemoryMappedCachedFile.load(DATA_FILE).duplicate()).toString();
        } else {
            data = "Hello World!";
        }

        ByteBuf content = ctx.channel().alloc().buffer(data.length());
        ByteBufUtil.writeAscii(content, data);

        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        fullHttpResponse.headers().add(HttpHeaderNames.SERVER, "Netty 4.1 HTTP/1.1 Server");
        fullHttpResponse.headers().add(HttpHeaderNames.CONTENT_LENGTH, data.length());
        fullHttpResponse.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);

        ctx.writeAndFlush(fullHttpResponse);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Swallow
    }
}

package com.aayushatharva.nrbench;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.aayushatharva.nrbench.FileUtil.readFileAsString;
import static com.aayushatharva.nrbench.Main.DATA_FILE;

public final class Http11Handler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final boolean hasDataFile = System.getProperty("data.file") != null;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (hasDataFile) {
            HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            httpResponse.headers().add(HttpHeaderNames.SERVER, "Netty 4.1 HTTP/1.1 Server");
            httpResponse.headers().add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
            httpResponse.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);

            RandomAccessFile raf = new RandomAccessFile(new File(System.getProperty("data.file")), "r");
            long fileLength = raf.length();

            ctx.writeAndFlush(httpResponse);
            ctx.writeAndFlush(new DefaultFileRegion(raf.getChannel(), 0, fileLength));
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            String data = "Hello World!";
            ByteBuf content = ctx.channel().alloc().buffer(data.length());
            ByteBufUtil.writeAscii(content, data);

            FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            fullHttpResponse.headers().add(HttpHeaderNames.SERVER, "Netty 4.1 HTTP/1.1 Server");
            fullHttpResponse.headers().add(HttpHeaderNames.CONTENT_LENGTH, data.length());
            fullHttpResponse.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);

            ctx.writeAndFlush(fullHttpResponse);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Swallow
    }
}

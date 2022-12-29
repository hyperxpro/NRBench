package com.aayushatharva.nrbench;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

public final class UdpEchoHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        DatagramPacket datagramPacket = (DatagramPacket) msg;
        ctx.writeAndFlush(new DatagramPacket(datagramPacket.content(), datagramPacket.sender(), datagramPacket.recipient()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Swallow
    }
}

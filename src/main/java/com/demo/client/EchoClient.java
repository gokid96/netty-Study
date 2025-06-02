package com.demo.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 최신 Netty 4.1.121 기반 에코 클라이언트
 * 개선된 기능: 비동기 입력 처리, 안정적인 연결 관리
 */
public class EchoClient {
    private final String host;
    private final int port;

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            // 프레임 구분자 (줄바꿈 기준)
                            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));

                            // 문자열 디코더/인코더
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

                            // 클라이언트 핸들러
                            pipeline.addLast(new EchoClientHandler());
                        }
                    });

            // 서버에 연결
            ChannelFuture future = bootstrap.connect(host, port).sync();
            System.out.println("서버에 연결되었습니다: " + host + ":" + port);
            System.out.println("명령어: 'quit'(종료), 'time'(시간 확인)");
            System.out.println("메시지를 입력하세요:");

            Channel channel = future.channel();

            // 사용자 입력을 처리하는 별도 스레드
            startInputThread(channel);

            // 연결이 종료될 때까지 대기
            channel.closeFuture().sync();

        } finally {
            group.shutdownGracefully();
            System.out.println("클라이언트가 종료되었습니다.");
        }
    }

    private void startInputThread(Channel channel) {
        Thread inputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (channel.isActive()) {
                        channel.writeAndFlush(line + "\n");

                        // quit 명령어 처리
                        if ("quit".equalsIgnoreCase(line.trim())) {
                            break;
                        }
                    } else {
                        System.out.println("서버 연결이 끊어졌습니다.");
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("입력 처리 중 오류: " + e.getMessage());
            }
        });

        inputThread.setDaemon(true);
        inputThread.start();
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        new EchoClient(host, port).start();
    }
}

/**
 * 에코 클라이언트 메시지 처리 핸들러
 */
class EchoClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("서버와 연결이 활성화되었습니다.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String response = (String) msg;
        System.out.println("서버 응답: " + response);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("서버와의 연결이 종료되었습니다.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("클라이언트 오류: " + cause.getMessage());
        ctx.close();
    }
}
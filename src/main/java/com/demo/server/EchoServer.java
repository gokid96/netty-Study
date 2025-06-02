package com.demo.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

/**
 * Netty 기반 에코 서버
 * 클라이언트가 보낸 메시지를 그대로 돌려주는 서버
 */
public class EchoServer {
    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        // Boss 그룹: 클라이언트 연결 수락만 담당 (1개 스레드)
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);

        // Worker 그룹: 실제 데이터 송수신 처리 (CPU 코어 수만큼 스레드 생성)
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // 서버 부트스트랩: 서버 설정을 위한 헬퍼 클래스
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)         // 스레드 그룹 지정
                    .channel(NioServerSocketChannel.class)  // NIO 기반 서버 소켓 채널
                    .option(ChannelOption.SO_BACKLOG, 128)  // 연결 대기 큐 크기
                    .childOption(ChannelOption.SO_KEEPALIVE, true)  // 연결 유지 설정
                    .childOption(ChannelOption.TCP_NODELAY, true)   // 작은 패킷도 즉시 전송
                    .handler(new LoggingHandler(LogLevel.INFO))     // 서버 레벨 로깅 활성화
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 채널 파이프라인: 데이터 처리 체인 구성
                            ChannelPipeline pipeline = ch.pipeline();

                            // 1단계: 프레임 디코더 - 줄바꿈 문자로 메시지 구분
                            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));

                            // 2단계: 문자열 디코더 - 바이트를 UTF-8 문자열로 변환
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));

                            // 3단계: 문자열 인코더 - 문자열을 바이트로 변환
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

                            // 4단계: 비즈니스 로직 핸들러 - 실제 메시지 처리
                            pipeline.addLast(new EchoServerHandler());
                        }
                    });

            // 서버를 지정된 포트에 바인딩하고 시작
            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("에코 서버가 포트 " + port + "에서 시작되었습니다.");
            System.out.println("클라이언트 연결을 기다리는 중...");

            // 서버가 종료될 때까지 메인 스레드 대기
            future.channel().closeFuture().sync();
        } finally {
            // 프로그램 종료 시 스레드 그룹 정리
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("서버가 종료되었습니다.");
        }
    }

    public static void main(String[] args) throws Exception {
        // 명령행 인자로 포트 지정 가능, 기본값은 9090
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
        new EchoServer(port).start();
    }
}

/**
 * 실제 메시지 처리를 담당하는 핸들러
 * Worker 스레드에서 실행됨
 */
class EchoServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 클라이언트가 연결되었을 때 호출
        String clientAddress = ctx.channel().remoteAddress().toString();
        System.out.println("클라이언트 연결: " + clientAddress);

        // 환영 메시지를 클라이언트에게 전송
        ctx.writeAndFlush("서버에 연결되었습니다! 메시지를 입력하세요.\n");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 클라이언트로부터 메시지를 받을 때마다 호출
        String message = (String) msg;  // StringDecoder에 의해 이미 문자열로 변환됨
        String clientAddress = ctx.channel().remoteAddress().toString();

        System.out.println(clientAddress + " -> " + message);

        // "quit" 명령어: 연결 종료
        if ("quit".equalsIgnoreCase(message.trim())) {
            ctx.writeAndFlush("안녕히 가세요\n").addListener(ChannelFutureListener.CLOSE);
            return;
        }

        // "time" 명령어: 현재 시간 응답
        if ("time".equalsIgnoreCase(message.trim())) {
            String timeResponse = "현재 시간: " + java.time.LocalDateTime.now() + "\n";
            ctx.writeAndFlush(timeResponse);
            return;
        }

        // 일반 메시지: 에코 응답 (받은 메시지를 그대로 돌려줌)
        String echoResponse = "[에코] " + message + "\n";
        ctx.writeAndFlush(echoResponse);  // 비동기로 응답 전송
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 클라이언트 연결이 끊어졌을 때 호출
        String clientAddress = ctx.channel().remoteAddress().toString();
        System.out.println("클라이언트 연결 종료: " + clientAddress);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 예외 발생 시 호출
        System.err.println("오류 발생: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();  // 오류 발생 시 연결 종료
    }
}
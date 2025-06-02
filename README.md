# Netty 에코 서버/클라이언트

## 프로젝트 소개
Netty 4.1.121 프레임워크를 사용하여 구현된 고성능 에코 서버와 클라이언트 애플리케이션입니다. TCP/IP 통신을 기반으로 하며, 비동기 이벤트 기반 아키텍처를 활용합니다.

## 기술 스택
- **Java 17**
- **Netty 4.1.121**
- **NIO** (Non-blocking I/O)

## 실행 방법

### 서버 실행
```bash
java -cp com.demo.server.EchoServer [port]
```
- 기본 포트: 8081

### 클라이언트 실행
```bash
java -cp com.demo.client.EchoClient [host] [port]
```
- 기본 호스트: localhost
- 기본 포트: 8081

## 사용 방법

### 지원되는 명령어
- `time`: 현재 서버 시간 조회
- `quit`: 클라이언트 연결 종료
- 그 외 메시지: 서버가 에코로 응답

## Netty 핵심 개념
- **비동기 + 논블로킹**: EventLoop를 통한 단일 스레드 다중 채널 처리
- **Channel Pipeline**: Decoder → Handler → Encoder 구조
- **스레드 모델**: Boss Group(연결 수락) + Worker Group(I/O 처리)

## 구체적인 동작
- Boss 스레드: 새로운 연결 수락
- Worker 스레드들: 각각 여러 채널을 논블로킹으로 처리
- EventLoop: 각 스레드에서 비동기 이벤트 처리

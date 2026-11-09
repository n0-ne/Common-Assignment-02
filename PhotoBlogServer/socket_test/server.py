import os
import socket
from datetime import datetime


class SocketServer:
    def __init__(self):
        self.bufsize = 1024  # 버퍼 크기 설정

        with open('./response.bin', 'rb') as file:
            self.RESPONSE = file.read()  # 응답 파일 읽기

        self.DIR_PATH = './request'
        self.createDir(self.DIR_PATH)

    def createDir(self, path):
        """디렉토리 생성"""
        try:
            if not os.path.exists(path):
                os.makedirs(path)
        except OSError:
            print("Error: Failed to create the directory.")

    def run(self, ip, port):
        """서버 실행"""
        # 소켓 생성
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind((ip, port))
        self.sock.listen(10)

        print("Start the socket server...")
        print("\"Ctrl+C\" for stopping the server!\r\n")

        try:
            while True:
                # 클라이언트의 요청 대기
                clnt_sock, req_addr = self.sock.accept()
                clnt_sock.settimeout(5.0)  # 타임아웃 설정 (5초)

                print("Request message...\r\n")

                response = b""

                # 여기에 구현 하세요

                # HTTP Request 메시지 수신
                while True:
                    try:
                        data = clnt_sock.recv(self.bufsize)

                        if not data:
                            break

                        response += data

                        # HTTP Header와 Body가 모두 들어오기 시작했는지 확인
                        if b"\r\n\r\n" in response:

                            header, body = response.split(
                                b"\r\n\r\n", 1
                            )

                            content_length = 0

                            # Content-Length 값 확인
                            for line in header.split(b"\r\n"):
                                if line.lower().startswith(
                                    b"content-length:"
                                ):
                                    content_length = int(
                                        line.split(b":", 1)[1].strip()
                                    )
                                    break

                            # Body를 전부 받았다면 수신 종료
                            if content_length > 0:
                                if len(body) >= content_length:
                                    break

                    except socket.timeout:
                        break

                # 실습 1: 클라이언트 요청을 그대로 "request"폴더 하위에
                # "년-월-일-시-분-초.bin" 파일명을 가진 이진파일로 저장함
                
                now = datetime.now().strftime(
                    "%Y-%m-%d-%H-%M-%S"
                )

                bin_path = os.path.join(
                    self.DIR_PATH,
                    now + ".bin"
                )

                with open(bin_path, "wb") as file:
                    file.write(response)

                print("Saved request:", bin_path)


                # 실습 2 : 멀티파트로 전송 받은 이미지 데이터를 별도
                # 이미지 파일로 저장하고, 저장 된 파일을 확인 함

                if b"\r\n\r\n" in response:

                    header, body = response.split(
                        b"\r\n\r\n", 1
                    )

                    boundary = None

                    # HTTP Header에서 boundary 찾기
                    for line in header.split(b"\r\n"):
                        if (
                            line.lower().startswith(b"content-type:")
                            and b"boundary=" in line
                        ):
                            boundary = line.split(
                                b"boundary=", 1
                            )[1].strip()

                            break

                    if boundary is not None:

                        delimiter = b"--" + boundary
                        parts = body.split(delimiter)

                        for part in parts:

                            # filename이 있는 multipart만 파일 데이터
                            if b'filename="' not in part:
                                continue

                            if b"\r\n\r\n" not in part:
                                continue

                            part_header, file_data = part.split(
                                b"\r\n\r\n", 1
                            )

                            filename = None

                            for line in part_header.split(b"\r\n"):

                                if b'filename="' in line:

                                    filename = line.split(
                                        b'filename="', 1
                                    )[1].split(
                                        b'"', 1
                                    )[0].decode(
                                        "utf-8",
                                        errors="ignore"
                                    )

                                    break

                            if filename is not None:

                                # multipart 마지막 CRLF 제거
                                if file_data.endswith(b"\r\n"):
                                    file_data = file_data[:-2]

                                filename = os.path.basename(filename)

                                image_path = os.path.join(
                                    self.DIR_PATH,
                                    filename
                                )

                                with open(image_path, "wb") as file:
                                    file.write(file_data)

                                print(
                                    "Saved image:",
                                    image_path
                                )

                # 응답 전송
                clnt_sock.sendall(self.RESPONSE)

                # 클라이언트 소켓 닫기
                clnt_sock.close()

        except KeyboardInterrupt:
            print("\r\nStop the server...")

        # 서버 소켓 닫기
        self.sock.close()


if __name__ == "__main__":
    server = SocketServer()
    server.run("127.0.0.1", 8000)
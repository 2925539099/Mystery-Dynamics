# -*- coding: utf-8 -*-
"""用 RCON 在开发服务器里放一个傀儡动力核心，读取它的 NBT 验证应力。"""
import socket
import struct
import sys
import time


class Rcon:
    def __init__(self, host="127.0.0.1", port=25575, password="golemtest"):
        self.sock = socket.create_connection((host, port), timeout=10)
        self._send(1, 3, password)
        rid, _ = self._recv()
        if rid == -1:
            raise RuntimeError("RCON 认证失败")

    def _send(self, rid, ptype, body):
        payload = struct.pack("<ii", rid, ptype) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(payload)) + payload)

    def _recv(self):
        raw = self._recv_exact(4)
        (length,) = struct.unpack("<i", raw)
        data = self._recv_exact(length)
        rid, ptype = struct.unpack("<ii", data[:8])
        body = data[8:].split(b"\x00", 1)[0].decode("utf-8", "replace")
        return rid, body

    def _recv_exact(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise RuntimeError("RCON 连接断开")
            buf += chunk
        return buf

    def cmd(self, command):
        self._send(2, 2, command)
        _, body = self._recv()
        return body


def main():
    r = Rcon()
    cmds = [
        "gamerule sendCommandFeedback true",
        "forceload add 0 0 16 16",
        "setblock 0 100 0 minecraft:air",
        "setblock 0 100 0 golem_power:golem_dynamo[facing=north]",
    ]
    for c in cmds:
        print(">", c)
        print(" ", r.cmd(c).strip())

    time.sleep(1.5)

    for q in ("data get block 0 100 0 Speed",
              "data get block 0 100 0 Network",
              "data get block 0 100 0"):
        print(">", q)
        print(" ", r.cmd(q).strip()[:600])

    # 顺便确认方块实体类型注册正确
    print(">", "execute if block 0 100 0 golem_power:golem_dynamo run say block-ok")
    print(" ", r.cmd("execute if block 0 100 0 golem_power:golem_dynamo run say block-ok").strip())


if __name__ == "__main__":
    sys.exit(main())

import json
import logging
import socket
import threading
from typing import List, Optional

logger = logging.getLogger(__name__)

DISCOVERY_PORT = 37020
DISCOVERY_MAGIC = "WATER_LEAKAGE_DISCOVER"


def get_local_ipv4_addresses() -> List[str]:
    """Collect active non-loopback local IPv4 addresses (Wi-Fi, Hotspot, Ethernet)."""
    ips = []

    def add_ip(ip: Optional[str]) -> None:
        if not ip:
            return
        if ip.startswith("127.") or ip.startswith("169.254.") or ip == "0.0.0.0":
            return
        if ip not in ips:
            ips.append(ip)

    # 1. Primary outbound interface IP (does not actually send packets)
    for target in (("8.8.8.8", 80), ("10.255.255.255", 1), ("192.168.255.255", 1), ("192.168.137.255", 1)):
        sock = None
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.settimeout(0.1)
            sock.connect(target)
            add_ip(sock.getsockname()[0])
        except OSError:
            pass
        finally:
            if sock:
                sock.close()

    # 2. All IPv4 interfaces bound to hostname (includes Hotspot / WSL / LAN adapters)
    try:
        hostname = socket.gethostname()
        for info in socket.getaddrinfo(hostname, None, socket.AF_INET):
            add_ip(info[4][0])
    except OSError:
        pass

    return ips


class DiscoveryResponder:
    """Lightweight UDP broadcast responder so the Android app can find the backend without manual IP entry."""

    def __init__(self, api_port: int = 8000, discovery_port: int = DISCOVERY_PORT):
        self.api_port = api_port
        self.discovery_port = discovery_port
        self._stop_event = threading.Event()
        self._thread: Optional[threading.Thread] = None
        self._sock: Optional[socket.socket] = None

    def start(self) -> None:
        if self._thread and self._thread.is_alive():
            return
        self._stop_event.clear()
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
            sock.bind(("0.0.0.0", self.discovery_port))
            sock.settimeout(1.0)
            self._sock = sock
        except OSError as exc:
            logger.warning("Could not bind UDP discovery port %s: %s", self.discovery_port, exc)
            return

        self._thread = threading.Thread(target=self._serve, name="udp-discovery", daemon=True)
        self._thread.start()
        logger.info("UDP LAN discovery responder listening on port %s", self.discovery_port)

    def _serve(self) -> None:
        while not self._stop_event.is_set() and self._sock:
            try:
                data, addr = self._sock.recvfrom(1024)
            except socket.timeout:
                continue
            except OSError:
                break

            try:
                msg = data.decode("utf-8", errors="ignore").strip()
                if DISCOVERY_MAGIC not in msg:
                    continue

                ips = get_local_ipv4_addresses()
                # Prioritize the interface on the same subnet as the requester
                requester_ip = addr[0]
                requester_prefix = ".".join(requester_ip.split(".")[:2])
                ips.sort(key=lambda ip: 0 if ip.startswith(requester_prefix) else 1)

                payload = json.dumps({
                    "service": "water-leakage-api",
                    "port": self.api_port,
                    "hostname": socket.gethostname(),
                    "urls": [f"http://{ip}:{self.api_port}/" for ip in ips],
                }).encode("utf-8")
                self._sock.sendto(payload, addr)
            except OSError:
                continue

    def stop(self) -> None:
        self._stop_event.set()
        if self._sock:
            try:
                self._sock.close()
            except OSError:
                pass
            self._sock = None
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=1.5)
        self._thread = None


discovery_responder = DiscoveryResponder()


"""inbound 토픽을 소비→처리→outbound 로 재발행하는 워커 (백그라운드 스레드)."""
import json
import logging
import threading
import time

from kafka import KafkaConsumer

from app.config.settings import settings
from app.kafka import producer

logger = logging.getLogger(__name__)


class InboundConsumer:
    def __init__(self):
        self.consumer = None
        self._running = False
        self.processed_count = 0

    def start(self):
        self._running = True
        threading.Thread(target=self._consume, daemon=True).start()
        logger.info("[KafkaConsumer] 시작 - topic=%s", settings.kafka_topic_inbound)

    def stop(self):
        self._running = False
        if self.consumer:
            self.consumer.close()

    def _consume(self):
        while self._running:
            try:
                self.consumer = KafkaConsumer(
                    settings.kafka_topic_inbound,
                    bootstrap_servers=settings.kafka_bootstrap_servers,
                    group_id=settings.kafka_consumer_group_id,
                    auto_offset_reset="latest",
                    enable_auto_commit=True,
                    value_deserializer=lambda v: json.loads(v.decode("utf-8")),
                    consumer_timeout_ms=1000,
                )
                while self._running:
                    for message in self.consumer:
                        if not self._running:
                            break
                        self._handle(message.value)
            except Exception as exc:
                logger.warning("[KafkaConsumer] 연결 오류: %s", exc)
                time.sleep(3)
            finally:
                if self.consumer:
                    self.consumer.close()
                    self.consumer = None

    def _handle(self, event: dict):
        self.processed_count += 1
        logger.info("[Worker] 이벤트 수신 - %s", event)
        # 예시 처리: 결과를 outbound 토픽으로 재발행
        result = {"sourceId": event.get("id"), "status": "PROCESSED"}
        try:
            producer.publish(key=event.get("id"), value=result)
        except Exception as exc:
            logger.warning("[Worker] 결과 발행 실패: %s", exc)


inbound_consumer = InboundConsumer()

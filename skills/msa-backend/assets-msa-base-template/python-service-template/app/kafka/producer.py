"""Kafka Producer 래퍼 (지연 초기화)."""
import json
import logging

from kafka import KafkaProducer

from app.config.settings import settings

logger = logging.getLogger(__name__)

_producer: KafkaProducer | None = None


def get_producer() -> KafkaProducer:
    global _producer
    if _producer is None:
        _producer = KafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            key_serializer=lambda v: str(v).encode("utf-8"),
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        )
    return _producer


def publish(key, value: dict, topic: str | None = None):
    topic = topic or settings.kafka_topic_outbound
    get_producer().send(topic, key=key, value=value)
    get_producer().flush()
    logger.info("[Kafka Producer] '%s' 발행 - key=%s", topic, key)

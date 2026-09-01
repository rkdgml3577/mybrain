import logging
from contextlib import asynccontextmanager

import py_eureka_client.eureka_client as eureka_client
from fastapi import FastAPI

from app.config.settings import settings
from app.kafka.consumer import inbound_consumer
from app.router import sample_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(_: FastAPI):
    logger.info("[%s] 서비스 시작", settings.app_name)

    # Eureka 등록 (실패해도 서비스는 계속 동작)
    try:
        await eureka_client.init_async(
            eureka_server=settings.eureka_server_url,
            app_name=settings.app_name,
            instance_port=settings.app_port,
            instance_host=settings.eureka_instance_host,
        )
        logger.info("[Eureka] 서비스 등록 완료")
    except Exception as exc:
        logger.warning("[Eureka] 등록 실패 (무시 가능): %s", exc)

    # Kafka Consumer(백그라운드 워커) 시작
    try:
        inbound_consumer.start()
    except Exception as exc:
        logger.warning("[Kafka] Consumer 시작 실패: %s", exc)

    yield

    logger.info("[%s] 서비스 종료", settings.app_name)
    inbound_consumer.stop()
    try:
        await eureka_client.stop_async()
    except Exception:
        pass


app = FastAPI(
    title="Python Service Template (FastAPI)",
    description="REST + Kafka(consume/produce) + 서비스 호출 + JWT 를 담은 Python 마이크로서비스 템플릿",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(sample_router.router)


@app.get("/health")
async def health():
    return {
        "status": "UP",
        "service": settings.app_name,
        "processedCount": inbound_consumer.processed_count,
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host="0.0.0.0", port=settings.app_port, reload=True)

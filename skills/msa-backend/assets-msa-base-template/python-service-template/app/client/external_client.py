"""다른 서비스를 httpx 로 호출하는 클라이언트 예시."""
import logging

import httpx

from app.config.settings import settings

logger = logging.getLogger(__name__)


async def fetch_resource(resource_id: int) -> dict | None:
    """대상 서비스의 리소스를 조회한다. 공통 응답 래퍼({data:...})를 벗겨서 반환."""
    url = f"{settings.target_service_url}/api/resources/{resource_id}"
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get(url)
            response.raise_for_status()
            body = response.json()
    except Exception as exc:
        logger.warning("[external_client] 호출 실패: %s", exc)
        return None

    if isinstance(body, dict) and "data" in body:
        return body["data"]
    return body

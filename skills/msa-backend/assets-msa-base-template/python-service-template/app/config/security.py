"""JWT 검증 유틸. Auth Server 의 JWK 로 Bearer 토큰을 검증한다.

라우터에서 `token: dict = Depends(verify_token)` 로 붙이면 인증이 강제된다.
인증이 필요 없으면 의존성을 빼면 된다.
"""
import httpx
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt

from app.config.settings import settings

security = HTTPBearer(auto_error=False)

_jwks_cache: dict = {}


async def _get_jwks() -> dict:
    global _jwks_cache
    if not _jwks_cache:
        async with httpx.AsyncClient() as client:
            response = await client.get(settings.jwk_set_uri)
            response.raise_for_status()
            _jwks_cache = response.json()
    return _jwks_cache


def _get_signing_key(token: str, jwks: dict) -> dict:
    header = jwt.get_unverified_header(token)
    kid = header.get("kid")
    for key in jwks.get("keys", []):
        if key.get("kid") == kid:
            return key
    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="일치하는 공개키를 찾을 수 없습니다",
        headers={"WWW-Authenticate": "Bearer"},
    )


async def verify_token(
    credentials: HTTPAuthorizationCredentials = Depends(security),
) -> dict:
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="인증 토큰이 필요합니다",
            headers={"WWW-Authenticate": "Bearer"},
        )
    token = credentials.credentials
    try:
        jwks = await _get_jwks()
        signing_key = _get_signing_key(token, jwks)
        return jwt.decode(
            token,
            signing_key,
            algorithms=["RS256"],
            issuer=settings.jwt_issuer_uri,
            options={"verify_aud": False},
        )
    except JWTError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"유효하지 않은 토큰입니다: {exc}",
            headers={"WWW-Authenticate": "Bearer"},
        )

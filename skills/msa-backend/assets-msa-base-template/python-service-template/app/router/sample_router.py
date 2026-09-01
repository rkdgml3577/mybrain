import logging

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.service import sample_service

# 인증을 켜려면:
#   from fastapi import Depends
#   from app.config.security import verify_token
#   그리고 엔드포인트에 token: dict = Depends(verify_token) 추가

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/resources", tags=["resources"])


class CreateRequest(BaseModel):
    name: str
    description: str | None = None


@router.post("")
async def create(request: CreateRequest):
    return sample_service.create(request.name, request.description)


@router.get("")
async def find_all():
    return sample_service.find_all()


@router.get("/{item_id}")
async def find_by_id(item_id: int):
    item = sample_service.find_by_id(item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="리소스를 찾을 수 없습니다")
    return item

"""간단한 인메모리 CRUD 서비스 예시. 실제로는 DB(SQLAlchemy 등)로 교체한다."""
from itertools import count

_store: dict[int, dict] = {}
_id_seq = count(1)


def create(name: str, description: str | None) -> dict:
    new_id = next(_id_seq)
    item = {"id": new_id, "name": name, "description": description}
    _store[new_id] = item
    return item


def find_all() -> list[dict]:
    return list(_store.values())


def find_by_id(item_id: int) -> dict | None:
    return _store.get(item_id)

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # 서버
    app_port: int = 8085
    app_name: str = "python-service-template"

    # Eureka
    eureka_server_url: str = "http://localhost:8761/eureka"
    eureka_instance_host: str = "localhost"

    # Auth Server (JWT 검증 — security.py 에서 사용)
    jwt_issuer_uri: str = "http://localhost:9000"
    jwk_set_uri: str = "http://localhost:9000/oauth2/jwks"

    # 서비스 간 호출 대상 URL (Eureka 미사용 시 직접 지정)
    target_service_url: str = "http://localhost:8081"

    # Kafka (inbound=구독, outbound=발행)
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group_id: str = "python-service-template"
    kafka_topic_inbound: str = "sample.created"
    kafka_topic_outbound: str = "python.processed"

    class Config:
        env_file = ".env"


settings = Settings()

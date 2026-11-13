"""Configuración y utilidades compartidas por las pruebas y por wait_for_stack.py.

Las credenciales salen de microservices/.env (ignorado por git). Una variable de
entorno con el mismo nombre tiene prioridad sobre el archivo.
"""

from __future__ import annotations

import base64
import json
import os
import random
import uuid
from dataclasses import dataclass
from pathlib import Path

import httpx

REPO_ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = REPO_ROOT / "microservices" / ".env"

# Usuario semilla que crea usuarios-service al arrancar (ver DataSeeder.java).
SEED_ADMIN_EMAIL = "admin@cinemax.pe"
SEED_ADMIN_PASSWORD = "Admin123!"

# Ids que no existen: sirven para probar escrituras sin tocar datos reales.
MISSING_MONGO_ID = "000000000000000000000000"
MISSING_SQL_ID = 999_999


class ConfigError(RuntimeError):
    """Falta configuración para correr las pruebas."""


def read_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.is_file():
        return values
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "'\"":
            value = value[1:-1]
        values[key.strip()] = value
    return values


@dataclass(frozen=True)
class Settings:
    base_url: str
    client_email: str
    client_password: str
    admin_email: str
    admin_password: str
    staff_email: str
    staff_password: str
    internal_token: str | None
    gateway_secret: str | None

    @property
    def gateway_root(self) -> str:
        """URL del gateway sin el prefijo /api/v1."""
        return str(httpx.URL(self.base_url).copy_with(path="/")).rstrip("/")


def load_settings() -> Settings:
    file_values = read_env_file(ENV_FILE)

    def get(name: str, default: str | None = None) -> str | None:
        return os.environ.get(name) or file_values.get(name) or default

    client_email = get("TEST_USER_EMAIL")
    client_password = get("TEST_USER_PASSWORD")
    if not client_email or not client_password:
        raise ConfigError(
            "Faltan TEST_USER_EMAIL y TEST_USER_PASSWORD. Defínelas en "
            f"{ENV_FILE} o como variables de entorno."
        )

    # El usuario con permisos de gestión se deriva del cliente de prueba para
    # no exigir más claves en el .env. Se puede fijar con TEST_STAFF_*.
    local, _, domain = client_email.partition("@")
    return Settings(
        base_url=get("API_BASE_URL", "http://localhost:8080/api/v1").rstrip("/"),
        client_email=client_email,
        client_password=client_password,
        admin_email=get("TEST_ADMIN_EMAIL", SEED_ADMIN_EMAIL),
        admin_password=get("TEST_ADMIN_PASSWORD", SEED_ADMIN_PASSWORD),
        staff_email=get("TEST_STAFF_EMAIL", f"gerente.{local}@{domain}"),
        staff_password=get("TEST_STAFF_PASSWORD", client_password),
        internal_token=get("INTERNAL_TOKEN"),
        gateway_secret=get("GATEWAY_SECRET"),
    )


def unique(prefix: str) -> str:
    """Nombre único y reconocible para los datos que crea una prueba."""
    return f"zz-test-{prefix}-{uuid.uuid4().hex[:8]}"


def random_document_number() -> str:
    return str(random.randint(10_000_000, 99_999_999))


def bearer(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def jwt_claims(token: str) -> dict:
    """Lee el cuerpo del JWT sin validar la firma (solo para inspeccionarlo)."""
    payload = token.split(".")[1]
    payload += "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(payload))


def login(api: httpx.Client, email: str, password: str) -> str | None:
    response = api.post("/auth/login", json={"email": email, "password": password})
    if response.status_code != 200:
        return None
    return response.json().get("token")


def dni_id(api: httpx.Client) -> int:
    documents = api.get("/documents").json()
    return next(d["idDocType"] for d in documents if d["docName"] == "DNI")


def register_client(api: httpx.Client, email: str, password: str) -> httpx.Response:
    return api.post(
        "/auth/register",
        json={
            "firstName": "Cliente",
            "lastName": "De Prueba",
            "email": email,
            "password": password,
            "documentNumber": random_document_number(),
            "idDocumentType": dni_id(api),
        },
    )


def retirar_funcion(api: httpx.Client, headers: dict[str, str], funcion: dict) -> None:
    """Limpieza de las pruebas: borra la función o, si ya vendió entradas (409), la cancela."""
    if api.delete(f"/showtimes/{funcion['idShowtime']}", headers=headers).status_code != 409:
        return
    cuerpo = {campo: funcion[campo] for campo in (
        "showDate", "startTime", "endTime", "languageFormat", "baseTicketPrice", "movieId", "idRoom")}
    api.put(f"/showtimes/{funcion['idShowtime']}", headers=headers, json={**cuerpo, "status": "Cancelada"})

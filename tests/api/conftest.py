"""Fixtures compartidas de las pruebas de API de CineMax.

Las pruebas hablan con el stack real a través del gateway.
"""

from __future__ import annotations

import shutil
import subprocess
from dataclasses import dataclass

import httpx
import pytest

from support import (
    ConfigError,
    Settings,
    bearer,
    dni_id,
    load_settings,
    login,
    random_document_number,
    register_client,
)


@pytest.fixture(scope="session")
def settings() -> Settings:
    try:
        return load_settings()
    except ConfigError as error:
        pytest.exit(str(error), returncode=2)


@pytest.fixture(scope="session")
def api(settings: Settings):
    with httpx.Client(base_url=settings.base_url, timeout=30.0) as client:
        try:
            client.get("/movies")
        except httpx.TransportError as error:
            pytest.exit(
                f"No hay respuesta del gateway en {settings.base_url} ({error}). "
                "Levanta el stack con `docker compose up -d` desde microservices/ "
                "y espera con `python tests/api/wait_for_stack.py`.",
                returncode=2,
            )
        yield client


@pytest.fixture(scope="session")
def client_token(api: httpx.Client, settings: Settings) -> str:
    """Token del cliente de prueba. Lo registra si todavía no existe."""
    token = login(api, settings.client_email, settings.client_password)
    if token is None:
        response = register_client(api, settings.client_email, settings.client_password)
        assert response.status_code == 200, (
            "No se pudo iniciar sesión ni registrar al cliente de prueba "
            f"(registro: HTTP {response.status_code})."
        )
        token = login(api, settings.client_email, settings.client_password)
    assert token, "El cliente de prueba no pudo iniciar sesión."
    return token


@pytest.fixture(scope="session")
def client_headers(client_token: str) -> dict[str, str]:
    return bearer(client_token)


@pytest.fixture(scope="session")
def admin_token(api: httpx.Client, settings: Settings) -> str:
    token = login(api, settings.admin_email, settings.admin_password)
    if token is None:
        pytest.skip("El administrador semilla no puede iniciar sesión.")
    return token


@pytest.fixture(scope="session")
def staff_token(api: httpx.Client, settings: Settings, admin_token: str) -> str:
    """Token de un GERENTE_GENERAL (todos los permisos salvo MANAGE_USERS).

    Si no existe, lo crea el administrador. Solo puede haber un gerente general
    activo: si ya hay otro, las pruebas que lo necesitan se omiten.
    """
    token = login(api, settings.staff_email, settings.staff_password)
    if token:
        return token

    roles = api.get("/roles").json()
    role_id = next(r["idRole"] for r in roles if r["roleName"] == "GERENTE_GENERAL")
    created = api.post(
        "/users",
        headers=bearer(admin_token),
        json={
            "firstName": "Gerente",
            "lastName": "De Prueba",
            "email": settings.staff_email,
            "password": settings.staff_password,
            "idRole": role_id,
            "documentNumber": random_document_number(),
            "idDocumentType": dni_id(api),
        },
    )
    token = login(api, settings.staff_email, settings.staff_password)
    if token is None:
        pytest.skip(
            "No se pudo crear el gerente de prueba "
            f"(HTTP {created.status_code}). ¿Ya existe otro GERENTE_GENERAL activo?"
        )
    return token


@pytest.fixture(scope="session")
def staff_headers(staff_token: str) -> dict[str, str]:
    return bearer(staff_token)


@pytest.fixture(scope="session")
def movies(api: httpx.Client) -> list[dict]:
    response = api.get("/movies")
    assert response.status_code == 200
    return response.json()


@pytest.fixture(scope="session")
def showtimes(api: httpx.Client) -> list[dict]:
    response = api.get("/showtimes")
    assert response.status_code == 200
    return response.json()


# --------------------------------------------------------------------------
# Acceso a la red interna de Docker (para probar lo que el gateway no expone)
# --------------------------------------------------------------------------

@dataclass(frozen=True)
class InternalResponse:
    status: int
    body: str


class InternalNetwork:
    """Hace peticiones GET desde dentro de la red de Docker.

    Usa el wget de BusyBox del contenedor api-gateway, que está en la misma red
    que los servicios. Así se comprueba qué responde un servicio cuando alguien
    lo llama directamente, sin pasar por el gateway.
    """

    CONTAINER = "api-gateway"

    def request(self, url: str, headers: dict[str, str] | None = None) -> InternalResponse:
        command = ["docker", "exec", self.CONTAINER, "wget", "-q", "-S", "-O", "-", "-T", "15"]
        for name, value in (headers or {}).items():
            command += ["--header", f"{name}: {value}"]
        command.append(url)
        result = subprocess.run(command, capture_output=True, text=True, timeout=60)
        status = None
        for line in result.stderr.splitlines():
            parts = line.strip().split()
            if len(parts) >= 2 and parts[0].startswith("HTTP/") and parts[1].isdigit():
                status = int(parts[1])
        if status is None:
            raise AssertionError(f"Sin respuesta HTTP de {url}: {result.stderr.strip()[:300]}")
        return InternalResponse(status=status, body=result.stdout)


@pytest.fixture(scope="session")
def internal_network() -> InternalNetwork:
    if shutil.which("docker") is None:
        pytest.skip("No hay CLI de Docker: no se puede probar la red interna.")
    probe = subprocess.run(
        ["docker", "inspect", "-f", "{{.State.Running}}", InternalNetwork.CONTAINER],
        capture_output=True,
        text=True,
    )
    if probe.returncode != 0 or probe.stdout.strip() != "true":
        pytest.skip("El contenedor api-gateway no está en ejecución en este host.")
    return InternalNetwork()

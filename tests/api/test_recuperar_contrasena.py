"""Recuperar la contraseña con el enlace que llega por correo.

En local y en CI los correos van a Mailpit (servicio del compose), que guarda
cada mensaje y los expone en su API: http://127.0.0.1:8025/api/v1.
"""

import re
import time

import httpx
import pytest

from support import login, register_client, unique

MAILPIT = "http://127.0.0.1:8025"


@pytest.fixture(scope="module")
def mailpit():
    try:
        httpx.get(f"{MAILPIT}/api/v1/messages", timeout=5.0).raise_for_status()
    except httpx.HTTPError:
        pytest.skip("Mailpit no está disponible en 127.0.0.1:8025.")
    with httpx.Client(base_url=MAILPIT, timeout=10.0) as client:
        yield client


def _token_del_correo(mailpit: httpx.Client, email: str) -> str:
    for _ in range(20):
        mensajes = mailpit.get("/api/v1/search", params={"query": f"to:{email}"}).json()["messages"]
        if mensajes:
            texto = mailpit.get(f"/api/v1/message/{mensajes[0]['ID']}").json()["Text"]
            enlace = re.search(r"/reset-password\?token=([\w-]+)", texto)
            assert enlace, f"El correo no trae el enlace de recuperación:\n{texto}"
            return enlace.group(1)
        time.sleep(0.5)
    pytest.fail(f"No llegó el correo de recuperación a {email}.")


def test_recuperar_la_contrasena_con_el_enlace_del_correo(api, mailpit):
    email = f"{unique('recupera')}@cinemax.test"
    assert register_client(api, email, "Clave12345").status_code == 200

    assert api.post("/auth/forgot-password", json={"email": email}).status_code == 200
    token = _token_del_correo(mailpit, email)
    assert api.get(f"/auth/validate-reset-token/{token}").json()["valid"] is True

    cambio = api.post("/auth/reset-password", json={"token": token, "newPassword": "Nueva12345"})
    assert cambio.status_code == 200, cambio.text
    assert login(api, email, "Nueva12345")
    assert login(api, email, "Clave12345") is None


def test_el_correo_sale_con_el_remitente_de_cinemax(api, mailpit):
    email = f"{unique('remitente')}@cinemax.test"
    assert register_client(api, email, "Clave12345").status_code == 200
    assert api.post("/auth/forgot-password", json={"email": email}).status_code == 200
    _token_del_correo(mailpit, email)

    mensaje = mailpit.get("/api/v1/search", params={"query": f"to:{email}"}).json()["messages"][0]
    assert mensaje["From"]["Address"] == "no-responder@cinemax.local"
    assert "Recuperación de Contraseña" in mensaje["Subject"]

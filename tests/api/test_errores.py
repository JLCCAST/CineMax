"""Todos los servicios responden los errores con el mismo formato (ApiError).

El frontend muestra el campo "message". "error" es el nombre del código HTTP.
"""

import pytest

from support import MISSING_MONGO_ID, MISSING_SQL_ID, bearer

CAMPOS = {"timestamp", "status", "error", "message", "path"}


def _assert_error(response, status: int):
    assert response.status_code == status, response.text
    cuerpo = response.json()
    assert set(cuerpo) == CAMPOS, cuerpo
    assert cuerpo["status"] == status
    assert cuerpo["message"], "el mensaje no puede venir vacío"


@pytest.mark.parametrize("servicio, ruta, con_sesion", [
    ("cartelera", f"/movies/{MISSING_MONGO_ID}", False),
    ("confitería", f"/snacks/{MISSING_SQL_ID}", False),
    ("sucursales", f"/venues/{MISSING_SQL_ID}", True),
    ("facturación", f"/promotions/{MISSING_SQL_ID}", True),
])
def test_un_recurso_inexistente_responde_404_con_el_formato_comun(api, staff_headers, servicio, ruta, con_sesion):
    _assert_error(api.get(ruta, headers=staff_headers if con_sesion else {}), 404)


@pytest.mark.parametrize("ruta", ["/users/profile", "/sale-transactions/my-purchases", "/snack-venue-stock"])
def test_sin_sesion_responde_401(api, ruta):
    response = api.get(ruta)
    _assert_error(response, 401)
    assert "Inicia sesión" in response.json()["message"]


def test_un_token_vencido_o_falso_responde_401(api):
    _assert_error(api.get("/users/profile", headers=bearer("esto.no.es-un-jwt")), 401)


@pytest.mark.parametrize("ruta", ["/venues", "/promotions"])
def test_sin_permiso_responde_403(api, client_headers, ruta):
    response = api.get(ruta, headers=client_headers)
    _assert_error(response, 403)
    assert "permiso" in response.json()["message"]


def test_una_validacion_dice_que_falta(api, staff_headers):
    response = api.post("/showtimes", headers=staff_headers, json={})
    _assert_error(response, 400)
    assert "obligatori" in response.json()["message"]


def test_un_cuerpo_mal_formado_responde_400(api, client_headers):
    response = api.post("/sale-transactions", headers={**client_headers, "Content-Type": "application/json"},
                        content=b"{esto no es json")
    _assert_error(response, 400)

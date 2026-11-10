"""Autorización: quién puede escribir en los endpoints de administración.

Las 20 escrituras de cartelera y confitería deben exigir un permiso:
MANAGE_SHOWTIMES (funciones), MANAGE_MOVIES (géneros y clasificaciones) y
MANAGE_CONFITERIA (confitería).

Las peticiones usan cuerpos válidos e ids que no existen. El cuerpo tiene que
ser válido porque Spring valida el cuerpo antes de evaluar @PreAuthorize (un
cuerpo inválido daría 400 aunque faltara el permiso). Los ids inexistentes
evitan tocar datos reales si un endpoint todavía no está protegido.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date, timedelta

import httpx
import pytest

from support import MISSING_MONGO_ID, MISSING_SQL_ID, bearer, unique

OPEN_WRITES = (
    "Hueco de seguridad conocido: este endpoint de escritura no exige ningún "
    "permiso, así que cualquier sesión iniciada (incluido un CLIENTE) puede usarlo. "
    "Se cierra en el paso 2 con @PreAuthorize."
)


@dataclass(frozen=True)
class Context:
    movie_id: str
    showtime_id: str
    category_id: int


@pytest.fixture(scope="module")
def ctx(api, movies, showtimes) -> Context:
    categories = api.get("/snack-categories").json()
    return Context(
        movie_id=movies[0]["idMovie"],
        showtime_id=showtimes[0]["idShowtime"],
        category_id=categories[0]["idSnackCategory"],
    )


def _showtime(ctx: Context) -> dict:
    return {
        "showDate": (date.today() + timedelta(days=1)).isoformat(),
        "startTime": "10:00:00",
        "endTime": "12:00:00",
        "languageFormat": "Doblada",
        "baseTicketPrice": 10.0,
        "availableSeats": 10,
        # Cancelada: si quedara huérfana, nunca aparecería como función vendible.
        "status": "Cancelada",
        "movieId": ctx.movie_id,
        "idRoom": 1,
    }


def _snack(ctx: Context) -> dict:
    return {
        "snackCategoryId": ctx.category_id,
        "nameSnack": unique("snack"),
        "price": 1.0,
        "stock": 0,
        "status": "Inactivo",
    }


def _stock() -> dict:
    return {"snackId": MISSING_SQL_ID, "idVenue": MISSING_SQL_ID, "stock": 0}


# nombre -> función que arma (método, ruta, argumentos de la petición)
ADMIN_WRITES = {
    # cartelera-service: géneros y clasificaciones (MANAGE_MOVIES)
    "POST /genres": lambda c: ("POST", "/genres", {"json": {"nameGenre": unique("genero")}}),
    "PUT /genres/{id}": lambda c: (
        "PUT", f"/genres/{MISSING_MONGO_ID}", {"json": {"nameGenre": unique("genero")}}),
    "DELETE /genres/{id}": lambda c: ("DELETE", f"/genres/{MISSING_MONGO_ID}", {}),
    "POST /classifications": lambda c: (
        "POST", "/classifications",
        {"json": {"nameClassification": unique("clasif"), "descriptionText": "Prueba"}}),
    "PUT /classifications/{id}": lambda c: (
        "PUT", f"/classifications/{MISSING_MONGO_ID}",
        {"json": {"nameClassification": unique("clasif"), "descriptionText": "Prueba"}}),
    "DELETE /classifications/{id}": lambda c: (
        "DELETE", f"/classifications/{MISSING_MONGO_ID}", {}),
    # cartelera-service: funciones (MANAGE_SHOWTIMES)
    "POST /showtimes": lambda c: ("POST", "/showtimes", {"json": _showtime(c)}),
    "PUT /showtimes/{id}": lambda c: (
        "PUT", f"/showtimes/{MISSING_MONGO_ID}", {"json": _showtime(c)}),
    "DELETE /showtimes/{id}": lambda c: ("DELETE", f"/showtimes/{MISSING_MONGO_ID}", {}),
    # cantidad=0 no cambia nada aunque el endpoint siga abierto.
    "PATCH /showtimes/{id}/decrease-seats": lambda c: (
        "PATCH", f"/showtimes/{c.showtime_id}/decrease-seats", {"params": {"cantidad": 0}}),
    # confiteria-service (MANAGE_CONFITERIA)
    "POST /snack-categories": lambda c: (
        "POST", "/snack-categories", {"json": {"nameCategory": unique("cat")}}),
    "PUT /snack-categories/{id}": lambda c: (
        "PUT", f"/snack-categories/{MISSING_SQL_ID}", {"json": {"nameCategory": unique("cat")}}),
    "DELETE /snack-categories/{id}": lambda c: (
        "DELETE", f"/snack-categories/{MISSING_SQL_ID}", {}),
    "POST /snacks": lambda c: ("POST", "/snacks", {"json": _snack(c)}),
    "PUT /snacks/{id}": lambda c: ("PUT", f"/snacks/{MISSING_SQL_ID}", {"json": _snack(c)}),
    "DELETE /snacks/{id}": lambda c: ("DELETE", f"/snacks/{MISSING_SQL_ID}", {}),
    "POST /snack-venue-stock": lambda c: ("POST", "/snack-venue-stock", {"json": _stock()}),
    "PUT /snack-venue-stock/{id}": lambda c: (
        "PUT", f"/snack-venue-stock/{MISSING_SQL_ID}", {"json": _stock()}),
    "PATCH /snack-venue-stock/descontar": lambda c: (
        "PATCH", "/snack-venue-stock/descontar",
        {"json": {"snackId": MISSING_SQL_ID, "idVenue": MISSING_SQL_ID, "quantity": 1}}),
    "DELETE /snack-venue-stock/{id}": lambda c: (
        "DELETE", f"/snack-venue-stock/{MISSING_SQL_ID}", {}),
}

# Campo con el id del recurso creado, por colección.
ID_FIELD = {
    "/genres": "idGenre",
    "/classifications": "idClassification",
    "/showtimes": "idShowtime",
    "/snack-categories": "idSnackCategory",
    "/snacks": "idSnack",
    "/snack-venue-stock": "idSnackVenueStock",
}


def _delete_if_created(api: httpx.Client, headers: dict, path: str, response: httpx.Response):
    """Si un POST que debía rechazarse creó algo, lo borra para no dejar basura."""
    if response.request.method != "POST" or response.status_code not in (200, 201):
        return
    created_id = response.json().get(ID_FIELD[path])
    if created_id is not None:
        api.delete(f"{path}/{created_id}", headers=headers)


def test_son_veinte_escrituras():
    assert len(ADMIN_WRITES) == 20


@pytest.mark.parametrize("name", list(ADMIN_WRITES))
def test_sin_sesion_no_se_puede_escribir(api, ctx, name):
    method, path, kwargs = ADMIN_WRITES[name](ctx)
    response = api.request(method, path, **kwargs)
    assert response.status_code in (401, 403)


@pytest.mark.xfail(strict=True, reason=OPEN_WRITES)
@pytest.mark.parametrize("name", list(ADMIN_WRITES))
def test_un_cliente_no_puede_escribir(api, client_headers, ctx, name):
    method, path, kwargs = ADMIN_WRITES[name](ctx)
    response = api.request(method, path, headers=client_headers, **kwargs)
    _delete_if_created(api, client_headers, path, response)
    assert response.status_code == 403


@pytest.mark.xfail(strict=True, reason=OPEN_WRITES)
def test_un_colaborador_sin_el_permiso_no_puede_escribir(api, admin_token, ctx):
    """El ADMIN solo tiene MANAGE_USERS y VIEW_VENUES: no gestiona la cartelera."""
    headers = bearer(admin_token)
    method, path, kwargs = ADMIN_WRITES["POST /genres"](ctx)
    response = api.request(method, path, headers=headers, **kwargs)
    _delete_if_created(api, headers, path, response)
    assert response.status_code == 403


# ---- Con el permiso sí se puede (evita que el cierre bloquee de más) -------

def _create_and_delete(api: httpx.Client, headers: dict, path: str, body: dict):
    created = api.post(path, headers=headers, json=body)
    assert created.status_code == 201, created.text
    resource_id = created.json()[ID_FIELD[path]]
    deleted = api.delete(f"{path}/{resource_id}", headers=headers)
    assert deleted.status_code == 204


def test_con_manage_movies_se_gestionan_generos(api, staff_headers):
    _create_and_delete(api, staff_headers, "/genres", {"nameGenre": unique("genero")})


def test_con_manage_showtimes_se_gestionan_funciones(api, staff_headers, ctx):
    _create_and_delete(api, staff_headers, "/showtimes", _showtime(ctx))


def test_con_manage_confiteria_se_gestiona_la_confiteria(api, staff_headers):
    _create_and_delete(api, staff_headers, "/snack-categories", {"nameCategory": unique("cat")})

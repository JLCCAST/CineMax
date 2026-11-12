"""Confitería por sede: lo que ve el cliente y lo que gestiona el administrador."""

import pytest

from support import MISSING_SQL_ID, unique

# Sedes del seed de sucursales.
VENUES = {1, 2}


def test_catalogo_de_una_sede(api):
    response = api.get("/snacks/venue/1")
    assert response.status_code == 200
    snacks = response.json()
    assert snacks, "La sede 1 no tiene snacks: ¿se cargó el seed de confitería?"
    for snack in snacks:
        assert snack["idVenue"] == 1
        assert snack["stock"] > 0
        assert snack["status"] == "Activo"
        assert snack["idSnackCategory"] and snack["nameCategory"]


def test_catalogo_de_una_sede_sin_snacks_esta_vacio(api):
    response = api.get(f"/snacks/venue/{MISSING_SQL_ID}")
    assert response.status_code == 200
    assert response.json() == []


def test_snacks_por_categoria(api):
    category = api.get("/snack-categories").json()[0]
    response = api.get(f"/snacks/category/{category['idSnackCategory']}")
    assert response.status_code == 200
    assert {s["idSnackCategory"] for s in response.json()} <= {category["idSnackCategory"]}


@pytest.fixture
def snack(api, staff_headers):
    category = api.get("/snack-categories").json()[0]
    created = api.post(
        "/snacks",
        headers=staff_headers,
        json={
            "snackCategoryId": category["idSnackCategory"],
            "nameSnack": unique("snack"),
            "price": 3.0,
            "stock": 0,
            "status": "Activo",
        },
    )
    assert created.status_code == 201, created.text
    snack_id = created.json()["idSnack"]
    yield snack_id
    api.delete(f"/snacks/{snack_id}/venue/todas", headers=staff_headers)


def _ids(api, path, headers=None):
    return {s["idSnack"] for s in api.get(path, headers=headers).json()}


def test_gestion_de_un_snack_en_las_sedes(api, staff_headers, snack):
    agregar = api.post(f"/snacks/{snack}/venue/1/agregar", headers=staff_headers, params={"stock": 5})
    assert agregar.status_code == 201, agregar.text
    assert snack in _ids(api, "/snacks/venue/1")
    assert snack in _ids(api, "/snacks/venue/1/admin", staff_headers)

    repetido = api.post(f"/snacks/{snack}/venue/1/agregar", headers=staff_headers, params={"stock": 5})
    assert repetido.status_code == 400

    todas = api.post(f"/snacks/{snack}/venue/todas/agregar", headers=staff_headers, params={"stock": 3})
    assert todas.status_code == 201, todas.text
    assert api.get(f"/snacks/{snack}/sedes-count", headers=staff_headers).json() == len(VENUES)

    assert api.patch(f"/snacks/{snack}/venue/1/inhabilitar", headers=staff_headers).status_code == 204
    assert snack not in _ids(api, "/snacks/venue/1")
    assert snack in _ids(api, "/snacks/venue/1/admin", staff_headers)

    assert api.patch(f"/snacks/{snack}/venue/1/habilitar", headers=staff_headers).status_code == 204
    assert snack in _ids(api, "/snacks/venue/1")

    stock = api.patch(f"/snacks/{snack}/venue/1/stock", headers=staff_headers, params={"stock": 7})
    assert stock.status_code == 204
    admin = {s["idSnack"]: s for s in api.get("/snacks/venue/1/admin", headers=staff_headers).json()}
    assert admin[snack]["stock"] == 7

    assert api.delete(f"/snacks/{snack}/venue/2", headers=staff_headers).status_code == 204
    assert api.get(f"/snacks/{snack}/sedes-count", headers=staff_headers).json() == 1

    assert api.delete(f"/snacks/{snack}/venue/todas", headers=staff_headers).status_code == 204
    assert api.get(f"/snacks/{snack}").status_code == 404


def test_no_se_agrega_un_snack_a_una_sede_inexistente(api, staff_headers, snack):
    response = api.post(
        f"/snacks/{snack}/venue/{MISSING_SQL_ID}/agregar", headers=staff_headers, params={"stock": 1}
    )
    assert response.status_code == 404


def test_el_stock_no_puede_ser_negativo(api, staff_headers, snack):
    response = api.post(f"/snacks/{snack}/venue/1/agregar", headers=staff_headers, params={"stock": -1})
    assert response.status_code == 400


def test_cambiar_un_snack_que_no_esta_en_la_sede_responde_404(api, staff_headers, snack):
    response = api.patch(f"/snacks/{snack}/venue/1/inhabilitar", headers=staff_headers)
    assert response.status_code == 404

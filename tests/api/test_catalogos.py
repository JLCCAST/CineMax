"""Catálogos y datos semilla.

Protegen las migraciones: si el esquema o los seeds cambian sin querer, estas
pruebas lo detectan. Comparan por inclusión para tolerar datos agregados a mano.
"""

import pytest

from support import jwt_claims

ALL_PERMISSIONS = {
    "VIEW_DASHBOARD",
    "VIEW_VENUES",
    "MANAGE_VENUES",
    "MANAGE_ROOMS",
    "MANAGE_SEATS",
    "MANAGE_MOVIES",
    "MANAGE_SHOWTIMES",
    "MANAGE_CONFITERIA",
    "MANAGE_USERS",
}

MYSQL_ENCODING_BUG = (
    "Bug conocido: los init-scripts de MySQL se cargan con el cliente en latin1 y "
    "los textos quedan con doble codificación (María se guarda como MarÃ­a). Se "
    "corrige en el paso 3, cuando el seed pasa a Flyway."
)


# ---- usuarios (PostgreSQL) ------------------------------------------------

def test_tipos_de_documento(api):
    response = api.get("/documents")
    assert response.status_code == 200
    assert {"DNI", "CE", "PASAPORTE"} <= {doc["docName"] for doc in response.json()}


def test_roles_de_colaborador(api):
    response = api.get("/roles")
    assert response.status_code == 200
    names = {role["roleName"] for role in response.json()}
    assert {"ADMIN", "GERENTE_GENERAL", "GERENTE_MARKETING", "GERENTE_OPERACIONES"} <= names
    assert "CLIENTE" not in names, "El rol CLIENTE no se ofrece al crear colaboradores."


def test_permisos_del_administrador(admin_token):
    claims = jwt_claims(admin_token)
    assert claims["role"] == "ADMIN"
    assert set(claims["permissions"]) == {"MANAGE_USERS", "VIEW_VENUES"}


def test_permisos_del_gerente_general(staff_token):
    claims = jwt_claims(staff_token)
    assert claims["role"] == "GERENTE_GENERAL"
    assert set(claims["permissions"]) == ALL_PERMISSIONS - {"MANAGE_USERS"}


# ---- sucursales (MySQL) ---------------------------------------------------

def test_sedes_publicas(api):
    response = api.get("/venues/public")
    assert response.status_code == 200
    venues = response.json()
    assert len(venues) >= 2
    assert "CineMax Miraflores" in {venue["nameVenue"] for venue in venues}
    for venue in venues:
        assert venue["status"] == "Activo"
        assert venue["districtName"] and venue["provinceName"] and venue["departmentName"]


def test_sedes_para_el_selector(api):
    response = api.get("/venues/dropdown")
    assert response.status_code == 200
    assert len(response.json()) >= 2


@pytest.mark.xfail(strict=True, reason=MYSQL_ENCODING_BUG)
def test_sedes_con_tildes_correctas(api):
    names = {venue["nameVenue"] for venue in api.get("/venues/public").json()}
    assert "CineMax Villa María del Triunfo" in names


def test_el_listado_completo_de_sedes_exige_permiso(api, client_headers):
    assert api.get("/venues", headers=client_headers).status_code == 403


# ---- confitería (MySQL) ---------------------------------------------------

def test_categorias_de_confiteria(api):
    response = api.get("/snack-categories")
    assert response.status_code == 200
    names = {category["nameCategory"] for category in response.json()}
    assert {"Bebidas", "Combos", "Dulces", "Salados"} <= names


def test_snacks(api):
    response = api.get("/snacks")
    assert response.status_code == 200
    snacks = response.json()
    assert len(snacks) >= 7
    names = {snack["nameSnack"] for snack in snacks}
    assert {"Gaseosa Grande", "Combo Pareja", "Canchita Grande"} <= names
    for snack in snacks:
        assert snack["price"] > 0
        assert snack["snackCategory"]["nameCategory"]


@pytest.mark.xfail(strict=True, reason=MYSQL_ENCODING_BUG)
def test_snacks_con_tildes_correctas(api):
    descriptions = {snack["descriptionSnack"] for snack in api.get("/snacks").json()}
    assert "Vaso grande de gaseosa a elección" in descriptions


def test_stock_por_sede(api, client_headers):
    response = api.get("/snack-venue-stock", headers=client_headers)
    assert response.status_code == 200
    stock = response.json()
    assert {record["idVenue"] for record in stock} >= {1, 2}
    assert all(record["stock"] >= 0 for record in stock)


def test_stock_por_sede_exige_iniciar_sesion(api):
    assert api.get("/snack-venue-stock").status_code in (401, 403)


# ---- cartelera (MongoDB) --------------------------------------------------

def test_generos(api):
    response = api.get("/genres")
    assert response.status_code == 200
    assert {"Acción", "Drama", "Terror"} <= {genre["nameGenre"] for genre in response.json()}


def test_clasificaciones(api):
    response = api.get("/classifications")
    assert response.status_code == 200
    names = {item["nameClassification"] for item in response.json()}
    assert {"APT", "PG-13", "PG-18"} <= names

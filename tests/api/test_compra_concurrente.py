"""Compras simultáneas y compras que fallan a la mitad.

Butacas y stock se descuentan en otros servicios. Estas pruebas comprueban que
dos compras a la vez no venden la misma butaca ni dejan el stock en negativo,
y que una compra rechazada devuelve lo que ya había descontado.
"""

import threading
from concurrent.futures import ThreadPoolExecutor
from datetime import date, timedelta

import httpx
import pytest

from support import retirar_funcion

SALA = 2
GASEOSA = 1
COMPRAS_A_LA_VEZ = 6


@pytest.fixture(scope="module")
def funcion(api, staff_headers, movies) -> dict:
    """Una función propia, para no competir por butacas con otras pruebas."""
    response = api.post("/showtimes", headers=staff_headers, json={
        "showDate": (date.today() + timedelta(days=12)).isoformat(),
        "startTime": "22:10:00",
        "endTime": "23:55:00",
        "languageFormat": "Doblada",
        "baseTicketPrice": 20,
        "status": "Programada",
        "movieId": movies[0]["idMovie"],
        "idRoom": SALA,
    })
    assert response.status_code == 201, response.text
    showtime = response.json()
    yield showtime
    retirar_funcion(api, staff_headers, showtime)


@pytest.fixture
def gaseosa(api, staff_headers, client_headers, funcion) -> dict:
    """El registro de stock de la gaseosa en la sede de la función; al final se restaura."""
    rows = api.get("/snack-venue-stock", headers=client_headers, params={"snackId": GASEOSA}).json()
    registro = next((r for r in rows if r["idVenue"] == funcion["idVenue"]), None)
    if registro is None:
        pytest.skip("La sede de la función no vende la gaseosa del seed.")
    yield registro
    _fijar_stock(api, staff_headers, registro, registro["stock"])


def _fijar_stock(api: httpx.Client, headers: dict, registro: dict, stock: int) -> None:
    response = api.put(f"/snack-venue-stock/{registro['idSnackVenueStock']}", headers=headers, json={
        "snackId": registro["snackId"],
        "idVenue": registro["idVenue"],
        "stock": stock,
        "status": "Activo",
    })
    assert response.status_code == 200, response.text


def _stock(api: httpx.Client, headers: dict, registro: dict) -> int:
    return api.get(f"/snack-venue-stock/{registro['idSnackVenueStock']}", headers=headers).json()["stock"]


def _mapa(api: httpx.Client, funcion: dict) -> dict[int, dict]:
    seats = api.get(f"/availability/showtimes/{funcion['idShowtime']}/seats").json()
    return {s["idSeat"]: s for s in seats}


def _libres(api: httpx.Client, funcion: dict, cuantas: int) -> list[int]:
    libres = [i for i, s in _mapa(api, funcion).items() if not s["isOccupied"] and s["status"] == "ACTIVO"]
    assert len(libres) >= cuantas, "No quedan butacas libres en la función de prueba."
    return libres[:cuantas]


def _disponibles(api: httpx.Client, funcion: dict) -> int:
    return api.get(f"/showtimes/{funcion['idShowtime']}").json()["availableSeats"]


def _compra(funcion: dict, butacas: list[int], snacks: dict | None = None) -> dict:
    return {
        "idShowtime": funcion["idShowtime"],
        "asientosIds": butacas,
        "tickets": [{"categoryCode": "ADULTO", "cantidad": len(butacas)}],
        "snacks": [{"idSnack": s, "cantidad": n} for s, n in (snacks or {}).items()],
        "paymentMethod": "TARJETA",
    }


def _en_paralelo(settings, headers: dict, cuerpos: list[dict]) -> list[httpx.Response]:
    """Lanza todas las compras a la vez: cada hilo espera en la barrera y sale junto a los demás."""
    barrera = threading.Barrier(len(cuerpos))

    def comprar(cuerpo: dict) -> httpx.Response:
        with httpx.Client(base_url=settings.base_url, timeout=60.0) as cliente:
            barrera.wait()
            return cliente.post("/sale-transactions", headers=headers, json=cuerpo)

    with ThreadPoolExecutor(max_workers=len(cuerpos)) as pool:
        return list(pool.map(comprar, cuerpos))


# ---- Concurrencia ------------------------------------------------------------

def test_varias_compras_de_la_misma_butaca_solo_una_gana(settings, api, client_headers, funcion):
    butaca = _libres(api, funcion, 1)
    antes = _disponibles(api, funcion)

    respuestas = _en_paralelo(settings, client_headers,
                              [_compra(funcion, butaca) for _ in range(COMPRAS_A_LA_VEZ)])

    codigos = [r.status_code for r in respuestas]
    assert codigos.count(200) == 1, [r.text for r in respuestas]
    assert codigos.count(409) == COMPRAS_A_LA_VEZ - 1, [r.text for r in respuestas]
    assert _disponibles(api, funcion) == antes - 1


def test_el_stock_no_queda_en_negativo(settings, api, client_headers, staff_headers, funcion, gaseosa):
    _fijar_stock(api, staff_headers, gaseosa, 2)
    butacas = _libres(api, funcion, COMPRAS_A_LA_VEZ)
    antes = _disponibles(api, funcion)

    respuestas = _en_paralelo(settings, client_headers,
                              [_compra(funcion, [b], {GASEOSA: 1}) for b in butacas])

    codigos = [r.status_code for r in respuestas]
    assert codigos.count(200) == 2, [r.text for r in respuestas]
    assert codigos.count(409) == COMPRAS_A_LA_VEZ - 2, [r.text for r in respuestas]
    assert _stock(api, staff_headers, gaseosa) == 0
    # Las compras rechazadas devolvieron la butaca que habían descontado.
    assert _disponibles(api, funcion) == antes - 2
    mapa = _mapa(api, funcion)
    rechazadas = [b for b, r in zip(butacas, respuestas) if r.status_code == 409]
    assert not any(mapa[b]["isOccupied"] for b in rechazadas)


# ---- Compensación ------------------------------------------------------------

def test_si_falta_stock_la_compra_se_deshace_completa(api, client_headers, staff_headers, funcion, gaseosa):
    _fijar_stock(api, staff_headers, gaseosa, 1)
    butaca = _libres(api, funcion, 1)
    antes = _disponibles(api, funcion)

    response = api.post("/sale-transactions", headers=client_headers,
                        json=_compra(funcion, butaca, {GASEOSA: 3}))

    assert response.status_code == 409, response.text
    assert "No hay stock suficiente" in response.text
    assert _disponibles(api, funcion) == antes
    assert _mapa(api, funcion)[butaca[0]]["isOccupied"] is False
    assert _stock(api, staff_headers, gaseosa) == 1


# ---- Validación de butacas --------------------------------------------------

def test_no_se_puede_comprar_una_butaca_de_otra_sala(api, client_headers, funcion):
    de_la_sala = set(_mapa(api, funcion))
    otra = next(i for i in range(1, 5000) if i not in de_la_sala)

    response = api.post("/sale-transactions", headers=client_headers, json=_compra(funcion, [otra]))

    assert response.status_code == 400
    assert "no pertenece a la sala" in response.text

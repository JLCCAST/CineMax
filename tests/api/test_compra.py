"""Compra de entradas y "mis compras"."""

import httpx
import pytest

from support import MISSING_MONGO_ID

# Capacidad de cada sala en el seed de sucursales.
ROOM_CAPACITY = {1: 80, 2: 60, 3: 40, 4: 100, 5: 70, 6: 30}
GASEOSA = 1


def _purchase_body(showtime: dict, seat_id: int) -> dict:
    """Mismo contrato que envía el frontend. Los precios los pone el servidor."""
    return {
        "idShowtime": showtime["idShowtime"],
        "asientosIds": [seat_id],
        "tickets": [{"categoryCode": "ADULTO", "cantidad": 1}],
        "snacks": [],
        "paymentMethod": "TARJETA",
    }


def _seat_map(api: httpx.Client, showtime: dict) -> list[dict]:
    response = api.get(f"/availability/showtimes/{showtime['idShowtime']}/seats")
    assert response.status_code == 200, response.text
    return response.json()


def _free_seat(api: httpx.Client, showtime: dict) -> dict:
    free = [s for s in _seat_map(api, showtime) if not s["isOccupied"] and s["status"] == "ACTIVO"]
    assert free, "La función no tiene butacas libres."
    return free[0]


def _schedulable(showtimes: list[dict]) -> dict:
    candidates = [s for s in showtimes if s["status"] == "Programada" and s["idRoom"]]
    assert candidates, "No hay funciones programadas: ¿se cargó el seed de MongoDB?"
    return candidates[0]


def _stock(api: httpx.Client, headers: dict, snack_id: int, venue_id: int) -> int:
    rows = api.get("/snack-venue-stock", headers=headers, params={"snackId": snack_id}).json()
    return next(r["stock"] for r in rows if r["snackId"] == snack_id and r["idVenue"] == venue_id)


@pytest.fixture(scope="module")
def showtime(showtimes) -> dict:
    return _schedulable(showtimes)


@pytest.fixture(scope="module")
def purchase(api, client_headers, showtime) -> dict:
    available_before = api.get(f"/showtimes/{showtime['idShowtime']}").json()["availableSeats"]
    seat = _free_seat(api, showtime)
    response = api.post("/sale-transactions", headers=client_headers, json=_purchase_body(showtime, seat["idSeat"]))
    return {"response": response, "seat": seat, "available_before": available_before}


def test_compra_de_una_entrada(purchase):
    response = purchase["response"]
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["idTransaction"] > 0
    assert body["qrCodeData"].startswith("TXN-")


def test_la_compra_aparece_en_mis_compras(api, client_headers, purchase):
    assert purchase["response"].status_code == 200, purchase["response"].text
    history = api.get("/sale-transactions/my-purchases", headers=client_headers).json()
    assert purchase["response"].json()["idTransaction"] in {item["idTransaction"] for item in history}


def test_la_butaca_comprada_queda_ocupada(api, showtime, purchase):
    assert purchase["response"].status_code == 200, purchase["response"].text
    seats = {s["idSeat"]: s for s in _seat_map(api, showtime)}
    assert seats[purchase["seat"]["idSeat"]]["isOccupied"] is True


def test_la_compra_descuenta_butacas_disponibles(api, showtime, purchase):
    assert purchase["response"].status_code == 200, purchase["response"].text
    after = api.get(f"/showtimes/{showtime['idShowtime']}").json()["availableSeats"]
    assert after == purchase["available_before"] - 1


def test_no_se_puede_comprar_una_butaca_vendida(api, client_headers, showtime, purchase):
    assert purchase["response"].status_code == 200, purchase["response"].text
    again = api.post(
        "/sale-transactions", headers=client_headers,
        json=_purchase_body(showtime, purchase["seat"]["idSeat"]),
    )
    assert again.status_code == 400
    assert "ya fue reservado" in again.text


def test_compra_con_snack_descuenta_el_stock_de_la_sede_de_la_funcion(api, client_headers, showtime):
    before = _stock(api, client_headers, GASEOSA, showtime["idVenue"])
    body = _purchase_body(showtime, _free_seat(api, showtime)["idSeat"])
    body["snacks"] = [{"idSnack": GASEOSA, "cantidad": 1}]

    response = api.post("/sale-transactions", headers=client_headers, json=body)

    assert response.status_code == 200, response.text
    assert _stock(api, client_headers, GASEOSA, showtime["idVenue"]) == before - 1
    # Precio del servidor: la entrada adulta más la gaseosa (S/ 8.50 en el seed).
    assert response.json()["subtotal"] == pytest.approx(showtime["baseTicketPrice"] + 8.5)


def test_la_compra_ignora_los_precios_que_manda_el_cliente(api, client_headers, showtime):
    body = _purchase_body(showtime, _free_seat(api, showtime)["idSeat"])
    body["tickets"][0]["precioUnitario"] = 0.01
    body["subtotal"] = body["discountAmount"] = 0

    response = api.post("/sale-transactions", headers=client_headers, json=body)

    assert response.status_code == 200, response.text
    assert response.json()["subtotal"] == pytest.approx(showtime["baseTicketPrice"])


def test_un_tipo_de_entrada_inexistente_se_rechaza(api, client_headers, showtime):
    body = _purchase_body(showtime, _free_seat(api, showtime)["idSeat"])
    body["tickets"][0]["categoryCode"] = "VIP_GRATIS"
    response = api.post("/sale-transactions", headers=client_headers, json=body)
    assert response.status_code == 400


def test_compra_exige_iniciar_sesion(api, showtimes):
    response = api.post("/sale-transactions", json=_purchase_body(showtimes[0], 1))
    assert response.status_code in (401, 403)


def test_mis_compras_devuelve_el_historial_del_cliente(api, client_headers):
    response = api.get("/sale-transactions/my-purchases", headers=client_headers)
    assert response.status_code == 200
    history = response.json()
    assert isinstance(history, list)
    for item in history:
        assert item["idTransaction"] > 0
        assert item["status"]


def test_mis_compras_exige_iniciar_sesion(api):
    assert api.get("/sale-transactions/my-purchases").status_code in (401, 403)


# ---- mapa de butacas -------------------------------------------------------

def test_el_mapa_tiene_todas_las_butacas_de_la_sala(api, showtime):
    seats = _seat_map(api, showtime)
    assert len(seats) == ROOM_CAPACITY[showtime["idRoom"]]
    for seat in seats:
        assert seat["idSeat"] and seat["rowLetter"] and seat["columnNumber"]
        assert seat["nameSeatType"]
        assert isinstance(seat["isOccupied"], bool)


def test_el_mapa_es_publico(settings, showtime):
    url = f"{settings.base_url}/availability/showtimes/{showtime['idShowtime']}/seats"
    assert httpx.get(url, timeout=30.0).status_code == 200


def test_el_mapa_de_una_funcion_inexistente_responde_404(api):
    assert api.get(f"/availability/showtimes/{MISSING_MONGO_ID}/seats").status_code == 404

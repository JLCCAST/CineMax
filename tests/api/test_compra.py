"""Compra de entradas y "mis compras"."""

from datetime import date, timedelta

import httpx
import pytest

from support import MISSING_MONGO_ID, retirar_funcion

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


def _stock(api: httpx.Client, headers: dict, snack_id: int, venue_id: int) -> int:
    rows = api.get("/snack-venue-stock", headers=headers, params={"snackId": snack_id}).json()
    return next(r["stock"] for r in rows if r["snackId"] == snack_id and r["idVenue"] == venue_id)


@pytest.fixture(scope="module")
def showtime(api, staff_headers, movies) -> dict:
    """Una función futura propia: las del seed pueden haber empezado ya hoy."""
    response = api.post("/showtimes", headers=staff_headers, json={
        "showDate": (date.today() + timedelta(days=10)).isoformat(),
        "startTime": "21:00:00",
        "endTime": "23:30:00",
        "languageFormat": "Subtitulada",
        "baseTicketPrice": 20,
        "status": "Programada",
        "movieId": movies[0]["idMovie"],
        "idRoom": 1,
    })
    assert response.status_code == 201, response.text
    created = response.json()
    yield created
    retirar_funcion(api, staff_headers, created)


def test_no_se_crea_una_funcion_en_el_pasado(api, staff_headers, movies):
    """Que no se venda una función que ya empezó lo prueba CotizacionServiceTest."""
    response = api.post("/showtimes", headers=staff_headers, json={
        "showDate": (date.today() - timedelta(days=1)).isoformat(),
        "startTime": "20:00:00", "endTime": "22:00:00", "languageFormat": "Doblada",
        "baseTicketPrice": 20, "status": "Programada", "movieId": movies[0]["idMovie"], "idRoom": 1,
    })
    assert response.status_code == 400
    assert "futuras" in response.json()["message"]


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
    assert again.status_code == 409
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


# ---- mis compras guarda los datos de la venta ---------------------------------

def test_mis_compras_muestra_pelicula_sala_butaca_y_snacks(api, client_headers, showtime):
    seat = _free_seat(api, showtime)
    body = _purchase_body(showtime, seat["idSeat"])
    body["snacks"] = [{"idSnack": GASEOSA, "cantidad": 2}]
    compra = api.post("/sale-transactions", headers=client_headers, json=body)
    assert compra.status_code == 200, compra.text

    historial = api.get("/sale-transactions/my-purchases", headers=client_headers).json()
    # La más reciente va primero (la fecha de pago se guarda en UTC, como las anteriores).
    assert historial[0]["idTransaction"] == compra.json()["idTransaction"]
    item = historial[0]
    assert item["movieTitle"] == showtime["movieTitle"]
    assert item["venueName"] == showtime["nameVenue"]
    assert item["roomName"] == showtime["nameRoom"]
    assert item["date"] == showtime["showDate"]
    assert item["time"] == showtime["startTime"][:5]
    assert item["seats"] == f"{seat['rowLetter']}{seat['columnNumber']}"
    assert item["snacks"] == ["2x Gaseosa Grande"]


def test_una_funcion_con_entradas_vendidas_no_se_borra_ni_cambia_de_sala(
        api, client_headers, staff_headers, movies):
    datos = {
        "showDate": (date.today() + timedelta(days=11)).isoformat(),
        "startTime": "19:00:00", "endTime": "21:00:00", "languageFormat": "Doblada",
        "baseTicketPrice": 20, "status": "Programada", "movieId": movies[0]["idMovie"], "idRoom": 1,
    }
    funcion = api.post("/showtimes", headers=staff_headers, json=datos).json()
    compra = api.post("/sale-transactions", headers=client_headers,
                      json=_purchase_body(funcion, _free_seat(api, funcion)["idSeat"]))
    assert compra.status_code == 200, compra.text
    try:
        borrar = api.delete(f"/showtimes/{funcion['idShowtime']}", headers=staff_headers)
        assert borrar.status_code == 409
        assert "Cancelada" in borrar.json()["message"]

        mover = api.put(f"/showtimes/{funcion['idShowtime']}", headers=staff_headers, json={**datos, "idRoom": 2})
        assert mover.status_code == 409

        # Cambiar la hora sí se puede, y el historial conserva la de la compra.
        cambio = api.put(f"/showtimes/{funcion['idShowtime']}", headers=staff_headers,
                         json={**datos, "startTime": "20:30:00", "endTime": "22:30:00"})
        assert cambio.status_code == 200, cambio.text
        historial = api.get("/sale-transactions/my-purchases", headers=client_headers).json()
        item = next(h for h in historial if h["idTransaction"] == compra.json()["idTransaction"])
        assert item["movieTitle"] == funcion["movieTitle"]
        assert item["time"] == "19:00"
    finally:
        retirar_funcion(api, staff_headers, funcion)


# ---- pedido solo de confitería -------------------------------------------------

def _pedido_confiteria(id_venue: int | None, cantidad: int = 1) -> dict:
    return {"idShowtime": None, "idVenue": id_venue, "asientosIds": [], "tickets": [],
            "snacks": [{"idSnack": GASEOSA, "cantidad": cantidad}], "paymentMethod": "YAPE"}


def test_pedido_solo_de_confiteria(api, client_headers):
    sede = 1
    antes = _stock(api, client_headers, GASEOSA, sede)

    vista = api.post("/promotions/preview", json={"idVenue": sede, "tickets": [],
                                                  "snacks": [{"idSnack": GASEOSA, "cantidad": 1}]})
    assert vista.status_code == 200, vista.text
    assert vista.json()["subtotal"] == pytest.approx(8.5)

    compra = api.post("/sale-transactions", headers=client_headers, json=_pedido_confiteria(sede))
    assert compra.status_code == 200, compra.text
    assert compra.json()["totalAmount"] == pytest.approx(8.5)
    assert _stock(api, client_headers, GASEOSA, sede) == antes - 1

    historial = api.get("/sale-transactions/my-purchases", headers=client_headers).json()
    item = next(h for h in historial if h["idTransaction"] == compra.json()["idTransaction"])
    assert item["movieTitle"] is None
    assert item["venueName"] == "CineMax Villa María del Triunfo"
    assert item["seats"] == ""
    assert item["snacks"] == ["1x Gaseosa Grande"]


@pytest.mark.parametrize("cuerpo, mensaje", [
    ({**_pedido_confiteria(None)}, "sede"),
    ({**_pedido_confiteria(1), "tickets": [{"categoryCode": "ADULTO", "cantidad": 1}]}, "función"),
    ({**_pedido_confiteria(1), "snacks": []}, "producto"),
])
def test_pedidos_de_confiteria_invalidos(api, client_headers, cuerpo, mensaje):
    response = api.post("/sale-transactions", headers=client_headers, json=cuerpo)
    assert response.status_code == 400
    assert mensaje in response.text


def test_pedido_de_confiteria_en_una_sede_inexistente(api, client_headers):
    response = api.post("/sale-transactions", headers=client_headers, json=_pedido_confiteria(9999))
    assert response.status_code in (404, 409)

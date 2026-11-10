"""Compra de entradas y "mis compras"."""

import random

import httpx
import pytest

PURCHASE_BUG = (
    "Bug conocido: la compra responde 400 'Esta función ya no está disponible'. El "
    "seed de MongoDB crea las funciones con status 'Disponible' y sin idRoom, pero "
    "facturación exige 'Programada'; además falta el estado COMPLETADO en "
    "transaction_status. Se corrige con el arreglo del flujo de compra."
)


def _purchase_body(showtime: dict, seat_id: int) -> dict:
    """Mismo contrato que envía el frontend (sale-transaction.service.ts)."""
    price = showtime["baseTicketPrice"]
    return {
        "idShowtime": showtime["idShowtime"],
        "asientosIds": [seat_id],
        "tickets": [{"categoryCode": "GENERAL", "cantidad": 1, "precioUnitario": price}],
        "snacks": [],
        "subtotal": price,
        "discountAmount": 0,
        "totalAmount": price,
        "paymentMethod": "TARJETA",
        "idPromotion": None,
    }


def _buy_one_ticket(api: httpx.Client, headers: dict, showtime: dict) -> httpx.Response:
    """Compra una entrada. Reintenta con otra butaca si la elegida ya se vendió."""
    response = None
    for _ in range(5):
        # Las butacas 1 a 80 son las de la Sala 1 en el seed de sucursales.
        seat_id = random.randint(1, 80)
        response = api.post(
            "/sale-transactions", headers=headers, json=_purchase_body(showtime, seat_id)
        )
        if not (response.status_code == 400 and "ya fue reservado" in response.text):
            break
    return response


@pytest.fixture(scope="module")
def purchase(api, client_headers, showtimes) -> httpx.Response:
    assert showtimes, "No hay funciones: ¿se cargó el seed de MongoDB?"
    return _buy_one_ticket(api, client_headers, showtimes[0])


@pytest.mark.xfail(strict=True, reason=PURCHASE_BUG)
def test_compra_de_una_entrada(purchase):
    assert purchase.status_code == 200, purchase.text
    body = purchase.json()
    assert body["idTransaction"] > 0
    assert body["qrCodeData"].startswith("TXN-")


@pytest.mark.xfail(strict=True, reason=PURCHASE_BUG)
def test_la_compra_aparece_en_mis_compras(api, client_headers, purchase):
    assert purchase.status_code == 200, purchase.text
    history = api.get("/sale-transactions/my-purchases", headers=client_headers).json()
    assert purchase.json()["idTransaction"] in {item["idTransaction"] for item in history}


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

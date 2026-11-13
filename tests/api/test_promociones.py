"""Promociones automáticas: vista previa del checkout y compra con descuento.

Cada prueba usa funciones creadas aquí para el día que necesita (próximo
miércoles, martes o viernes), con precio base S/ 25: adulto 25, niño 14 (55 %)
y adulto mayor 16,50 (65 %).
"""

from datetime import date, timedelta

import httpx
import pytest

from support import MISSING_MONGO_ID, retirar_funcion, unique

FIN_DE_VIGENCIA = date(2027, 12, 31)  # de las promociones semilla (V7)
SALA = 2
PRECIO_BASE = 25.0
COMBO_PAREJA = 3  # categoría Combos en el seed de confitería


def _proximo(dia_iso: int) -> date:
    hoy = date.today()
    return hoy + timedelta(days=(dia_iso - hoy.isoweekday()) % 7 or 7)


@pytest.fixture(scope="module")
def funciones(api, staff_headers, movies):
    if _proximo(5) > FIN_DE_VIGENCIA:
        pytest.skip("Las promociones semilla vencen el 2027-12-31.")
    pelicula = next((m for m in movies if m["titleMovie"] == "Interestelar"), None)
    if pelicula is None:
        pytest.skip("Falta Interestelar en el seed de MongoDB.")

    creadas, respuestas = {}, []
    for nombre, fecha, hora in [
        ("miercoles", _proximo(3), "19:00:00"),
        ("martes", _proximo(2), "20:00:00"),
        ("viernes_matine", _proximo(5), "13:00:00"),
        ("viernes_noche", _proximo(5), "20:00:00"),
    ]:
        response = api.post("/showtimes", headers=staff_headers, json={
            "showDate": fecha.isoformat(),
            "startTime": hora,
            "endTime": "23:50:00",
            "languageFormat": "Subtitulada",
            "baseTicketPrice": PRECIO_BASE,
            "status": "Programada",
            "movieId": pelicula["idMovie"],
            "idRoom": SALA,
        })
        assert response.status_code == 201, response.text
        creadas[nombre] = response.json()["idShowtime"]
        respuestas.append(response.json())

    yield creadas

    for funcion in respuestas:
        retirar_funcion(api, staff_headers, funcion)


def _preview(api: httpx.Client, id_showtime: str, tickets: dict, snacks: dict | None = None,
             code: str | None = None, headers: dict | None = None) -> httpx.Response:
    return api.post("/promotions/preview", headers=headers or {}, json={
        "idShowtime": id_showtime,
        "tickets": [{"categoryCode": c, "cantidad": n} for c, n in tickets.items()],
        "snacks": [{"idSnack": s, "cantidad": n} for s, n in (snacks or {}).items()],
        "promotionCode": code,
    })


def _ok(response: httpx.Response) -> dict:
    assert response.status_code == 200, response.text
    return response.json()


def _assert_promocion(body: dict, nombre: str | None, descuento: float, subtotal: float):
    assert body["namePromotion"] == nombre
    assert body["discountAmount"] == pytest.approx(descuento)
    assert body["subtotal"] == pytest.approx(subtotal)
    assert body["totalAmount"] == pytest.approx(subtotal - descuento)


# ---- Vista previa --------------------------------------------------------------

def test_miercoles_de_cine(api, client_headers, funciones):
    body = _ok(_preview(api, funciones["miercoles"], {"ADULTO": 2}, headers=client_headers))
    _assert_promocion(body, "Miércoles de cine", 25.0, 50.0)


def test_martes_2x1_regala_la_entrada_mas_barata(api, client_headers, funciones):
    body = _ok(_preview(api, funciones["martes"], {"ADULTO": 1, "NINO": 1}, headers=client_headers))
    _assert_promocion(body, "Martes 2x1", 14.0, 39.0)


def test_matine(api, client_headers, funciones):
    body = _ok(_preview(api, funciones["viernes_matine"], {"ADULTO": 2}, headers=client_headers))
    _assert_promocion(body, "Matiné", 10.0, 50.0)


def test_combo_y_pelicula(api, client_headers, funciones):
    body = _ok(_preview(api, funciones["viernes_noche"], {"ADULTO": 2}, {COMBO_PAREJA: 1},
                        headers=client_headers))
    _assert_promocion(body, "Combo + película", 5.0, 75.0)
    assert body["snacks"][0]["discountAmount"] == pytest.approx(5.0)
    assert all(t["discountAmount"] == 0 for t in body["tickets"])


def test_cupon_de_la_app(api, client_headers, funciones):
    body = _ok(_preview(api, funciones["viernes_noche"], {"ADULTO": 2}, code="app10",
                        headers=client_headers))
    _assert_promocion(body, "Cupón de la app", 5.0, 50.0)


def test_sin_promocion(api, client_headers, funciones):
    body = _ok(_preview(api, funciones["viernes_noche"], {"ADULTO": 2}, headers=client_headers))
    _assert_promocion(body, None, 0.0, 50.0)


def test_la_vista_previa_no_exige_sesion(api, funciones):
    body = _ok(_preview(api, funciones["miercoles"], {"ADULTO": 2}))
    _assert_promocion(body, "Miércoles de cine", 25.0, 50.0)


def test_la_vista_previa_usa_las_tarifas_de_cartelera(api, funciones):
    body = _ok(_preview(api, funciones["viernes_noche"], {"ADULTO": 1, "NINO": 1, "ADULTO_MAYOR": 1}))
    precios = {t["categoryCode"]: t["price"] for t in body["tickets"]}
    assert precios == {"ADULTO": pytest.approx(25.0), "NINO": pytest.approx(14.0),
                       "ADULTO_MAYOR": pytest.approx(16.5)}


def test_tipo_de_entrada_invalido(api, funciones):
    assert _preview(api, funciones["viernes_noche"], {"GRATIS": 1}).status_code == 400


def test_funcion_inexistente(api):
    assert _preview(api, MISSING_MONGO_ID, {"ADULTO": 1}).status_code == 404


# ---- Compra ---------------------------------------------------------------------

def _butaca_libre(api: httpx.Client, id_showtime: str) -> int:
    seats = api.get(f"/availability/showtimes/{id_showtime}/seats").json()
    return next(s["idSeat"] for s in seats if not s["isOccupied"] and s["status"] == "ACTIVO")


def _compra(id_showtime: str, butacas: list[int], tickets: dict, total: float | None) -> dict:
    return {
        "idShowtime": id_showtime,
        "asientosIds": butacas,
        "tickets": [{"categoryCode": c, "cantidad": n} for c, n in tickets.items()],
        "snacks": [],
        "totalAmount": total,
        "paymentMethod": "TARJETA",
    }


def test_la_compra_aplica_la_promocion_de_la_vista_previa(api, client_headers, funciones):
    id_showtime = funciones["miercoles"]
    preview = _ok(_preview(api, id_showtime, {"ADULTO": 1}, headers=client_headers))
    butaca = _butaca_libre(api, id_showtime)

    response = api.post("/sale-transactions", headers=client_headers,
                        json=_compra(id_showtime, [butaca], {"ADULTO": 1}, preview["totalAmount"]))

    body = _ok(response)
    assert body["namePromotion"] == "Miércoles de cine"
    assert body["discountAmount"] == pytest.approx(preview["discountAmount"])
    assert body["totalAmount"] == pytest.approx(preview["totalAmount"])


def test_si_el_total_cambio_la_compra_responde_409(api, client_headers, funciones):
    id_showtime = funciones["miercoles"]
    butaca = _butaca_libre(api, id_showtime)
    response = api.post("/sale-transactions", headers=client_headers,
                        json=_compra(id_showtime, [butaca], {"ADULTO": 1}, PRECIO_BASE))
    assert response.status_code == 409
    assert "El precio cambió" in response.text


# ---- Gestión desde el admin -----------------------------------------------------

def _nueva_promocion(**cambios) -> dict:
    promocion = {
        "namePromotion": unique("promo"),
        "promotionType": "PORCENTAJE",
        "promotionCode": None,
        "discountPercentage": 5,
        "startDate": date.today().isoformat(),
        "endDate": (date.today() + timedelta(days=30)).isoformat(),
        "appliesTo": "TODO",
        "status": "Inactivo",
    }
    promocion.update(cambios)
    return promocion


def test_los_tipos_de_beneficio(api, staff_headers):
    response = api.get("/promotions/types", headers=staff_headers)
    assert response.status_code == 200
    assert set(response.json()) == {"PORCENTAJE", "MONTO_FIJO", "DOS_POR_UNO", "ENTRADA_GRATIS"}


def test_el_admin_gestiona_una_promocion_de_cupon(api, staff_headers, funciones):
    codigo = unique("C").upper()[:30]
    creada = api.post("/promotions", headers=staff_headers, json=_nueva_promocion(promotionCode=codigo))
    assert creada.status_code == 201, creada.text
    promocion = creada.json()
    id_promocion = promocion["idPromotion"]
    funcion = funciones["viernes_noche"]
    try:
        listadas = {p["idPromotion"] for p in api.get("/promotions", headers=staff_headers).json()}
        assert id_promocion in listadas

        # Inactiva: el cupón no aplica.
        assert _ok(_preview(api, funcion, {"ADULTO": 2}, code=codigo))["namePromotion"] is None

        activar = api.patch(f"/promotions/{id_promocion}/estado", headers=staff_headers, params={"status": "Activo"})
        assert activar.status_code == 204
        body = _ok(_preview(api, funcion, {"ADULTO": 2}, code=codigo))
        _assert_promocion(body, promocion["namePromotion"], 2.5, 50.0)
        assert body["promotionCode"] == codigo

        cambio = _nueva_promocion(namePromotion=promocion["namePromotion"], promotionCode=codigo,
                                  discountPercentage=8, status="Activo")
        assert api.put(f"/promotions/{id_promocion}", headers=staff_headers, json=cambio).status_code == 200
        _assert_promocion(_ok(_preview(api, funcion, {"ADULTO": 2}, code=codigo)),
                          promocion["namePromotion"], 4.0, 50.0)
    finally:
        api.patch(f"/promotions/{id_promocion}/estado", headers=staff_headers, params={"status": "Inactivo"})


@pytest.mark.parametrize("cambios", [
    {"endDate": (date.today() - timedelta(days=1)).isoformat()},
    {"discountPercentage": None},
    {"promotionCode": "APP10"},
    {"promotionType": "REGALO"},
    {"dayOfWeek": "8"},
])
def test_promociones_invalidas_se_rechazan(api, staff_headers, cambios):
    response = api.post("/promotions", headers=staff_headers, json=_nueva_promocion(**cambios))
    assert response.status_code == 400, response.text


def test_promocion_inexistente_responde_404(api, staff_headers):
    assert api.get("/promotions/999999", headers=staff_headers).status_code == 404

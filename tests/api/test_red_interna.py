"""Superficie expuesta: qué se puede alcanzar sin pasar por el gateway.

Modelo de acceso (ServiceAccessFilter, en common-library):

- La API pública de cada servicio (/api/**) solo acepta peticiones del gateway,
  que se identifica con un secreto, o de otro servicio.
- /internal/** solo acepta peticiones de otro servicio, que se identifica con el
  token de servicio. El gateway no enruta esas rutas.
- Solo el gateway publica su puerto en el host.

Las pruebas que usan `internal_network` llaman a los servicios desde dentro de
la red de Docker (con `docker exec`), que es lo que podría hacer alguien que ya
tuviera acceso a esa red. Se omiten si no hay Docker en la máquina.
"""

import socket

import httpx
import pytest

# Puertos de los servicios Java que no son el gateway (8080) ni Eureka (8761).
SERVICE_PORTS = {
    "config-server": 8888,
    "auth-service": 8081,
    "cartelera-service": 8082,
    "sucursales-service": 8083,
    "confiteria-service": 8084,
    "usuarios-service": 8085,
    "facturacion-service": 8086,
}

DIRECT_MOVIES = "http://cartelera-service:8082/api/movies"
INTERNAL_SEAT = "http://sucursales-service:8083/internal/seats/1"

# nombre -> (url, usa POST)
INTERNAL_ENDPOINTS = {
    "sucursales: butaca": (INTERNAL_SEAT, False),
    "sucursales: sala": ("http://sucursales-service:8083/internal/rooms/1", False),
    "sucursales: salas por lote": ("http://sucursales-service:8083/internal/rooms?ids=1,2", False),
    "usuarios: validar credenciales": (
        "http://usuarios-service:8085/internal/users/validate-credentials", True),
    "cartelera: contexto de venta": (
        "http://cartelera-service:8082/internal/showtimes/{showtime}/contexto-venta", False),
    "confitería: snacks por id": ("http://confiteria-service:8084/internal/snacks?ids=1,2", False),
    "cartelera: descontar butacas": (
        "http://cartelera-service:8082/internal/showtimes/{showtime}/decrease-seats?cantidad=0",
        True),
    "confitería: actualizar stock": (
        "http://confiteria-service:8084/internal/snack-venue-stock/1", False),
}


@pytest.fixture(scope="module")
def tokens(settings) -> dict[str, dict[str, str]]:
    assert settings.internal_token and settings.gateway_secret, (
        "Faltan INTERNAL_TOKEN o GATEWAY_SECRET en microservices/.env."
    )
    return {
        "internal": {"X-Internal-Token": settings.internal_token},
        "gateway": {"X-Gateway-Secret": settings.gateway_secret},
    }


def _internal(name: str, showtimes: list[dict]) -> tuple[str, bool]:
    url, post = INTERNAL_ENDPOINTS[name]
    return url.format(showtime=showtimes[0]["idShowtime"]), post


# ---- Desde fuera: lo que ve un navegador ----------------------------------

@pytest.mark.parametrize(
    "path",
    [
        "/internal/seats/1",
        "/internal/rooms/1",
        "/internal/rooms?ids=1",
        "/internal/users/validate-credentials",
        "/internal/showtimes/x/decrease-seats",
        "/internal/showtimes/x/contexto-venta",
        "/internal/snacks?ids=1",
        "/internal/snack-venue-stock/1",
    ],
)
def test_el_gateway_no_enruta_internal(settings, path):
    for prefix in (settings.gateway_root, settings.base_url):
        response = httpx.get(prefix + path, timeout=30.0)
        assert response.status_code == 404, f"{prefix}{path} respondió {response.status_code}"


@pytest.mark.parametrize("service", list(SERVICE_PORTS))
def test_los_servicios_no_publican_su_puerto_en_el_host(settings, service):
    host = httpx.URL(settings.base_url).host
    with pytest.raises(OSError):
        socket.create_connection((host, SERVICE_PORTS[service]), timeout=3).close()


def test_un_cliente_no_puede_hacerse_pasar_por_el_gateway(api, client_headers):
    """El gateway pisa la cabecera del secreto: mandarla desde fuera no cambia nada."""
    headers = {**client_headers, "X-Gateway-Secret": "valor-inventado"}
    assert api.get("/users/profile", headers=headers).status_code == 200


# ---- Desde dentro de la red de Docker: la API pública ---------------------

def test_un_servicio_rechaza_peticiones_que_no_vienen_del_gateway(internal_network):
    assert internal_network.request(DIRECT_MOVIES).status == 403


def test_la_cabecera_fija_antigua_ya_no_abre_la_puerta(internal_network):
    response = internal_network.request(DIRECT_MOVIES, {"X-Gateway-Request": "true"})
    assert response.status == 403


def test_un_secreto_de_gateway_incorrecto_no_abre_la_puerta(internal_network):
    response = internal_network.request(DIRECT_MOVIES, {"X-Gateway-Secret": "no-es-el-secreto"})
    assert response.status == 403


def test_la_api_acepta_el_secreto_del_gateway(internal_network, tokens):
    assert internal_network.request(DIRECT_MOVIES, tokens["gateway"]).status == 200


def test_la_api_acepta_el_token_de_servicio(internal_network, tokens):
    assert internal_network.request(DIRECT_MOVIES, tokens["internal"]).status == 200


# ---- Desde dentro de la red de Docker: /internal --------------------------

@pytest.mark.parametrize("name", list(INTERNAL_ENDPOINTS))
def test_internal_exige_el_token_de_servicio(internal_network, showtimes, name):
    url, post = _internal(name, showtimes)
    assert internal_network.request(url, post=post).status == 403


@pytest.mark.parametrize("name", list(INTERNAL_ENDPOINTS))
def test_internal_rechaza_un_token_incorrecto(internal_network, showtimes, name):
    url, post = _internal(name, showtimes)
    response = internal_network.request(url, {"X-Internal-Token": "no-es-el-token"}, post=post)
    assert response.status == 403


@pytest.mark.parametrize("name", list(INTERNAL_ENDPOINTS))
def test_internal_no_acepta_el_secreto_del_gateway(internal_network, showtimes, tokens, name):
    """Aunque el gateway enrutara /internal por error, su secreto no bastaría."""
    url, post = _internal(name, showtimes)
    assert internal_network.request(url, tokens["gateway"], post=post).status == 403


def test_internal_responde_con_el_token_de_servicio(internal_network, tokens):
    response = internal_network.request(INTERNAL_SEAT, tokens["internal"])
    assert response.status == 200
    assert '"idSeat":1' in response.body


def test_descontar_butacas_funciona_como_operacion_interna(
    api, internal_network, showtimes, tokens
):
    showtime_id = showtimes[0]["idShowtime"]
    seats_before = api.get(f"/showtimes/{showtime_id}").json()["availableSeats"]

    url, post = _internal("cartelera: descontar butacas", showtimes)
    assert internal_network.request(url, tokens["internal"], post=post).status == 204

    # cantidad=0: la operación corre, pero no cambia las butacas disponibles.
    assert api.get(f"/showtimes/{showtime_id}").json()["availableSeats"] == seats_before


def test_actualizar_stock_existe_como_operacion_interna(internal_network, showtimes, tokens):
    """Es un PUT y el wget del contenedor solo sabe GET y POST.

    Con el token, un GET llega al controlador y responde 405 (método no
    permitido); sin el token no pasaría del filtro (403).
    """
    url, _ = _internal("confitería: actualizar stock", showtimes)
    assert internal_network.request(url, tokens["internal"]).status == 405

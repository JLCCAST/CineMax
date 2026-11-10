"""Superficie expuesta: qué se puede alcanzar sin pasar por el gateway.

Las pruebas marcadas con `internal_network` llaman a los servicios desde dentro
de la red de Docker (con `docker exec`), que es lo que podría hacer alguien que
ya tuviera acceso a esa red. Se omiten si no hay Docker en la máquina.
"""

import socket

import httpx
import pytest

OPEN_SURFACE = (
    "Hueco de seguridad conocido: los servicios confían en la cabecera fija "
    "'X-Gateway-Request: true', /internal/** no pide credenciales y docker-compose "
    "publica los puertos de todos los servicios. Se cierra en el paso 2."
)

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

INTERNAL_SEAT = "http://sucursales-service:8083/internal/seats/1"
DIRECT_MOVIES = "http://cartelera-service:8082/api/movies"


# ---- Desde fuera: lo que ve un navegador ----------------------------------

@pytest.mark.parametrize("path", ["/internal/seats/1", "/internal/users/validate-credentials"])
def test_el_gateway_no_enruta_internal(settings, path):
    for prefix in (settings.gateway_root, settings.base_url):
        response = httpx.get(prefix + path, timeout=30.0)
        assert response.status_code == 404, f"{prefix}{path} respondió {response.status_code}"


@pytest.mark.xfail(strict=True, reason=OPEN_SURFACE)
@pytest.mark.parametrize("service", list(SERVICE_PORTS))
def test_los_servicios_no_publican_su_puerto_en_el_host(settings, service):
    host = httpx.URL(settings.base_url).host
    with pytest.raises(OSError):
        socket.create_connection((host, SERVICE_PORTS[service]), timeout=3).close()


# ---- Desde dentro de la red de Docker -------------------------------------

def test_un_servicio_rechaza_peticiones_que_no_vienen_del_gateway(internal_network):
    assert internal_network.request(DIRECT_MOVIES).status == 403


@pytest.mark.xfail(strict=True, reason=OPEN_SURFACE)
def test_la_cabecera_fija_del_gateway_ya_no_abre_la_puerta(internal_network):
    response = internal_network.request(DIRECT_MOVIES, {"X-Gateway-Request": "true"})
    assert response.status == 403


@pytest.mark.xfail(strict=True, reason=OPEN_SURFACE)
def test_internal_exige_el_token_de_servicio(internal_network):
    assert internal_network.request(INTERNAL_SEAT).status == 403


@pytest.mark.xfail(strict=True, reason=OPEN_SURFACE)
def test_internal_rechaza_un_token_incorrecto(internal_network):
    response = internal_network.request(INTERNAL_SEAT, {"X-Internal-Token": "no-es-el-token"})
    assert response.status == 403

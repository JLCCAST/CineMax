"""Espera a que el stack esté listo para las pruebas.

Uso:
    python tests/api/wait_for_stack.py [--timeout SEGUNDOS]

"Listo" significa que cada servicio responde a través del gateway y que las
llamadas entre servicios funcionan. No basta con mirar el estado de los
contenedores: un servicio recién registrado en Eureka tarda hasta 30 segundos
en ser visible para los demás.

Termina con código 0 si el stack está listo y con 1 si se agota el tiempo.
"""

from __future__ import annotations

import argparse
import sys
import time

import httpx

from support import ConfigError, bearer, load_settings

# (servicio, ruta pública que debe responder 200)
PUBLIC_CHECKS = [
    ("cartelera-service", "/movies"),
    ("sucursales-service", "/venues/public"),
    ("confiteria-service", "/snack-categories"),
    ("usuarios-service", "/documents"),
]


def pending_services(api: httpx.Client, admin_email: str, admin_password: str) -> list[str]:
    """Devuelve lo que todavía no responde, con el motivo."""
    pending: list[str] = []

    def check(name: str, call) -> httpx.Response | None:
        try:
            response = call()
        except httpx.TransportError as error:
            pending.append(f"{name} ({type(error).__name__})")
            return None
        if response.status_code != 200:
            pending.append(f"{name} (HTTP {response.status_code})")
            return None
        return response

    for name, path in PUBLIC_CHECKS:
        check(name, lambda path=path: api.get(path))

    # El login recorre gateway -> auth-service -> usuarios-service -> PostgreSQL.
    logged_in = check(
        "auth-service",
        lambda: api.post("/auth/login", json={"email": admin_email, "password": admin_password}),
    )
    if logged_in is not None:
        # "Mis compras" recorre gateway -> facturacion-service -> usuarios-service.
        headers = bearer(logged_in.json()["token"])
        check(
            "facturacion-service",
            lambda: api.get("/sale-transactions/my-purchases", headers=headers),
        )
    else:
        pending.append("facturacion-service (depende del login)")
    return pending


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--timeout", type=int, default=600, help="segundos máximos de espera")
    parser.add_argument("--interval", type=int, default=5, help="segundos entre intentos")
    args = parser.parse_args()

    try:
        settings = load_settings()
    except ConfigError as error:
        print(error, file=sys.stderr)
        return 1

    deadline = time.monotonic() + args.timeout
    last_report = ""
    with httpx.Client(base_url=settings.base_url, timeout=10.0) as api:
        while True:
            pending = pending_services(api, settings.admin_email, settings.admin_password)
            if not pending:
                print(f"Stack listo en {settings.base_url}")
                return 0
            report = ", ".join(pending)
            if report != last_report:
                print(f"Esperando: {report}", flush=True)
                last_report = report
            if time.monotonic() >= deadline:
                print(
                    f"Tiempo agotado tras {args.timeout} s. Sigue sin responder: {report}\n"
                    "Revisa `docker compose ps` y `docker compose logs <servicio>`. Si el "
                    "administrador semilla cambió, define TEST_ADMIN_EMAIL y TEST_ADMIN_PASSWORD.",
                    file=sys.stderr,
                )
                return 1
            time.sleep(args.interval)


if __name__ == "__main__":
    sys.exit(main())

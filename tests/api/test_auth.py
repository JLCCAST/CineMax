"""Registro, inicio de sesión y perfil."""

from support import bearer, jwt_claims, login, register_client, unique


def test_registro_crea_un_cliente_que_puede_iniciar_sesion(api):
    email = f"{unique('registro')}@cinemax.test"
    password = "Prueba-123"

    response = register_client(api, email, password)
    assert response.status_code == 200
    assert "message" in response.json()

    token = login(api, email, password)
    assert token, "El usuario recién registrado no pudo iniciar sesión."

    profile = api.get("/users/profile", headers=bearer(token))
    assert profile.status_code == 200
    assert profile.json()["email"] == email
    assert profile.json()["roleName"] == "CLIENTE"

    # Limpieza: la baja es lógica (el usuario queda Inactivo).
    assert api.delete("/users/me", headers=bearer(token)).status_code == 204
    assert login(api, email, password) is None


def test_registro_rechaza_un_correo_ya_registrado(api, settings, client_token):
    response = register_client(api, settings.client_email, "Otra-Clave-123")
    # Hoy responde 403 sin cuerpo (lo ideal sería 409); basta con que no lo acepte.
    assert response.status_code >= 400


def test_registro_valida_los_campos_obligatorios(api):
    response = api.post("/auth/register", json={"email": "no-es-un-correo"})
    assert response.status_code >= 400


def test_login_devuelve_un_jwt_con_el_rol_del_usuario(client_token, settings):
    claims = jwt_claims(client_token)
    assert claims["sub"] == settings.client_email
    assert claims["role"] == "CLIENTE"
    assert claims["permissions"] == []
    assert claims["exp"] > claims["iat"]


def test_login_rechaza_una_clave_incorrecta(api, settings):
    response = api.post(
        "/auth/login",
        json={"email": settings.client_email, "password": "clave-incorrecta"},
    )
    # Hoy responde 403 sin cuerpo (lo ideal sería 401).
    assert response.status_code in (401, 403)
    assert "token" not in response.text


def test_perfil_devuelve_los_datos_del_usuario_autenticado(api, client_headers, settings):
    response = api.get("/users/profile", headers=client_headers)
    assert response.status_code == 200
    profile = response.json()
    assert profile["email"] == settings.client_email
    assert profile["roleName"] == "CLIENTE"
    assert profile["status"] == "Activo"
    assert "password" not in profile


def test_perfil_exige_iniciar_sesion(api):
    assert api.get("/users/profile").status_code in (401, 403)


def test_perfil_rechaza_un_token_invalido(api):
    response = api.get("/users/profile", headers=bearer("esto.no.es-un-jwt"))
    assert response.status_code in (401, 403)

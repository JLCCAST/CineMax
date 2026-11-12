"""Cartelera pública: películas, detalle y funciones."""

import json
import math
from datetime import date, timedelta

import httpx
import pytest

from support import MISSING_MONGO_ID, MISSING_SQL_ID, unique


def test_cartelera_lista_las_peliculas(movies):
    assert movies, "La cartelera está vacía: ¿se cargó el seed de MongoDB?"
    for movie in movies:
        assert movie["idMovie"]
        assert movie["titleMovie"]
        assert movie["status"]


def test_cartelera_filtra_por_estado(api):
    response = api.get("/movies", params={"status": "Cartelera"})
    assert response.status_code == 200
    filtered = response.json()
    assert filtered
    assert {movie["status"] for movie in filtered} == {"Cartelera"}


def test_detalle_de_pelicula(api, movies):
    expected = movies[0]
    response = api.get(f"/movies/{expected['idMovie']}")
    assert response.status_code == 200
    movie = response.json()
    assert movie["idMovie"] == expected["idMovie"]
    assert movie["titleMovie"] == expected["titleMovie"]
    assert movie["durationMinutes"] > 0
    assert movie["genres"] and all(genre["nameGenre"] for genre in movie["genres"])
    assert movie["classification"]["nameClassification"]


def test_detalle_de_pelicula_inexistente_responde_404(api):
    assert api.get(f"/movies/{MISSING_MONGO_ID}").status_code == 404


def test_detalle_de_funcion(api, showtimes):
    expected = showtimes[0]
    response = api.get(f"/showtimes/{expected['idShowtime']}")
    assert response.status_code == 200
    showtime = response.json()
    assert showtime["idShowtime"] == expected["idShowtime"]
    assert showtime["movieId"] == expected["movieId"]
    assert showtime["availableSeats"] >= 0


def test_las_funciones_tienen_sala_y_sede(showtimes):
    assert showtimes
    for showtime in showtimes:
        assert showtime["idRoom"], showtime
        assert showtime["idVenue"], showtime


def test_las_funciones_del_seed_estan_programadas(showtimes):
    assert {s["status"] for s in showtimes} <= {"Programada", "En Curso", "Finalizada", "Cancelada"}
    assert any(s["status"] == "Programada" for s in showtimes)


def _new_showtime(movie_id: str, id_room: int) -> dict:
    return {
        "showDate": (date.today() + timedelta(days=2)).isoformat(),
        "startTime": "09:00:00",
        "endTime": "11:00:00",
        "languageFormat": "Doblada",
        "baseTicketPrice": 15.0,
        "availableSeats": 10,
        "status": "Cancelada",
        "movieId": movie_id,
        "idRoom": id_room,
    }


def test_crear_funcion_guarda_la_sede_de_la_sala(api, staff_headers, movies):
    # En el seed de sucursales, la sala 4 es de la sede 2 (Miraflores) y tiene 100 butacas.
    body = _new_showtime(movies[0]["idMovie"], 4)
    del body["availableSeats"]  # el admin no lo envía: debe salir de la capacidad de la sala
    response = api.post("/showtimes", headers=staff_headers, json=body)
    assert response.status_code == 201, response.text
    created = response.json()
    try:
        assert created["idRoom"] == 4
        assert created["idVenue"] == 2
        assert created["availableSeats"] == 100
        assert created["roomCapacity"] == 100
        assert created["durationMinutes"] == movies[0]["durationMinutes"]
    finally:
        api.delete(f"/showtimes/{created['idShowtime']}", headers=staff_headers)


def test_crear_funcion_en_una_sala_inexistente_responde_404(api, staff_headers, movies):
    response = api.post(
        "/showtimes", headers=staff_headers, json=_new_showtime(movies[0]["idMovie"], MISSING_SQL_ID)
    )
    assert response.status_code == 404, response.text


def _movie_with_showtimes(showtimes: list[dict]) -> str:
    movie_ids = {showtime["movieId"] for showtime in showtimes}
    if len(movie_ids) < 2:
        pytest.skip("Hacen falta funciones de al menos dos películas para probar el filtro.")
    return showtimes[0]["movieId"]


def test_funciones_por_pelicula_con_la_ruta_dedicada(api, showtimes):
    movie_id = _movie_with_showtimes(showtimes)
    response = api.get(f"/showtimes/movie/{movie_id}")
    assert response.status_code == 200
    result = response.json()
    assert result
    assert {showtime["movieId"] for showtime in result} == {movie_id}


def test_funciones_por_pelicula_con_el_filtro_que_usa_el_frontend(api, showtimes):
    movie_id = _movie_with_showtimes(showtimes)
    response = api.get("/showtimes", params={"idMovie": movie_id})
    assert response.status_code == 200
    result = response.json()
    assert result
    assert {showtime["movieId"] for showtime in result} == {movie_id}


def test_las_funciones_traen_el_nombre_de_sala_y_sede(api):
    for showtime in api.get("/showtimes").json():
        assert showtime["nameRoom"], showtime
        assert showtime["nameVenue"], showtime


def test_las_funciones_salen_ordenadas_por_fecha_y_hora(api):
    result = api.get("/showtimes").json()
    keys = [(s["showDate"], s["startTime"]) for s in result]
    assert keys == sorted(keys)


def test_funciones_por_sede_y_fecha(api, showtimes):
    sample = showtimes[0]
    response = api.get("/showtimes/by-venue", params={"idVenue": sample["idVenue"], "date": sample["showDate"]})
    assert response.status_code == 200
    result = response.json()
    assert sample["idShowtime"] in {s["idShowtime"] for s in result}
    assert {(s["idVenue"], s["showDate"]) for s in result} == {(sample["idVenue"], sample["showDate"])}


def test_funciones_por_sede_es_publico_y_exige_la_fecha(api):
    assert api.get("/showtimes/by-venue", params={"idVenue": 1}).status_code == 400


def test_resumen_de_la_funcion(api, showtimes):
    # Detalle recién pedido: la lista de la sesión se descarga al inicio y puede
    # haberse armado antes de que cartelera viera a sucursales.
    sample = api.get(f"/showtimes/{showtimes[0]['idShowtime']}").json()
    response = api.get(f"/showtimes/{sample['idShowtime']}/summary")
    assert response.status_code == 200
    summary = response.json()
    assert summary["titleMovie"] == sample["movieTitle"]
    assert summary["nameVenue"] == sample["nameVenue"]
    assert summary["nameRoom"] == sample["nameRoom"]
    assert summary["showDate"] == sample["showDate"]
    assert summary["startTime"] == sample["startTime"]
    assert summary["languageFormat"] == sample["languageFormat"]


def _medio_sol(monto: float) -> float:
    """Redondeo a S/ 0,50, con las mitades hacia arriba (como Tarifas.java)."""
    return math.floor(monto * 2 + 0.5) / 2


def test_tarifas_de_la_funcion(api, showtimes):
    sample = showtimes[0]
    base = sample["baseTicketPrice"]
    response = api.get(f"/showtimes/{sample['idShowtime']}/fares")
    assert response.status_code == 200
    fares = {fare["categoryCode"]: fare["price"] for fare in response.json()}
    assert fares == {
        "ADULTO": pytest.approx(base),
        "NINO": pytest.approx(_medio_sol(base * 0.55)),
        "ADULTO_MAYOR": pytest.approx(_medio_sol(base * 0.65)),
        "DISCAPACITADO": pytest.approx(_medio_sol(base * 0.65)),
    }


@pytest.mark.parametrize("suffix", ["summary", "fares"])
def test_resumen_y_tarifas_de_una_funcion_inexistente_responden_404(api, suffix):
    assert api.get(f"/showtimes/{MISSING_MONGO_ID}/{suffix}").status_code == 404


# ---- formulario del admin de películas ---------------------------------------

def _formulario_pelicula(api, headers, metodo: str, ruta: str, pelicula: dict) -> httpx.Response:
    """Igual que el admin: los datos en la parte "movie" (JSON) y el póster opcional en "file"."""
    return api.request(metodo, ruta, headers=headers,
                       files={"movie": (None, json.dumps(pelicula), "application/json")})


def test_el_admin_crea_y_edita_una_pelicula_con_su_formulario(api, staff_headers):
    clasificacion = api.get("/classifications").json()[0]["idClassification"]
    pelicula = {
        "titleMovie": unique("pelicula"), "director": "Prueba", "synopsis": "Prueba",
        "durationMinutes": 95, "releaseDate": None, "status": "Preventa", "premiereWeek": False,
        "classificationId": clasificacion, "genreIds": [], "posterUrl": "https://ejemplo.test/poster.jpg",
    }
    creada = _formulario_pelicula(api, staff_headers, "POST", "/movies", pelicula)
    assert creada.status_code == 201, creada.text
    id_movie = creada.json()["idMovie"]
    try:
        # Editar sin subir otro póster conserva el que tenía.
        cambios = {**pelicula, "durationMinutes": 100, "posterUrl": None}
        editada = _formulario_pelicula(api, staff_headers, "PUT", f"/movies/{id_movie}", cambios)
        assert editada.status_code == 200, editada.text
        assert editada.json()["durationMinutes"] == 100
        assert editada.json()["posterUrl"] == "https://ejemplo.test/poster.jpg"
    finally:
        api.delete(f"/movies/{id_movie}", headers=staff_headers)


def test_el_formulario_de_pelicula_valida_los_datos(api, staff_headers):
    respuesta = _formulario_pelicula(api, staff_headers, "POST", "/movies", {"titleMovie": ""})
    assert respuesta.status_code == 400
    assert "obligatori" in respuesta.text

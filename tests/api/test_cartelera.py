"""Cartelera pública: películas, detalle y funciones."""

import pytest

from support import MISSING_MONGO_ID


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


@pytest.mark.xfail(
    strict=True,
    reason=(
        "Bug conocido: GET /showtimes ignora el parámetro idMovie y devuelve las "
        "funciones de todas las películas (ShowtimeController.findAll no recibe "
        "filtros). El frontend usa esta forma en movie-detail.ts, así que el "
        "detalle de una película muestra horarios de otras. Se corrige con el "
        "arreglo del flujo de compra."
    ),
)
def test_funciones_por_pelicula_con_el_filtro_que_usa_el_frontend(api, showtimes):
    movie_id = _movie_with_showtimes(showtimes)
    response = api.get("/showtimes", params={"idMovie": movie_id})
    assert response.status_code == 200
    result = response.json()
    assert result
    assert {showtime["movieId"] for showtime in result} == {movie_id}

// ===================================================================
// SECCIÓN: CONEXIÓN Y LIMPIEZA (Reset de la base de datos)
// ===================================================================
db = db.getSiblingDB('cartelera_db');

db.movies.drop();
db.genres.drop();
db.classifications.drop();
db.showtimes.drop();

db.createCollection('genres');
db.createCollection('classifications');
db.createCollection('movies');
db.createCollection('showtimes');

// ===================================================================
// SECCIÓN: GENRES (Géneros de película)
// ===================================================================
db.genres.insertMany([
    { nameGenre: "Acción" },
    { nameGenre: "Aventura" },
    { nameGenre: "Ciencia Ficción" },
    { nameGenre: "Comedia" },
    { nameGenre: "Drama" },
    { nameGenre: "Terror" },
    { nameGenre: "Animación" },
    { nameGenre: "Romance" }
]);

// ===================================================================
// SECCIÓN: CLASSIFICATIONS (Clasificaciones por edad)
// ===================================================================
db.classifications.insertMany([
    { nameClassification: "APT", descriptionText: "Apto para todo público" },
    { nameClassification: "PG-13", descriptionText: "Mayores de 13 años" },
    { nameClassification: "PG-18", descriptionText: "Mayores de 18 años" }
]);

// ===================================================================
// SECCIÓN: HELPERS DE REFERENCIA (Buscar IDs de genres/classifications)
// ===================================================================
const genres = db.genres.find().toArray();
const classifications = db.classifications.find().toArray();

function genreId(name) {
    return genres.find(g => g.nameGenre === name)._id.toString();
}

function classificationId(name) {
    return classifications.find(c => c.nameClassification === name)._id.toString();
}

// ===================================================================
// SECCIÓN: MOVIES (Películas: cartelera, estreno y preventa)
// ===================================================================
db.movies.insertMany([
    {
        titleMovie: "Interestelar",
        synopsis: "Un grupo de exploradores viaja a través de un agujero de gusano en el espacio en un intento de garantizar la supervivencia de la humanidad.",
        durationMinutes: 169,
        posterUrl: "",
        releaseDate: ISODate("2026-07-01"),
        status: "Cartelera",
        director: "Christopher Nolan",
        genreIds: [
            genreId("Ciencia Ficción"),
            genreId("Aventura"),
            genreId("Drama")
        ],
        classificationId: classificationId("PG-13"),
        premiereWeek: false,
        isActive: true,
        createdAt: new Date()
    },
    {
        titleMovie: "Sonic 3",
        synopsis: "Sonic se enfrenta a un nuevo y poderoso enemigo mientras descubre los secretos del pasado.",
        durationMinutes: 110,
        posterUrl: "",
        releaseDate: ISODate("2026-06-20"),
        status: "Cartelera",
        director: "Jeff Fowler",
        genreIds: [
            genreId("Animación"),
            genreId("Aventura"),
            genreId("Comedia")
        ],
        classificationId: classificationId("APT"),
        premiereWeek: false,
        isActive: true,
        createdAt: new Date()
    },
    {
        titleMovie: "El Conjuro 4",
        synopsis: "Los Warren enfrentan uno de los casos más aterradores de su carrera.",
        durationMinutes: 120,
        posterUrl: "",
        releaseDate: ISODate("2026-07-10"),
        status: "Estreno",
        director: "Michael Chaves",
        genreIds: [
            genreId("Terror"),
            genreId("Drama")
        ],
        classificationId: classificationId("PG-18"),
        premiereWeek: true,
        isActive: true,
        createdAt: new Date()
    },
    {
        titleMovie: "Spider-Man",
        synopsis: "El Hombre Araña enfrenta una nueva amenaza que pone en riesgo a toda la ciudad.",
        durationMinutes: 130,
        posterUrl: "",
        releaseDate: ISODate("2026-08-15"),
        status: "Preventa",
        director: "Jon Watts",
        genreIds: [
            genreId("Acción"),
            genreId("Aventura"),
            genreId("Ciencia Ficción")
        ],
        classificationId: classificationId("PG-13"),
        premiereWeek: false,
        isActive: true,
        createdAt: new Date()
    }
]);

// ===================================================================
// SECCIÓN: HELPERS DE FECHAS (Cálculo de días y horarios)
// ===================================================================
const movies = db.movies.find({}, { idMovie: 1, titleMovie: 1 }).toArray();

function getMovieId(title) {
    return movies.find(m => m.titleMovie === title)._id.toString();
}

const today = new Date();
const tomorrow = new Date(today);
tomorrow.setDate(tomorrow.getDate() + 1);

// Calcula el próximo miércoles a partir de hoy (si hoy es miércoles, toma hoy mismo)
function getNextWednesday(fromDate) {
    const date = new Date(fromDate);
    const day = date.getDay(); // 0=Domingo, 1=Lunes, ..., 3=Miércoles
    const daysUntilWednesday = (3 - day + 7) % 7;
    date.setDate(date.getDate() + daysUntilWednesday);
    return date;
}

const nextWednesday = getNextWednesday(today);

function makeShowtime(date, hour, minute) {
    const dt = new Date(date);
    dt.setHours(hour, minute, 0, 0);
    return dt;
}

function makeEndtime(start, durationMin) {
    return new Date(start.getTime() + durationMin * 60000);
}

// ===================================================================
// SECCIÓN: SHOWTIMES (Funciones de cine por película y horario)
// ===================================================================
db.showtimes.insertMany([
    {
        movieId: getMovieId("Interestelar"),
        showDate: ISODate(today.toISOString().split('T')[0]),
        startTime: makeShowtime(today, 14, 30),
        endTime: makeEndtime(makeShowtime(today, 14, 30), 169),
        languageFormat: "Subtitulada",
        baseTicketPrice: NumberDecimal("18.00"),
        status: "Programada",
        idRoom: 1,
        idVenue: 1,
        availableSeats: 80
    },
    {
        movieId: getMovieId("Interestelar"),
        showDate: ISODate(today.toISOString().split('T')[0]),
        startTime: makeShowtime(today, 18, 0),
        endTime: makeEndtime(makeShowtime(today, 18, 0), 169),
        languageFormat: "Subtitulada",
        baseTicketPrice: NumberDecimal("20.00"),
        status: "Programada",
        idRoom: 1,
        idVenue: 1,
        availableSeats: 80
    },
    {
        movieId: getMovieId("Interestelar"),
        showDate: ISODate(tomorrow.toISOString().split('T')[0]),
        startTime: makeShowtime(tomorrow, 16, 0),
        endTime: makeEndtime(makeShowtime(tomorrow, 16, 0), 169),
        languageFormat: "Subtitulada",
        baseTicketPrice: NumberDecimal("18.00"),
        status: "Programada",
        idRoom: 4,
        idVenue: 2,
        availableSeats: 100
    },
    {
        movieId: getMovieId("Sonic 3"),
        showDate: ISODate(today.toISOString().split('T')[0]),
        startTime: makeShowtime(today, 15, 0),
        endTime: makeEndtime(makeShowtime(today, 15, 0), 110),
        languageFormat: "Doblada",
        baseTicketPrice: NumberDecimal("15.00"),
        status: "Programada",
        idRoom: 2,
        idVenue: 1,
        availableSeats: 60
    },
    {
        movieId: getMovieId("Sonic 3"),
        showDate: ISODate(tomorrow.toISOString().split('T')[0]),
        startTime: makeShowtime(tomorrow, 11, 0),
        endTime: makeEndtime(makeShowtime(tomorrow, 11, 0), 110),
        languageFormat: "Doblada",
        baseTicketPrice: NumberDecimal("12.00"),
        status: "Programada",
        idRoom: 2,
        idVenue: 1,
        availableSeats: 60
    },
    {
        movieId: getMovieId("El Conjuro 4"),
        showDate: ISODate(today.toISOString().split('T')[0]),
        startTime: makeShowtime(today, 20, 0),
        endTime: makeEndtime(makeShowtime(today, 20, 0), 120),
        languageFormat: "Subtitulada",
        baseTicketPrice: NumberDecimal("22.00"),
        status: "Programada",
        idRoom: 3,
        idVenue: 1,
        availableSeats: 40
    },
    {
        movieId: getMovieId("El Conjuro 4"),
        showDate: ISODate(tomorrow.toISOString().split('T')[0]),
        startTime: makeShowtime(tomorrow, 19, 30),
        endTime: makeEndtime(makeShowtime(tomorrow, 19, 30), 120),
        languageFormat: "Subtitulada",
        baseTicketPrice: NumberDecimal("22.00"),
        status: "Programada",
        idRoom: 5,
        idVenue: 2,
        availableSeats: 70
    },
    {

        movieId: getMovieId("Spider-Man"),
        showDate: ISODate(nextWednesday.toISOString().split('T')[0]),
        startTime: makeShowtime(nextWednesday, 19, 0),
        endTime: makeEndtime(makeShowtime(nextWednesday, 19, 0), 130),
        languageFormat: "Subtitulada",
        baseTicketPrice: NumberDecimal("25.00"),
        status: "Programada",
        idRoom: 1,
        idVenue: 1,
        availableSeats: 80
    }
]);

// ===================================================================
// SECCIÓN: VERIFICACIÓN (Conteo final de documentos cargados)
// ===================================================================
print("Seed cargado: " + db.movies.countDocuments() + " movies, " + db.showtimes.countDocuments() + " showtimes");
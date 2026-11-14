// Página de un listado, tal como la devuelve el backend.
export interface Pagina<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

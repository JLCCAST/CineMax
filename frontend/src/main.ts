import { bootstrapApplication } from '@angular/platform-browser';
// Bootstrap (JS) una sola vez, como módulo: activa los data-bs-* (menú, paneles, modales).
// Si además se cargara como script global, cada clic abriría y cerraría a la vez.
import 'bootstrap';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

// La misma regla que el registro: 8 caracteres o más, con letras y números.
const PATRON_CLAVE = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/;

function passwordsCoinciden(group: AbstractControl): ValidationErrors | null {
  const nueva = group.get('newPassword')?.value;
  const confirmar = group.get('confirmPassword')?.value;
  return nueva === confirmar ? null : { noCoinciden: true };
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css'
})
export class ResetPasswordComponent implements OnInit {
  token = '';
  resetForm!: FormGroup;
  errorMessage = '';
  successMessage = '';
  loading = false;
  mostrarPassword = false;
  mostrarConfirm = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';

    this.resetForm = this.fb.group({
      newPassword:     ['', [Validators.required, Validators.pattern(PATRON_CLAVE)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: passwordsCoinciden });

    if (!this.token) {
      this.errorMessage = 'El enlace no es válido. Pide uno nuevo desde "¿Olvidaste tu contraseña?".';
      this.cdr.detectChanges();
    }
  }

  get nf() { return this.resetForm.controls; }

  get confirmacionInvalida(): boolean {
    const tocado = this.nf['confirmPassword'].touched;
    return tocado && (this.nf['confirmPassword'].invalid || !!this.resetForm.errors?.['noCoinciden']);
  }

  cambiarContrasena() {
    this.errorMessage = '';
    this.successMessage = '';
    this.resetForm.markAllAsTouched();

    if (this.resetForm.invalid || !this.token) return;

    this.loading = true;
    this.cdr.detectChanges();

    this.authService.resetPassword(this.token, this.resetForm.value.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Listo, tu contraseña cambió. Te llevamos al inicio para que ingreses.';
        this.cdr.detectChanges();
        setTimeout(() => { this.router.navigate(['/']); }, 3000);
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || err.error?.error || 'El enlace venció o ya se usó. Pide uno nuevo.';
        this.cdr.detectChanges();
      }
    });
  }
}

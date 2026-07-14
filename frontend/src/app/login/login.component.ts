import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  errorMessage = '';
  isLoading = false;
  passwordVisible = false;

  togglePasswordVisibility(): void {
    this.passwordVisible = !this.passwordVisible;
  }

  onSubmit(): void {
    if (!this.email.trim() || !this.password.trim()) {
      this.errorMessage = 'Por favor, completa todos los campos.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.email, this.password).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 401) {
          this.errorMessage = 'Correo o contraseña incorrectos.';
        } else {
          this.errorMessage = 'Ocurrió un error al iniciar sesión. Inténtalo de nuevo.';
        }
      }
    });
  }

  // --- MÉTODOS DE RECUPERACIÓN DE CONTRASEÑA ---
  mostrarModalRecuperar = false;
  recuperarPaso: 'solicitar' | 'restablecer' = 'solicitar';
  recuperarEmail = '';
  recuperarCodigo = '';
  recuperarNuevaPass = '';
  mensajeRecuperar = '';
  errorRecuperar = '';
  cargandoRecuperar = false;

  abrirModalRecuperar(): void {
    this.mostrarModalRecuperar = true;
    this.recuperarPaso = 'solicitar';
    this.recuperarEmail = '';
    this.recuperarCodigo = '';
    this.recuperarNuevaPass = '';
    this.mensajeRecuperar = '';
    this.errorRecuperar = '';
  }

  cerrarModalRecuperar(): void {
    this.mostrarModalRecuperar = false;
  }

  enviarCodigo(): void {
    if (!this.recuperarEmail.trim()) {
      this.errorRecuperar = 'Por favor ingresa tu correo electrónico.';
      return;
    }

    this.cargandoRecuperar = true;
    this.errorRecuperar = '';
    this.mensajeRecuperar = '';

    this.authService.solicitarRecuperacion(this.recuperarEmail.trim()).subscribe({
      next: (res) => {
        this.cargandoRecuperar = false;
        this.mensajeRecuperar = res.message || 'Código enviado con éxito.';
        this.recuperarPaso = 'restablecer';
      },
      error: (err) => {
        this.cargandoRecuperar = false;
        this.errorRecuperar = err.error?.error || 'Error al enviar el código de recuperación.';
        console.error(err);
      }
    });
  }

  confirmarRestablecimiento(): void {
    if (!this.recuperarCodigo.trim() || !this.recuperarNuevaPass.trim()) {
      this.errorRecuperar = 'Por favor ingresa el código y tu nueva contraseña.';
      return;
    }

    this.cargandoRecuperar = true;
    this.errorRecuperar = '';
    this.mensajeRecuperar = '';

    const payload = {
      email: this.recuperarEmail.trim(),
      code: this.recuperarCodigo.trim(),
      newPassword: this.recuperarNuevaPass.trim()
    };

    this.authService.restablecerContrasena(payload).subscribe({
      next: () => {
        this.cargandoRecuperar = false;
        this.mensajeRecuperar = 'Contraseña restablecida con éxito. Redirigiendo...';
        setTimeout(() => {
          this.cerrarModalRecuperar();
        }, 2000);
      },
      error: (err) => {
        this.cargandoRecuperar = false;
        this.errorRecuperar = err.error?.error || 'Código incorrecto o expirado.';
        console.error(err);
      }
    });
  }
}
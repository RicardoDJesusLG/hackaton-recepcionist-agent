import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  // Datos de usuario
  username = '';
  password = '';

  // Configuración de empresa
  crearNuevaEmpresa = true;
  empresaId = '';
  
  // Datos de nueva empresa
  nombreEmpresa = '';
  whatsappPhoneId = '';
  whatsappToken = '';
  direccion = '';
  descripcionNegocio = '';
  telefonoContacto = '';
  mapsLink = '';

  errorMessage = '';
  successMessage = '';
  isLoading = false;

  onSubmit(): void {
    if (!this.username.trim() || !this.password.trim()) {
      this.errorMessage = 'El nombre de usuario y contraseña son obligatorios.';
      return;
    }

    const payload: any = {
      username: this.username,
      password: this.password,
      crearNuevaEmpresa: this.crearNuevaEmpresa
    };

    if (this.crearNuevaEmpresa) {
      if (!this.nombreEmpresa.trim() || !this.whatsappPhoneId.trim() || !this.whatsappToken.trim()) {
        this.errorMessage = 'El nombre de la empresa, el ID de WhatsApp y el token de acceso son obligatorios.';
        return;
      }
      payload.nombreEmpresa = this.nombreEmpresa.trim();
      payload.whatsappPhoneId = this.whatsappPhoneId.trim();
      payload.whatsappToken = this.whatsappToken.trim();
      payload.direccion = this.direccion.trim();
      payload.descripcionNegocio = this.descripcionNegocio.trim();
      payload.telefonoContacto = this.telefonoContacto.trim();
      payload.mapsLink = this.mapsLink.trim();
    } else {
      if (!this.empresaId.trim()) {
        this.errorMessage = 'El ID de la empresa es obligatorio.';
        return;
      }
      payload.empresaId = this.empresaId.trim();
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.register(payload).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.successMessage = 'Registro exitoso. Redirigiendo al inicio de sesión...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 409) {
          this.errorMessage = 'El nombre de usuario ya está tomado.';
        } else if (err.status === 400) {
          this.errorMessage = err.error?.error || 'ID de empresa no válido o datos incorrectos.';
        } else if (err.status === 404) {
          this.errorMessage = 'La empresa vinculada no existe.';
        } else {
          this.errorMessage = 'Error en el servidor. Inténtalo más tarde.';
        }
      }
    });
  }

  toggleCrearEmpresa(val: boolean): void {
    this.crearNuevaEmpresa = val;
    this.errorMessage = '';
  }
}

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

  // Datos Originales Conservados
  email = '';
  password = '';
  crearNuevaEmpresa = true;
  empresaId = '';
  nombreEmpresa = '';
  whatsappPhoneId = '';
  whatsappToken = '';
  direccion = '';
  descripcionNegocio = '';
  telefonoContacto = '';
  prefijoTelefono = '52';
  mapsLink = '';
  errorMessage = '';
  successMessage = '';
  isLoading = false;

  // Control de pasos
  pasoRegistro: 1 | 2 = 1;

  avanzarAlPaso2(): void {
    if (!this.email.trim() || !this.password.trim()) {
      this.errorMessage = 'El correo electrónico y contraseña son obligatorios.';
      return;
    }
    if (!this.email.includes('@') || !this.email.includes('.')) {
      this.errorMessage = 'Por favor ingresa un correo electrónico válido.';
      return;
    }

    // --- NUEVA VALIDACIÓN DE CONTRASEÑA ---
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,}$/;
    if (!passwordRegex.test(this.password)) {
      this.errorMessage = 'La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.';
      return;
    }
    
    // Si todo está bien, limpiamos errores y avanzamos
    this.errorMessage = '';
    this.pasoRegistro = 2;
  }

  regresarAlPaso1(): void {
    this.pasoRegistro = 1;
    this.errorMessage = '';
  }

  onSubmit(): void {
    if (!this.email.trim() || !this.password.trim()) {
      this.errorMessage = 'El correo electrónico y contraseña son obligatorios.';
      return;
    }
    if (!this.email.includes('@') || !this.email.includes('.')) {
      this.errorMessage = 'Por favor ingresa un correo electrónico válido.';
      return;
    }

    // --- VALIDACIÓN DE CONTRASEÑA (RE-COMPROBACIÓN EN SUBMIT) ---
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,}$/;
    if (!passwordRegex.test(this.password)) {
      this.errorMessage = 'La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.';
      return;
    }

    const payload: any = {
      email: this.email.trim().toLowerCase(),
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
      payload.telefonoContacto = this.telefonoContacto.trim() ? `+${this.prefijoTelefono}${this.telefonoContacto.trim().replace(/\D/g, '')}` : '';
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
          this.errorMessage = 'El correo electrónico ya está registrado.';
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
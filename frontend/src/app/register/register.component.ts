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

  username = '';
  password = '';
  empresaId = '';
  errorMessage = '';
  successMessage = '';
  isLoading = false;

  onSubmit(): void {
    if (!this.username.trim() || !this.password.trim() || !this.empresaId.trim()) {
      this.errorMessage = 'Todos los campos son obligatorios.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.register(this.username, this.password, this.empresaId).subscribe({
      next: () => {
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
          this.errorMessage = 'ID de empresa no válido o datos incorrectos.';
        } else {
          this.errorMessage = 'Error en el servidor. Inténtalo más tarde.';
        }
      }
    });
  }
}

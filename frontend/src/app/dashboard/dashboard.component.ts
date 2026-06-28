import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { DashboardService } from '../dashboard.service';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
  private router = inject(Router);

  citas: any[] = [];
  username = '';
  empresaId = '';
  errorMessage = '';
  isLoading = false;

  // Estadísticas básicas
  totalCitas = 0;
  citasConfirmadas = 0;
  citasCanceladas = 0;

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    this.username = this.authService.getUsername() || '';
    this.empresaId = this.authService.getEmpresaId() || '';
    
    this.cargarCitas();
  }

  cargarCitas(): void {
    this.isLoading = true;
    this.errorMessage = '';
    
    this.dashboardService.getCitas().subscribe({
      next: (data) => {
        this.citas = data;
        this.calcularEstadisticas();
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'No se pudieron cargar las citas. Verifica tu sesión.';
        console.error(err);
      }
    });
  }

  calcularEstadisticas(): void {
    this.totalCitas = this.citas.length;
    this.citasConfirmadas = this.citas.filter(c => c.estado === 'CONFIRMADA').length;
    this.citasCanceladas = this.citas.filter(c => c.estado === 'CANCELADA').length;
  }

  cancelarCita(idCita: string): void {
    if (!confirm('¿Estás seguro de que deseas cancelar esta cita?')) {
      return;
    }

    this.dashboardService.cancelarCita(idCita).subscribe({
      next: () => {
        this.cargarCitas();
      },
      error: (err) => {
        alert('Error al cancelar la cita. Inténtalo de nuevo.');
        console.error(err);
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}

import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardService } from '../dashboard.service';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
  private router = inject(Router);

  // Navegación
  activeTab: 'citas' | 'negocio' | 'horarios' = 'citas';

  // Datos del Propietario
  username = '';
  empresaId = '';
  errorMessage = '';
  successMessage = '';
  isLoading = false;

  // Citas y Estadísticas
  citas: any[] = [];
  totalCitas = 0;
  citasConfirmadas = 0;
  citasCanceladas = 0;

  // Datos de la Empresa
  empresa: any = {
    nombre: '',
    whatsappPhoneId: '',
    direccion: '',
    descripcionNegocio: '',
    telefonoContacto: '',
    mapsLink: '',
    suscripcionActiva: true
  };

  // Horarios de Agenda
  horarios: any[] = [];
  diasSemanaNombres = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    this.username = this.authService.getUsername() || '';
    this.empresaId = this.authService.getEmpresaId() || '';
    
    this.cargarCitas();
    this.cargarDatosEmpresa();
    this.cargarHorariosAgenda();
  }

  // --- NAVEGACIÓN ---
  setTab(tab: 'citas' | 'negocio' | 'horarios'): void {
    this.activeTab = tab;
    this.errorMessage = '';
    this.successMessage = '';
  }

  // --- GESTIÓN DE CITAS ---
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

  // --- GESTIÓN DE EMPRESA ---
  cargarDatosEmpresa(): void {
    this.dashboardService.getEmpresa().subscribe({
      next: (data) => {
        this.empresa = data;
      },
      error: (err) => {
        console.error('Error al cargar datos de empresa:', err);
      }
    });
  }

  guardarDatosEmpresa(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.dashboardService.updateEmpresa(this.empresa).subscribe({
      next: (data) => {
        this.empresa = data;
        this.successMessage = 'Información de la empresa guardada correctamente.';
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Error al guardar la información de la empresa.';
        console.error(err);
      }
    });
  }

  // --- GESTIÓN DE HORARIOS ---
  cargarHorariosAgenda(): void {
    this.dashboardService.getAgenda().subscribe({
      next: (data) => {
        // Mapear respuesta del backend asegurando estructura para cada día (0 a 6)
        this.horarios = this.diasSemanaNombres.map((nombre, index) => {
          const config = data.find(c => c.diaSemana === index);
          if (config) {
            return {
              diaSemana: index,
              nombreDia: nombre,
              horaInicio: config.horaInicio.substring(0, 5), // HH:MM
              horaFin: config.horaFin.substring(0, 5),     // HH:MM
              cerrado: false
            };
          } else {
            return {
              diaSemana: index,
              nombreDia: nombre,
              horaInicio: '09:00',
              horaFin: '18:00',
              cerrado: true
            };
          }
        });
      },
      error: (err) => {
        console.error('Error al cargar horarios:', err);
      }
    });
  }

  guardarHorariosAgenda(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    // Mapear los datos para el formato esperado por el backend
    const payload = this.horarios.map(h => ({
      diaSemana: h.diaSemana,
      horaInicio: h.cerrado ? null : h.horaInicio + ':00',
      horaFin: h.cerrado ? null : h.horaFin + ':00',
      cerrado: h.cerrado
    }));

    this.dashboardService.updateAgenda(payload).subscribe({
      next: () => {
        this.successMessage = 'Horarios de atención actualizados con éxito.';
        this.isLoading = false;
        this.cargarHorariosAgenda();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Error al actualizar los horarios de atención.';
        console.error(err);
      }
    });
  }

  // --- STRIPE BILLING ---
  pagarSuscripcion(): void {
    this.isLoading = true;
    this.dashboardService.crearCheckoutSession(this.empresaId).subscribe({
      next: (res) => {
        if (res && res.url) {
          window.location.href = res.url;
        } else {
          this.isLoading = false;
          alert('Error al iniciar pasarela de pagos.');
        }
      },
      error: (err) => {
        this.isLoading = false;
        alert('Error al conectar con Stripe.');
        console.error(err);
      }
    });
  }

  // --- SALIDA ---
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}

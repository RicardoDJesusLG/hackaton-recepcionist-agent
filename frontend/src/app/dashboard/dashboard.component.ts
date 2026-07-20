import { Component, OnInit, inject } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
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
  private route = inject(ActivatedRoute);

  // Navegación
  activeTab: 'citas' | 'negocio' | 'horarios' | 'servicios' | 'agente' = 'citas';
  
  camposRequeridos = {
    nombre: true,
    numero: true,
    correo: false
  };
  
  // Datos del Propietario
  email = '';
  empresaId = '';
  errorMessage = '';
  successMessage = '';
  isLoading = false;
  googleCalendarVinculado = false;

  // Citas y Estadísticas
  citas: any[] = [];
  totalCitas = 0;
  citasConfirmadas = 0;
  citasCanceladas = 0;

  // Datos de la Empresa
  empresa: any = {
    nombre: '',
    whatsappPhoneId: '',
    whatsappToken: '',
    direccion: '',
    descripcionNegocio: '',
    telefonoContacto: '',
    mapsLink: '',
    suscripcionActiva: true,
    planSuscripcion: 'BASIC'
  };

  // Estadísticas de Suscripción
  subStats: any = {
    planSuscripcion: 'BASIC',
    suscripcionActiva: true,
    totalServicios: 0,
    limiteServicios: 3,
    citasMesActual: 0,
    limiteCitas: 60
  };

  // Catálogo de Servicios
  servicios: any[] = [];
  mostrarModalServicio = false;
  editandoServicio = false;
  formServicio: any = {
    id: '',
    nombre: '',
    descripcion: '',
    precio: 0,
    duracionMinutos: 30,
    activo: true,
    tipoPromocion: 'NINGUNA',
    valorPromocion: '',
    promocionActiva: false
  };

  // Re-autenticación
  mostrarModalReauth = false;
  reauthEmail = '';
  reauthPassword = '';
  errorReauth = '';
  descripcionNegocioOriginal = '';
  prefijoTelefono = '52';
  telefonoLocal = '';

  // Horarios de Agenda
  horarios: any[] = [];
  diasSemanaNombres = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    this.email = this.authService.getEmail() || '';
    this.empresaId = this.authService.getEmpresaId() || '';
    
    this.cargarCitas();
    this.cargarDatosEmpresa();
    this.cargarHorariosAgenda();
    this.cargarServicios();
    this.cargarEstadisticasSuscripcion();
    this.cargarEstadoGoogleCalendar();

    // Escuchar parámetros de pago de Stripe
    this.route.queryParams.subscribe(params => {
      if (params['payment'] === 'success') {
        this.successMessage = '¡Gracias por tu pago! Tu suscripción ha sido procesada con éxito.';
        this.cargarDatosEmpresa();
        this.cargarEstadisticasSuscripcion();
        
        // Limpiar parámetros de la URL
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { payment: null, mock: null, idNegocio: null },
          queryParamsHandling: 'merge'
        });
      } else if (params['payment'] === 'cancel') {
        this.errorMessage = 'El proceso de pago fue cancelado.';
        this.cargarDatosEmpresa();
        this.cargarEstadisticasSuscripcion();
        
        // Limpiar parámetros de la URL
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { payment: null, mock: null, idNegocio: null },
          queryParamsHandling: 'merge'
        });
      }

      // Escuchar parámetros de vinculación de Google Calendar
      if (params['googleCalendar'] === 'success') {
        this.activeTab = 'negocio';
        this.successMessage = '¡Google Calendar vinculado exitosamente!';
        this.cargarEstadoGoogleCalendar();
        
        // Limpiar parámetros de la URL
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { googleCalendar: null },
          queryParamsHandling: 'merge'
        });
      } else if (params['googleCalendar'] === 'error') {
        this.activeTab = 'negocio';
        this.errorMessage = 'Hubo un error al vincular tu cuenta de Google Calendar. Inténtalo de nuevo.';
        this.cargarEstadoGoogleCalendar();
        
        // Limpiar parámetros de la URL
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { googleCalendar: null },
          queryParamsHandling: 'merge'
        });
      }
    });
  }

  // --- NAVEGACIÓN ---
  setTab(tab: 'citas' | 'negocio' | 'horarios' | 'servicios' | 'agente'): void {
    this.activeTab = tab;
    this.errorMessage = '';
    this.successMessage = '';
    if (tab === 'servicios') {
      this.cargarServicios();
    }
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
        this.descripcionNegocioOriginal = data.descripcionNegocio || '';
        this.extraerPrefijoYNumero();
      },
      error: (err) => {
        console.error('Error al cargar datos de empresa:', err);
      }
    });
  }

  cargarEstadoGoogleCalendar(): void {
    if (!this.empresaId) return;
    this.dashboardService.getGoogleCalendarStatus(this.empresaId).subscribe({
      next: (res) => {
        this.googleCalendarVinculado = res.googleCalendarVinculado;
      },
      error: (err) => {
        console.error('Error al cargar estado de Google Calendar:', err);
      }
    });
  }

  vincularGoogleCalendar(): void {
    if (!this.empresaId) return;
    this.isLoading = true;
    this.dashboardService.getGoogleCalendarAuthUrl(this.empresaId).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.url) {
          window.location.href = res.url;
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Error al obtener la URL de vinculación de Google Calendar.';
        console.error(err);
      }
    });
  }

  desvincularGoogleCalendar(): void {
    if (!this.empresaId) return;
    if (!confirm('¿Estás seguro de que deseas desvincular tu cuenta de Google Calendar? Tus citas ya no se sincronizarán.')) {
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.dashboardService.desvincularGoogleCalendar(this.empresaId).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.googleCalendarVinculado = false;
        this.successMessage = 'Google Calendar desvinculado exitosamente.';
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Error al desvincular Google Calendar.';
        console.error(err);
      }
    });
  }

  extraerPrefijoYNumero(): void {
    const tel = this.empresa.telefonoContacto || '';
    if (!tel) {
      this.prefijoTelefono = '52';
      this.telefonoLocal = '';
      return;
    }
    const limpio = tel.startsWith('+') ? tel.substring(1) : tel;
    const prefijos = ['52', '57', '54', '56', '51', '34', '1'];
    const coincidencia = prefijos.find(p => limpio.startsWith(p));
    if (coincidencia) {
      this.prefijoTelefono = coincidencia;
      this.telefonoLocal = limpio.substring(coincidencia.length);
    } else {
      this.prefijoTelefono = '52';
      this.telefonoLocal = limpio;
    }
  }

  guardarDatosEmpresa(): void {
    const nuevaDescripcion = this.empresa.descripcionNegocio || '';
    if (nuevaDescripcion.trim() !== this.descripcionNegocioOriginal.trim()) {
      this.mostrarModalReauth = true;
      this.reauthEmail = this.email;
      this.reauthPassword = '';
      this.errorReauth = '';
      return;
    }
    this.ejecutarGuardarEmpresa();
  }

  ejecutarGuardarEmpresa(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    const numLimpio = this.telefonoLocal.replace(/\D/g, '');
    this.empresa.telefonoContacto = numLimpio ? `+${this.prefijoTelefono}${numLimpio}` : '';
    this.dashboardService.updateEmpresa(this.empresa).subscribe({
      next: (data) => {
        this.empresa = data;
        this.descripcionNegocioOriginal = data.descripcionNegocio || '';
        this.extraerPrefijoYNumero();
        this.successMessage = 'Información de la empresa guardada correctamente.';
        this.isLoading = false;
        this.cargarEstadisticasSuscripcion();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Error al guardar la información de la empresa.';
        console.error(err);
      }
    });
  }

  // --- RE-AUTENTICACIÓN TODOS ---
  confirmarReauthYGuardar(): void {
    if (!this.reauthPassword.trim()) {
      this.errorReauth = 'La contraseña es requerida.';
      return;
    }
    this.isLoading = true;
    this.errorReauth = '';
    this.authService.verificarCredenciales(this.reauthEmail, this.reauthPassword).subscribe({
      next: () => {
        this.mostrarModalReauth = false;
        this.isLoading = false;
        this.ejecutarGuardarEmpresa();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorReauth = 'Contraseña incorrecta de administrador.';
        console.error(err);
      }
    });
  }

  cancelarReauth(): void {
    this.mostrarModalReauth = false;
    this.empresa.descripcionNegocio = this.descripcionNegocioOriginal;
    this.errorReauth = '';
  }

  // --- GESTIÓN DE SERVICIOS CRUD ---
  cargarServicios(): void {
    this.isLoading = true;
    this.dashboardService.getServicios().subscribe({
      next: (data) => {
        this.servicios = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Error al cargar el catálogo de servicios.';
        console.error(err);
      }
    });
  }

  abrirModalServicio(servicio?: any): void {
    if (servicio) {
      this.editandoServicio = true;
      this.formServicio = { ...servicio };
    } else {
      this.editandoServicio = false;
      this.formServicio = {
        id: '',
        nombre: '',
        descripcion: '',
        precio: 0,
        duracionMinutos: 30,
        activo: true,
        tipoPromocion: 'NINGUNA',
        valorPromocion: '',
        promocionActiva: false
      };
    }
    this.mostrarModalServicio = true;
  }

  cerrarModalServicio(): void {
    this.mostrarModalServicio = false;
  }

  guardarServicio(): void {
    if (!this.formServicio.nombre.trim() || this.formServicio.precio < 0 || this.formServicio.duracionMinutos < 5) {
      alert('Por favor llena los campos obligatorios con valores correctos.');
      return;
    }

    if (this.formServicio.promocionActiva && this.formServicio.tipoPromocion === 'PERSONALIZADA') {
      const confirmacion = confirm(
        '⚠️ Estás activando una promoción personalizada.\n\n' +
        'Por favor, asegúrate de verificar que el texto (prompt) de la promoción sea correcto, profesional y no altere de forma negativa el comportamiento del agente.\n\n' +
        '¿Deseas continuar?'
      );
      if (!confirmacion) {
        return;
      }
    }

    this.isLoading = true;
    if (this.editandoServicio) {
      this.dashboardService.updateServicio(this.formServicio.id, this.formServicio).subscribe({
        next: () => {
          this.cerrarModalServicio();
          this.cargarServicios();
        },
        error: (err) => {
          this.isLoading = false;
          alert('Error al actualizar el servicio.');
          console.error(err);
        }
      });
    } else {
      this.dashboardService.crearServicio(this.formServicio).subscribe({
        next: () => {
          this.cerrarModalServicio();
          this.cargarServicios();
        },
        error: (err) => {
          this.isLoading = false;
          alert('Error al crear el servicio.');
          console.error(err);
        }
      });
    }
  }

  eliminarServicio(id: string): void {
    if (!confirm('¿Estás seguro de que deseas eliminar este servicio? Si tiene citas asociadas, se desactivará en su lugar.')) {
      return;
    }
    this.isLoading = true;
    this.dashboardService.eliminarServicio(id).subscribe({
      next: (res: any) => {
        if (res && res.softDeleted) {
          alert('El servicio se desactivó porque tiene citas asociadas.');
        }
        this.cargarServicios();
      },
      error: (err) => {
        this.isLoading = false;
        alert('Error al eliminar el servicio.');
        console.error(err);
      }
    });
  }

  // --- GESTIÓN DE HORARIOS ---
  cargarHorariosAgenda(): void {
    this.dashboardService.getAgenda().subscribe({
      next: (data) => {
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
  pagarSuscripcion(plan: string): void {
    this.isLoading = true;
    this.dashboardService.crearCheckoutSession(this.empresaId, plan).subscribe({
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

  gestionarSuscripcion(): void {
    this.isLoading = true;
    this.dashboardService.crearPortalSession(this.empresaId).subscribe({
      next: (res) => {
        if (res && res.url) {
          window.location.href = res.url;
        } else {
          this.isLoading = false;
          alert('Error al redirigir al portal de facturación.');
        }
      },
      error: (err) => {
        this.isLoading = false;
        alert('Error al conectar con Stripe.');
        console.error(err);
      }
    });
  }

  cargarEstadisticasSuscripcion(): void {
    this.dashboardService.getSubscriptionStats().subscribe({
      next: (data) => {
        this.subStats = data;
        this.empresa.suscripcionActiva = data.suscripcionActiva;
        this.empresa.planSuscripcion = data.planSuscripcion;
      },
      error: (err) => {
        console.error('Error al cargar estadísticas de suscripción:', err);
      }
    });
  }

  // --- SALIDA ---
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
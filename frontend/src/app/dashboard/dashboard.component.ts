import { Component, OnInit, inject } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardService } from '../dashboard.service';
import { AuthService } from '../auth.service';
import { PhoneFormatPipe } from '../pipes/phone-format.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, FormsModule, PhoneFormatPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  // ==========================================
  // CONTROL DE LA BARRA LATERAL MÓVIL
  // ==========================================
  sidebarOpen: boolean = false;

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

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
  citasFiltradas: any[] = [];
  totalCitas = 0;
  citasConfirmadas = 0;
  citasCanceladas = 0;

  // Filtros de Citas
  filtroEstado: 'TODOS' | 'CONFIRMADA' | 'CANCELADA' = 'TODOS';
  filtroFechaTipo: 'HOY_Y_MANANA' | 'ESPECIFICO' | 'TODAS' = 'HOY_Y_MANANA';
  fechaSeleccionada: string = ''; // YYYY-MM-DD

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
    planSuscripcion: 'BASIC',
    urlMenuImagen: '',
    activarEnvioMenu: false,
    envioMenuInmediato: false
  };

  selectedMenuFile: File | null = null;
  isUploadingMenu = false;

  // Estadísticas de Suscripción
  subStats: any = {
    planSuscripcion: 'BASIC',
    suscripcionActiva: true,
    totalServicios: 0,
    limiteServicios: 2147483647,
    limiteCitas: 60,
    tieneStripeCustomer: false
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

  // Re-autenticación y Detección de Cambios
  mostrarModalReauth = false;
  reauthEmail = '';
  reauthPassword = '';
  errorReauth = '';
  descripcionNegocioOriginal = '';
  prefijoTelefono = '52';
  telefonoLocal = '';
  estadoOriginalAgente: string = '';

  actualizarEstadoOriginalAgente(): void {
    const Snapshot = {
      descripcionNegocio: (this.empresa.descripcionNegocio || '').trim(),
      prefijoTelefono: this.prefijoTelefono,
      telefonoLocal: this.telefonoLocal.trim(),
      nombre: this.camposRequeridos.nombre,
      numero: this.camposRequeridos.numero,
      correo: this.camposRequeridos.correo,
      activarEnvioMenu: !!this.empresa.activarEnvioMenu,
      envioMenuInmediato: !!this.empresa.envioMenuInmediato,
      urlMenuImagen: (this.empresa.urlMenuImagen || '').trim()
    };
    this.estadoOriginalAgente = JSON.stringify(Snapshot);
  }

  hayCambiosEnAgente(): boolean {
    if (!this.estadoOriginalAgente) return false;
    const SnapshotActual = {
      descripcionNegocio: (this.empresa.descripcionNegocio || '').trim(),
      prefijoTelefono: this.prefijoTelefono,
      telefonoLocal: this.telefonoLocal.trim(),
      nombre: this.camposRequeridos.nombre,
      numero: this.camposRequeridos.numero,
      correo: this.camposRequeridos.correo,
      activarEnvioMenu: !!this.empresa.activarEnvioMenu,
      envioMenuInmediato: !!this.empresa.envioMenuInmediato,
      urlMenuImagen: (this.empresa.urlMenuImagen || '').trim()
    };
    return JSON.stringify(SnapshotActual) !== this.estadoOriginalAgente;
  }

  // Cancelación de cuenta
  mostrarModalDeleteAccount = false;
  confirmDeleteInput = '';

  // ESTADO MODAL GLOBAL (Reemplazo Alert/Confirm)

  modalNotificacion = {
    visible: false,
    titulo: '',
    mensaje: '',
    tipo: 'info', // 'info', 'error', 'confirm'
    onConfirm: null as Function | null
  };

  mostrarAlerta(titulo: string, mensaje: string, tipo: 'info' | 'error' = 'info'): void {
    this.modalNotificacion = { visible: true, titulo, mensaje, tipo, onConfirm: null };
  }

  mostrarConfirmacion(titulo: string, mensaje: string, onConfirm: Function): void {
    this.modalNotificacion = { visible: true, titulo, mensaje, tipo: 'confirm', onConfirm };
  }

  cerrarNotificacion(): void {
    this.modalNotificacion.visible = false;
    this.modalNotificacion.onConfirm = null;
  }

  aceptarNotificacion(): void {
    if (this.modalNotificacion.onConfirm) {
      this.modalNotificacion.onConfirm();
    }
    this.cerrarNotificacion();
  }

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
        this.aplicarFiltrosCitas();
        
        setTimeout(() => {
          this.isLoading = false;
        }, 600); 
      },
      error: (err) => {
        setTimeout(() => {
          this.isLoading = false;
        }, 600);
        this.errorMessage = 'No se pudieron cargar las citas. Verifica tu sesión.';
        console.error(err);
      }
    });
  }

  aplicarFiltrosCitas(): void {
    const hoy = new Date();
    const hoyStr = hoy.toISOString().split('T')[0];
    
    const manana = new Date(hoy);
    manana.setDate(manana.getDate() + 1);
    const mananaStr = manana.toISOString().split('T')[0];

    this.citasFiltradas = this.citas.filter(cita => {
      // 1. Filtro de Estado
      if (this.filtroEstado !== 'TODOS' && cita.estado !== this.filtroEstado) {
        return false;
      }

      // Extraer fecha en YYYY-MM-DD
      const fechaCitaStr = cita.fechaHoraInicio ? cita.fechaHoraInicio.split('T')[0] : '';

      // 2. Filtro de Fecha
      if (this.filtroFechaTipo === 'HOY_Y_MANANA') {
        return fechaCitaStr === hoyStr || fechaCitaStr === mananaStr;
      } else if (this.filtroFechaTipo === 'ESPECIFICO') {
        if (!this.fechaSeleccionada) return true;
        return fechaCitaStr === this.fechaSeleccionada;
      }

      return true; // TODAS
    });
  }

  calcularEstadisticas(): void {
    this.totalCitas = this.citas.length;
    this.citasConfirmadas = this.citas.filter(c => c.estado === 'CONFIRMADA').length;
    this.citasCanceladas = this.citas.filter(c => c.estado === 'CANCELADA').length;
  }

  cancelarCita(idCita: string): void {
    this.mostrarConfirmacion(
      'Cancelar Cita',
      '¿Estás seguro de que deseas cancelar esta cita?',
      () => {
        this.dashboardService.cancelarCita(idCita).subscribe({
          next: () => {
            this.cargarCitas();
          },
          error: (err) => {
            this.mostrarAlerta('Error', 'Error al cancelar la cita. Inténtalo de nuevo.', 'error');
            console.error(err);
          }
        });
      }
    );
  }

  // --- GESTIÓN DE EMPRESA ---
  cargarDatosEmpresa(): void {
    this.dashboardService.getEmpresa().subscribe({
      next: (data) => {
        this.empresa = data;
        this.descripcionNegocioOriginal = data.descripcionNegocio || '';
        this.extraerPrefijoYNumero();
        // Cargar campos requeridos del backend (con fallbacks si no existen)
        this.camposRequeridos.nombre = data.requiereNombre !== undefined ? data.requiereNombre : true;
        this.camposRequeridos.numero = data.requiereTelefono !== undefined ? data.requiereTelefono : true;
        this.camposRequeridos.correo = data.requiereCorreo !== undefined ? data.requiereCorreo : false;
        
        this.empresa.activarEnvioMenu = data.activarEnvioMenu !== undefined ? data.activarEnvioMenu : false;
        this.empresa.envioMenuInmediato = data.envioMenuInmediato !== undefined ? data.envioMenuInmediato : false;
        this.empresa.urlMenuImagen = data.urlMenuImagen || '';

        this.actualizarEstadoOriginalAgente();
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
    this.mostrarConfirmacion(
      'Desvincular Google Calendar',
      '¿Estás seguro de que deseas desvincular tu cuenta de Google Calendar? Tus citas ya no se sincronizarán.',
      () => {
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
    );
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

  guardarDatosEmpresaDirecto(): void {
    this.ejecutarGuardarEmpresa();
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
    
    // Inyectar campos requeridos y de menú en el payload enviado al backend
    this.empresa.requiereNombre = this.camposRequeridos.nombre;
    this.empresa.requiereTelefono = this.camposRequeridos.numero;
    this.empresa.requiereCorreo = this.camposRequeridos.correo;

    this.dashboardService.updateEmpresa(this.empresa).subscribe({
      next: (data) => {
        this.empresa = data;
        this.descripcionNegocioOriginal = data.descripcionNegocio || '';
        this.extraerPrefijoYNumero();
        
        // Volver a cargar el estado mapeado
        this.camposRequeridos.nombre = data.requiereNombre !== undefined ? data.requiereNombre : true;
        this.camposRequeridos.numero = data.requiereTelefono !== undefined ? data.requiereTelefono : true;
        this.camposRequeridos.correo = data.requiereCorreo !== undefined ? data.requiereCorreo : false;

        this.empresa.activarEnvioMenu = data.activarEnvioMenu !== undefined ? data.activarEnvioMenu : false;
        this.empresa.envioMenuInmediato = data.envioMenuInmediato !== undefined ? data.envioMenuInmediato : false;
        this.empresa.urlMenuImagen = data.urlMenuImagen || '';

        this.actualizarEstadoOriginalAgente();
        this.isLoading = false;
        this.mostrarAlerta('Configuración Guardada', 'Los cambios en la configuración de tu Agente Virtual se guardaron correctamente.');
        this.cargarEstadisticasSuscripcion();
      },
      error: (err) => {
        this.isLoading = false;
        this.mostrarAlerta('Error al Guardar', 'Ocurrió un problema al guardar la información de la empresa.', 'error');
        console.error(err);
      }
    });
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'Por favor selecciona un archivo de imagen válido (PNG, JPG, WEBP).';
        return;
      }
      this.selectedMenuFile = file;
      this.subirImagenMenu();
    }
  }

  subirImagenMenu(): void {
    if (!this.selectedMenuFile) return;
    this.isUploadingMenu = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.dashboardService.uploadMenuImage(this.selectedMenuFile).subscribe({
      next: (res) => {
        this.isUploadingMenu = false;
        this.empresa.urlMenuImagen = res.urlMenuImagen;
        this.empresa.activarEnvioMenu = true;
        this.selectedMenuFile = null;
        this.successMessage = '¡Imagen de menú / catálogo cargada con éxito!';
      },
      error: (err) => {
        this.isUploadingMenu = false;
        this.errorMessage = err.error?.error || 'Error al subir la imagen del menú. Inténtalo de nuevo.';
        console.error(err);
      }
    });
  }

  obtenerListaImagenesMenu(): String[] {
    if (!this.empresa || !this.empresa.urlMenuImagen) return [];
    return this.empresa.urlMenuImagen.split(',').map((url: string) => url.trim()).filter((url: string) => url.length > 0);
  }

  eliminarImagenMenuEspecifica(index: number): void {
    const imagenes = this.obtenerListaImagenesMenu();
    if (index >= 0 && index < imagenes.length) {
      imagenes.splice(index, 1);
      this.empresa.urlMenuImagen = imagenes.join(',');
      this.guardarDatosEmpresaDirecto();
    }
  }

  eliminarImagenMenu(): void {
    this.empresa.urlMenuImagen = '';
    this.guardarDatosEmpresaDirecto();
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
      this.mostrarAlerta('Datos incompletos', 'Por favor llena los campos obligatorios con valores correctos.', 'error');
      return;
    }

    if (this.formServicio.promocionActiva && this.formServicio.tipoPromocion === 'PERSONALIZADA') {
      this.mostrarConfirmacion(
        'Promoción Personalizada',
        'Estás activando una promoción personalizada.\n\nPor favor, asegúrate de verificar que el texto (prompt) de la promoción sea correcto, profesional y no altere de forma negativa el comportamiento del agente.\n\n¿Deseas continuar?',
        () => {
          this.ejecutarGuardarServicio();
        }
      );
      return;
    }
    
    this.ejecutarGuardarServicio();
  }

  // Nueva subfunción complementaria:
  ejecutarGuardarServicio(): void {
    this.isLoading = true;
    if (this.editandoServicio) {
      this.dashboardService.updateServicio(this.formServicio.id, this.formServicio).subscribe({
        next: () => {
          this.cerrarModalServicio();
          this.cargarServicios();
        },
        error: (err) => {
          this.isLoading = false;
          this.mostrarAlerta('Error', 'Error al actualizar el servicio.', 'error');
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
          this.mostrarAlerta('Error', 'Error al crear el servicio.', 'error');
          console.error(err);
        }
      });
    }
  }

  eliminarServicio(id: string): void {
    this.mostrarConfirmacion(
      'Eliminar Servicio',
      '¿Estás seguro de que deseas eliminar este servicio? Si tiene citas asociadas, se desactivará en su lugar.',
      () => {
        this.isLoading = true;
        this.dashboardService.eliminarServicio(id).subscribe({
          next: (res: any) => {
            if (res && res.softDeleted) {
              this.mostrarAlerta('Aviso', 'El servicio se desactivó porque tiene citas asociadas.', 'info');
            }
            this.cargarServicios();
          },
          error: (err) => {
            this.isLoading = false;
            this.mostrarAlerta('Error', 'Error al eliminar el servicio.', 'error');
            console.error(err);
          }
        });
      }
    );
  }

  toggleEstadoServicio(servicio: any): void {
    const estadoOriginal = servicio.activo;
    
    servicio.activo = !servicio.activo;
    this.isLoading = true;
    this.dashboardService.updateServicio(servicio.id, servicio).subscribe({
      next: () => {
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        servicio.activo = estadoOriginal;
        this.mostrarAlerta('Error', 'Error al actualizar el estado del servicio. Inténtalo de nuevo.', 'error');
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
          this.mostrarAlerta('Error', 'Error al iniciar pasarela de pagos.', 'error');
        }
      },
      error: (err) => {
        this.isLoading = false;
        const msg = err?.error?.error || 'Error al conectar con Stripe.';
        this.mostrarAlerta('Error de Facturación', msg, 'error');
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
          this.mostrarAlerta('Error', 'Error al redirigir al portal de facturación.', 'error');
        }
      },
      error: (err) => {
        this.isLoading = false;
        const msg = err?.error?.error || 'Error al conectar con Stripe.';
        this.mostrarAlerta('Error de Facturación', msg, 'error');
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

  abrirModalDeleteAccount(): void {
    this.confirmDeleteInput = '';
    this.mostrarModalDeleteAccount = true;
  }

  cerrarModalDeleteAccount(): void {
    this.mostrarModalDeleteAccount = false;
    this.confirmDeleteInput = '';
  }

  confirmarDeleteAccount(): void {
    if (this.confirmDeleteInput !== 'ELIMINAR') return;
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.dashboardService.deleteAccount().subscribe({
      next: (res) => {
        this.isLoading = false;
        this.mostrarModalDeleteAccount = false;
        // Cerrar sesión y redirigir
        this.authService.logout();
        alert('Tu cuenta ha sido eliminada permanentemente. Lamentamos verte partir.');
        this.router.navigate(['/register']);
      },
      error: (err) => {
        this.isLoading = false;
        this.mostrarModalDeleteAccount = false;
        this.errorMessage = err?.error?.error || 'Ocurrió un error al intentar eliminar la cuenta.';
        console.error('Error al eliminar cuenta:', err);
      }
    });
  }

  // --- SALIDA ---
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
import { Component, inject, ElementRef, ViewChild, AfterViewInit, OnDestroy } from '@angular/core';
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
export class LoginComponent implements AfterViewInit, OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);

  @ViewChild('particleCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  private animationFrameId!: number;
  private resizeCtx!: () => void;

  email = '';
  password = '';
  errorMessage = '';
  isLoading = false;
  passwordVisible = false;

  togglePasswordVisibility(): void {
    this.passwordVisible = !this.passwordVisible;
  }

  ngAfterViewInit(): void {
    this.initParticleNetwork();
  }

  ngOnDestroy(): void {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
    }
    window.removeEventListener('resize', this.resizeCtx);
  }

  private initParticleNetwork(): void {
    const canvas = this.canvasRef.nativeElement;
    const ctx = canvas.getContext('2d')!;
    
    const particles: Array<{
      x: number;
      y: number;
      vx: number;
      vy: number;
      radius: number;
    }> = [];
    
    const maxParticles = 65; // Densidad ideal de nodos
    const connectionDistance = 110; // Distancia máxima para enlazar líneas

    const resize = () => {
      const rect = canvas.parentElement?.getBoundingClientRect();
      canvas.width = rect?.width || window.innerWidth;
      canvas.height = rect?.height || window.innerHeight;
    };
    
    this.resizeCtx = resize;
    window.addEventListener('resize', resize);
    resize();

    // Inicializar partículas con vectores de movimiento aleatorios
    for (let i = 0; i < maxParticles; i++) {
      particles.push({
        x: Math.random() * canvas.width,
        y: Math.random() * canvas.height,
        vx: (Math.random() - 0.5) * 0.6,
        vy: (Math.random() - 0.5) * 0.6,
        radius: Math.random() * 2 + 1
      });
    }

    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      
      // Actualizar posiciones y dibujar nodos
      // Actualizar posiciones y dibujar nodos
        for (let i = 0; i < maxParticles; i++) {
          const p = particles[i];
          p.x += p.vx;
          p.y += p.vy;

          if (p.x < 0) { 
            p.x = 0; 
            p.vx *= -1; 
          } else if (p.x > canvas.width) { 
            p.x = canvas.width; 
            p.vx *= -1; 
          }

          if (p.y < 0) { 
            p.y = 0; 
            p.vy *= -1; 
          } else if (p.y > canvas.height) { 
            p.y = canvas.height; 
            p.vy *= -1; 
          }

          ctx.beginPath();

        // Rebote en bordes del contenedor
        if (p.x < 0 || p.x > canvas.width) p.vx *= -1;
        if (p.y < 0 || p.y > canvas.height) p.vy *= -1;

        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(71, 121, 34, 0.6)'; // Tu verde claro
        ctx.fill();

        // Calcular conexiones entre nodos cercanos (Red Neuronal)
        for (let j = i + 1; j < maxParticles; j++) {
          const p2 = particles[j];
          const dx = p.x - p2.x;
          const dy = p.y - p2.y;
          const dist = Math.sqrt(dx * dx + dy * dy);

          if (dist < connectionDistance) {
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(p2.x, p2.y);
            // La opacidad depende de la cercanía del enlace
            const alpha = (1 - dist / connectionDistance) * 0.18;
            ctx.strokeStyle = `rgba(116, 153, 198, ${alpha})`; // Tu azul complementario
            ctx.lineWidth = 0.8;
            ctx.stroke();
          }
        }
      }
      
      this.animationFrameId = requestAnimationFrame(draw);
    };

    draw();
  }

  // --- LAS FUNCIONES EXISTENTES DE AUTENTICACIÓN SE MANTIENEN COMPLETAMENTE IGUALES ---
  onSubmit(): void {
    if (!this.email.trim() || !this.password.trim()) {
      this.errorMessage = 'Por favor, completa todos los campos.';
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.login(this.email, this.password).subscribe({
      next: () => { this.router.navigate(['/dashboard']); },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.status === 401 ? 'Correo o contraseña incorrectos.' : 'Ocurrió un error al iniciar sesión. Inténtalo de nuevo.';
      }
    });
  }

  // Métodos de recuperación de contraseña funcionales (conservados al 100%)
  mostrarModalRecuperar = false;
  recuperarPaso: 'solicitar' | 'restablecer' = 'solicitar';
  recuperarEmail = ''; recuperarCodigo = ''; recuperarNuevaPass = '';
  mensajeRecuperar = ''; errorRecuperar = ''; cargandoRecuperar = false;

  abrirModalRecuperar(): void {
    this.mostrarModalRecuperar = true; this.recuperarPaso = 'solicitar';
    this.recuperarEmail = ''; this.recuperarCodigo = ''; this.recuperarNuevaPass = '';
    this.mensajeRecuperar = ''; this.errorRecuperar = '';
  }
  cerrarModalRecuperar(): void { this.mostrarModalRecuperar = false; }
  enviarCodigo(): void {
    if (!this.recuperarEmail.trim()) { this.errorRecuperar = 'Por favor ingresa tu correo electrónico.'; return; }
    this.cargandoRecuperar = true; this.errorRecuperar = ''; this.mensajeRecuperar = '';
    this.authService.solicitarRecuperacion(this.recuperarEmail.trim()).subscribe({
      next: (res) => { this.cargandoRecuperar = false; this.mensajeRecuperar = res.message || 'Código enviado con éxito.'; this.recuperarPaso = 'restablecer'; },
      error: (err) => { this.cargandoRecuperar = false; this.errorRecuperar = err.error?.error || 'Error al enviar el código de recuperación.'; }
    });
  }
  confirmarRestablecimiento(): void {
    if (!this.recuperarCodigo.trim() || !this.recuperarNuevaPass.trim()) { this.errorRecuperar = 'Por favor ingresa el código y tu nueva contraseña.'; return; }
    this.cargandoRecuperar = true; this.errorRecuperar = ''; this.mensajeRecuperar = '';
    this.authService.restablecerContrasena({ email: this.recuperarEmail.trim(), code: this.recuperarCodigo.trim(), newPassword: this.recuperarNuevaPass.trim() }).subscribe({
      next: () => { this.cargandoRecuperar = false; this.mensajeRecuperar = 'Contraseña restablecida con éxito. Redirigiendo...'; setTimeout(() => { this.cerrarModalRecuperar(); }, 2000); },
      error: (err) => { this.cargandoRecuperar = false; this.errorRecuperar = err.error?.error || 'Código incorrecto o expirado.'; }
    });
  }
}
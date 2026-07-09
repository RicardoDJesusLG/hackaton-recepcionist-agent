import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/v1/auth';

  login(email: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap(res => {
        if (res && res.token) {
          localStorage.setItem('token', res.token);
          localStorage.setItem('email', res.email);
          localStorage.setItem('empresaId', res.empresaId);
        }
      })
    );
  }

  register(payload: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/register`, payload);
  }

  verificarCredenciales(email: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, { email, password });
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('email');
    localStorage.removeItem('empresaId');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getEmail(): string | null {
    return localStorage.getItem('email');
  }

  getEmpresaId(): string | null {
    return localStorage.getItem('empresaId');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  solicitarRecuperacion(email: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/forgot-password`, { email });
  }

  restablecerContrasena(payload: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/reset-password`, payload);
  }
}

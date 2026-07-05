import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/v1/dashboard';
  private paymentsUrl = 'http://localhost:8080/api/v1/payments';

  getCitas(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/citas`);
  }

  cancelarCita(idCita: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/citas/cancelar`, { idCita });
  }

  getEmpresa(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/empresa`);
  }

  updateEmpresa(empresaData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/empresa`, empresaData);
  }

  getAgenda(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/empresa/agenda`);
  }

  updateAgenda(agendaData: any[]): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/empresa/agenda`, agendaData);
  }

  crearCheckoutSession(empresaId: string): Observable<any> {
    return this.http.post<any>(`${this.paymentsUrl}/create-checkout-session`, { empresaId });
  }

  getServicios(): Observable<any[]> {
    return this.http.get<any[]>(`http://localhost:8080/api/v1/servicios/admin`);
  }

  crearServicio(servicio: any): Observable<any> {
    return this.http.post<any>(`http://localhost:8080/api/v1/servicios`, servicio);
  }

  updateServicio(id: string, servicio: any): Observable<any> {
    return this.http.put<any>(`http://localhost:8080/api/v1/servicios/${id}`, servicio);
  }

  eliminarServicio(id: string): Observable<any> {
    return this.http.delete<any>(`http://localhost:8080/api/v1/servicios/${id}`);
  }
}

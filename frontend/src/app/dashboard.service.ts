import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/dashboard`;
  private stripeUrl = `${environment.apiUrl}/stripe`;
  private serviciosUrl = `${environment.apiUrl}/servicios`;

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

  crearCheckoutSession(idNegocio: string, plan: string): Observable<any> {
    return this.http.post<any>(`${this.stripeUrl}/checkout`, { idNegocio, plan });
  }

  getSubscriptionStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/suscripcion/stats`);
  }

  getServicios(): Observable<any[]> {
    return this.http.get<any[]>(`${this.serviciosUrl}/admin`);
  }

  crearServicio(servicio: any): Observable<any> {
    return this.http.post<any>(`${this.serviciosUrl}`, servicio);
  }

  updateServicio(id: string, servicio: any): Observable<any> {
    return this.http.put<any>(`${this.serviciosUrl}/${id}`, servicio);
  }

  eliminarServicio(id: string): Observable<any> {
    return this.http.delete<any>(`${this.serviciosUrl}/${id}`);
  }
}

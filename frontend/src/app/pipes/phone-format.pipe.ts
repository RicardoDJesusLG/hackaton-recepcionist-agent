import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'phoneFormat',
  standalone: true
})
export class PhoneFormatPipe implements PipeTransform {

  transform(value: string | number): string {
    if (!value) return '';

    // Convertimos a string y eliminamos cualquier caracter que no sea número
    let cleaned = value.toString().replace(/\D/g, '');

    // Caso 1: Número de 12 dígitos (Ej. con código de país 52 incluido: 525555555555)
    if (cleaned.length === 12) {
      const country = cleaned.slice(0, 2);
      const lada = cleaned.slice(2, 4);
      const part1 = cleaned.slice(4, 8);
      const part2 = cleaned.slice(8, 12);
      return `+${country} ${lada} ${part1} ${part2}`;
    }

    // Caso 2: Número de 10 dígitos (Sin código de país, asumiendo +52 por defecto)
    if (cleaned.length === 10) {
      const lada = cleaned.slice(0, 2);
      const part1 = cleaned.slice(2, 6);
      const part2 = cleaned.slice(6, 10);
      return `+52 ${lada} ${part1} ${part2}`;
    }

    // Caso 3: Número de 13 dígitos (A veces WhatsApp añade un '1' extra: 5215555555555)
    if (cleaned.length === 13 && cleaned.startsWith('521')) {
      const country = cleaned.slice(0, 2); // Ignoramos el '1' intermedio
      const lada = cleaned.slice(3, 5);
      const part1 = cleaned.slice(5, 9);
      const part2 = cleaned.slice(9, 13);
      return `+${country} ${lada} ${part1} ${part2}`;
    }

    // Fallback: Si no coincide con las longitudes esperadas, se devuelve el original
    return value.toString();
  }
}
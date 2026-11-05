export interface Direccion {
  id: number
  calle: string
  numero: string
  colonia: string
  ciudad: string
  codigoPostal: string
}

export interface DireccionRequest {
  calle: string
  numero: string
  colonia: string
  ciudad: string
  codigoPostal: string
}
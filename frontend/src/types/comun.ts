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

// Dirección de la obra (DatosDireccion): además del estado, lleva referencias para llegar
export interface DatosDireccion {
  id: number
  calle: string
  numero: string
  colonia: string
  ciudad: string
  estado: string
  codigoPostal: string
  referencias: string | null
}

export interface DatosDireccionRequest {
  calle: string
  numero: string
  colonia: string
  ciudad: string
  estado: string
  codigoPostal: string
  referencias: string | null
}

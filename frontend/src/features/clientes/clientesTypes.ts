import type { Direccion } from '../../types/comun'

// Equivale a RazonSocialResponse
export interface RazonSocial {
  id: number
  nombre: string
  rfc: string
  direccionFiscal: Direccion
}

// Equivale a ClienteResponse
export interface Cliente {
  id: number
  nombre: string
  apellidoPaterno: string
  apellidoMaterno: string | null
  empresa: string
  telefono: string
  correo: string
  razonesSociales: RazonSocial[]
}
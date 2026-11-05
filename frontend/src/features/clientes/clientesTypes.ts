import type { Direccion, DireccionRequest} from '../../types/comun'

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

// Equivale a RazonSocialRequest
export interface RazonSocialRequest {
  nombre: string
  rfc: string
  direccionFiscal: DireccionRequest
}

// Equivale a ClienteRequest
export interface ClienteRequest {
  nombre: string
  apellidoPaterno: string
  apellidoMaterno: string | null
  empresa: string | null
  telefono: string
  correo: string
  razonesSociales: RazonSocialRequest[]
}
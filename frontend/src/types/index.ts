export interface Medicine {
  id?: string
  name: string
  imageUrl?: string
  dosageForm: DosageForm
  storageLocation?: string
  totalQuantity: number
  unit: string
  threshold: number
  productionDate?: string
  expiryDate: string
  lastPurchaseDate?: string
  symptomsTreated?: string
  dosageInstruction?: string
  sideEffects?: string
  usageFrequencyScore: number
  status: MedicineStatus
  createdAt?: string
  updatedAt?: string
}

export enum DosageForm {
  TABLET = 'TABLET',
  CAPSULE = 'CAPSULE',
  GRANULE = 'GRANULE',
  LIQUID = 'LIQUID',
  EXTERNAL = 'EXTERNAL',
  INJECTION = 'INJECTION',
  OTHER = 'OTHER'
}

export enum MedicineStatus {
  NORMAL = 'NORMAL',
  LOW_STOCK = 'LOW_STOCK',
  EXPIRED = 'EXPIRED',
  DEPLETED = 'DEPLETED'
}

export interface ShoppingListItem {
  id?: string
  medicineId: string
  medicineName?: string
  reason: ShoppingReason
  customNote?: string
  status: ShoppingStatus
  addedAt?: string
  purchasedAt?: string
}

export enum ShoppingReason {
  EXPIRED = 'EXPIRED',
  DEPLETED = 'DEPLETED',
  MANUAL = 'MANUAL'
}

export enum ShoppingStatus {
  PENDING = 'PENDING',
  PURCHASED = 'PURCHASED'
}

export interface Category {
  id?: string
  name: string
  description?: string
  icon?: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

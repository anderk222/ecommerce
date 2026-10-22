/**
 * The models of the frontend.
 *
 * <p>They mirror the DTOs of the API (section 12.6 of the use-case document).
 * They are plain interfaces: no class, no behaviour, no framework.
 */

/** The price of an item: what it costs, and what the customer pays. */
export interface Price {
  basePrice: number;
  finalPrice: number;
  discountPercentage: number;
  currency: string;
}

/** One row of the specification table of an item. */
export interface Specification {
  name: string;
  value: string;
}

/** An item of the catalog, in a listing. */
export interface CatalogItemSummary {
  id: number;
  slug: string;
  name: string;
  type: 'PRODUCT' | 'SERVICE';
  price: Price;
  image: string | null;
  featured: boolean;
  available: boolean;
  stock: number | null;
  categories: string[];
}

/** An item of the catalog, in its detail page. */
export interface CatalogItemDetail {
  id: number;
  slug: string;
  name: string;
  type: 'PRODUCT' | 'SERVICE';
  description: string;
  price: Price;
  images: string[];
  specifications: Specification[];
  categories: string[];
  featured: boolean;
  available: boolean;
  stock: number | null;
  stockState: 'NOT_TRACKED' | 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
}

/** The content of the home page. */
export interface HomeContent {
  /** The items the shop wants to sell: the ones of the carousel. */
  featured: CatalogItemSummary[];
  /** The most recent published services. */
  services: CatalogItemSummary[];
  /** The most recent published products. */
  products: CatalogItemSummary[];
  bestSelling: CatalogItemSummary[];
  onSale: CatalogItemSummary[];
}

/** A category, with the number of published items it holds. */
export interface Category {
  id: number;
  name: string;
  slug: string;
  itemCount: number;
}

/** The envelope of every list of the API. */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** The criteria of a listing. */
export interface CatalogSearchCriteria {
  type?: 'PRODUCT' | 'SERVICE';
  q?: string;
  category?: string;
  sort?: 'newest' | 'oldest' | 'price_asc' | 'price_desc' | 'name_asc' | 'name_desc' | 'best_selling';
  onSale?: boolean;
  lowStock?: boolean;
  page?: number;
  size?: number;
}

/** The error envelope of the API. */
export interface ApiError {
  code: string;
  message: string;
  details: { field: string; issue: string }[];
  timestamp: string;
}

export const TYPES = [
  { value: '', label: 'Todos' },
  { value: 'PRODUCT', label: 'Productos' },
  { value: 'SERVICE', label: 'Servicios' },
];

export const STATES = [
  { value: 'ALL', label: 'Todos' },
  { value: 'ACTIVE', label: 'Activos' },
  { value: 'INACTIVE', label: 'Inactivos' },
  { value: 'DELETED', label: 'Eliminados' },
];
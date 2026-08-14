/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Origin of the backend. Absent in development, where the default applies. */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
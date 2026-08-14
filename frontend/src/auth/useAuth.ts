import { useContext } from 'react'

import { AuthContext, type AuthValue } from './AuthContext.ts'

export function useAuth(): AuthValue {
  const value = useContext(AuthContext)

  if (value === null) {
    // A default object would let a component render a signed-out view forever
    // and never say why. Failing here names the mistake at the moment it is made.
    throw new Error('useAuth must be called inside an AuthProvider')
  }

  return value
}
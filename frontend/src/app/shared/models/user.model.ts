export interface User {
  id?: string; // L'ID est généralement fourni par le backend, donc optionnel côté client au début.
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  password?: string; // Le mot de passe est souvent requis uniquement pour la création/mise à jour.
}

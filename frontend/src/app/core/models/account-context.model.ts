export type AccountType = 'ORGANIZATION' | 'COLLABORATOR' | null;

export interface AccountContext {
  accountType: AccountType;
  organizationEmail: string | null;
}


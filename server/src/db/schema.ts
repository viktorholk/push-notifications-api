import { Generated } from 'kysely';

export interface NotificationTable {
    id: string;
    title: string;
    message: string | null;
    url: string | null;
    icon: string | null;
    color: string | null;
    token: string | null;
    createdAt: string;
}

export interface RegistrationTable {
    token: string | null;
    createdAt: Generated<Date>;
}

export interface Database {
    notifications: NotificationTable;
    registrations: RegistrationTable;
}

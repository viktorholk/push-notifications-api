import { Request, Response } from "express";

export type ClientContext = { req: Request; res: Response };

export type PushNotification = {
    id: string;
    title: string;
    message?: string;
    url?: string,
    icon?: string,
    color?: string,
    createdAt: string
}
